package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import nl.rutgerkok.worldgeneratorapi.WorldGeneratorApi;
import nl.rutgerkok.worldgeneratorapi.WorldRef;

/**
 * An internal class to satisfy the Bukkit API, so that plugins can still
 * properly override {@code JavaPlugin.getDefaultWorldGenerator(...)}. It is
 * normally not used, as the {@link WorldInitEvent} handler normally replaces
 * the complete Bukkit implementation with its own. In cases where that event
 * fires too late (which happens if the server generates the initial chunks of a
 * brand new world), {@link #getDefaultPopulators(World)} takes over this task.
 *
 */
public final class DummyBukkitChunkGenerator extends ChunkGenerator {

    /**
     * Marks the chunk generator as async-safe. See the following link for info:
     * https://github.com/PaperMC/Paper/commit/e91eb53286395dcb2932e84c7240aa0e1baa1447
     */
    public final boolean PAPER_ASYNC_SAFE = true;

    private final WorldGeneratorApi impl;
    private final WorldRef worldRef;

    public DummyBukkitChunkGenerator(WorldGeneratorApi impl, WorldRef worldRef) {
        this.impl = Objects.requireNonNull(impl, "impl");
        this.worldRef = Objects.requireNonNull(worldRef, "worldRef");
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        throw new UnsupportedOperationException("This is a dummy class, used"
                + " because a custom world generator was registered. However,"
                + " either the author forgot to use"
                + " WorldGenerator.setBaseTerrainGenerator(...), or a plugin"
                + " is directly calling this method.\n\nSee"
                + " https://github.com/rutgerkok/WorldGeneratorApi/wiki/Dummy-class-error"
                + " for more information.");
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

    /**
     * Gets the world this chunk generator was originally created for.
     * @return The world.
     */
    public WorldRef getWorldRef() {
        return worldRef;
    }

    @Override
    public boolean isParallelCapable() {
        return true;
    }


}
