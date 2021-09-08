package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.List;
import java.util.Objects;

import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlock;

import com.google.common.collect.ImmutableSet;

import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;

/**
 * Wraps a vanilla biome generator into a WorldGeneratorApi one.
 */
@Deprecated
public final class BiomeGeneratorImpl implements BiomeGenerator {

    final Registry<Biome> biomeRegistry;
    final BiomeSource internal;

    public BiomeGeneratorImpl(Registry<Biome> biomeRegistry, BiomeSource worldChunkManager) {
        if (worldChunkManager instanceof InjectedBiomeGenerator) {
            // Not allowed - the injected biome generator itself wraps a BiomeGenerator
            throw new IllegalArgumentException("double wrapping of biome generator");
        }
        this.biomeRegistry = Objects.requireNonNull(biomeRegistry, "biomeRegistry");
        this.internal = Objects.requireNonNull(worldChunkManager, "worldChunkManager");
    }

    @Override
    public ImmutableSet<org.bukkit.block.Biome> getStructureBiomes() {
        ImmutableSet.Builder<org.bukkit.block.Biome> biomes = ImmutableSet.builder();

        try {
            @SuppressWarnings("unchecked")
            List<Biome> biomeBases = (List<Biome>) ReflectionUtil.getFieldOfType(BiomeSource.class, List.class)
                    .get(this.internal);

            for (Biome biome : biomeBases) {
                biomes.add(CraftBlock.biomeBaseToBiome(this.biomeRegistry, biome));
            }
            return biomes.build();
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access structure biomes", e);
        }


    }

    @Override
    public org.bukkit.block.Biome getZoomedOutBiome(int x, int y, int z) {
        return CraftBlock.biomeBaseToBiome(this.biomeRegistry, internal.getNoiseBiome(x, y, z));
    }

}
