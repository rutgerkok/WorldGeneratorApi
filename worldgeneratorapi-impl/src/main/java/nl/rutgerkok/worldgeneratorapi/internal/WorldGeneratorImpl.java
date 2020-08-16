package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.NoSuchElementException;
import java.util.Objects;

import javax.annotation.Nullable;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.generator.CustomChunkGenerator;

import net.minecraft.server.v1_16_R2.ChunkGenerator;
import net.minecraft.server.v1_16_R2.ChunkProviderServer;
import net.minecraft.server.v1_16_R2.GeneratorSettingBase;
import net.minecraft.server.v1_16_R2.WorldChunkManager;
import net.minecraft.server.v1_16_R2.WorldServer;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseNoiseGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseTerrainGenerator;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldRef;
import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.InjectedChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.NoiseToTerrainGenerator;

final class WorldGeneratorImpl implements WorldGenerator {

    private static GeneratorSettingBase createDefaultSettings() {
        return GeneratorSettingBase.i();
    }

    private static GeneratorSettingBase extractSettings(ChunkGenerator chunkGenerator, long seed) {
        try {
            // First, unwrap Bukkit generator if necessary
            if (chunkGenerator instanceof CustomChunkGenerator) {
                chunkGenerator = (ChunkGenerator) ReflectionUtil.getFieldOfType(chunkGenerator, ChunkGenerator.class)
                        .get(chunkGenerator);
            }

            // Then, extract the settings
            return (GeneratorSettingBase) ReflectionUtil.getFieldOfType(chunkGenerator, GeneratorSettingBase.class)
                    .get(chunkGenerator);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchElementException e) {
            // Get default settings
            return createDefaultSettings();
        }
    }

    private @Nullable InjectedChunkGenerator injected;
    private final World world;

    private final WorldRef worldRef;

    /**
     * The original world generator before any changes were made. Will be restored
     * when {@link #reset()} is called.
     */
    private final ChunkGenerator oldChunkGenerator;

    WorldGeneratorImpl(World world) {
        this.world = Objects.requireNonNull(world, "world");
        this.worldRef = WorldRef.of(world);
        this.oldChunkGenerator = this.getWorldHandle().getChunkProvider().getChunkGenerator();
    }

    @Override
    public BaseChunkGenerator getBaseChunkGenerator() throws UnsupportedOperationException {
        return getBaseTerrainGenerator();
    }

    @Override
    public BaseTerrainGenerator getBaseTerrainGenerator() throws UnsupportedOperationException {
        return BaseTerrainGeneratorImpl.fromMinecraft(world);
    }

