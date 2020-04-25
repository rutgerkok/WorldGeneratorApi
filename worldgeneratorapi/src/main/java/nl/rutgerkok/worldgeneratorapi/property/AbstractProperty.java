package nl.rutgerkok.worldgeneratorapi.property;

import java.util.Objects;
import java.util.Optional;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;

import nl.rutgerkok.worldgeneratorapi.WorldRef;

/**
 * Represents some abstract property of an unknown type.
 *
 * @since 0.5
 */
public abstract class AbstractProperty implements Keyed {
    protected final NamespacedKey name;

    protected AbstractProperty(NamespacedKey name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    /**
     * Gets the name of this property.
     *
     * @return The name.
     * @since 0.1
     */
    @Override
    public NamespacedKey getKey() {
        return this.name;
    }

    /**
     * Gets the stringified version of the value of this property.
     *
     * @param world
     *            Used to get the world-specific value.
     * @param biome
     *            Used to get the biome-specific value.
     * @return The stringified value.
     * @since 0.5
     */
    public abstract String getStringValue(Optional<WorldRef> world, Optional<Biome> biome);

    /**
     * Updates the property with the given value.
     *
     * @param worldRef
     *            The world to set the value for.
     * @param biome
     *            The biome to set the value for.
     * @param string
     *            The new value.
     * @throws IllegalArgumentException
     *             The value could not be parsed.
     * @throws UnsupportedOperationException
     *             Changing this value is not allowed.
     * @since 0.5
     */
    public abstract void setStringValue(Optional<WorldRef> worldRef, Optional<Biome> biome, String string)
            throws IllegalArgumentException, UnsupportedOperationException;
}
