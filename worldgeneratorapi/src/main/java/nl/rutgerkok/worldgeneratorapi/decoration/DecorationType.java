package nl.rutgerkok.worldgeneratorapi.decoration;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * The type of a decoration. The type of the decoration only influences the
 * moment it is spawned.
 *
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
     */
    @Deprecated
    CARVING_AIR,

    /**
     * Used to generate underwater caves and ravines.
     * 
     * @deprecated Replaced by {@link BaseDecorationType#CARVING_LIQUID}.
     */
    @Deprecated
    CARVING_LIQUID,

    /**
     * Early stage of decoration, just after the two carving stages. Seems to be
     * unused by Minecraft.
     */
    RAW_GENERATION,

    /**
     * Small modifications to the shape of the terrain. Small water/lava lakes,
     * icebergs, etc.
     */
    LOCAL_MODIFICATIONS,

    /**
     * Structures that spawn underground. Mineshafts, strongholds, buried treasures,
     * etc.
     */
    UNDERGROUND_STRUCTURES,

    /**
     * Structures that spawn on the surface. Villages, pyramids, igloos, shipwrecks,
     * ocean monuments, etc.
     */
    SURFACE_STRUCTURES,

    /**
     * The ores: diamond, redstone, iron, coal, etc.
     */
    UNDERGROUND_ORES,

    /**
     * Small structures that spawn underground. Fossils.
     */
    UNDERGROUND_DECORATION,

    /**
     * Small structures that spawn on the surface. Plants, grass, flowers,
     * mushrooms, etc.
     */
    VEGETAL_DECORATION,

    /**
     * A few final modifications, like the placement of vines.
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
