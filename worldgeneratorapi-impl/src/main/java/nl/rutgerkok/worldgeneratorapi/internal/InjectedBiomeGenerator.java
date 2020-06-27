package nl.rutgerkok.worldgeneratorapi.internal;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Set;

import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBlock;

import com.mojang.serialization.Codec;

import net.minecraft.server.v1_16_R1.BiomeBase;
import net.minecraft.server.v1_16_R1.WorldChunkManager;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;

public class InjectedBiomeGenerator extends WorldChunkManager {

    private static List<BiomeBase> toBiomeBase(Set<Biome> biomes) {
        return biomes.stream().map(CraftBlock::biomeToBiomeBase).collect(toList());
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
    public static WorldChunkManager wrapOrUnwrap(BiomeGenerator biomeGenerator) {
        if (biomeGenerator instanceof BiomeGeneratorImpl) {
            return ((BiomeGeneratorImpl) biomeGenerator).internal;
        }
        return new InjectedBiomeGenerator(biomeGenerator);
    }

    private final BiomeGenerator biomeGenerator;

    public InjectedBiomeGenerator(BiomeGenerator biomeGenerator) {
        super(toBiomeBase(biomeGenerator.getStructureBiomes()));

        if (biomeGenerator instanceof BiomeGeneratorImpl) {
            throw new IllegalArgumentException("Double wrapping of biome generator");
        }

        this.biomeGenerator = biomeGenerator; // Null check not necessary - was done in first line
    }

    @Override
    protected Codec<? extends WorldChunkManager> a() {
        throw new UnsupportedOperationException("Custom biome generators be stored in a config file");
    }

    @Override
    public BiomeBase getBiome(int x, int y, int z) {
        return CraftBlock.biomeToBiomeBase(biomeGenerator.getZoomedOutBiome(x, y, z));
    }
}
