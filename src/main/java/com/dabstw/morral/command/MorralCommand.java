package com.dabstw.morral.command;

import com.dabstw.morral.MorralPlugin;
import com.dabstw.morral.backpack.BackpackManager;
import com.dabstw.morral.item.PatchType;
import com.dabstw.morral.item.Tier;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public final class MorralCommand {
    private static final String HELP = """
            <gradient:#c08552:#f3c892><bold>Morral</bold></gradient> <gray>- mochilas vivas</gray>
            <yellow>/morral abrir</yellow> <gray>Abre la mochila que tienes en la mano.</gray>
            <yellow>/morral dar <jugadores> <nivel></yellow> <gray>Entrega una mochila.</gray>
            <yellow>/morral parche <jugadores> <tipo></yellow> <gray>Entrega un parche.</gray>
            <yellow>/morral recargar</yellow> <gray>Recarga la configuración.</gray>
            <dark_gray>Niveles: cuero, cobre, hierro, oro, diamante, netherita</dark_gray>
            <dark_gray>Parches: iman, recolector, reabastecer, alma, compactador, candado</dark_gray>""";

    private MorralCommand() {
    }

    public static void register(MorralPlugin plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(build(plugin), "Mochilas de Morral", List.of("mochila")));
    }

    private static LiteralCommandNode<CommandSourceStack> build(MorralPlugin plugin) {
        return Commands.literal("morral")
                .executes(MorralCommand::help)
                .then(Commands.literal("ayuda").executes(MorralCommand::help))
                .then(Commands.literal("abrir")
                        .requires(source -> source.getExecutor() instanceof Player
                                && source.getSender().hasPermission("morral.use"))
                        .executes(ctx -> {
                            Player player = (Player) ctx.getSource().getExecutor();
                            if (!plugin.items().isBackpack(player.getInventory().getItemInMainHand())) {
                                plugin.messages().send(player, "no-mochila-mano");
                                return 0;
                            }
                            plugin.manager().open(player, player.getInventory().getHeldItemSlot());
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("dar")
                        .requires(source -> source.getSender().hasPermission("morral.admin"))
                        .then(Commands.argument("jugadores", ArgumentTypes.players())
                                .then(Commands.argument("nivel", StringArgumentType.word())
                                        .suggests(suggest(Arrays.stream(Tier.values()).map(Tier::id).toList()))
                                        .executes(ctx -> give(plugin, ctx, "nivel", value -> {
                                            Tier tier = Tier.parse(value);
                                            return tier == null ? null : plugin.items().createBackpack(tier);
                                        })))))
                .then(Commands.literal("parche")
                        .requires(source -> source.getSender().hasPermission("morral.admin"))
                        .then(Commands.argument("jugadores", ArgumentTypes.players())
                                .then(Commands.argument("tipo", StringArgumentType.word())
                                        .suggests(suggest(Arrays.stream(PatchType.values()).map(PatchType::id).toList()))
                                        .executes(ctx -> give(plugin, ctx, "tipo", value -> {
                                            PatchType patch = PatchType.parse(value);
                                            return patch == null ? null : plugin.items().createPatch(patch);
                                        })))))
                .then(Commands.literal("recargar")
                        .requires(source -> source.getSender().hasPermission("morral.admin"))
                        .executes(ctx -> {
                            plugin.reloadConfig();
                            plugin.messages().send(ctx.getSource().getSender(), "recargado");
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(MiniMessage.miniMessage().deserialize(HELP));
        return Command.SINGLE_SUCCESS;
    }

    private static int give(MorralPlugin plugin, CommandContext<CommandSourceStack> ctx, String argument,
                            Function<String, ItemStack> factory) throws CommandSyntaxException {
        String value = StringArgumentType.getString(ctx, argument);
        ItemStack item = factory.apply(value);
        if (item == null) {
            throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
                    .serialize(Component.text("Valor desconocido: " + value))).create();
        }
        List<Player> targets = ctx.getArgument("jugadores", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());
        for (Player target : targets) {
            BackpackManager.give(target, item.clone());
        }
        plugin.messages().send(ctx.getSource().getSender(), "entregado",
                Placeholder.component("objeto", item.effectiveName()),
                Placeholder.unparsed("jugadores", String.valueOf(targets.size())));
        return targets.size();
    }

    private static SuggestionProvider<CommandSourceStack> suggest(List<String> values) {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            values.stream().filter(v -> v.startsWith(remaining)).forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
