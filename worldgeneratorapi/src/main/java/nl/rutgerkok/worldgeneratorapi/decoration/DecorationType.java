package nl.rutgerkok.worldgeneratorapi.decoration;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * The type of a decoration. The type of the decoration only influences the
 * moment it is spawned.
 *
 * @since 0.2
 */
public enum DecorationType {
    /*
     * Keep the names in sync with Minecraft's equivalent. Also, for clarity, try to
     * keep the order here the same as the order in which Minecraft generates them.
     */

    /**
     * Used to generate caves and ravines.
     *
     * @deprecated Replaced by {@link BaseDecorationType#CARVING_AIR}.
     * @since 0.3
     */
    @Deprecated
    CARVING_AIR,

    /**
     * Used to generate underwater caves and ravines.
     *
     * @deprecated Replaced by {@link BaseDecorationType#CARVING_LIQUID}.
     * @since 0.3
     */
    @Deprecated
    CARVING_LIQUID,

    /**
     * Early stage of decoration, just after the two carving stages. Seems to be
     * unused by Minecraft, but you can use it just fine.
     *
     * @since 0.2
     */
    RAW_GENERATION,

    /**
     * Small water/lava lakes.
     *
     * @since 1.0
     */
    LAKES,

    /**
     * Small modifications to the shape of the terrain, like icebergs. Since
     * Minecraft 1.16, {@link #LAKES} have their own type.
     *
     * @since 0.2
     */
    LOCAL_MODIFICATIONS,

    /**
     * Structures that spawn underground. Mineshafts, buried treasures, etc. Since
     * Minecraft 1.16, {@link #STRONGHOLDS} have their own type.
     *
     * @since 0.2
     */
    UNDERGROUND_STRUCTURES,

    /**
     * Structures that spawn on the surface. Villages, pyramids, igloos, shipwrecks,
     * ocean monuments, etc.
     *
     * @since 0.2
     */
    SURFACE_STRUCTURES,

    /**
     * Strongholds.
     *
     * @since 1.0
     */
    STRONGHOLDS,

    /**
     * The ores: diamond, redstone, iron, coal, etc.
     *
     * @since 0.2
     */
    UNDERGROUND_ORES,

    /**
     * Small structures that spawn underground. Fossils.
     *
     * @since 0.2
     */
    UNDERGROUND_DECORATION,

    /**
     * Small structures that spawn on the surface. Plants, grass, flowers,
     * mushrooms, etc.
     *
     * @since 0.2
     */
    VEGETAL_DECORATION,

    /**
     * A few final modifications, like the placement of vines.
     *
     * @since 0.2
     */
    TOP_LAYER_MODIFICATION;

    private static final List<DecorationType> all = ImmutableList.copyOf(values());

    /**
     * Gets a list of all decoration types. The contents are equal to
     * {@link #values()}. However, unlike that other method this method does not
     * allocate a new array every time it is called.
     *
     * @return All decoration types.
     */
    public static List<DecorationType> all() {
        return all;
    }
}
