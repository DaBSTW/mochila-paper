package com.dabstw.morral.item;

import com.dabstw.morral.MorralPlugin;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Recipes {
    private static final String UPGRADE = "mejora_";
    private static final String DYE = "tinte_";

    private final MorralPlugin plugin;
    private final Items items;
    private final List<NamespacedKey> keys = new ArrayList<>();

    public Recipes(MorralPlugin plugin) {
        this.plugin = plugin;
        this.items = plugin.items();
    }

    public List<NamespacedKey> keys() {
        return keys;
    }

    public void register() {
        ShapedRecipe base = new ShapedRecipe(key("mochila_cuero"), items.createBackpack(Tier.CUERO));
        base.shape("LSL", "LCL", "LLL");
        base.setIngredient('L', Material.LEATHER);
        base.setIngredient('S', Material.STRING);
        base.setIngredient('C', Material.CHEST);
        add(base);

        RecipeChoice backpack = new RecipeChoice.MaterialChoice(Items.BASE);
        for (Tier tier : Tier.values()) {
            Material material = tier.upgradeMaterial();
            if (material == null) continue;
            ShapedRecipe upgrade = new ShapedRecipe(key(UPGRADE + tier.id()), items.createBackpack(tier));
            if (tier == Tier.NETHERITA) {
                upgrade.shape(" T ", " B ", " N ");
                upgrade.setIngredient('T', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
            } else {
                upgrade.shape("NNN", "NBN", "NNN");
            }
            upgrade.setIngredient('N', material);
            upgrade.setIngredient('B', backpack);
            add(upgrade);
        }

        for (DyeColor color : DyeColor.values()) {
            Material dye = Material.matchMaterial(color.name() + "_DYE");
            if (dye == null) continue;
            ItemStack preview = items.createBackpack(Tier.CUERO);
            BackpackData data = items.read(preview);
            if (data != null) {
                data.color = color;
                items.write(preview, data);
            }
            ShapelessRecipe recipe = new ShapelessRecipe(key(DYE + color.name().toLowerCase(Locale.ROOT)), preview);
            recipe.addIngredient(backpack);
            recipe.addIngredient(dye);
            recipe.setGroup("morral_tinte");
            add(recipe);
        }

        for (PatchType patch : PatchType.values()) {
            ShapelessRecipe recipe = new ShapelessRecipe(key("parche_" + patch.id()), items.createPatch(patch));
            recipe.addIngredient(Material.LEATHER);
            recipe.addIngredient(Material.STRING);
            patch.ingredients().forEach(recipe::addIngredient);
            recipe.setGroup("morral_parche");
            add(recipe);
        }
    }

    public void unregister() {
        keys.forEach(Bukkit::removeRecipe);
        keys.clear();
    }

    public boolean isOurs(@Nullable NamespacedKey key) {
        return key != null && key.getNamespace().equals(plugin.namespace());
    }

    /** Whether the recipe's result depends on the backpack placed in the grid. */
    public boolean isDynamic(NamespacedKey key) {
        return isOurs(key) && (key.getKey().startsWith(UPGRADE) || key.getKey().startsWith(DYE));
    }

    /** Computes the real result of an upgrade/dye recipe, keeping the backpack's contents. Null if invalid. */
    public @Nullable ItemStack dynamicResult(NamespacedKey key, ItemStack[] matrix) {
        ItemStack source = null;
        for (ItemStack item : matrix) {
            if (item == null || item.getType() != Items.BASE) continue;
            if (source != null || !items.isBackpack(item)) return null;
            source = item;
        }
        BackpackData data = items.read(source);
        if (source == null || data == null) return null;

        String name = key.getKey();
        if (name.startsWith(UPGRADE)) {
            Tier target = Tier.parse(name.substring(UPGRADE.length()));
            if (target == null || data.tier != target.previous()) return null;
            data.tier = target;
        } else if (name.startsWith(DYE)) {
            DyeColor color;
            try {
                color = DyeColor.valueOf(name.substring(DYE.length()).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
            if (color == data.color) return null;
            data.color = color;
        } else {
            return null;
        }
        ItemStack result = source.asOne();
        items.write(result, data);
        return result;
    }

    private void add(Recipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) shaped.setCategory(CraftingBookCategory.EQUIPMENT);
        if (recipe instanceof ShapelessRecipe shapeless) shapeless.setCategory(CraftingBookCategory.EQUIPMENT);
        Bukkit.addRecipe(recipe);
        keys.add(((org.bukkit.Keyed) recipe).getKey());
    }

    private NamespacedKey key(String value) {
        return new NamespacedKey(plugin, value);
    }
}
