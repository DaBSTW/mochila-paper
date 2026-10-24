package com.dabstw.morral.listener;

import com.dabstw.morral.MorralPlugin;
import com.dabstw.morral.backpack.BackpackHolder;
import com.dabstw.morral.backpack.BackpackManager;
import com.dabstw.morral.item.BackpackData;
import com.dabstw.morral.item.Items;
import com.dabstw.morral.item.PatchType;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public final class InventoryListener implements Listener {
    /** Stations where paper-based items could be consumed or traded away. */
    private static final Set<InventoryType> FORBIDDEN = EnumSet.of(
            InventoryType.MERCHANT, InventoryType.CARTOGRAPHY, InventoryType.LOOM, InventoryType.GRINDSTONE,
            InventoryType.STONECUTTER, InventoryType.ENCHANTING, InventoryType.BREWING, InventoryType.FURNACE,
            InventoryType.BLAST_FURNACE, InventoryType.SMOKER, InventoryType.BEACON, InventoryType.SMITHING,
            InventoryType.CRAFTER);

    private static final Sound SEW_SOUND = Sound.sound(org.bukkit.Sound.BLOCK_WOOL_PLACE, Sound.Source.PLAYER, 1f, 1.4f);
    private static final Sound UNSEW_SOUND = Sound.sound(org.bukkit.Sound.ENTITY_SHEEP_SHEAR, Sound.Source.PLAYER, 1f, 1.2f);
    private static final Sound DENY_SOUND = Sound.sound(org.bukkit.Sound.ENTITY_VILLAGER_NO, Sound.Source.PLAYER, 1f, 1f);

    private final MorralPlugin plugin;
    private final Items items;
    private final BackpackManager manager;

    public InventoryListener(MorralPlugin plugin) {
        this.plugin = plugin;
        this.items = plugin.items();
        this.manager = plugin.manager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        Inventory clicked = event.getClickedInventory();
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        ItemStack hotbar = event.getClick() == ClickType.NUMBER_KEY
                ? player.getInventory().getItem(event.getHotbarButton()) : null;
        ItemStack offhand = event.getClick() == ClickType.SWAP_OFFHAND
                ? player.getInventory().getItemInOffHand() : null;
        boolean intoTop = clicked != null && clicked.equals(top);
        boolean shiftFromBottom = clicked != null && !intoTop && event.isShiftClick();

        if (top.getHolder(false) instanceof BackpackHolder) {
            if (manager.isOpenBackpack(player, current) || manager.isOpenBackpack(player, cursor)
                    || manager.isOpenBackpack(player, hotbar) || manager.isOpenBackpack(player, offhand)) {
                event.setCancelled(true);
                return;
            }
            if ((intoTop && (!manager.isAllowedInside(cursor) || !manager.isAllowedInside(hotbar)
                    || !manager.isAllowedInside(offhand)))
                    || (shiftFromBottom && !manager.isAllowedInside(current))) {
                event.setCancelled(true);
                plugin.messages().send(player, "objeto-prohibido");
                player.playSound(DENY_SOUND);
                return;
            }
        } else if (FORBIDDEN.contains(top.getType())) {
            if ((intoTop && (items.isOurs(cursor) || items.isOurs(hotbar) || items.isOurs(offhand)))
                    || (shiftFromBottom && items.isOurs(current))) {
                event.setCancelled(true);
                return;
            }
        }

        if (event instanceof InventoryCreativeEvent || clicked == null
                || !clicked.equals(player.getInventory()) || !items.isBackpack(current)) {
            return;
        }
        int slot = event.getSlot();
        PatchType patch = items.patchOf(cursor);
        if (patch != null && (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT)) {
            event.setCancelled(true);
            sew(player, event, slot, current, cursor, patch);
        } else if (cursor.getType() == Material.SHEARS && event.getClick() == ClickType.RIGHT) {
            event.setCancelled(true);
            unsew(player, clicked, slot, current);
        } else if (cursor.isEmpty() && event.getClick() == ClickType.RIGHT) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> manager.open(player, slot));
        }
    }

    private void sew(Player player, InventoryClickEvent event, int slot, ItemStack backpack, ItemStack cursor, PatchType patch) {
        BackpackData data = items.read(backpack);
        if (data == null) return;
        if (data.has(patch)) {
            plugin.messages().send(player, "parche-repetido");
            player.playSound(DENY_SOUND);
            return;
        }
        if (data.patches.size() >= data.tier.patchSlots()) {
            plugin.messages().send(player, "parche-lleno");
            player.playSound(DENY_SOUND);
            return;
        }
        if (patch == PatchType.CANDADO) {
            data.owner = player.getUniqueId();
            data.ownerName = player.getName();
        }
        data.patches.add(patch);
        items.write(backpack, data);
        player.getInventory().setItem(slot, backpack);
        cursor.setAmount(cursor.getAmount() - 1);
        event.getView().setCursor(cursor.getAmount() <= 0 ? ItemStack.empty() : cursor);
        player.playSound(SEW_SOUND);
        plugin.messages().send(player, "parche-cosido", Placeholder.unparsed("parche", patch.displayName()));
    }

    private void unsew(Player player, Inventory inventory, int slot, ItemStack backpack) {
        BackpackData data = items.read(backpack);
        if (data == null) return;
        if (data.patches.isEmpty()) {
            plugin.messages().send(player, "sin-parches");
            return;
        }
        PatchType patch = data.patches.getLast();
        if (patch == PatchType.CANDADO && data.owner != null && !data.owner.equals(player.getUniqueId())
                && !player.hasPermission("morral.bypass.candado")) {
            plugin.messages().send(player, "candado-ajeno");
            player.playSound(DENY_SOUND);
            return;
        }
        data.patches.removeLast();
        if (patch == PatchType.CANDADO) {
            data.owner = null;
            data.ownerName = null;
        }
        items.write(backpack, data);
        inventory.setItem(slot, backpack);
        player.playSound(UNSEW_SOUND);
        plugin.messages().send(player, "parche-descosido", Placeholder.unparsed("parche", patch.displayName()));
        plugin.getServer().getScheduler().runTask(plugin, () -> BackpackManager.give(player, items.createPatch(patch)));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        boolean touchesTop = event.getRawSlots().stream().anyMatch(raw -> raw < top.getSize());
        if (!touchesTop) return;
        ItemStack dragged = event.getOldCursor();
        if (top.getHolder(false) instanceof BackpackHolder) {
            if (!manager.isAllowedInside(dragged) || manager.isOpenBackpack(player, dragged)) {
                event.setCancelled(true);
            }
        } else if (FORBIDDEN.contains(top.getType()) && items.isOurs(dragged)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent event) {
        if (FORBIDDEN.contains(event.getDestination().getType()) && items.isOurs(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player
                && event.getInventory().getHolder(false) instanceof BackpackHolder holder) {
            manager.handleClose(player, holder);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (manager.isOpenBackpack(event.getPlayer(), event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        @Nullable BackpackHolder holder = manager.session(event.getPlayer());
        if (holder != null) manager.handleClose(event.getPlayer(), holder);
    }
}
