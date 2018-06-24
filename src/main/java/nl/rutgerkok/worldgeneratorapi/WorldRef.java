package nl.rutgerkok.worldgeneratorapi;

import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nullable;

import org.bukkit.World;

/**
 * Holds the UUID and name of a world. Useful if you want to keep a reference to
 * the world, but not prevent it from unloading.
 *
 */
public final class WorldRef {

    /**
     * Creates a reference to the given world.
     * @param world The world.
     * @return The reference.
     */
    public static WorldRef of(World world) {
        return new WorldRef(world.getName());
    }

    /**
     * Creates a reference to the world with the given name. The actual existance of
     * the world is not checked.
     *
     * @param name
     *            The name, case insensitive.
     * @return A reference to the world.
     */
    public static WorldRef ofName(String name) {
        return new WorldRef(name);
    }

    private final String name;

    private WorldRef(String name) {
        this.name = Objects.requireNonNull(name, "name").toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        WorldRef other = (WorldRef) obj;
        if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the name of this world.
     *
     * @return The name, always in lowercase.
     */
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Checks if the given Minecraft world matches this world reference.
     * @param world The Minecraft world.
     * @return True if the world names match, false otherwise.
     */
    public boolean matches(World world) {
        return world.getName().equalsIgnoreCase(this.name);
    }

    @Override
    public String toString() {
        return "WorldRef [name=" + name + "]";
    }

}
