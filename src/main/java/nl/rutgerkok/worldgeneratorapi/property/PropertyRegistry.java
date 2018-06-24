package nl.rutgerkok.worldgeneratorapi.property;

import javax.annotation.Nullable;

import org.bukkit.NamespacedKey;

public interface PropertyRegistry {

    /**
     * The temperature of a biome (float), from 0 (frozen) to 2 (desert).
     */
    public static NamespacedKey TEMPERATURE = NamespacedKey.minecraft("temperature");

    /**
     * The wetness of a biome (float), from 0 (dry) to 1 (wet).
     */
    public static NamespacedKey WETNESS = NamespacedKey.minecraft("wetness");

    /**
     * Variable used in height generation (float).
     */
    public static NamespacedKey BASE_HEIGHT = NamespacedKey.minecraft("base_height");

    /**
     * Variable used in height generation (float).
     */
    public static NamespacedKey HEIGHT_VARIATION = NamespacedKey.minecraft("height_variation");

    /**
     * The world seed (long).
     */
    public static NamespacedKey WORLD_SEED = NamespacedKey.minecraft("world_seed");

    /**
     * The sea level (float).
     */
    public static NamespacedKey SEA_LEVEL = NamespacedKey.minecraft("sea_level");

    /**
     * Gets the property with the given name. If no such property exists, it is
     * created.
     *
     * @param name
     *            Name of the property.
     * @param defaultValue
     *            If this property does not exist yet, this value will be used as
     *            the default value.
     * @return The property.
     * @throws ClassCastException
     *             If a property of another type with the same name is already
     *             stored.
     */
    FloatProperty getFloat(NamespacedKey name, float defaultValue);

    /**
     * Gets the property with the given name. Its type is not checked. If no
     * property with the name exists, it is created.
     *
     * @param name
     *            Name of the property.
     * @param defaultValue
     *            If the property does not exist yet, this value will be used as the
     *            default value. Note that this value may not be null, just like
     *            every other parameter without the {@link Nullable} annotation.
     * @return The property.
     * @throws ClassCastException
     *             If a property of another type with the same name is already
     *             stored.
     */
    <T> Property<T> getProperty(NamespacedKey name, T defaultValue);

}
