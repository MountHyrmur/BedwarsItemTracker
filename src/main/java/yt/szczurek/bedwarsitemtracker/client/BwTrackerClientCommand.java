package yt.szczurek.bedwarsitemtracker.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import yt.szczurek.bedwarsitemtracker.TrackingMode;

import java.util.Optional;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class BwTrackerClientCommand {
    public static LiteralArgumentBuilder<FabricClientCommandSource> get() {
        var resetSubcommand = literal("reset").executes(ctx -> {
            BedwarsitemtrackerClient.reset();
            ctx.getSource().sendFeedback(Text.literal("Reset start time and counters"));
            return Command.SINGLE_SUCCESS;
        });

        var saveWithNameSubcommand = argument("name", StringArgumentType.word())
                .executes(ctx -> executeSave(ctx, StringArgumentType.getString(ctx, "name")));

        var saveSubcommand = literal("save")
                .then(saveWithNameSubcommand).executes(ctx -> executeSave(ctx, null));

        var modeSubcommand = literal("mode").then(literal("spawn").executes(ctx -> {
            BedwarsitemtrackerClient.trackingMode = TrackingMode.Spawn;
            return Command.SINGLE_SUCCESS;
        })).then(literal("pickup").executes(ctx -> {
            BedwarsitemtrackerClient.trackingMode = TrackingMode.Pickup;
            return Command.SINGLE_SUCCESS;
        }));

        var statusSubcommand = literal("status").executes(ctx -> {
            ctx.getSource().sendFeedback(Text.literal(BedwarsitemtrackerClient.getSummary()));
            return Command.SINGLE_SUCCESS;
        });

        return literal("bwtracker").then(resetSubcommand).then(saveSubcommand).then(modeSubcommand).then(statusSubcommand);
    }

    private static int executeSave(CommandContext<FabricClientCommandSource> ctx, String name) {
        Optional<String> saveError = BedwarsitemtrackerClient.saveRaport(name);
        if (saveError.isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("Saved counters"));
        } else {
            ctx.getSource().sendError(Text.literal("Failed to save counters: " + saveError.get()));
        }
        return Command.SINGLE_SUCCESS;
    }
}
