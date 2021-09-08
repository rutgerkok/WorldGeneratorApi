package nl.rutgerkok.worldgeneratorapi.event;

import java.util.function.Consumer;

import org.bukkit.event.HandlerList;
import org.bukkit.event.world.WorldEvent;
import org.bukkit.event.world.WorldInitEvent;

import nl.rutgerkok.worldgeneratorapi.WorldGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldGeneratorApi;
import nl.rutgerkok.worldgeneratorapi.WorldRef;

/**
 * Called when a world is being initialized. Plugins wishing to modify the world
 * generation of a world can listen to this event, and make their changes to the
 * world generator returned by {@link #getWorldGenerator()}.
 *
 * <p>
 * However, an alternative is to use
 * {@link WorldGeneratorApi#createCustomGenerator(WorldRef, Consumer)}. This is
 * useful if you want your plugin to be activated using the bukkit.yml file or
 * any multiworld plugin (like Multiverse).
 *
 * @since 0.2
 * @deprecated Use {@link WorldInitEvent}.
 */
@Deprecated(forRemoval = true)
public class WorldGeneratorInitEvent extends WorldEvent {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    private final WorldGenerator worldGenerator;

    /**
     * Constructs the event. This is normally done by WorldGeneratorApi.
     *
     * @param worldGenerator
     *            The world generator that will be modified.
     * @since 0.2
     */
    public WorldGeneratorInitEvent(WorldGenerator worldGenerator) {
        super(worldGenerator.getWorld());
        this.worldGenerator = worldGenerator;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Gets the world generator, ready for you to modify.
     *
     * @return The world generator.
     * @seince 0.2
     */
    public WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }

}
