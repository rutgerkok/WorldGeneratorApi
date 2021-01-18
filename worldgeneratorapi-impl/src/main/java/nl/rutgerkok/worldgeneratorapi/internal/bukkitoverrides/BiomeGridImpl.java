package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import java.util.Objects;

import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBlock;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;

import net.minecraft.server.v1_16_R3.BiomeBase;
import net.minecraft.server.v1_16_R3.BiomeStorage;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.RegistryGeneration;

public final class BiomeGridImpl implements BiomeGrid {
    private final BiomeStorage biomeStorage;

    BiomeGridImpl(BiomeStorage biomeStorage) {
        this.biomeStorage = Objects.requireNonNull(biomeStorage);
    }

    @Override
    public Biome getBiome(final int x, final int z) {
        return CraftBlock.biomeBaseToBiome((IRegistry<BiomeBase>) this.biomeStorage.registry,
                this.biomeStorage.getBiome(x >> 2, 0, z >> 2));
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        return CraftBlock
                .biomeBaseToBiome((IRegistry<BiomeBase>) this.biomeStorage.registry,
                this.biomeStorage.getBiome(x >> 2, y >> 2, z >> 2));
    }

    /**
     * Gets the internal biome storage.
     *
     * @return The biome storage.
     */
    public BiomeStorage getHandle() {
        return biomeStorage;
    }

    @Override
    public void setBiome(int x, int z, Biome biome) {
        this.biomeStorage.setBiome(x >> 2, 0, z >> 2,
                CraftBlock.biomeToBiomeBase(RegistryGeneration.WORLDGEN_BIOME, biome));
    }

    @Override
    public void setBiome(int x, int y, int z, Biome biome) {
        this.biomeStorage.setBiome(x >> 2, y >> 2, z >> 2,
                CraftBlock.biomeToBiomeBase(RegistryGeneration.WORLDGEN_BIOME, biome));
    }
}