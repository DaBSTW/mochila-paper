package com.dabstw.morral;

import com.dabstw.morral.backpack.BackpackManager;
import com.dabstw.morral.backpack.BackpackTask;
import com.dabstw.morral.command.MorralCommand;
import com.dabstw.morral.item.Items;
import com.dabstw.morral.item.Recipes;
import com.dabstw.morral.listener.CraftListener;
import com.dabstw.morral.listener.InventoryListener;
import com.dabstw.morral.listener.PlayerListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class MorralPlugin extends JavaPlugin {
    private Items items;
    private Messages messages;
    private BackpackManager manager;
    private Recipes recipes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        items = new Items(this);
        messages = new Messages(this);
        manager = new BackpackManager(this);
        recipes = new Recipes(this);
        recipes.register();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new InventoryListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new CraftListener(this), this);
        MorralCommand.register(this);
        getServer().getScheduler().runTaskTimer(this, new BackpackTask(this), 20L, BackpackTask.PERIOD_TICKS);

        for (Player player : getServer().getOnlinePlayers()) {
            player.discoverRecipes(recipes.keys());
        }
        getLogger().info("Morral listo: " + recipes.keys().size() + " recetas registradas.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.closeAll();
        if (recipes != null) recipes.unregister();
    }

    public String namespace() {
        return getName().toLowerCase(Locale.ROOT);
    }

    public Items items() {
        return items;
    }

    public Messages messages() {
        return messages;
    }

    public BackpackManager manager() {
        return manager;
    }

    public Recipes recipes() {
        return recipes;
    }
}
