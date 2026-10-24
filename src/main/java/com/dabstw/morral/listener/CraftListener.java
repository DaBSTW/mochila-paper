package com.dabstw.morral.listener;

import com.dabstw.morral.MorralPlugin;
import com.dabstw.morral.item.Items;
import com.dabstw.morral.item.Recipes;
import org.bukkit.Keyed;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public final class CraftListener implements Listener {
    private final Items items;
    private final Recipes recipes;

    public CraftListener(MorralPlugin plugin) {
        this.items = plugin.items();
        this.recipes = plugin.recipes();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipes(recipes.keys());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        Recipe recipe = event.getRecipe();
        ItemStack[] matrix = inventory.getMatrix();
        if (recipe instanceof Keyed keyed && recipes.isOurs(keyed.getKey())) {
            if (recipes.isDynamic(keyed.getKey())) {
                inventory.setResult(recipes.dynamicResult(keyed.getKey(), matrix));
            } else if (containsOurs(matrix)) {
                inventory.setResult(null);
            }
        } else if (containsOurs(matrix)) {
            // Backpacks and patches are paper underneath: never let vanilla recipes consume them.
            inventory.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        if (!(event.getBlock().getState() instanceof Crafter crafter)) return;
        ItemStack[] matrix = crafter.getInventory().getContents();
        if (recipes.isDynamic(event.getRecipe().getKey())) {
            ItemStack result = recipes.dynamicResult(event.getRecipe().getKey(), matrix);
            if (result == null) {
                event.setCancelled(true);
            } else {
                event.setResult(result);
            }
        } else if (containsOurs(matrix)) {
            event.setCancelled(true);
        }
    }

    private boolean containsOurs(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (items.isOurs(item)) return true;
        }
        return false;
    }
}
