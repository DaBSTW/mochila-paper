package com.dabstw.morral.item;

import org.bukkit.DyeColor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class BackpackData {
    public Tier tier;
    public @Nullable UUID id;
    public byte @Nullable [] contentsRaw;
    public int used;
    public final List<PatchType> patches = new ArrayList<>();
    public @Nullable UUID owner;
    public @Nullable String ownerName;
    public @Nullable DyeColor color;

    public BackpackData(Tier tier) {
        this.tier = tier;
    }

    public boolean has(PatchType patch) {
        return patches.contains(patch);
    }

    /** Returns at least {@code tier.size()} slots; may be longer if stored contents exceed the tier size. */
    public ItemStack[] contents() {
        ItemStack[] stored = contentsRaw == null || contentsRaw.length == 0
                ? new ItemStack[0]
                : ItemStack.deserializeItemsFromBytes(contentsRaw);
        ItemStack[] result = Arrays.copyOf(stored, Math.max(stored.length, tier.size()));
        for (int i = 0; i < result.length; i++) {
            if (result[i] == null) result[i] = ItemStack.empty();
        }
        return result;
    }

    public void setContents(ItemStack[] items) {
        ItemStack[] clean = new ItemStack[items.length];
        int count = 0;
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item == null || item.isEmpty()) {
                clean[i] = ItemStack.empty();
            } else {
                clean[i] = item;
                count++;
            }
        }
        contentsRaw = ItemStack.serializeItemsAsBytes(clean);
        used = count;
    }
}
