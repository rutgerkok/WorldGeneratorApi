package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_16_R2.block.CraftBlock;

import com.google.common.collect.ImmutableSet;

import net.minecraft.server.v1_16_R2.BiomeBase;
import net.minecraft.server.v1_16_R2.IRegistry;
import net.minecraft.server.v1_16_R2.WorldChunkManager;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;

/**
 * Wraps a vanilla biome generator into a WorldGeneratorApi one.
 */
public final class BiomeGeneratorImpl implements BiomeGenerator {

    public static final Field STRUCTURE_FIELD;

    static {
        try {
            STRUCTURE_FIELD = WorldChunkManager.class.getDeclaredField("c");
            STRUCTURE_FIELD.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to get structure field", e);
        }
    }

    private final IRegistry<BiomeBase> biomeRegistry;
    final WorldChunkManager internal;

    public BiomeGeneratorImpl(IRegistry<BiomeBase> biomeRegistry, WorldChunkManager worldChunkManager) {
        if (worldChunkManager instanceof InjectedBiomeGenerator) {
            // Not allowed - the injected biome generator itself wraps a BiomeGenerator
            throw new IllegalArgumentException("double wrapping of biome generator");
        }
        this.biomeRegistry = Objects.requireNonNull(biomeRegistry, "biomeRegistry");
        this.internal = Objects.requireNonNull(worldChunkManager, "worldChunkManager");
    }

    @Override
    public ImmutableSet<Biome> getStructureBiomes() {
        ImmutableSet.Builder<Biome> biomes = ImmutableSet.builder();

        try {
            @SuppressWarnings("unchecked")
            List<BiomeBase> biomeBases = (List<BiomeBase>) ReflectionUtil.getFieldByName(this.internal, "d")
                    .get(this.internal);

            for (BiomeBase biome : biomeBases) {
                biomes.add(CraftBlock.biomeBaseToBiome(this.biomeRegistry, biome));
            }
            return biomes.build();
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access structure biomes", e);
        }


    }

    @Override
    public Biome getZoomedOutBiome(int x, int y, int z) {
        return CraftBlock.biomeBaseToBiome(this.biomeRegistry, internal.getBiome(x, y, z));
    }

}
