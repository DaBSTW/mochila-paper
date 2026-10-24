package com.dabstw.morral.item;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum Tier {
    CUERO("Cuero", 1, 1, "#b5703a", "bundle", null),
    COBRE("Cobre", 2, 1, "#e07b53", "orange_bundle", Material.COPPER_INGOT),
    HIERRO("Hierro", 3, 2, "#d9d9d9", "light_gray_bundle", Material.IRON_INGOT),
    ORO("Oro", 4, 2, "#fcd34d", "yellow_bundle", Material.GOLD_INGOT),
    DIAMANTE("Diamante", 5, 3, "#5eead4", "cyan_bundle", Material.DIAMOND),
    NETHERITA("Netherita", 6, 4, "#a78b8b", "black_bundle", Material.NETHERITE_INGOT);

    private final String displayName;
    private final int rows;
    private final int patchSlots;
    private final String color;
    private final String model;
    private final @Nullable Material upgradeMaterial;

    Tier(String displayName, int rows, int patchSlots, String color, String model, @Nullable Material upgradeMaterial) {
        this.displayName = displayName;
        this.rows = rows;
        this.patchSlots = patchSlots;
        this.color = color;
        this.model = model;
        this.upgradeMaterial = upgradeMaterial;
    }

    public String displayName() {
        return displayName;
    }

    public int size() {
        return rows * 9;
    }

    public int patchSlots() {
        return patchSlots;
    }

    public String color() {
        return color;
    }

    public String model() {
        return model;
    }

    public @Nullable Material upgradeMaterial() {
        return upgradeMaterial;
    }

    public @Nullable Tier previous() {
        return ordinal() == 0 ? null : values()[ordinal() - 1];
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static @Nullable Tier parse(@Nullable String value) {
        if (value == null) return null;
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
