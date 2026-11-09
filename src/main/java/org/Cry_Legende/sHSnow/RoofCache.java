package org.Cry_Legende.sHSnow;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Roof height of every column, one table of 256 columns per chunk. Tables are built on the main thread, where the
 * world can be read, and never modified afterwards, so a {@link RoofGrid} made of them can be read from any thread.
 */
final class RoofCache {

    private final Map<String, Map<Long, Table>> worlds = new HashMap<>();
    private Set<Material> ignored = Set.of();
    private int lifetimeTicks = 200;
    private int purgedAt;

    void configure(Set<Material> ignored, int lifetimeTicks) {
        this.ignored = ignored;
        this.lifetimeTicks = lifetimeTicks;
        worlds.clear();
    }

    /** Forgets the tables nobody asked for since they expired, chunks players left or worlds that unloaded. */
    void purge(int currentTick) {
        if (currentTick - purgedAt < lifetimeTicks) {
            return;
        }
        purgedAt = currentTick;
        worlds.values().forEach(tables -> tables.values().removeIf(table -> table.expiresAt() <= currentTick));
        worlds.values().removeIf(Map::isEmpty);
    }

    /** Tables of every chunk within {@code radius} blocks of (x, z) on both axes. */
    RoofGrid around(World world, double x, double z, double radius, int currentTick) {
        Map<Long, Table> tables = worlds.computeIfAbsent(world.getName(), name -> new HashMap<>());
        int minChunkX = (int) Math.floor(x - radius) >> 4;
        int minChunkZ = (int) Math.floor(z - radius) >> 4;
        int width = ((int) Math.floor(x + radius) >> 4) - minChunkX + 1;
        int depth = ((int) Math.floor(z + radius) >> 4) - minChunkZ + 1;
        int[][] grid = new int[width * depth][];
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                grid[dx * depth + dz] = table(world, tables, minChunkX + dx, minChunkZ + dz, currentTick);
            }
        }
        return new RoofGrid(minChunkX, minChunkZ, depth, grid);
    }

    private int[] table(World world, Map<Long, Table> tables, int chunkX, int chunkZ, int currentTick) {
        long key = (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32);
        Table table = tables.get(key);
        if (table != null && currentTick < table.expiresAt()) {
            return table.roofs();
        }
        // Reading an unloaded chunk would load it: no table, so no flake there
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            tables.remove(key);
            return null;
        }
        int[] roofs = build(world, chunkX << 4, chunkZ << 4);
        // Spread the rebuilds so that chunks first seen together do not all expire on the same wave
        int lifetime = lifetimeTicks / 2 + ThreadLocalRandom.current().nextInt(lifetimeTicks / 2 + 1);
        tables.put(key, new Table(roofs, currentTick + lifetime));
        return roofs;
    }

    private int[] build(World world, int baseX, int baseZ) {
        int[] roofs = new int[256];
        int minY = world.getMinHeight();
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int x = baseX + dx;
                int z = baseZ + dz;
                int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING);
                while (y >= minY) {
                    Material type = world.getType(x, y, z);
                    if (!type.isAir() && !ignored.contains(type)) {
                        break;
                    }
                    y--;
                }
                roofs[dz << 4 | dx] = Math.max(y, minY - 1);
            }
        }
        return roofs;
    }

    private record Table(int[] roofs, int expiresAt) {
    }

    /** Roof tables around one player, safe to read from any thread. */
    record RoofGrid(int minChunkX, int minChunkZ, int depth, int[][] tables) {

        /** Returned for a column whose chunk is not loaded: no flake may spawn there. */
        static final int UNLOADED = Integer.MAX_VALUE;

        /** Y of the highest block of the column that is neither air nor ignored, or {@link #UNLOADED}. */
        int roofY(int x, int z) {
            int dx = (x >> 4) - minChunkX;
            int dz = (z >> 4) - minChunkZ;
            if (dx < 0 || dz < 0 || dz >= depth || dx * depth + dz >= tables.length) {
                return UNLOADED;
            }
            int[] roofs = tables[dx * depth + dz];
            return roofs == null ? UNLOADED : roofs[(z & 15) << 4 | (x & 15)];
        }
    }
}
