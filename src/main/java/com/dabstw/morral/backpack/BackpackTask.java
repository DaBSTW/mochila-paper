package com.dabstw.morral.backpack;

import com.dabstw.morral.MorralPlugin;
import com.dabstw.morral.item.Items;
import com.dabstw.morral.item.PatchType;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Runs every few ticks: magnet patch and backpack weight. */
public final class BackpackTask implements Runnable {
    public static final long PERIOD_TICKS = 5L;

    private final MorralPlugin plugin;
    private final Items items;
    private final Set<UUID> heavy = new HashSet<>();

    public BackpackTask(MorralPlugin plugin) {
        this.plugin = plugin;
        this.items = plugin.items();
    }

    @Override
    public void run() {
        FileConfiguration config = plugin.getConfig();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR || player.isDead()) continue;
            boolean magnet = false;
            int used = 0;
            PlayerInventory inventory = player.getInventory();
            for (int slot = 0; slot <= BackpackManager.LAST_SLOT; slot++) {
                ItemStack item = inventory.getItem(slot);
                if (!items.isBackpack(item)) continue;
                used += items.usedOf(item);
                magnet |= items.hasPatch(item, PatchType.IMAN);
            }
            if (magnet && !player.isSneaking()) {
                pull(player, config.getDouble("iman.radio", 6.0), config.getDouble("iman.fuerza", 0.35));
            }
            applyWeight(player, used, config);
        }
    }

    private void pull(Player player, double radius, double force) {
        Location target = player.getLocation().add(0, 0.5, 0);
        for (Item item : player.getWorld().getNearbyEntitiesByType(Item.class, target, radius)) {
            if (player.getUniqueId().equals(item.getThrower()) || item.getPickupDelay() > 100) continue;
            Vector direction = target.toVector().subtract(item.getLocation().toVector());
            if (direction.lengthSquared() < 0.8) continue;
            item.setVelocity(direction.normalize().multiply(force));
        }
    }

    private void applyWeight(Player player, int used, FileConfiguration config) {
        if (!config.getBoolean("peso.activado", true) || player.getGameMode() == GameMode.CREATIVE) {
            heavy.remove(player.getUniqueId());
            return;
        }
        int light = config.getInt("peso.umbral-lento", 100);
        int veryHeavy = config.getInt("peso.umbral-muy-lento", 180);
        int amplifier = used >= veryHeavy ? 1 : used >= light ? 0 : -1;
        if (amplifier < 0) {
            heavy.remove(player.getUniqueId());
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) PERIOD_TICKS * 5, amplifier, true, false, true));
        if (heavy.add(player.getUniqueId())) {
            plugin.messages().actionBar(player, "peso", Placeholder.unparsed("used", String.valueOf(used)));
        }
    }
}
