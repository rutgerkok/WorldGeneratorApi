package nl.rutgerkok.worldgeneratorapi.decoration;

/**
 * Represents a decoration type that is spawned before the normal
 * {@link DecorationType decoration type}. Unlike those decorations, the base
 * decorations are strictly forbidden from crossing chunk boundaries. Things
 * like grass and caves are examples of base decorators.
 *
 * @since 0.3
 */
@Deprecated(forRemoval = true)
public enum BaseDecorationType {

    /**
     * Ran directly after the terrain shape has been generated. Generally, there
     * will only be stone, air and water in the world.
     *
     * @since 0.3
     */
    RAW_GENERATION,

    /**
     * The surface is generated.
     *
     * @since 0.3
     */
    SURFACE,

    /**
     * The bedrock layers are generated.
     *
     * @since 0.3
     */
    BEDROCK,

    /**
     * Used to generate caves and ravines.
     *
     * @since 0.3
     */
    CARVING_AIR,

    /**
     * Used to generate underwater caves and ravines.
     *
     * @since 0.3
     */
    CARVING_LIQUID
}
