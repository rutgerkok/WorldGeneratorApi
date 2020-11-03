package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;

import net.minecraft.server.v1_16_R3.BiomeBase;
import net.minecraft.server.v1_16_R3.ChunkGenerator;
import net.minecraft.server.v1_16_R3.ChunkProviderServer;
import net.minecraft.server.v1_16_R3.GeneratorSettingBase;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.WorldChunkManager;
import net.minecraft.server.v1_16_R3.WorldServer;
import net.minecraft.server.v1_16_R3.StructureSettings;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import net.minecraft.server.v1_16_R3.ResourceKey;
import net.minecraft.server.v1_16_R3.Blocks;
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

    private static GeneratorSettingBase createDefaultNetherSettings() {
        // Equiv. to `a(e, a(new StructureSettings(false), Blocks.NETHERRACK.getBlockData(), Blocks.LAVA.getBlockData(), e.a()));`
        // in GeneratorSettingsBase
        try {
            Method meth1 = GeneratorSettingBase.class.getDeclaredMethod("a", StructureSettings.class,
                    IBlockData.class, IBlockData.class, MinecraftKey.class);
            Method meth2 = GeneratorSettingBase.class.getDeclaredMethod("a", ResourceKey.class,
                    GeneratorSettingBase.class);

            meth1.setAccessible(true);
            meth2.setAccessible(true);

            StructureSettings genStronghold = new StructureSettings(false);
            GeneratorSettingBase generatorSettings = (GeneratorSettingBase)meth1.invoke(null, genStronghold,
                    Blocks.NETHERRACK.getBlockData(), Blocks.LAVA.getBlockData(), GeneratorSettingBase.e.a());

            return (GeneratorSettingBase)meth2.invoke(null, GeneratorSettingBase.e, generatorSettings);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // If something goes wrong just default to Overworld Settings.
            e.printStackTrace();
            return createDefaultSettings();
        }
    }

    private static GeneratorSettingBase createDefaultEndSettings() {
        // Equiv. to `a(f, a(new StructureSettings(false), Blocks.END_STONE.getBlockData(), Blocks.AIR.getBlockData(), f.a(), true, true));`
        // in GeneratorSettingsBase
        try {
            Method meth1 = GeneratorSettingBase.class.getDeclaredMethod("a", StructureSettings.class,
                    IBlockData.class, IBlockData.class, MinecraftKey.class, boolean.class, boolean.class);
            Method meth2 = GeneratorSettingBase.class.getDeclaredMethod("a", ResourceKey.class,
                    GeneratorSettingBase.class);

            meth1.setAccessible(true);
            meth2.setAccessible(true);

            StructureSettings genStronghold = new StructureSettings(false);
            GeneratorSettingBase generatorSettings = (GeneratorSettingBase)meth1.invoke(null, genStronghold,
                    Blocks.END_STONE.getBlockData(), Blocks.AIR.getBlockData(), GeneratorSettingBase.f.a(), true, true);

            return (GeneratorSettingBase)meth2.invoke(null, GeneratorSettingBase.f, generatorSettings);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // If something goes wrong just default to Overworld Settings.
            e.printStackTrace();
            return createDefaultSettings();
        }
    }

    private static GeneratorSettingBase extractSettings(CraftWorld world) {
        // Get default settings based on environment
        // Not sure if there's a new way to extract the existing settings, which may be a better option, since
        // ChunkGenerator has no GeneratorSettingBase fields in it (anymore?)
        World.Environment env = world.getEnvironment();

        if (env.equals(World.Environment.THE_END)) {
            return createDefaultEndSettings();
        } else if (env.equals(World.Environment.NETHER)) {
            return createDefaultNetherSettings();
        } else {
            return createDefaultSettings();
        }
    }

    private static IRegistry<BiomeBase> getBiomeRegistry(WorldServer world) {
        return world.r().b(IRegistry.ay); // Copied from CraftWorld.getBiome
    }
    private @Nullable InjectedChunkGenerator injected;

    private final World world;

    private final WorldRef worldRef;

    /**
     * The original world generator before any changes were made. Will be restored
     * when {@link #reset()} is called.
     */
    private final ChunkGenerator oldChunkGenerator;

    /**
     * Cache of the vanilla biome generator. Assumes that the WorldChunkManager is never replaced outside of the {@link #setBiomeGenerator(BiomeGenerator)} or {@link #injectInternalChunkGenerator(ChunkGenerator)} methods.
     */
    private BiomeGeneratorImpl cachedVanillaWrapperBiomeGenerator = null;

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

        if (this.cachedVanillaWrapperBiomeGenerator == null) {
            WorldServer world = getWorldHandle();
            WorldChunkManager worldChunkManager = world
                    .getChunkProvider()
                    .getChunkGenerator()
                    .getWorldChunkManager();
            this.cachedVanillaWrapperBiomeGenerator = new BiomeGeneratorImpl(getBiomeRegistry(world),
                    worldChunkManager);
        }

        // Create a new one, based on the currently used biome generator
        return this.cachedVanillaWrapperBiomeGenerator;
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
        this.cachedVanillaWrapperBiomeGenerator = null; // Wipe out cache - we're replacing the chunk generator

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
        GeneratorSettingBase settings = extractSettings(world.getWorld());
        InjectedChunkGenerator injected = new InjectedChunkGenerator(worldChunkManager, getBiomeRegistry(world), base,
                seed, settings);

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
        Supplier<BiomeGenerator> biomeGenerator = this::getBiomeGenerator;
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

        this.cachedVanillaWrapperBiomeGenerator = null; // Wipe out cache - we're replacing the biome generator

        InjectedChunkGenerator injected = this.injected;
        if (injected == null) {
            replaceChunkGenerator(BaseTerrainGeneratorImpl.fromMinecraft(world));
        }
        this.injected.setBiomeGenerator(biomeGenerator);
    }

}
