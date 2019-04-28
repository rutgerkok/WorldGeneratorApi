package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;

import net.minecraft.server.v1_14_R1.BiomeBase;

public final class BiomeGridImpl implements BiomeGrid {
    private final BiomeBase[] biomeArray;

    BiomeGridImpl(BiomeBase[] biome) {
        this.biomeArray = biome;
    }

    @Override
    public Biome getBiome(final int x, final int z) {
        return CraftBlock.biomeBaseToBiome(this.biomeArray[z << 4 | x]);
    }

    /**
     * Gets the internal biome array.
     * 
     * @return The biome array.
     */
    public BiomeBase[] getHandle() {
        return biomeArray;
    }

    @Override
    public void setBiome(final int x, final int z, final Biome bio) {
        this.biomeArray[z << 4 | x] = CraftBlock.biomeToBiomeBase(bio);
    }
}