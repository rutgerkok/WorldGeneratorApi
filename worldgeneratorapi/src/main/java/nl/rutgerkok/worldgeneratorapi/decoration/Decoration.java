package nl.rutgerkok.worldgeneratorapi.decoration;

import java.util.Random;

import nl.rutgerkok.worldgeneratorapi.property.PropertyRegistry;

/**
 * Represents a decoration of a world. Plugins are allowed to implement this
 * class. These decorations are allowed to cross chunk boundaries.
 *
 * @since 0.2
 */
public interface Decoration {

    /**
     * Decorates a 16x16 area. See the documentation of {@link DecorationArea} for
     * where exactly you should place objects.
     *
     * <p>
     * Note: <strong>this method can be called on any thread</strong>, including the
     * main server thread. As long as you only use the methods contained in the
     * decoration area and in the {@link PropertyRegistry property registry},
     * there's no need to worry about this. However, if you use/call code from other
     * areas (like the rest of the world or an ordinary hash map from your plugin)
     * you will get into trouble. Exceptions may be thrown, or worse: your world may
     * be corrupted silently.
     *
     * @param area
     *            The decoration area.
     * @param random
     *            Random number generator.
     * @since 0.2
     */
    void decorate(DecorationArea area, Random random);
}
