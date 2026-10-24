package com.dabstw.morral.backpack;

import com.dabstw.morral.MorralPlugin;
import com.dabstw.morral.item.BackpackData;
import com.dabstw.morral.item.Items;
import com.dabstw.morral.item.PatchType;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BackpackManager {
    /** Player inventory slots that can hold a backpack: storage (0-35), armor (36-39) and off hand (40). */
    public static final int LAST_SLOT = 40;

    private static final Sound OPEN_SOUND = Sound.sound(org.bukkit.Sound.ITEM_ARMOR_EQUIP_LEATHER, Sound.Source.PLAYER, 1f, 1.1f);
    private static final Sound CLOSE_SOUND = Sound.sound(org.bukkit.Sound.ITEM_BUNDLE_INSERT, Sound.Source.PLAYER, 1f, 0.9f);
    private static final Sound DENY_SOUND = Sound.sound(org.bukkit.Sound.ENTITY_VILLAGER_NO, Sound.Source.PLAYER, 1f, 1f);

    private final MorralPlugin plugin;
    private final Items items;
    private final Map<UUID, BackpackHolder> sessions = new HashMap<>();
    private final Map<Material, ItemStack> compactCache = new EnumMap<>(Material.class);

    public BackpackManager(MorralPlugin plugin) {
        this.plugin = plugin;
        this.items = plugin.items();
    }

    public @Nullable BackpackHolder session(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public boolean isOpenBackpack(Player player, @Nullable ItemStack item) {
        BackpackHolder holder = session(player);
        return holder != null && holder.backpackId().equals(items.idOf(item));
    }

    public boolean isAllowedInside(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) return true;
        if (items.isBackpack(item)) return false;
        String name = item.getType().name();
        if (name.endsWith("SHULKER_BOX") && !plugin.getConfig().getBoolean("permitir-shulkers")) return false;
        return !name.endsWith("BUNDLE") || plugin.getConfig().getBoolean("permitir-bundles");
    }

    public void open(Player player, int slot) {
        if (!player.hasPermission("morral.use")) {
            plugin.messages().send(player, "sin-permiso");
            return;
        }
        if (session(player) != null) {
            player.closeInventory();
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack item = inventory.getItem(slot);
        BackpackData data = items.read(item);
        if (item == null || data == null) return;

        if (data.owner != null && !data.owner.equals(player.getUniqueId()) && !player.hasPermission("morral.bypass.candado")) {
            plugin.messages().send(player, "candado", Placeholder.unparsed("dueno", data.ownerName == null ? "?" : data.ownerName));
            player.playSound(DENY_SOUND);
            return;
        }
        if (data.id == null) {
            data.id = UUID.randomUUID();
        }

        int size = data.tier.size();
        ItemStack[] contents = data.contents();
        if (contents.length > size) {
            ItemStack[] overflow = Arrays.copyOfRange(contents, size, contents.length);
            contents = Arrays.copyOf(contents, size);
            data.setContents(contents);
            give(player, overflow);
            plugin.messages().send(player, "desbordado");
        }
        items.write(item, data);
        inventory.setItem(slot, item);

        BackpackHolder holder = new BackpackHolder(player.getUniqueId(), data.id, data.tier);
        Inventory gui = Bukkit.createInventory(holder, size, title(item));
        holder.setInventory(gui);
        gui.setContents(contents);

        sessions.put(player.getUniqueId(), holder);
        player.openInventory(gui);
        if (player.getOpenInventory().getTopInventory() != gui) {
            sessions.remove(player.getUniqueId());
            return;
        }
        player.playSound(OPEN_SOUND);
    }

    private static Component title(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() ? meta.displayName() : meta.itemName();
    }

    /** Persists an open GUI back into its backpack item. Called when the GUI closes. */
    public void handleClose(Player player, BackpackHolder holder) {
        if (sessions.get(player.getUniqueId()) != holder) return;
        sessions.remove(player.getUniqueId());

        ItemStack[] contents = holder.getInventory().getContents();
        PlayerInventory inventory = player.getInventory();
        int slot = findSlot(inventory, holder.backpackId());
        if (slot < 0) {
            plugin.getLogger().warning("No se encontró la mochila " + holder.backpackId() + " de " + player.getName()
                    + "; se sueltan sus objetos en el suelo.");
            drop(player.getLocation(), contents);
            return;
        }
        ItemStack item = inventory.getItem(slot);
        BackpackData data = items.read(item);
        if (item == null || data == null) return;
        if (data.has(PatchType.COMPACTADOR)) {
            contents = compact(contents, player.getWorld());
        }
        data.setContents(contents);
        items.write(item, data);
        inventory.setItem(slot, item);
        player.playSound(CLOSE_SOUND);
    }

    /** On death the inventory is already turned into drops, so write the open GUI into the matching drop. */
    public void flushInto(Player player, Collection<ItemStack> candidates) {
        BackpackHolder holder = sessions.remove(player.getUniqueId());
        if (holder == null) return;
        ItemStack[] contents = holder.getInventory().getContents();
        holder.getInventory().clear();
        for (ItemStack candidate : candidates) {
            if (holder.backpackId().equals(items.idOf(candidate))) {
                BackpackData data = items.read(candidate);
                if (data == null) continue;
                data.setContents(contents);
                items.write(candidate, data);
                return;
            }
        }
        drop(player.getLocation(), contents);
    }

    public void closeAll() {
        for (BackpackHolder holder : List.copyOf(sessions.values())) {
            Player player = Bukkit.getPlayer(holder.viewer());
            if (player == null) {
                sessions.remove(holder.viewer());
                continue;
            }
            handleClose(player, holder);
            player.closeInventory();
        }
    }

    public int findSlot(PlayerInventory inventory, UUID id) {
        for (int slot = 0; slot <= LAST_SLOT; slot++) {
            if (id.equals(items.idOf(inventory.getItem(slot)))) return slot;
        }
        return -1;
    }

    // ---------------------------------------------------------------- content helpers

    /** Inserts {@code amount} copies of {@code proto} into {@code contents}, returns what did not fit. */
    public static int insert(ItemStack[] contents, ItemStack proto, int amount) {
        int max = proto.getMaxStackSize();
        for (int i = 0; i < contents.length && amount > 0; i++) {
            ItemStack slot = contents[i];
            if (slot != null && !slot.isEmpty() && slot.isSimilar(proto) && slot.getAmount() < max) {
                int moved = Math.min(amount, max - slot.getAmount());
                slot.setAmount(slot.getAmount() + moved);
                amount -= moved;
            }
        }
        for (int i = 0; i < contents.length && amount > 0; i++) {
            if (contents[i] == null || contents[i].isEmpty()) {
                int moved = Math.min(amount, max);
                contents[i] = proto.asQuantity(moved);
                amount -= moved;
            }
        }
        return amount;
    }

    /** Removes up to {@code amount} items similar to {@code proto}, returns how many were removed. */
    public static int take(ItemStack[] contents, ItemStack proto, int amount) {
        int taken = 0;
        for (int i = contents.length - 1; i >= 0 && taken < amount; i--) {
            ItemStack slot = contents[i];
            if (slot == null || slot.isEmpty() || !slot.isSimilar(proto)) continue;
            int moved = Math.min(amount - taken, slot.getAmount());
            slot.setAmount(slot.getAmount() - moved);
            if (slot.getAmount() <= 0) contents[i] = ItemStack.empty();
            taken += moved;
        }
        return taken;
    }

    /** Turns every 9 plain items into their 3x3 crafting result (ingots -> blocks, wheat -> hay...). */
    public ItemStack[] compact(ItemStack[] contents, World world) {
        ItemStack[] result = cloneAll(contents);
        Set<Material> seen = EnumSet.noneOf(Material.class);
        List<String> excluded = plugin.getConfig().getStringList("compactador.excluir").stream()
                .map(s -> s.toUpperCase(Locale.ROOT)).toList();
        for (ItemStack item : contents) {
            if (item == null || item.isEmpty()) continue;
            Material material = item.getType();
            if (!seen.add(material) || excluded.contains(material.name())) continue;
            ItemStack plain = ItemStack.of(material);
            ItemStack output = compactResult(material, world);
            if (output.isEmpty()) continue;

            int total = 0;
            for (ItemStack slot : result) {
                if (slot != null && slot.isSimilar(plain)) total += slot.getAmount();
            }
            int crafts = total / 9;
            if (crafts == 0) continue;

            ItemStack[] snapshot = cloneAll(result);
            take(result, plain, crafts * 9);
            if (insert(result, output, crafts * output.getAmount()) > 0) {
                result = snapshot;
            }
        }
        return result;
    }

    private ItemStack compactResult(Material material, World world) {
        return compactCache.computeIfAbsent(material, m -> {
            ItemStack[] matrix = new ItemStack[9];
            Arrays.fill(matrix, ItemStack.of(m));
            Recipe recipe = Bukkit.getCraftingRecipe(matrix, world);
            if (recipe == null) return ItemStack.empty();
            ItemStack output = recipe.getResult();
            if (output.isEmpty() || output.getType() == m || items.isOurs(output)) return ItemStack.empty();
            return output.clone();
        });
    }

    private static ItemStack[] cloneAll(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            copy[i] = contents[i] == null ? ItemStack.empty() : contents[i].clone();
        }
        return copy;
    }

    public static void give(Player player, ItemStack... stacks) {
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            player.getInventory().addItem(stack).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }

    private static void drop(Location location, ItemStack[] contents) {
        for (ItemStack stack : contents) {
            if (stack != null && !stack.isEmpty()) location.getWorld().dropItemNaturally(location, stack);
        }
    }
}
