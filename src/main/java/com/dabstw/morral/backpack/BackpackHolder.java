package com.dabstw.morral.backpack;

import com.dabstw.morral.item.Tier;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Marks an open backpack GUI and links it to the backpack item (by id) that owns its contents. */
public final class BackpackHolder implements InventoryHolder {
    private final UUID viewer;
    private final UUID backpackId;
    private final Tier tier;
    private Inventory inventory;

    public BackpackHolder(UUID viewer, UUID backpackId, Tier tier) {
        this.viewer = viewer;
        this.backpackId = backpackId;
        this.tier = tier;
    }

    public UUID viewer() {
        return viewer;
    }

    public UUID backpackId() {
        return backpackId;
    }

    public Tier tier() {
        return tier;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
