package com.dabstw.morral.item;

import com.dabstw.morral.MorralPlugin;
import io.papermc.paper.persistence.PersistentDataContainerView;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.DamageTypeTagKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/** Creates, reads and writes backpack and patch items. All state lives in the item's PersistentDataContainer. */
public final class Items {
    public static final Material BASE = Material.PAPER;
    private static final int PREVIEW_LINES = 4;

    private final NamespacedKey tierKey;
    private final NamespacedKey idKey;
    private final NamespacedKey contentsKey;
    private final NamespacedKey usedKey;
    private final NamespacedKey patchesKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey ownerNameKey;
    private final NamespacedKey colorKey;
    private final NamespacedKey patchKey;

    public Items(MorralPlugin plugin) {
        tierKey = new NamespacedKey(plugin, "tier");
        idKey = new NamespacedKey(plugin, "id");
        contentsKey = new NamespacedKey(plugin, "contents");
        usedKey = new NamespacedKey(plugin, "used");
        patchesKey = new NamespacedKey(plugin, "patches");
        ownerKey = new NamespacedKey(plugin, "owner");
        ownerNameKey = new NamespacedKey(plugin, "owner_name");
        colorKey = new NamespacedKey(plugin, "color");
        patchKey = new NamespacedKey(plugin, "patch");
    }

    // ---------------------------------------------------------------- backpacks

    public ItemStack createBackpack(Tier tier) {
        ItemStack item = ItemStack.of(BASE);
        write(item, new BackpackData(tier));
        return item;
    }

    public boolean isBackpack(@Nullable ItemStack item) {
        if (item == null || item.getType() != BASE) return false;
        return item.getPersistentDataContainer().has(tierKey, PersistentDataType.STRING);
    }

    public boolean isOurs(@Nullable ItemStack item) {
        return isBackpack(item) || patchOf(item) != null;
    }

    public @Nullable BackpackData read(@Nullable ItemStack item) {
        if (!isBackpack(item)) return null;
        PersistentDataContainerView pdc = item.getPersistentDataContainer();
        Tier tier = Tier.parse(pdc.get(tierKey, PersistentDataType.STRING));
        if (tier == null) return null;
        BackpackData data = new BackpackData(tier);
        data.id = parseUuid(pdc.get(idKey, PersistentDataType.STRING));
        data.contentsRaw = pdc.get(contentsKey, PersistentDataType.BYTE_ARRAY);
        data.used = pdc.getOrDefault(usedKey, PersistentDataType.INTEGER, 0);
        String patches = pdc.get(patchesKey, PersistentDataType.STRING);
        if (patches != null && !patches.isBlank()) {
            for (String part : patches.split(",")) {
                PatchType patch = PatchType.parse(part);
                if (patch != null && !data.patches.contains(patch)) data.patches.add(patch);
            }
        }
        data.owner = parseUuid(pdc.get(ownerKey, PersistentDataType.STRING));
        data.ownerName = pdc.get(ownerNameKey, PersistentDataType.STRING);
        String color = pdc.get(colorKey, PersistentDataType.STRING);
        if (color != null) {
            try {
                data.color = DyeColor.valueOf(color);
            } catch (IllegalArgumentException ignored) {
                // unknown color, keep tier default
            }
        }
        return data;
    }

    public @Nullable UUID idOf(@Nullable ItemStack item) {
        if (!isBackpack(item)) return null;
        return parseUuid(item.getPersistentDataContainer().get(idKey, PersistentDataType.STRING));
    }

    public int usedOf(@Nullable ItemStack item) {
        if (!isBackpack(item)) return 0;
        return item.getPersistentDataContainer().getOrDefault(usedKey, PersistentDataType.INTEGER, 0);
    }

    public boolean hasPatch(@Nullable ItemStack item, PatchType patch) {
        if (!isBackpack(item)) return false;
        String patches = item.getPersistentDataContainer().get(patchesKey, PersistentDataType.STRING);
        return patches != null && Arrays.asList(patches.split(",")).contains(patch.id());
    }

