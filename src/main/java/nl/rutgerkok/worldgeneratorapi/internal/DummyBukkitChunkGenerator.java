package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

final class DummyBukkitChunkGenerator extends ChunkGenerator {

    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        throw new UnsupportedOperationException("This is a dummy class, used"
                + " because a custom world generator was registered, but no"
                + " base chunk generator has been set. Please use"
                + " WorldGenerator.setBaseChunkGenerator(...).");
    }

}
