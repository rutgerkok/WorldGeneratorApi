package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.generator.CustomWorldChunkManager;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Wraps a Minecraft biome provider into a Bukkit one.
 */
public class BiomeProviderImpl extends BiomeProvider {

    /**
     * Converts a Bukkit biome generator into a Minecraft one. Avoids
     * double-wrapping if possible.
     *
     * @param world
     *            The world the biome provider will be used for. (Minecraft biomes
     *            can be specific to a world.)
     * @param provider
     *            The Bukkit biome generator.
     * @return The Minecraft biome source.
     */
    public static BiomeSource bukkitToMinecraft(ServerLevel world, BiomeProvider provider) {
        Registry<Biome> worldBiomeRegistry = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);

        if (provider instanceof BiomeProviderImpl impl) {
            // Found an underlying Minecraft biome generator, check if it's compatible
            if (impl.registry.equals(worldBiomeRegistry)) {
                // Yes! We can directly return that, no need for two rounds of conversion
                return impl.biomeSource;
            }
        }

        return new CustomWorldChunkManager(world.getWorld(), provider, worldBiomeRegistry);
    }

    /**
     * Converts a Minecraft biome generator into a Bukkit one. Avoids
     * double-wrapping.
     *
     * @param world
     *            The world the biome source is from. (Minecraft biomes can be
     *            specific to a world.)
     * @param chunkGenerator
     *            The Minecraft biome generator.
     * @return The Bukkit biome source.
     */
    public static BiomeProvider minecraftToBukkit(ServerLevel world, ChunkGenerator chunkGenerator) {
        BiomeSource biomeSource = chunkGenerator.getBiomeSource();
        if (biomeSource instanceof CustomWorldChunkManager worldChunkManager) {
            // Just return the BiomeProvider stored inside the CustomWorldChunkManager
            // Dig it up using reflection
            try {
                return (BiomeProvider) ReflectionUtil.getFieldOfType(worldChunkManager, BiomeProvider.class)
                        .get(worldChunkManager);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to get BiomeProvider from CustomWorldChunkManager", e);
            }
        }

        // Ok, we need to wrap
        Registry<Biome> worldBiomeRegistry = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        return new BiomeProviderImpl(chunkGenerator.getBiomeSource(), chunkGenerator.climateSampler(),
                worldBiomeRegistry);
    }

    private final BiomeSource biomeSource;
    private final Registry<Biome> registry;
    private final Sampler sampler;

    BiomeProviderImpl(BiomeSource biomeSource, Sampler sampler, Registry<Biome> registry) {
        this.biomeSource = Objects.requireNonNull(biomeSource, "biomeSource");
        this.sampler = Objects.requireNonNull(sampler, "sampler");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public org.bukkit.block.Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
        return CraftBlock.biomeBaseToBiome(registry, biomeSource.getNoiseBiome(x >> 2, y >> 2, z >> 2, sampler));
    }

    @Override
    public List<org.bukkit.block.Biome> getBiomes(WorldInfo worldInfo) {
        List<org.bukkit.block.Biome> possibleBiomes = new ArrayList<>();
        for (Holder<Biome> biome : biomeSource.possibleBiomes()) {
            possibleBiomes.add(CraftBlock.biomeBaseToBiome(registry, biome));
        }
        return possibleBiomes;
    }
}
