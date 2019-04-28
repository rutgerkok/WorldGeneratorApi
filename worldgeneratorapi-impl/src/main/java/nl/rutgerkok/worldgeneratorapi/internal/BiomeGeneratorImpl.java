package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Objects;

import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;

import net.minecraft.server.v1_14_R1.BiomeBase;
import net.minecraft.server.v1_14_R1.WorldChunkManager;

import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;

public final class BiomeGeneratorImpl implements BiomeGenerator {

    private final WorldChunkManager internal;

    public BiomeGeneratorImpl(WorldChunkManager worldChunkManager) {
        internal = Objects.requireNonNull(worldChunkManager, "worldChunkManager");
    }

    @Override
    public Biome[] getBiomes(int minX, int minZ, int xSize, int zSize) {
        BiomeBase[] biomeArray = internal.getBiomeBlock(minX, minZ, xSize, zSize);

        Biome[] biomes = new Biome[xSize * zSize];
        for (int i = 0; i < xSize * zSize; i++) {
            biomes[i] = CraftBlock.biomeBaseToBiome(biomeArray[i]);
        }
        return biomes;
    }

    @Override
    public Biome[] getZoomedOutBiomes(int minX, int minZ, int xSize, int zSize) {
        Biome[] biomes = new Biome[xSize * zSize];
        for (int i = 0; i < xSize * zSize; i++) {
            int xLocal = i % xSize;
            int zLocal = i / xSize;
            biomes[i] = CraftBlock.biomeBaseToBiome(internal.b(minX + xLocal, minZ + zLocal));
        }
        return biomes;
    }

}
