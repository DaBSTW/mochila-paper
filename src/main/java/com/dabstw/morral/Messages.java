package com.dabstw.morral;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

public final class Messages {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final MorralPlugin plugin;

    public Messages(MorralPlugin plugin) {
        this.plugin = plugin;
    }

    public Component get(String key, TagResolver... resolvers) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mensajes");
        String raw = section == null ? null : section.getString(key);
        if (raw == null) raw = "<red>Mensaje no encontrado: " + key;
        String prefix = section == null ? "" : section.getString("prefijo", "");
        return MM.deserialize(prefix + raw, resolvers);
    }

    public void send(CommandSender sender, String key, TagResolver... resolvers) {
        sender.sendMessage(get(key, resolvers));
    }

    public void actionBar(org.bukkit.entity.Player player, String key, TagResolver... resolvers) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mensajes");
        String raw = section == null ? null : section.getString(key);
        if (raw == null) return;
        player.sendActionBar(MM.deserialize(raw, resolvers));
    }
}
