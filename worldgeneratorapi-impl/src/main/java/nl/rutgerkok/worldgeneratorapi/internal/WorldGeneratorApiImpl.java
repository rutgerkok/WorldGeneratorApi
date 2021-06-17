package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
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
import nl.rutgerkok.worldgeneratorapi.internal.command.CommandHandler;
import nl.rutgerkok.worldgeneratorapi.internal.recording.RecordingWorldGeneratorImpl;
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

    /**
     * If you create a new world, copying over {@link World#getGenerator()}, then by
     * default all the modifications made to the world generator aren't copied over.
     * This method takes care of that.
     *
     * @param world
     *            The world that is being loaded.
     * @return The world generator, if it needed to be copied over from another
     *         world (transferring only the settings), otherwise empty.
     */
    private Optional<WorldGeneratorImpl> checkForCopiedWorldGenerator(World world) {
        ChunkGenerator chunkGenerator = world.getGenerator();
        if (chunkGenerator instanceof DummyBukkitChunkGenerator) {
            WorldRef worldRef = ((DummyBukkitChunkGenerator) chunkGenerator).getWorldRef();
            if (!worldRef.matches(world)) {
                // Generator from another world was copied to a new world
                // WorldEdit does this. Initialize for that world
                World copiedFrom = this.getServer().getWorld(worldRef.getName());
                if (copiedFrom != null) {
                    WorldGenerator existing = Objects
                            .requireNonNull(worldGenerators.get(copiedFrom.getUID()), "existing");

                    // Record modifications done to the world generator
                    RecordingWorldGeneratorImpl recording = new RecordingWorldGeneratorImpl(existing);
                    this.worldGeneratorModifiers.getOrDefault(worldRef, empty -> {
                    }).accept(recording);
                    getServer().getPluginManager().callEvent(new WorldGeneratorInitEvent(recording));

                    // Apply to new world
                    WorldGeneratorImpl newWorldGenerator = new WorldGeneratorImpl(world);
                    recording.applyTo(newWorldGenerator);
                    return Optional.of(newWorldGenerator);
                }

            }
        }
        return Optional.empty();
    }

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
            Optional<WorldGeneratorImpl> copiedFromAnotherWorld = checkForCopiedWorldGenerator(world);
            if (copiedFromAnotherWorld.isPresent()) {
                return copiedFromAnotherWorld.get();
            }

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

    private void injectWorldRemovalListener() {
        // Just relying on WorldInitEvent would be easier, but unfortunately that event
        // is not called by WorldEdit, so proper initialization doesn't happen
        Server server = this.getServer();
        try {
            @SuppressWarnings("unchecked")
            Map<String, World> oldMap = (Map<String, World>) SERVER_WORLDS_FIELD.get(server);
            this.oldWorldsMap = oldMap;
            ChangeListeningMap<String, World> wrapper = new ChangeListeningMap<>(oldMap);
            wrapper.addListener((world, addition) -> onWorldAddOrRemove(world, addition));
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
        injectWorldRemovalListener();

        redirectCommand(getCommand("worldgeneratorapi"),
                new CommandHandler(this::reloadWorldGenerators, propertyRegistry));
    }

    public void onWorldAddOrRemove(World world, boolean addition) {
        if (addition) {
            // Force initialization
            getForWorld(world);
        } else {
            // Unload that world (WorldUnloadEvent is not called for WorldEdit regen temp
            // worlds)
            this.worldGenerators.remove(world.getUID());
        }
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        // Initialize world
        getForWorld(event.getWorld());
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
