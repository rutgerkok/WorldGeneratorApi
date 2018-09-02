package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import nl.rutgerkok.worldgeneratorapi.Version;
import nl.rutgerkok.worldgeneratorapi.WorldGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldGeneratorApi;
import nl.rutgerkok.worldgeneratorapi.WorldRef;
import nl.rutgerkok.worldgeneratorapi.event.WorldGeneratorInitEvent;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.DummyBukkitChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.property.PropertyRegistry;

public class WorldGeneratorApiImpl extends JavaPlugin implements WorldGeneratorApi, Listener {

    private final Map<UUID, WorldGeneratorImpl> worldGenerators = new HashMap<>();
    private final PropertyRegistry propertyRegistry = new PropertyRegistryImpl();
    private final Map<WorldRef, Consumer<WorldGenerator>> worldGeneratorModifiers = new HashMap<>();

    @Override
    public ChunkGenerator createCustomGenerator(WorldRef world, Consumer<WorldGenerator> consumer) {
        this.worldGeneratorModifiers.putIfAbsent(world, consumer);
        return new DummyBukkitChunkGenerator(this);
    }

    @Override
    public Version getApiVersion() {
        return new VersionImpl(getDescription().getVersion());
    }

    @Override
    public WorldGenerator getForWorld(World world) {
        return worldGenerators.computeIfAbsent(world.getUID(), uuid -> {
            // Initialize world generator
            WorldGeneratorImpl worldGenerator = new WorldGeneratorImpl(world);

            // Allow registered plugin to modify the world
            Consumer<WorldGenerator> worldGeneratorModifier = this.worldGeneratorModifiers
                    .get(worldGenerator.getWorldRef());
            if (worldGeneratorModifier != null) {
                worldGeneratorModifier.accept(worldGenerator);
            }

            // Allow other plugins to modify the world
            getServer().getPluginManager().callEvent(new WorldGeneratorInitEvent(worldGenerator));

            return worldGenerator;
        });
    }

    @Override
    public PropertyRegistry getPropertyRegistry() {
        return propertyRegistry;
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        getForWorld(event.getWorld()); // Force initialization
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        this.worldGenerators.remove(event.getWorld().getUID());
    }

}
