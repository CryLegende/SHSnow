package org.Cry_Legende.sHSnow;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

final class SnowCommand {

    static final String TOGGLE_PERMISSION = "shsnow.toggle";
    static final String RELOAD_PERMISSION = "shsnow.reload";

    private SnowCommand() {
    }

    static LiteralCommandNode<CommandSourceStack> create(SHSnow plugin) {
        return Commands.literal("snow")
            .requires(source -> source.getSender().hasPermission(TOGGLE_PERMISSION))
            .executes(context -> toggle(plugin, context.getSource()))
            .then(Commands.literal("toggle")
                .executes(context -> toggle(plugin, context.getSource())))
            .then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission(RELOAD_PERMISSION))
                .executes(context -> {
                    plugin.load();
                    plugin.send(context.getSource().getSender(), "reloaded");
                    return Command.SINGLE_SUCCESS;
                }))
            .build();
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
