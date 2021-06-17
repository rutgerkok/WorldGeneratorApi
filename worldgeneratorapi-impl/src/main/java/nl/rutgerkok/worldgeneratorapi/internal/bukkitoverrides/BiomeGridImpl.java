package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import java.util.Objects;

import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlock;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;

import net.minecraft.core.Registry;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;

public final class BiomeGridImpl implements BiomeGrid {
    private final ChunkBiomeContainer biomeStorage;

    BiomeGridImpl(ChunkBiomeContainer biomeStorage) {
        this.biomeStorage = Objects.requireNonNull(biomeStorage);
    }

    @Override
    public Biome getBiome(final int x, final int z) {
        return CraftBlock
                .biomeBaseToBiome((Registry<net.minecraft.world.level.biome.Biome>) this.biomeStorage.biomeRegistry, this.biomeStorage
                        .getNoiseBiome(x >> 2, 0, z >> 2));
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        return CraftBlock
                .biomeBaseToBiome((Registry<net.minecraft.world.level.biome.Biome>) this.biomeStorage.biomeRegistry, this.biomeStorage
                        .getNoiseBiome(x >> 2, y >> 2, z >> 2));
    }

    /**
     * Gets the internal biome storage.
     *
     * @return The biome storage.
     */
    public ChunkBiomeContainer getHandle() {
        return biomeStorage;
    }

    @Override
    public void setBiome(int x, int z, Biome biome) {
        this.biomeStorage.setBiome(x >> 2, 0, z >> 2, CraftBlock
                .biomeToBiomeBase((Registry<net.minecraft.world.level.biome.Biome>) this.biomeStorage.biomeRegistry, biome));
    }

    @Override
    public void setBiome(int x, int y, int z, Biome biome) {
        this.biomeStorage.setBiome(x >> 2, y >> 2, z >> 2, CraftBlock
                .biomeToBiomeBase((Registry<net.minecraft.world.level.biome.Biome>) this.biomeStorage.biomeRegistry, biome));
    }
}