package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.Objects;

import javax.annotation.Nullable;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;

import net.minecraft.server.v1_13_R2.ChunkGenerator;
import net.minecraft.server.v1_13_R2.ChunkProviderServer;
import net.minecraft.server.v1_13_R2.ChunkTaskScheduler;
import net.minecraft.server.v1_13_R2.WorldChunkManager;
import net.minecraft.server.v1_13_R2.WorldServer;
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
        this.world = world;
        this.worldRef = WorldRef.of(world);
    }

    @Override
    public BaseChunkGenerator getBaseChunkGenerator() throws UnsupportedOperationException {
        InjectedChunkGenerator injected = this.injected;
        if (injected == null) {
            // In the future, it would be interesting to somehow extract this from the world
            throw new UnsupportedOperationException("I don't know what base chunk generator has been set for world \""
                    + world.getName() + "\" - Minecraft itself or some other plugin is used as the world generator.");
        }
        return injected.getBaseChunkGenerator();
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
            throw new UnsupportedOperationException("At the moment, it is required to"
                    + " set a custom base chunk generator before decorations"
                    + " can be added. It is not possible to modify the default"
                    + " Minecraft generator yet.");
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

    @Override
    public void setBaseChunkGenerator(BaseChunkGenerator base) {
        Objects.requireNonNull(base, "base");
        InjectedChunkGenerator injected = this.injected;
        if (injected == null) {

            // Need to inject ourselves into the world
            injected = new InjectedChunkGenerator(getWorldHandle(), base);
            ChunkProviderServer chunkProvider = getWorldHandle().getChunkProviderServer();
            try {
                Field chunkGeneratorField = ReflectionUtil.getFieldOfType(chunkProvider, ChunkGenerator.class);
                chunkGeneratorField.set(chunkProvider, injected);

                Field chunkTaskSchedulerField = ReflectionUtil.getFieldOfType(chunkProvider, ChunkTaskScheduler.class);
                ChunkTaskScheduler scheduler = (ChunkTaskScheduler) chunkTaskSchedulerField.get(chunkProvider);
                chunkGeneratorField = ReflectionUtil.getFieldOfType(scheduler, ChunkGenerator.class);
                chunkGeneratorField.set(scheduler, injected);

                this.injected = injected;
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to inject world generator", e);
            }
        } else {
            injected.setBaseChunkGenerator(base);
        }
    }

}
