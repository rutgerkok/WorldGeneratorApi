package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.Version;
import nl.rutgerkok.worldgeneratorapi.WorldGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldGeneratorApi;
import nl.rutgerkok.worldgeneratorapi.WorldGeneratorBuilder;
import nl.rutgerkok.worldgeneratorapi.WorldRef;
import nl.rutgerkok.worldgeneratorapi.property.PropertyRegistry;

public class WorldGeneratorApiImpl extends JavaPlugin implements WorldGeneratorApi, Listener {

    private final Map<UUID, WorldGeneratorImpl> worldGenerators = new HashMap<>();
    private final PropertyRegistry propertyRegistry = new PropertyRegistryImpl();

    @Override
    public WorldGeneratorBuilder buildTerrainGenerator(WorldRef world, Function<World, BaseChunkGenerator> base) {
        return new WorldGeneratorBuilderImpl(this, world, base);
    }

    @Override
    public Version getApiVersion() {
        return new VersionImpl(getDescription().getVersion());
    }

    @Override
    public WorldGenerator getForWorld(World world) {
        return worldGenerators.computeIfAbsent(world.getUID(), uuid -> new WorldGeneratorImpl(world));
    }

    @Override
    public PropertyRegistry getPropertyRegistry() {
        return propertyRegistry ;
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        this.worldGenerators.remove(event.getWorld().getUID());
    }

}
