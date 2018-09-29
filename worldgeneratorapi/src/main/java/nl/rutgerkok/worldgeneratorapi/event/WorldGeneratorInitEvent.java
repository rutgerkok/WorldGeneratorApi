package nl.rutgerkok.worldgeneratorapi.event;

import java.util.function.Consumer;

import org.bukkit.event.HandlerList;
import org.bukkit.event.world.WorldEvent;

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
 * useful if you want your plugin to be activated using the bukkit.yml file/any
 * multiworld plugin (like Multiverse). To use this event, you'll need to
 *
 */
public class WorldGeneratorInitEvent extends WorldEvent {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    private final WorldGenerator worldGenerator;

    public WorldGeneratorInitEvent(WorldGenerator worldGenerator) {
        super(worldGenerator.getWorld());
        this.worldGenerator = worldGenerator;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }

}
