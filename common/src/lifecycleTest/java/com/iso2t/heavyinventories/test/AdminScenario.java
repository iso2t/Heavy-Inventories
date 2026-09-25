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

/**
 * Mutates only disposable test files; restores original bytes and permissions in finally.
 */
public final class AdminScenario {
	public static void run (ServerPlayer player) {
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
		String packName = "admin-test-" + java.util.UUID.randomUUID();
		var converted = game.resolve("weight-packs").resolve(packName + ".zip");
		try {
			Files.createDirectories(directory);
			try (var paths = Files.list(directory)) {
				oldExports = paths.collect(java.util.stream.Collectors.toSet());
			}
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
		try {
			server.getPlayerList().op(profile);
			Files.createDirectories(file.getParent());
			Files.writeString(config, "{\"startingWeight\":1000,\"walkingMode\":\"at_ninety_percent\"}");
			String valid = "{\"stone\":{\"weight\":2,\"density\":7},\"dirt\":{\"weight\":3}}";
			Files.writeString(file, valid);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIRT));
			state.reload(server);
			float stone = state.unitWeight(Identifier.parse("minecraft:stone"));
			Files.writeString(file, "{");
			require(execute(player, "reload") == 1, "Malformed legacy file still affects gameplay");
			require(state.unitWeight(Identifier.parse("minecraft:stone")) == stone, "Legacy file changed weight");
			long revision = state.revision();
			Files.writeString(config, "{");
			require(execute(player, "reload") == 0, "Reload accepted malformed configuration");
			require(state.revision() == revision, "Failed configuration changed session");
			Files.writeString(config, "{\"startingWeight\":1000,\"walkingMode\":\"at_ninety_percent\"}");
			Files.writeString(file, valid);
			for (String command : new String[] { "config invalid", "dump ../escape" })
				require(execute(player, command) == 0, "Invalid command succeeded: " + command);
			require(state.revision() == revision, "Invalid command changed state");
			require(execute(player, "dump minecraft") == 1, "Export command failed");
			require(state.revision() == revision && Files.readString(file).equals(valid), "Dump altered active overrides");
			try (var paths = Files.list(directory)) {
				var added = paths.filter(path -> !oldExports.contains(path)).toList();
				require(added.size() == 1, "Dump did not create exactly one export");
				var report = JsonFiles.readObject(added.getFirst());
				var entries = report.getAsJsonObject("items");
				require(entries.getAsJsonObject("minecraft:stone").get("weight").getAsFloat() == stone, "Export differs from active gameplay");
				require(entries.getAsJsonObject("minecraft:arrow").get("source").getAsString().equals("recipe"), "Export lacks inferred provenance");
				require(entries.getAsJsonObject("minecraft:barrier").get("source").getAsString().equals("fallback"), "Export lacks fallback provenance");
				require(report.get("revision").getAsLong() == state.revision(), "Export has wrong revision");
			}
			var selectedPacks = java.util.List.copyOf(server.getPackRepository().getSelectedIds());
			require(execute(player, "convert legacy " + packName) == 1, "Conversion command failed");
			require(Files.exists(converted) && Files.readString(file).equals(valid), "Conversion did not preserve legacy file");
			require(state.revision() == revision && selectedPacks.equals(java.util.List.copyOf(server.getPackRepository().getSelectedIds())), "Conversion applied or enabled weights");
			byte[] zipBytes = Files.readAllBytes(converted);
			require(execute(player, "convert legacy " + packName) == 0, "Conversion overwrote an existing ZIP");
			require(java.util.Arrays.equals(zipBytes, Files.readAllBytes(converted)), "Existing ZIP changed");
			Files.writeString(file, "{");
			require(execute(player, "convert legacy " + packName + "-invalid") == 0, "Malformed legacy input converted");
			require(!Files.exists(game.resolve("weight-packs").resolve(packName + "-invalid.zip")), "Invalid conversion published a pack");
			Files.writeString(file, valid);
			require(execute(player, "reload") == 1 && state.unitWeight(Identifier.parse("minecraft:stone")) == stone, "Reload read legacy overrides");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			server.getPlayerList().deop(profile);
			require(execute(player, "reload") == 0 && execute(player, "dump minecraft") == 0 && execute(player, "convert legacy " + packName + "-denied") == 0, "Non-operator could write");
			HeavyInventories.LOGGER.info("ADMIN TOOLS PASSED: ignored legacy files, failed configuration retention, invalid commands, active-table provenance export, conversion without application, collision rejection, reload, permission checks");
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				Files.deleteIfExists(converted);
			} catch (java.io.IOException e) {
				throw new RuntimeException(e);
			}
			restore(file, original);
			restore(config, originalConfig);
			if (wasOp) server.getPlayerList().op(profile);
			else server.getPlayerList().deop(profile);
			try (var paths = Files.list(directory)) {
				for (var path : paths.filter(path -> !oldExports.contains(path)).toList()) Files.delete(path);
				state.reload(server);
			} catch (java.io.IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static int execute (ServerPlayer player, String command) {
		try {
			return player.level().getServer().getCommands().getDispatcher().execute("heavyinventories " + command, player.createCommandSourceStack());
		} catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
			return 0;
		}
	}

	private static byte[] read (Path path) {
		try {
			return Files.exists(path) ? Files.readAllBytes(path) : null;
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void restore (Path path, byte[] contents) {
		try {
			if (contents == null) Files.deleteIfExists(path);
			else Files.write(path, contents);
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
