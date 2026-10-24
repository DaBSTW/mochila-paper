package com.dabstw.morral.item;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public enum PatchType {
    IMAN("Imán", "#f87171", "lodestone", "Atrae los objetos cercanos.", "Agáchate para pausarlo.",
            List.of(Material.IRON_INGOT, Material.REDSTONE)),
    RECOLECTOR("Recolector", "#facc15", "hopper", "Lo que recoges va directo a la mochila", "si ya hay de ese objeto dentro.",
            List.of(Material.HOPPER)),
    REABASTECER("Reabastecer", "#4ade80", "dispenser", "Rellena tu mano cuando se acaba", "un bloque o una comida.",
            List.of(Material.DISPENSER)),
    ALMA("Alma", "#a78bfa", "echo_shard", "La mochila se queda contigo", "cuando mueres.",
            List.of(Material.ECHO_SHARD, Material.ENDER_EYE)),
    COMPACTADOR("Compactador", "#fb923c", "piston", "Al cerrar, convierte 9 objetos", "en su bloque (lingotes, trigo...).",
            List.of(Material.PISTON, Material.CRAFTING_TABLE)),
    CANDADO("Candado", "#e5e7eb", "tripwire_hook", "Solo quien lo cosió", "puede abrir la mochila.",
            List.of(Material.TRIPWIRE_HOOK, Material.IRON_INGOT));

    private final String displayName;
    private final String color;
    private final String model;
    private final List<String> description;
    private final List<Material> ingredients;

    PatchType(String displayName, String color, String model, String line1, String line2, List<Material> ingredients) {
        this.displayName = displayName;
        this.color = color;
        this.model = model;
        this.description = List.of(line1, line2);
        this.ingredients = ingredients;
    }

    public String displayName() {
        return displayName;
    }

    public String color() {
        return color;
    }

    public String model() {
        return model;
    }

    public List<String> description() {
        return description;
    }

    public List<Material> ingredients() {
        return ingredients;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static @Nullable PatchType parse(@Nullable String value) {
        if (value == null) return null;
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
