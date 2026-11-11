package org.Cry_Legende.sHSnow;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /snow is the player command (toggle). /shsnow is the admin root (reload); without an admin permission it still
 * toggles the snow, as the alias it used to be.
 */
final class SnowCommand {

    static final String TOGGLE_PERMISSION = "shsnow.toggle";
    static final String RELOAD_PERMISSION = "shsnow.admin.reload";

    private SnowCommand() {
    }

    /** /snow, /snow toggle. /snow reload is still accepted but no longer shown anywhere. */
    static LiteralCommandNode<CommandSourceStack> player(SHSnow plugin) {
        return Commands.literal("snow")
            .requires(source -> source.getSender().hasPermission(TOGGLE_PERMISSION))
            .executes(context -> toggle(plugin, context.getSource()))
            .then(Commands.literal("toggle")
                .executes(context -> toggle(plugin, context.getSource())))
            .then(reload(plugin))
            .then(Commands.argument("arguments", StringArgumentType.greedyString())
                .executes(context -> usage(plugin, context.getSource().getSender())))
            .build();
    }

    /** /shsnow reload. Visible to anyone who can toggle or reload. */
    static LiteralCommandNode<CommandSourceStack> admin(SHSnow plugin) {
        return Commands.literal("shsnow")
            .requires(source -> can(source.getSender(), TOGGLE_PERMISSION) || can(source.getSender(), RELOAD_PERMISSION))
            .executes(context -> {
                CommandSender sender = context.getSource().getSender();
                // Without an admin permission /shsnow keeps toggling the snow, as it did as an alias of /snow
                return can(sender, RELOAD_PERMISSION)
                    ? usage(plugin, sender)
                    : toggle(plugin, context.getSource());
            })
            .then(Commands.literal("toggle")
                .requires(source -> can(source.getSender(), TOGGLE_PERMISSION))
                .executes(context -> toggle(plugin, context.getSource())))
            .then(reload(plugin))
            .then(Commands.argument("arguments", StringArgumentType.greedyString())
                .executes(context -> usage(plugin, context.getSource().getSender())))
            .build();
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> reload(SHSnow plugin) {
        return Commands.literal("reload")
            .requires(source -> can(source.getSender(), RELOAD_PERMISSION))
            .executes(context -> {
                CommandSender sender = context.getSource().getSender();
                try {
                    plugin.reload();
                    plugin.send(sender, "reloaded");
                } catch (SHSnow.ReloadException e) {
                    plugin.getLogger().warning(e.getMessage() + " (the previous configuration stays active)");
                    plugin.send(sender, "reload-failed", "file", e.file, "error", e.detail);
                }
                return Command.SINGLE_SUCCESS;
            });
    }

    private static boolean can(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }

    /** One usage line listing only what the sender may use. */
    private static int usage(SHSnow plugin, CommandSender sender) {
        StringBuilder commands = new StringBuilder();
        if (can(sender, TOGGLE_PERMISSION)) {
            commands.append("/snow [toggle]");
        }
        if (can(sender, RELOAD_PERMISSION)) {
            if (!commands.isEmpty()) {
                commands.append(" | ");
            }
            commands.append("/shsnow reload");
        }
        plugin.send(sender, "usage", "commands", commands.toString());
        return Command.SINGLE_SUCCESS;
    }

    private static int toggle(SHSnow plugin, CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            plugin.send(source.getSender(), "players-only");
            return 0;
        }
        plugin.send(player, plugin.toggle(player) ? "disabled" : "enabled");
        return Command.SINGLE_SUCCESS;
    }
}
