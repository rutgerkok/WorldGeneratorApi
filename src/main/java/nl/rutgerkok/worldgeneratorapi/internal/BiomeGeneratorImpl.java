package nl.rutgerkok.worldgeneratorapi.internal;

import javax.annotation.Nullable;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.block.CraftBlock;

import net.minecraft.server.v1_12_R1.BiomeBase;
import net.minecraft.server.v1_12_R1.WorldChunkManager;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;

final class BiomeGeneratorImpl implements BiomeGenerator {

    private final WorldChunkManager internal;
    private @Nullable BiomeBase[] biomeArray = null;

    BiomeGeneratorImpl(World world) {
        internal = ((CraftWorld) world).getHandle().getWorldChunkManager();
    }

    @Override
    public Biome[] getZoomedOutBiomes(int minX, int minZ, int xSize, int zSize) {
        BiomeBase[] biomeArray = internal.getBiomes(this.biomeArray, minX, minZ, xSize, zSize);
        this.biomeArray = biomeArray;

        Biome[] biomes = new Biome[xSize * zSize];
        for (int i = 0; i < xSize * zSize; i++) {
            biomes[i] = CraftBlock.biomeBaseToBiome(biomeArray[i]);
        }
        return biomes;
    }

}
