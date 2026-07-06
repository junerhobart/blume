package io.blume.ecology.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.blume.BlumePlugin;
import io.blume.ecology.EcologyModule;
import io.blume.ecology.items.EcologyItems;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

public final class EcologyGiveCommand {

    private EcologyGiveCommand() {}

    public static @NotNull LiteralArgumentBuilder<CommandSourceStack> literal(@NotNull BlumePlugin plugin) {
        return Commands.literal("give")
            .then(Commands.argument("item", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
                    for (String itemId : EcologyItems.GIVE_IDS) {
                        if (remaining.isEmpty() || itemId.startsWith(remaining)) {
                            builder.suggest(itemId);
                        }
                    }
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                        ctx.getSource().getSender().sendMessage(Component.text(
                            "Console must specify a target player: /blume give <item> <amount> <player>",
                            NamedTextColor.RED
                        ));
                        return 0;
                    }
                    return give(plugin, ctx.getSource().getSender(), item(ctx), 1, player);
                })
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                    .executes(ctx -> {
                        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                            ctx.getSource().getSender().sendMessage(Component.text(
                                "Console must specify a target player: /blume give <item> <amount> <player>",
                                NamedTextColor.RED
                            ));
                            return 0;
                        }
                        return give(
                            plugin,
                            ctx.getSource().getSender(),
                            item(ctx),
                            IntegerArgumentType.getInteger(ctx, "amount"),
                            player
                        );
                    })
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                String name = online.getName();
                                if (remaining.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                                    builder.suggest(name);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "player"));
                            if (target == null) {
                                ctx.getSource().getSender().sendMessage(
                                    Component.text("Player not found.", NamedTextColor.RED)
                                );
                                return 0;
                            }
                            return give(
                                plugin,
                                ctx.getSource().getSender(),
                                item(ctx),
                                IntegerArgumentType.getInteger(ctx, "amount"),
                                target
                            );
                        }))));
    }

    private static int give(
        @NotNull BlumePlugin plugin,
        @NotNull CommandSender sender,
        @NotNull String itemId,
        int amount,
        @NotNull Player target
    ) {
        EcologyModule ecology = plugin.ecologyModule();
        if (ecology == null) {
            sender.sendMessage(Component.text("Ecology module is not loaded.", NamedTextColor.RED));
            return 0;
        }

        ItemStack stack = ecology.items().createByGiveId(itemId, amount);
        if (stack == null) {
            sender.sendMessage(Component.text(
                "Unknown item. Try: carrot_seeds, potato_seeds, mixed_seeds",
                NamedTextColor.RED
            ));
            return 0;
        }

        Map<Integer, ItemStack> overflow = target.getInventory().addItem(stack);
        for (ItemStack leftover : overflow.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), leftover);
        }

        sender.sendMessage(Component.text(
            "Gave " + amount + "x blume:" + itemId.toLowerCase(Locale.ROOT) + " to " + target.getName() + ".",
            NamedTextColor.GREEN
        ));
        return 1;
    }

    private static @NotNull String item(@NotNull CommandContext<CommandSourceStack> ctx) {
        return StringArgumentType.getString(ctx, "item");
    }
}
