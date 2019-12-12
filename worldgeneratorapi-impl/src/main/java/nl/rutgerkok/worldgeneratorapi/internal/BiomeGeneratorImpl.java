package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Objects;

import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_15_R1.block.CraftBlock;

import net.minecraft.server.v1_15_R1.BiomeManager;
import net.minecraft.server.v1_15_R1.WorldChunkManager;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;

public final class BiomeGeneratorImpl implements BiomeGenerator {

    private final BiomeManager.Provider internal;

    public BiomeGeneratorImpl(WorldChunkManager worldChunkManager) {
        internal = Objects.requireNonNull(worldChunkManager, "worldChunkManager");
    }

    @Override
    public Biome[] getBiomes(int minX, int minZ, int xSize, int zSize) {

        Biome[] biomes = new Biome[xSize * zSize];
        for (int i = 0; i < xSize * zSize; i++) {
            int x = i % xSize + minX;
            int z = i / xSize + minZ;
            biomes[i] = CraftBlock.biomeBaseToBiome(internal.getBiome(x >> 2, 0, z >> 2));
        }
        return biomes;
    }

    @Override
    public Biome[] getZoomedOutBiomes(int minX, int minZ, int xSize, int zSize) {
        Biome[] biomes = new Biome[xSize * zSize];
        for (int i = 0; i < xSize * zSize; i++) {
            int xLocal = i % xSize;
            int zLocal = i / xSize;
            biomes[i] = CraftBlock.biomeBaseToBiome(internal.getBiome(minX + xLocal, 0, minZ + zLocal));
        }
        return biomes;
    }

}
