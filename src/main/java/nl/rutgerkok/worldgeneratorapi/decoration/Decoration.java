package nl.rutgerkok.worldgeneratorapi.decoration;

import java.util.Random;

/**
 * Represents a decoration of a world. Plugins are allowed to implement this
 * class.
 */
public interface Decoration {

    /**
     * Decorates a 16x16 area. See the documentation of {@link DecorationArea} for
     * where exactly you should place objects.
     *
     * @param area
     *            The decoration area.
     * @param random
     *            Random number generator.
     */
    void decorate(DecorationArea area, Random random);
}
