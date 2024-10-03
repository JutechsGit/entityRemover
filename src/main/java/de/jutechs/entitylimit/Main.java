package de.jutechs.entitylimit;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.util.math.Box;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import java.io.ObjectInputFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main implements ModInitializer {
    int ENTITY_LIMIT = ConfigManager.config.maxEntities;

    //private static final Logger LOGGER = LogManager.getLogger("EntityCleanerMod");

    @Override
    public void onInitialize() {
        ConfigManager.loadConfig();
        //LOGGER.info("EntityCleanerMod initialized");

        // Register server tick event to run our entity check
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    // Method that runs every server tick
    private void onServerTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            // Iterate over all players in the world
            for (ServerPlayerEntity player : world.getPlayers()) {
                ChunkPos playerChunkPos = player.getChunkPos();
                //LOGGER.info("Checking chunk for player at: " + playerChunkPos.x + ", " + playerChunkPos.z);
                checkAndRemoveEntities(world, playerChunkPos);
            }
        }
    }

    // Method to check and remove entities
    public void checkAndRemoveEntities(ServerWorld world, ChunkPos chunkPos) {
        WorldChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
        //LOGGER.info("Checking chunk at position: " + chunkPos.x + ", " + chunkPos.z);

        // Define the boundaries of the chunk
        int chunkMinX = chunkPos.getStartX();
        int chunkMinZ = chunkPos.getStartZ();
        int chunkMaxX = chunkPos.getEndX();
        int chunkMaxZ = chunkPos.getEndZ();

        // Get all entities in this chunk using a bounding box
        Box chunkBox = new Box(chunkMinX, world.getBottomY(), chunkMinZ, chunkMaxX + 1, world.getTopY(), chunkMaxZ + 1);
        List<Entity> entitiesInChunk = world.getEntitiesByClass(Entity.class, chunkBox, entity -> true);

        //LOGGER.info("Found " + entitiesInChunk.size() + " entities in chunk (" + chunkPos.x + ", " + chunkPos.z + ")");

        // Count entities by type
        Map<EntityType<?>, Integer> entityCountMap = new HashMap<>();
        int totalEntities = 0;

        for (Entity entity : entitiesInChunk) {
            EntityType<?> entityType = entity.getType();
            entityCountMap.put(entityType, entityCountMap.getOrDefault(entityType, 0) + 1);
            totalEntities++;
        }

       // LOGGER.info("Total entity count: " + totalEntities);

        // If the total entity count exceeds the limit
        if (totalEntities > ENTITY_LIMIT) {
            EntityType<?> mostCommonEntity = null;
            int maxCount = 0;

            // Find the entity type with the most occurrences
            for (Map.Entry<EntityType<?>, Integer> entry : entityCountMap.entrySet()) {
                //LOGGER.info("Entity type: " + entry.getKey().getName().getString() + ", Count: " + entry.getValue());
                if (entry.getValue() > maxCount) {
                    mostCommonEntity = entry.getKey();
                    maxCount = entry.getValue();
                }
            }

            if (mostCommonEntity != null) {
               // LOGGER.info("Most common entity type: " + mostCommonEntity.getName().getString() + ", Count: " + maxCount);

                // Remove entities of the most common type
                for (Entity entity : entitiesInChunk) {
                    if (entity.getType() == mostCommonEntity) {
                        // LOGGER.info("Removing entity of type: " + entity.getType().getName().getString());
                        entity.remove(Entity.RemovalReason.KILLED);  // Remove the entity
                    }
                }
            } else {
              //  LOGGER.warn("No common entity found in chunk (" + chunkPos.x + ", " + chunkPos.z + ")");
            }
        } else {
           // LOGGER.info("Entity count is below the limit, no entities removed.");
        }
    }
}