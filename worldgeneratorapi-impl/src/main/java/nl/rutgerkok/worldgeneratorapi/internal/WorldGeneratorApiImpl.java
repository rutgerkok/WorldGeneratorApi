package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.Objects;

import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.generator.CustomChunkGenerator;
import org.bukkit.event.Listener;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.java.JavaPlugin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseNoiseProvider;
import nl.rutgerkok.worldgeneratorapi.BasePopulator;
import nl.rutgerkok.worldgeneratorapi.Version;
import nl.rutgerkok.worldgeneratorapi.WorldGeneratorApi;
import nl.rutgerkok.worldgeneratorapi.internal.command.CommandHandler;
import nl.rutgerkok.worldgeneratorapi.property.PropertyRegistry;

public class WorldGeneratorApiImpl extends JavaPlugin implements WorldGeneratorApi, Listener {


    private final PropertyRegistry propertyRegistry = new PropertyRegistryImpl();

    @Override
    public BasePopulator createBasePopulatorFromNoiseFunction(BaseNoiseProvider noiseProvider) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public Version getApiVersion() {
        return new VersionImpl(getDescription().getVersion());
    }

    @Override
    public BiomeProvider getBiomeProvider(WorldInfo world) throws IllegalStateException {
        if (!(world instanceof CraftWorld)) {
            world = getServer().getWorld(world.getUID());
        }

        if (world instanceof CraftWorld craftWorld) {
            ServerLevel serverLevel = craftWorld.getHandle();
            ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
            return BiomeProviderImpl.minecraftToBukkit(serverLevel, chunkGenerator);
        }

        throw new IllegalStateException("World not yet loaded");
    }



    @Override
    public PropertyRegistry getPropertyRegistry() {
        return propertyRegistry;
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        redirectCommand(getCommand("worldgeneratorapi"), new CommandHandler(propertyRegistry));
    }

    private void redirectCommand(PluginCommand command, TabExecutor executor) {
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    @Override
    public void setBiomeProvider(World world, BiomeProvider biomeProvider) {
        Objects.requireNonNull(biomeProvider, "biomeProvider");

        // First, set in CraftWorld
        CraftWorld craftWorld = (CraftWorld) world;
        try {
            ReflectionUtil.getFieldOfType(craftWorld, BiomeProvider.class).set(craftWorld, biomeProvider);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to inject biome provider into world", e);
        }

        // Next, set in Minecraft's ChunkGenerator
        BiomeSource biomeSource = BiomeProviderImpl.bukkitToMinecraft(craftWorld.getHandle(), biomeProvider);
        net.minecraft.world.level.chunk.ChunkGenerator chunkGenerator;
        chunkGenerator = craftWorld.getHandle().getChunkSource().getGenerator();

        // Bukkit's chunk generator uses a delegate field, in which the biome provider
        // is stored
        if (chunkGenerator instanceof CustomChunkGenerator) {
            try {
                chunkGenerator = (net.minecraft.world.level.chunk.ChunkGenerator) ReflectionUtil
                        .getFieldOfType(chunkGenerator, net.minecraft.world.level.chunk.ChunkGenerator.class)
                        .get(chunkGenerator);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to get CustomChunkGenerator.delegate field");
            }
        }

        for (Field field : ReflectionUtil.getAllFieldsOfType(chunkGenerator.getClass(), BiomeSource.class)) {
            try {
                field.set(chunkGenerator, biomeSource);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to inject biome source into chunkGenerator." + field.getName(), e);
            }
        }

        // Test if it worked (better to catch errors early)
        if (this.getBiomeProvider(world) != biomeProvider) {
            throw new RuntimeException("Failed to inject biome provider; unknown reason");
        }
    }

}
