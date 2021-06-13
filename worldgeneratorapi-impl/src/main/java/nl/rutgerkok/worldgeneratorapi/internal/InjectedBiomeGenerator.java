package nl.rutgerkok.worldgeneratorapi.internal;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlock;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldGeneratorApi;

public class InjectedBiomeGenerator extends BiomeSource {

    private final static Function<? super Supplier<Biome>, ? extends InjectedBiomeGenerator> BIOME_TO_GENERATOR = biome -> new InjectedBiomeGenerator();
    private final static Function<? super InjectedBiomeGenerator, ? extends Supplier<Biome>> GENERATOR_TO_BIOME = generator -> () -> BuiltinRegistries.BIOME
            .get(Biomes.OCEAN);

    /**
     * Apparently, sometimes we need to serialize the biome generator. We cannot do
     * this, as this is not how our API is designed. (We don't register our biome
     * generators.) Instead, we serialize as if we are a biome generator that
     * generates only oceans.
     */
    private static final Codec<? extends InjectedBiomeGenerator> DUMMY_CODEC = Biome.CODEC
            .fieldOf(
            "[" + WorldGeneratorApi.class.getSimpleName()
                    + "] Custom biome generators cannot be stored in the level.dat, please ignore this error")
            .xmap(BIOME_TO_GENERATOR, GENERATOR_TO_BIOME)
            .stable().codec();

    private static List<Biome> toBiomeBase(Registry<Biome> biomeRegistry, Set<org.bukkit.block.Biome> biomes) {
        return biomes.stream()
                .map(biome -> CraftBlock.biomeToBiomeBase(biomeRegistry, biome))
                .collect(toList());
    }

    /**
     * If this is a custom biome generator, it is wrapped to conform to Minecraft's
     * interface. If it is instead a wrapped Minecraft biome generator, it is
     * unwrapped.
     *
     * @param biomeGenerator
     *            The biome generator.
     * @return A Minecraft-compatible biome generator.
     */
    public static BiomeSource wrapOrUnwrap(Registry<Biome> registry, BiomeGenerator biomeGenerator) {
        if (biomeGenerator instanceof BiomeGeneratorImpl) {
            // Already wrapping a WorldChunkManager
            BiomeGeneratorImpl biomeGeneratorImpl = (BiomeGeneratorImpl) biomeGenerator;
            if (biomeGeneratorImpl.biomeRegistry == registry) {
                // Uses the same biome registry - safe to use that instance directly
                return biomeGeneratorImpl.internal;
            }
        }
        return new InjectedBiomeGenerator(registry, biomeGenerator);
    }

    private final BiomeGenerator biomeGenerator;
    private final Registry<Biome> biomeRegistry;

    /**
     * Constructor only used for deserialization. Just provides a dummy biome
     * generator, as the biome generator plugin should inject its own biome
     * generator.
     */
    private InjectedBiomeGenerator() {
        // Dummy constructor
        super(Arrays.asList(BuiltinRegistries.BIOME.get(Biomes.OCEAN)));
        this.biomeRegistry = BuiltinRegistries.BIOME;
        this.biomeGenerator = (x, y, z) -> org.bukkit.block.Biome.OCEAN;
    }

    public InjectedBiomeGenerator(Registry<Biome> biomeRegistry, BiomeGenerator biomeGenerator) {
        super(toBiomeBase(biomeRegistry, biomeGenerator.getStructureBiomes()));

        if (biomeGenerator instanceof BiomeGeneratorImpl
                && ((BiomeGeneratorImpl) biomeGenerator).biomeRegistry == biomeRegistry) {
            throw new IllegalArgumentException("Double wrapping of biome generator (that uses the same biomeRegistry)");
        }

        this.biomeRegistry = Objects.requireNonNull(biomeRegistry, "biomeRegistry");
        this.biomeGenerator = biomeGenerator; // Null check not necessary - was done in first line
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return DUMMY_CODEC;
    }

    @Override
    public Biome getNoiseBiome(int x, int y, int z) {
        return CraftBlock.biomeToBiomeBase(biomeRegistry,
                biomeGenerator.getZoomedOutBiome(x, y, z));
    }

    @Override
    public BiomeSource withSeed(long seed) {
        return this; // We cannot change the seed in our biome generators
    }
}
