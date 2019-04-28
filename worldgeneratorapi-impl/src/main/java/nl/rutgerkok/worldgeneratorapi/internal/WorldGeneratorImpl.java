package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.Objects;

import javax.annotation.Nullable;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;

import net.minecraft.server.v1_14_R1.ChunkGenerator;
import net.minecraft.server.v1_14_R1.ChunkProviderServer;
import net.minecraft.server.v1_14_R1.WorldChunkManager;
import net.minecraft.server.v1_14_R1.WorldServer;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldRef;
import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.InjectedChunkGenerator;

final class WorldGeneratorImpl implements WorldGenerator {

    private @Nullable InjectedChunkGenerator injected;
    private final World world;
    private final WorldRef worldRef;

    WorldGeneratorImpl(World world) {
        this.world = Objects.requireNonNull(world, "world");
        this.worldRef = WorldRef.of(world);
    }

    @Override
    public BaseChunkGenerator getBaseChunkGenerator() throws UnsupportedOperationException {
        return BaseChunkGeneratorImpl.fromMinecraft(world);
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
            replaceChunkGenerator(BaseChunkGeneratorImpl.fromMinecraft(world));
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

    private Class<?> nmsClass(String simpleName) throws ClassNotFoundException {
        // Returns a class in the net.mineraft.server package
        Class<?> exampleNmsClass = ChunkGenerator.class;
        String name = exampleNmsClass.getName().replace(exampleNmsClass.getSimpleName(), simpleName);
        return Class.forName(name);
    }

    /**
     * Injects {@link InjectedChunkGenerator} into the world, so that we can
     * customize how blocks are generated.
     * 
     * @param base
     *            Base chunk generator.
     */
    private void replaceChunkGenerator(BaseChunkGenerator base) {
        InjectedChunkGenerator injected;
        // Need to inject ourselves into the world
        injected = new InjectedChunkGenerator(getWorldHandle(), base);
        ChunkProviderServer chunkProvider = getWorldHandle().getChunkProvider();
        try {
            Field chunkGeneratorField = ReflectionUtil.getFieldOfType(chunkProvider, ChunkGenerator.class);
            chunkGeneratorField.set(chunkProvider, injected);

            try {
                Field chunkTaskSchedulerField = ReflectionUtil.getFieldOfType(chunkProvider,
                        nmsClass("ChunkTaskScheduler"));
                Object scheduler = chunkTaskSchedulerField.get(chunkProvider);
                chunkGeneratorField = ReflectionUtil.getFieldOfType(scheduler, ChunkGenerator.class);
                chunkGeneratorField.set(scheduler, injected);
            } catch (ClassNotFoundException e) {
                // Ignore, we're not on Paper but on Spigot
            }

            this.injected = injected;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to inject world generator", e);
        }
    }

    @Override
    public void setBaseChunkGenerator(BaseChunkGenerator base) {
        Objects.requireNonNull(base, "base");
        InjectedChunkGenerator injected = this.injected;
        if (injected == null) {
            replaceChunkGenerator(base);
        } else {
            injected.setBaseChunkGenerator(base);
        }
    }

}
