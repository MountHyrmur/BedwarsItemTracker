package yt.szczurek.bedwarsitemtracker.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.tag.client.v1.ClientTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yt.szczurek.bedwarsitemtracker.ItemPickupEntry;
import yt.szczurek.bedwarsitemtracker.TrackingMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class BedwarsitemtrackerClient implements ClientModInitializer {

    public static final String MOD_ID = "bedwarsitemtracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final TagKey<Item> BEDWARS_RESOURCES_TAG = TagKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "bedwars_resources"));

    private static long start = 0;
    public static TrackingMode trackingMode = TrackingMode.Spawn;
    private static Vec3d forgePos = null;
    private static final HashMap<Identifier, List<ItemPickupEntry>> STORE = new HashMap<>();
    private static final ArrayList<Integer> ENTITIES_TO_CHECK = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(BwTrackerClientCommand.get()));

        ClientTickEvents.END_WORLD_TICK.register(clientWorld -> {
            ENTITIES_TO_CHECK.removeIf(eid -> {
                Entity entity = clientWorld.getEntityById(eid);
                if (!(entity instanceof ItemEntity itemEntity)) {
                    return true;
                }
                if (!isNearForge(entity)) {
                    return true;
                }

                ItemStack item = itemEntity.getStack();
                if (item.isEmpty()) {
                    return false;
                }
                logItem(item.getItem(), item.getCount());
                return true;
            });
        });
    }

    private static boolean isNearForge(Entity entity) {
        double maxPlayerDistanceSquared = 8 * 8;
        double maxForgeDistanceSquared = 0.5 * 0.5;
        if (forgePos == null) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            assert player != null;

            if (player.squaredDistanceTo(entity) < maxPlayerDistanceSquared) {
                forgePos = entity.getPos();
                return true;
            }
        } else {
            return entity.getPos().squaredDistanceTo(forgePos) < maxForgeDistanceSquared;
        }
        return false;
    }

    private static boolean shouldTrack(Item item) {
        return ClientTags.isInWithLocalFallback(BEDWARS_RESOURCES_TAG, item);
    }

    public static void addEntityToCheck(int eid) {
        if (trackingMode == TrackingMode.Spawn) {
            ENTITIES_TO_CHECK.add(eid);
        }
    }

    public static void logItemPickup(Item item, int count) {
        if (trackingMode == TrackingMode.Pickup) {
            logItem(item, count);
        }
    }

    private static void logItem(Item item, int count) {
        if (!shouldTrack(item)) {
            return;
        }

        long timestamp = System.currentTimeMillis() - start;

        @SuppressWarnings("OptionalGetWithoutIsPresent") // Safe in the case of items
        Identifier id = item.getRegistryEntry().getKey().get().getValue();
        List<ItemPickupEntry> list = STORE.computeIfAbsent(id, k -> new ArrayList<>());
        list.add(new ItemPickupEntry(count, timestamp));
    }

    public static void reset() {
        forgePos = null;
        start = System.currentTimeMillis();
        STORE.clear();
    }

    public static Optional<String> saveRaport() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm");
        String timestamp = LocalDateTime.now().format(formatter);

        if (STORE.isEmpty() || STORE.values().stream().allMatch(List::isEmpty)) {
            return Optional.of("Not saving report, nothing to save");
        }

        String fileName = "drops_" + timestamp + ".csv";
        StringBuilder builder = new StringBuilder("item,timeMs,count\n");

        for (var entry: STORE.entrySet()) {
            for (var pickup: entry.getValue()) {
                builder.append(entry.getKey().getPath());
                builder.append(',');
                builder.append(pickup.timestamp());
                builder.append(',');
                builder.append(pickup.count());
                builder.append('\n');
            }
        }

        try {
            Files.writeString(Path.of(fileName), builder.toString());
        } catch (IOException e) {
            String error = "Error saving raport: " + e.getMessage();
            LOGGER.error(error);
            return Optional.of(error);
        }
        return Optional.empty();
    }
}
