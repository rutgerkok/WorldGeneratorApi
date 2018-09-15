package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;

import net.minecraft.server.v1_13_R2.BiomeBase;

class BiomeGridImpl implements BiomeGrid {
    final BiomeBase[] biomeArray;

    BiomeGridImpl(BiomeBase[] biome) {
        this.biomeArray = biome;
    }

    @Override
    public Biome getBiome(final int x, final int z) {
        return CraftBlock.biomeBaseToBiome(this.biomeArray[z << 4 | x]);
    }

    @Override
    public void setBiome(final int x, final int z, final Biome bio) {
        this.biomeArray[z << 4 | x] = CraftBlock.biomeToBiomeBase(bio);
    }
}