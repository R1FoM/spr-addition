package dev.rifo.spraddition.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.rifo.spraddition.api.SPRAdditionAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SPRAdditionCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spraddition")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("removeall")
                .executes(context -> {
                    SPRAdditionAPI.removeAllRagdolls(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.literal("All ragdolls removed."), true);
                    return 1;
                })
            )
            .then(Commands.literal("remove")
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
                        SPRAdditionAPI.removePlayerlessRagdollForPlayer(targetPlayer);
                        context.getSource().sendSuccess(() -> Component.literal("Removed playerless ragdoll for " + targetPlayer.getScoreboardName()), true);
                        return 1;
                    })
                )
            )
        );
    }
}