    @Override
    public BiomeGenerator getBiomeGenerator() {
        InjectedChunkGenerator injected = this.injected;
        if (injected != null) {
            return injected.getBiomeGenerator();
        }

        // Create a new one, based on the currently used biome generator
        WorldChunkManager worldChunkManager = getWorldHandle()
                .getChunkProvider()
                .getChunkGenerator()
                .getWorldChunkManager();
        return new BiomeGeneratorImpl(worldChunkManager);
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public WorldDecorator getWorldDecorator() {
        InjectedChunkGenerator injected = this.injected;
        if (injected == null) {
            replaceChunkGenerator(BaseTerrainGeneratorImpl.fromMinecraft(world));
            return this.injected.worldDecorator;
        }
        return injected.worldDecorator;
    }

    private WorldServer getWorldHandle() {
        return ((CraftWorld) world).getHandle();
    }

    @Override
    public WorldRef getWorldRef() {
        return worldRef;
    }

    /**
     * Injects a nms.ChunkGenerator.
     *
     * @param injected
     *            The new ChunkGenerator.
     */
    private void injectInternalChunkGenerator(ChunkGenerator injected) {
        ChunkProviderServer chunkProvider = getWorldHandle().getChunkProvider();
        try {
            Field chunkGeneratorField = ReflectionUtil.getFieldOfType(chunkProvider, ChunkGenerator.class);
            chunkGeneratorField.set(chunkProvider, injected);

            // Also replace chunk generator in PlayerChunkMap
            chunkGeneratorField = ReflectionUtil.getFieldOfType(chunkProvider.playerChunkMap, ChunkGenerator.class);
            chunkGeneratorField.set(chunkProvider.playerChunkMap, injected);

            // Paper only: replace chunk generator in ChunkTaskScheduler
            try {
                Field chunkTaskSchedulerField = ReflectionUtil.getFieldOfType(chunkProvider,
                        nmsClass("ChunkTaskScheduler"));
                Object scheduler = chunkTaskSchedulerField.get(chunkProvider);
                chunkGeneratorField = ReflectionUtil.getFieldOfType(scheduler, ChunkGenerator.class);
                chunkGeneratorField.set(scheduler, injected);
            } catch (ClassNotFoundException e) {
                // Ignore, we're not on Paper but on Spigot
            }

            getWorldHandle().generator = null; // Clear out the custom generator
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to inject world generator", e);
        }
    }

    private Class<?> nmsClass(String simpleName) throws ClassNotFoundException {
        // Returns a class in the net.mineraft.server package
        Class<?> exampleNmsClass = ChunkGenerator.class;
        String name = exampleNmsClass.getName().replace(exampleNmsClass.getSimpleName(), simpleName);
        return Class.forName(name);
    }

    /**
     * Injects an {@link InjectedChunkGenerator} into the world, so that we can
     * customize how blocks are generated.
     *
     * @param base
     *            Base chunk generator.
     */
    private void replaceChunkGenerator(BaseTerrainGenerator base) {
        WorldServer world = this.getWorldHandle();
        ChunkGenerator chunkGenerator = world.getChunkProvider().getChunkGenerator();
        WorldChunkManager worldChunkManager = chunkGenerator.getWorldChunkManager();
        long seed = world.getSeed();
        GeneratorSettingBase settings = extractSettings(chunkGenerator, seed);
        InjectedChunkGenerator injected = new InjectedChunkGenerator(worldChunkManager, base, seed, settings);

        injectInternalChunkGenerator(injected);
        this.injected = injected;
    }

    /**
     * Resets all modifications done to the world generator, by any plugin.
     */
    public void reset() {
        if (this.injected == null) {
            return; // Nothing to reset
        }
        this.injected.resetBiomeGenerator();
        this.injectInternalChunkGenerator(this.oldChunkGenerator);
        this.injected = null;
    }

    @Override
    public void setBaseChunkGenerator(BaseChunkGenerator base) {
        this.setBaseTerrainGenerator(new BaseTerrainGenerator() {

            @Override
            public int getHeight(int x, int z, HeightType type) {
                return 65; // Whatever, we cannot know.
            }

            @Override
            public void setBlocksInChunk(GeneratingChunk chunk) {
                base.setBlocksInChunk(chunk);
            }
        });
    }

    @Override
    public BaseTerrainGenerator setBaseNoiseGenerator(BaseNoiseGenerator base) {
        BiomeGenerator biomeGenerator = this.getBiomeGenerator();
        WorldServer world = getWorldHandle();
        BaseTerrainGenerator generator = new NoiseToTerrainGenerator(world, world.getStructureManager(), biomeGenerator,
                base, world.getSeed());
        setBaseTerrainGenerator(generator);
        return generator;
    }

    @Override
    public void setBaseTerrainGenerator(BaseTerrainGenerator base) {
        Objects.requireNonNull(base, "base");
        InjectedChunkGenerator injected = this.injected;
        if (injected == null) {
            replaceChunkGenerator(base);
        } else {
            injected.setBaseChunkGenerator(base);
        }
    }

    @Override
    public void setBiomeGenerator(BiomeGenerator biomeGenerator) {
        Objects.requireNonNull(biomeGenerator, "biomeGenerator");
        InjectedChunkGenerator injected = this.injected;
        if (injected == null) {
            replaceChunkGenerator(BaseTerrainGeneratorImpl.fromMinecraft(world));
        }
        this.injected.setBiomeGenerator(biomeGenerator);
    }

}
