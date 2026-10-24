package com.dabstw.morral.listener;

import com.dabstw.morral.MorralPlugin;
import com.dabstw.morral.backpack.BackpackHolder;
import com.dabstw.morral.backpack.BackpackManager;
import com.dabstw.morral.item.BackpackData;
import com.dabstw.morral.item.Items;
import com.dabstw.morral.item.PatchType;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class PlayerListener implements Listener {
    private static final Sound REFILL_SOUND = Sound.sound(org.bukkit.Sound.ITEM_BUNDLE_REMOVE_ONE, Sound.Source.PLAYER, 0.8f, 1.2f);

    private final MorralPlugin plugin;
    private final Items items;
    private final BackpackManager manager;

    public PlayerListener(MorralPlugin plugin) {
        this.plugin = plugin;
        this.items = plugin.items();
        this.manager = plugin.manager();
    }

    // ---------------------------------------------------------------- open with right click

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!items.isBackpack(event.getItem()) || event.getHand() == null) return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block != null && !player.isSneaking() && block.getType().isInteractable()
                && event.useInteractedBlock() != Event.Result.DENY) {
            return;
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND && items.isBackpack(player.getInventory().getItemInMainHand())) {
            return;
        }
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        int slot = event.getHand() == EquipmentSlot.OFF_HAND ? 40 : player.getInventory().getHeldItemSlot();
        manager.open(player, slot);
    }

    // ---------------------------------------------------------------- patch: Recolector

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || manager.session(player) != null) return;
        Item entity = event.getItem();
        ItemStack stack = entity.getItemStack();
        if (items.isOurs(stack) || !manager.isAllowedInside(stack)) return;

        PlayerInventory inventory = player.getInventory();
        int remaining = stack.getAmount();
        for (int slot = 0; slot <= BackpackManager.LAST_SLOT && remaining > 0; slot++) {
            ItemStack backpack = inventory.getItem(slot);
            if (!items.hasPatch(backpack, PatchType.RECOLECTOR)) continue;
            BackpackData data = items.read(backpack);
            if (data == null) continue;
            ItemStack[] contents = data.contents();
            boolean known = false;
            for (ItemStack content : contents) {
                if (content.isSimilar(stack)) {
                    known = true;
                    break;
                }
            }
            if (!known) continue;
            int left = BackpackManager.insert(contents, stack, remaining);
            if (left == remaining) continue;
            remaining = left;
            data.setContents(contents);
            items.write(backpack, data);
            inventory.setItem(slot, backpack);
        }
        int collected = stack.getAmount() - remaining;
        if (collected == 0) return;

        event.setCancelled(true);
        player.playPickupItemAnimation(entity, collected);
        player.playSound(Sound.sound(org.bukkit.Sound.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 0.2f,
                1.4f + ThreadLocalRandom.current().nextFloat() * 0.4f));
        if (remaining <= 0) {
            entity.remove();
        } else {
            entity.setItemStack(stack.asQuantity(remaining));
        }
    }

    // ---------------------------------------------------------------- patch: Reabastecer

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack hand = event.getItemInHand();
        if (hand.getAmount() == 1) scheduleRefill(event.getPlayer(), event.getHand(), hand.asOne());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getAmount() == 1 && item.getMaxStackSize() > 1) {
            scheduleRefill(event.getPlayer(), event.getHand(), item.asOne());
        }
    }

    private void scheduleRefill(Player player, EquipmentSlot hand, ItemStack proto) {
        if (player.getGameMode() == GameMode.CREATIVE || items.isOurs(proto)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> refill(player, hand, proto));
    }

    private void refill(Player player, EquipmentSlot hand, ItemStack proto) {
        if (!player.isOnline() || manager.session(player) != null) return;
        PlayerInventory inventory = player.getInventory();
        if (!inventory.getItem(hand).isEmpty()) return;
        int wanted = proto.getMaxStackSize();
        int got = 0;
        for (int slot = 0; slot <= BackpackManager.LAST_SLOT && got < wanted; slot++) {
            ItemStack backpack = inventory.getItem(slot);
            if (!items.hasPatch(backpack, PatchType.REABASTECER)) continue;
            BackpackData data = items.read(backpack);
            if (data == null) continue;
            ItemStack[] contents = data.contents();
            int taken = BackpackManager.take(contents, proto, wanted - got);
            if (taken == 0) continue;
            got += taken;
            data.setContents(contents);
            items.write(backpack, data);
            inventory.setItem(slot, backpack);
        }
        if (got == 0) return;
        inventory.setItem(hand, proto.asQuantity(got));
        player.playSound(REFILL_SOUND);
        plugin.messages().actionBar(player, "rellenado", Placeholder.component("objeto", proto.effectiveName()));
    }

    // ---------------------------------------------------------------- death & patch: Alma

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathFlush(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        BackpackHolder holder = manager.session(player);
        if (holder == null) return;
        if (event.getKeepInventory()) {
            manager.handleClose(player, holder);
            return;
        }
        List<ItemStack> candidates = new ArrayList<>(event.getDrops());
        candidates.addAll(event.getItemsToKeep());
        manager.flushInto(player, candidates);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeathSoulbound(PlayerDeathEvent event) {
        if (event.getKeepInventory()) return;
        Iterator<ItemStack> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            ItemStack drop = drops.next();
            if (items.hasPatch(drop, PatchType.ALMA)) {
                drops.remove();
                event.getItemsToKeep().add(drop);
            }
        }
    }
}
