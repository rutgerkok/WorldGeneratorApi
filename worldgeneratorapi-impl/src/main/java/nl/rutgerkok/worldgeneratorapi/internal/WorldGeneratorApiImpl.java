package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import nl.rutgerkok.worldgeneratorapi.Version;
import nl.rutgerkok.worldgeneratorapi.WorldGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldGeneratorApi;
import nl.rutgerkok.worldgeneratorapi.WorldRef;
import nl.rutgerkok.worldgeneratorapi.event.WorldGeneratorInitEvent;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.DummyBukkitChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.command.CommandHandler;
import nl.rutgerkok.worldgeneratorapi.property.PropertyRegistry;

public class WorldGeneratorApiImpl extends JavaPlugin implements WorldGeneratorApi, Listener {

    private static final Field SERVER_WORLDS_FIELD;
    static {
        try {
            SERVER_WORLDS_FIELD = CraftServer.class.getDeclaredField("worlds");
            SERVER_WORLDS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            throw new RuntimeException("Failed to access CraftServer.worlds. Incompatible server?", e);
        }
    }

    private final Map<UUID, WorldGeneratorImpl> worldGenerators = new HashMap<>();
    private final PropertyRegistry propertyRegistry = new PropertyRegistryImpl();
    private final Map<WorldRef, Consumer<WorldGenerator>> worldGeneratorModifiers = new HashMap<>();
    private Map<String, World> oldWorldsMap;

    @Override
    public ChunkGenerator createCustomGenerator(WorldRef world, Consumer<WorldGenerator> consumer) {
        this.worldGeneratorModifiers.putIfAbsent(world, consumer);
        return new DummyBukkitChunkGenerator(this, world);
    }

    private void disableWorldGenerators() {
        for (WorldGeneratorImpl worldGenerator : this.worldGenerators.values()) {
            worldGenerator.reset();
        }
        this.worldGenerators.clear();
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
                try {
                    worldGenerator.getBaseChunkGenerator();
                } catch (UnsupportedOperationException e) {
                    throw new IllegalStateException(
                            "The custom world generator forgot to set a base"
                                    + " chunk generator. If the custom world generator"
                                    + " does not intend to replace the base terrain, it"
                                    + " should modify the world using the"
                                    + " WorldGeneratorInitEvent instead of using"
                                    + " JavaPlugin.getDefaultWorldGenerator");
                }
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

    private void injectWorldAddListener() {
        // Listening to the WorldInitEvent would be easier, but unfortunately that event
        // fires after the first chunks have been generated.
        // See https://github.com/rutgerkok/WorldGeneratorApi/issues/13
        Server server = this.getServer();
        try {
            @SuppressWarnings("unchecked")
            Map<String, World> oldMap = (Map<String, World>) SERVER_WORLDS_FIELD.get(server);
            this.oldWorldsMap = oldMap;
            PutListeningMap<String, World> wrapper = new PutListeningMap<>(oldMap);
            wrapper.addListener((name, world) -> onWorldAdd(world));
            SERVER_WORLDS_FIELD.set(server, wrapper);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to change CraftServer.world", e);
        }
    }

    @Override
    public void onDisable() {
        disableWorldGenerators();
        restoreOriginalWorldsMap();
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        injectWorldAddListener();

        redirectCommand(getCommand("worldgeneratorapi"),
                new CommandHandler(this::reloadWorldGenerators, propertyRegistry));
    }

    public void onWorldAdd(World world) {
        ChunkGenerator chunkGenerator = world.getGenerator();
        if (chunkGenerator instanceof DummyBukkitChunkGenerator) {
            WorldRef worldRef = ((DummyBukkitChunkGenerator) chunkGenerator).getWorldRef();
            if (!worldRef.matches(world)) {
                // Generator from another world was copied to a new world
                // WorldEdit does this. Initialize for that world
                World copiedFrom = this.getServer().getWorld(worldRef.getName());
                if (copiedFrom == null) {
                    return;
                }

                // TODO: record settings from other world, reapply here
            }
        }
        getForWorld(world); // Force initialization
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        this.worldGenerators.remove(event.getWorld().getUID());
    }

    private void redirectCommand(PluginCommand command, TabExecutor executor) {
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    /**
     * Reloads all world generators: resets them, re-applies the modifiers and calls
     * the init event again.
     */
    private void reloadWorldGenerators() {
        disableWorldGenerators();
        for (World world : this.getServer().getWorlds()) {
            getForWorld(world);
        }
    }

    private void restoreOriginalWorldsMap() {
        if (this.oldWorldsMap != null) {
            try {
                SERVER_WORLDS_FIELD.set(getServer(), this.oldWorldsMap);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to restore CraftServer.world", e);
            }
        }
    }

}