    /** Writes the data into the item in place and refreshes its name, lore and look. */
    public void write(ItemStack item, BackpackData data) {
        ItemStack[] contents = data.contentsRaw == null ? new ItemStack[0] : data.contents();
        item.editMeta(meta -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(tierKey, PersistentDataType.STRING, data.tier.id());
            setOrRemove(pdc, idKey, data.id == null ? null : data.id.toString());
            if (data.contentsRaw != null) {
                pdc.set(contentsKey, PersistentDataType.BYTE_ARRAY, data.contentsRaw);
            } else {
                pdc.remove(contentsKey);
            }
            pdc.set(usedKey, PersistentDataType.INTEGER, data.used);
            setOrRemove(pdc, patchesKey, data.patches.isEmpty() ? null
                    : data.patches.stream().map(PatchType::id).collect(Collectors.joining(",")));
            setOrRemove(pdc, ownerKey, data.owner == null ? null : data.owner.toString());
            setOrRemove(pdc, ownerNameKey, data.ownerName);
            setOrRemove(pdc, colorKey, data.color == null ? null : data.color.name());

            meta.setMaxStackSize(1);
            meta.itemName(Component.text("Mochila de " + data.tier.displayName(), TextColor.fromHexString(data.tier.color())));
            meta.setItemModel(NamespacedKey.minecraft(data.color == null
                    ? data.tier.model()
                    : data.color.name().toLowerCase(Locale.ROOT) + "_bundle"));
            meta.setEnchantmentGlintOverride(data.tier == Tier.NETHERITA ? Boolean.TRUE : null);
            if (data.tier == Tier.NETHERITA) {
                meta.setDamageResistantTypes(RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.DAMAGE_TYPE).getTag(DamageTypeTagKeys.IS_FIRE));
            }
            meta.lore(lore(data, contents));
        });
    }

    private List<Component> lore(BackpackData data, ItemStack[] contents) {
        List<Component> lines = new ArrayList<>();
        TextColor tierColor = TextColor.fromHexString(data.tier.color());
        lines.add(line(Component.text("Nivel: ", NamedTextColor.GRAY)
                .append(Component.text(data.tier.displayName(), tierColor))));

        int size = data.tier.size();
        int bars = 12;
        int filled = size == 0 ? 0 : Math.round((float) data.used / size * bars);
        TextColor barColor = data.used >= size ? NamedTextColor.RED
                : data.used * 4 >= size * 3 ? NamedTextColor.GOLD : NamedTextColor.GREEN;
        lines.add(line(Component.text("|".repeat(filled), barColor)
                .append(Component.text("|".repeat(bars - filled), NamedTextColor.DARK_GRAY))
                .append(Component.text(" " + data.used + "/" + size, NamedTextColor.WHITE))));

        lines.add(Component.empty());
        lines.add(line(Component.text("Parches (" + data.patches.size() + "/" + data.tier.patchSlots() + ")", NamedTextColor.GRAY)));
        if (data.patches.isEmpty()) {
            lines.add(line(Component.text(" - ninguno -", NamedTextColor.DARK_GRAY)));
        }
        for (PatchType patch : data.patches) {
            lines.add(line(Component.text(" * ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(patch.displayName(), TextColor.fromHexString(patch.color())))));
        }

        List<ItemStack> grouped = group(contents);
        if (!grouped.isEmpty()) {
            lines.add(Component.empty());
            lines.add(line(Component.text("Contenido", NamedTextColor.GRAY)));
            for (int i = 0; i < Math.min(PREVIEW_LINES, grouped.size()); i++) {
                ItemStack stack = grouped.get(i);
                lines.add(line(Component.text(" " + stack.getAmount() + "x ", NamedTextColor.WHITE)
                        .append(stack.effectiveName().colorIfAbsent(NamedTextColor.GRAY))));
            }
            if (grouped.size() > PREVIEW_LINES) {
                lines.add(line(Component.text(" ...y " + (grouped.size() - PREVIEW_LINES) + " más", NamedTextColor.DARK_GRAY)));
            }
        }

        if (data.owner != null) {
            lines.add(Component.empty());
            lines.add(line(Component.text("Candado: ", NamedTextColor.GRAY)
                    .append(Component.text(data.ownerName == null ? "?" : data.ownerName, NamedTextColor.GOLD))));
        }

        lines.add(Component.empty());
        lines.add(line(Component.text("Clic derecho para abrir", NamedTextColor.YELLOW)));
        lines.add(line(Component.text("Suelta un parche encima para coserlo", NamedTextColor.DARK_GRAY)));
        lines.add(line(Component.text("Clic derecho con tijeras para descoser", NamedTextColor.DARK_GRAY)));
        return lines;
    }

    /** Groups similar stacks, summing amounts, sorted by amount descending. */
    private static List<ItemStack> group(ItemStack[] contents) {
        List<ItemStack> grouped = new ArrayList<>();
        outer:
        for (ItemStack item : contents) {
            if (item == null || item.isEmpty()) continue;
            for (ItemStack existing : grouped) {
                if (existing.isSimilar(item)) {
                    existing.setAmount(existing.getAmount() + item.getAmount());
                    continue outer;
                }
            }
            grouped.add(item.clone());
        }
        grouped.sort((a, b) -> Integer.compare(b.getAmount(), a.getAmount()));
        return grouped;
    }

    // ---------------------------------------------------------------- patches

    public ItemStack createPatch(PatchType patch) {
        ItemStack item = ItemStack.of(BASE);
        item.editMeta(meta -> applyPatchMeta(meta, patch));
        return item;
    }

    private void applyPatchMeta(ItemMeta meta, PatchType patch) {
        meta.getPersistentDataContainer().set(patchKey, PersistentDataType.STRING, patch.id());
        meta.itemName(Component.text("Parche: " + patch.displayName(), TextColor.fromHexString(patch.color())));
        meta.setItemModel(NamespacedKey.minecraft(patch.model()));
        meta.setEnchantmentGlintOverride(true);
        meta.setMaxStackSize(16);
        List<Component> lore = new ArrayList<>();
        for (String text : patch.description()) {
            lore.add(line(Component.text(text, NamedTextColor.GRAY)));
        }
        lore.add(Component.empty());
        lore.add(line(Component.text("Arrástralo sobre una mochila", NamedTextColor.YELLOW)));
        lore.add(line(Component.text("en tu inventario para coserlo.", NamedTextColor.YELLOW)));
        meta.lore(lore);
    }

    public @Nullable PatchType patchOf(@Nullable ItemStack item) {
        if (item == null || item.getType() != BASE) return null;
        return PatchType.parse(item.getPersistentDataContainer().get(patchKey, PersistentDataType.STRING));
    }

    // ---------------------------------------------------------------- helpers

    private static Component line(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private static void setOrRemove(PersistentDataContainer pdc, NamespacedKey key, @Nullable String value) {
        if (value == null) {
            pdc.remove(key);
        } else {
            pdc.set(key, PersistentDataType.STRING, value);
        }
    }

    private static @Nullable UUID parseUuid(@Nullable String value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
