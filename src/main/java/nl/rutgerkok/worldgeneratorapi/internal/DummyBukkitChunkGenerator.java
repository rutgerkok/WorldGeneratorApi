package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import nl.rutgerkok.worldgeneratorapi.WorldGeneratorApi;

/**
 * An internal class to satisfy the Bukkit API, so that plugins can still
 * properly override {@code JavaPlugin.getDefaultWorldGenerator(...)}. It is
 * normally not used, as the {@link WorldInitEvent} handler normally replaces
 * the complete Bukkit implementation with its own. In cases where that event
 * fires too late (which happens if the server generates the initial chunks of a
 * brand new world), {@link #getDefaultPopulators(World)} takes over this task.
 *
 */
final class DummyBukkitChunkGenerator extends ChunkGenerator {

    private final WorldGeneratorApi impl;

    DummyBukkitChunkGenerator(WorldGeneratorApi impl) {
        this.impl = Objects.requireNonNull(impl);
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        throw new UnsupportedOperationException("This is a dummy class, used"
                + " because a custom world generator was registered, but no"
                + " base chunk generator has been set. Please use"
                + " WorldGenerator.setBaseChunkGenerator(...).");
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        // In cases where a brand new world is generated (as opposed to generating new
        // chunks for an existing world), the WorldInitEvent is fired after the first
        // few chunks have been generated. So this method has been overridden, as a way
        // to initialize the world generator earlier

        impl.getForWorld(world); // Force initialization

        return new ArrayList<>(); // We don't use Bukkit's populators
    }


}
