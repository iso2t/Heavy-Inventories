package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.files.JsonFiles;
import com.iso2t.heavyinventories.platform.Services;
import com.iso2t.heavyinventories.server.ServerWeightState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.nio.file.Files;
import java.nio.file.Path;

/** Mutates only disposable test files; restores original bytes and permissions in finally. */
public final class AdminScenario {
    public static void run(ServerPlayer player) {
        var server = player.level().getServer();
        var state = ServerWeightState.of(server);
        var game = Services.PLATFORM.getGameDirectory();
        var file = game.resolve("weights/minecraft.json");
        var config = game.resolve("config/heavyinventories-server.json");
        var profile = new NameAndId(player.getGameProfile());
        boolean wasOp = server.getPlayerList().isOp(profile);
        byte[] original = read(file), originalConfig = read(config);
        java.util.Set<Path> oldExports;
        var directory = game.resolve("weight-exports");
        try {
            Files.createDirectories(directory);
            try (var paths = Files.list(directory)) { oldExports = paths.collect(java.util.stream.Collectors.toSet()); }
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
        try {
            server.getPlayerList().op(profile);
            Files.createDirectories(file.getParent());
            Files.writeString(config, "{\"startingWeight\":1000,\"walkingMode\":\"at_ninety_percent\"}");
            String valid = "{\"stone\":{\"weight\":2,\"density\":7},\"dirt\":{\"weight\":3}}";
            Files.writeString(file, valid);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIRT));
            require(execute(player, "set weight 4.25") == 1, "Valid set failed");
            require(state.unitWeight(Identifier.parse("minecraft:stone")) == 4.25f, "Set did not apply");
            require(state.unitWeight(Identifier.parse("minecraft:dirt")) == 3f, "Set changed wrong item");
            require(JsonFiles.readObject(file).getAsJsonObject("stone").get("density").getAsInt() == 7, "Set removed unrelated data");
            long revision = state.revision();
            Files.writeString(file, "{");
            require(execute(player, "set weight 9") == 0, "Set accepted malformed file");
            require(execute(player, "reload") == 0, "Reload accepted malformed file");
            require(Files.readString(file).equals("{") && state.revision() == revision, "Failure changed file/session");
            Files.writeString(file, valid);
            for (String command : new String[]{"set weight -1", "set weight NaN", "set weight 1e100", "config invalid", "dump ../escape"})
                require(execute(player, command) == 0, "Invalid command succeeded: " + command);
            require(state.revision() == revision, "Invalid command changed state");
            require(execute(player, "dump minecraft") == 1, "Export command failed");
            require(state.revision() == revision && Files.readString(file).equals(valid), "Dump altered active overrides");
            try (var paths = Files.list(directory)) {
                var added = paths.filter(path -> !oldExports.contains(path)).toList();
                require(added.size() == 1, "Dump did not create exactly one export");
                require(JsonFiles.readObject(added.getFirst()).has("stone"), "Export omitted registered items");
            }
            require(execute(player, "reload") == 1 && state.unitWeight(Identifier.parse("minecraft:stone")) == 2f, "Reload did not apply reviewed values");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            require(execute(player, "set weight 9") == 0, "Empty hand accepted");
            server.getPlayerList().deop(profile);
            require(execute(player, "set weight 9") == 0 && execute(player, "dump minecraft") == 0, "Non-operator could write");
            HeavyInventories.LOGGER.info("ADMIN TOOLS PASSED: main-hand unit edits, persistence, malformed-file preservation, failed/invalid command results, export-only dumps, reload, permission checks");
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
        finally {
            restore(file, original);
            restore(config, originalConfig);
            if (wasOp) server.getPlayerList().op(profile); else server.getPlayerList().deop(profile);
            try (var paths = Files.list(directory)) {
                for (var path : paths.filter(path -> !oldExports.contains(path)).toList()) Files.delete(path);
                state.reload(server);
            } catch (java.io.IOException e) { throw new RuntimeException(e); }
        }
    }
    private static int execute(ServerPlayer player, String command) {
        try { return player.level().getServer().getCommands().getDispatcher().execute("heavyinventories " + command, player.createCommandSourceStack()); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) { return 0; }
    }
    private static byte[] read(Path path) {
        try { return Files.exists(path) ? Files.readAllBytes(path) : null; }
        catch (java.io.IOException e) { throw new RuntimeException(e); }
    }
    private static void restore(Path path, byte[] contents) {
        try { if (contents == null) Files.deleteIfExists(path); else Files.write(path, contents); }
        catch (java.io.IOException e) { throw new RuntimeException(e); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
