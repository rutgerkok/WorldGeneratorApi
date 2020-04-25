package nl.rutgerkok.worldgeneratorapi.property;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;

import nl.rutgerkok.worldgeneratorapi.WorldRef;

/**
 * Some abstract property, which can take world-specific or biome-specific
 * values. This class is more efficient than the generic {@link Property}, as
 * boxing and unboxing can be avoided.
 * <p>
 * This class is thread-safe. However, most getters in this class can produce
 * slightly outdated values if another thread is currently modifying those
 * values.
 *
 * @since 0.1
 */
public class FloatProperty extends AbstractProperty {

    private final Map<WorldRef, float[]> allWorldValues = new ConcurrentHashMap<>(1);
    private volatile float[] defaultValues = { 0 };

    /**
     * Only one thread may modify the class at a time. This is necessary. Imagine
     * that this lock wasn't there, and one method (say
     * {@link #setBiomeDefault(Biome, float)} needs to replace an array, while
     * another method changes a value in the old array (say
     * {@link #setDefault(float)}): then the second method may silently fail.
     *
     * <p>
     * For reading values no lock is necessary. If a getter runs at the same time as
     * a setter, then the getter may return a now slightly outdated value. This
     * shouldn't be a problem in practise. There is one other risk though: an array
     * might suddenly be replaced by one with a different length. To protect against
     * this, each getter stats by taking a local reference to the old array, so it
     * can continue working with that array even if another thread makes a new array
     * available.
     */
    private final Object mutationLock = new Object();

    public FloatProperty(NamespacedKey name, float defaultValue) {
        super(name);
        setDefault(defaultValue);
    }

    private void checkForNaN(float value) {
        // We do not allow NaN to be stored, as it is used as a null-value
        if (Float.isNaN(value)) {
            throw new UnsupportedOperationException("Cannot set to NaN");
        }
    }

    /**
     * Gets the property, ignoring biome-specific values.
     *
     * @param world
     *            The world.
     * @return The value of the property.
     * @since 0.1
     */
    public final float get(WorldRef world) {
        float value = getWorldDefault(world);
        if (!Float.isNaN(value)) {
            return value;
        }
        return getDefault();
    }

    /**
     * Gets the property.
     *
     * @param world
     *            The world.
     * @param biome
     *            The biome.
     * @return The value of the property.
     */
    public final float get(WorldRef world, Biome biome) {
        float value = getBiomeInWorldDefault(world, biome);
        if (!Float.isNaN(value)) {
            return value;
        }

        value = getBiomeDefault(biome);
        if (!Float.isNaN(value)) {
            return value;
        }

        return get(world);
    }

    /**
     * Gets the default value explicitly specified for a given biome. May return
     * {@link Float#NaN}, in which case the call is driven to
     * {@link #getWorldDefault(WorldRef)}.
     *
     * @param biome
     *            The biome.
     * @return The default value.
     */
    protected float getBiomeDefault(Biome biome) {
        float[] defaultValues = this.defaultValues;
        if (defaultValues.length == 1) {
            return Float.NaN;
        }
        return defaultValues[biome.ordinal()];
    }

    /**
     * Gets the default value explicitly specified for a given biome in a world.
     * May return {@link Float#NaN}, in which case the call is driven to
     * {@link #getBiomeDefault(Biome)}.
     *
     * @param world
     *            The world.
     * @param biome
     *            The biome.
     * @return The default value.
     */
    protected float getBiomeInWorldDefault(WorldRef world, Biome biome) {
        float[] worldValues = allWorldValues.get(world);
        if (worldValues == null || worldValues.length == 1) {
            return Float.NaN;
        }
        return worldValues[biome.ordinal()];
    }

    /**
     * Gets a default value. May not be {@link Float#NaN}.
     *
     * @return A default value.
     */
    protected float getDefault() {
        float[] defaultValues = this.defaultValues;
        return defaultValues[defaultValues.length - 1];
    }

    @Override
    public String getStringValue(Optional<WorldRef> world, Optional<Biome> biome) {
        if (world.isPresent()) {
            if (biome.isPresent()) {
                return String.valueOf(this.get(world.get(), biome.get()));
            }
            return String.valueOf(this.get(world.get()));
        }
        if (biome.isPresent()) {
            float biomeDefault = this.getBiomeDefault(biome.get());
            if (!Float.isNaN(biomeDefault)) {
                return String.valueOf(biomeDefault);
            }
        }
        return String.valueOf(this.getDefault());
    }

    /**
     * Gets the default value explicitly specified for a given world. May return
     * {@link Float#NaN}, in which case the call is driven to
     * {@link #getDefault()}.
     *
     * @param world
     *            The world.
     * @return The default value.
     */
    protected float getWorldDefault(WorldRef world) {
        float[] worldValues = allWorldValues.get(world);
        if (worldValues != null) {
            return worldValues[worldValues.length - 1];
        }
        return Float.NaN;
    }

    /**
     * Sets the property. Biome-specific values override world-specific values.
     *
     * @param biome
     *            The biome.
     * @param value
     *            The new value.
     * @throws UnsupportedOperationException
     *             If the property cannot be changed to the given value for whatever
     *             reason.
     * @since 0.1
     */
    public void setBiomeDefault(Biome biome, float value) {
        Objects.requireNonNull(biome, "biome");
        checkForNaN(value);

        synchronized (mutationLock) {
            if (defaultValues.length == 1) {
                // Make biome-specific default values possible
                float globalDefault = defaultValues[0];
                defaultValues = new float[Biome.values().length + 1];
                Arrays.fill(defaultValues, Float.NaN);

                // Restore all-default value, always stored in last slot
                defaultValues[defaultValues.length - 1] = globalDefault;
            }

            defaultValues[biome.ordinal()] = value;
        }
    }

    /**
     * Sets the property. This overrides all other default values.
     *
     * @param world
     *            The world.
     * @param biome
     *            The biome.
     * @param value
     *            The new value.
     * @throws UnsupportedOperationException
     *             If the property cannot be changed to the given value for whatever
     *             reason.
     * @since 0.1
     */
    public void setBiomeInWorldDefault(WorldRef world, Biome biome, float value) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(biome, "biome");
        checkForNaN(value);

        synchronized (mutationLock) {
            float[] worldValues = this.allWorldValues.get(world);
            if (worldValues == null) {
                worldValues = new float[Biome.values().length + 1];
                Arrays.fill(worldValues, Float.NaN);
                this.allWorldValues.put(world, worldValues);
            } else if (worldValues.length == 1) {
                float worldDefault = worldValues[0];
                worldValues = new float[Biome.values().length + 1];
                Arrays.fill(worldValues, Float.NaN);
                worldValues[worldValues.length - 1] = worldDefault;
                this.allWorldValues.put(world, worldValues);
            }
            worldValues[biome.ordinal()] = value;
        }
    }

    /**
     * Sets the property. Biome-specific and world-specific values will override
     * this value.
     *
     * @param value
     *            The value.
     * @throws UnsupportedOperationException
     *             If the property cannot be changed to the given value for whatever
     *             reason.
     * @since 0.1
     */
    public void setDefault(float value) {
        checkForNaN(value);
        synchronized (mutationLock) {
            defaultValues[defaultValues.length - 1] = value;
        }
    }

    @Override
    public void setStringValue(Optional<WorldRef> worldRef, Optional<Biome> biome, String string)
            throws IllegalArgumentException, UnsupportedOperationException {
        float value;
        try {
            value = Float.parseFloat(string);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + string);
        }

        if (worldRef.isPresent()) {
            if (biome.isPresent()) {
                this.setBiomeInWorldDefault(worldRef.get(), biome.get(), value);
            } else {
                this.setWorldDefault(worldRef.get(), value);
            }
        } else if (biome.isPresent()) {
            this.setBiomeDefault(biome.get(), value);
        } else {
            this.setDefault(value);
        }
    }

    /**
     * Sets the property for the given world.
     *
     * @param world
     *            The world.
     * @param value
     *            The value.
     * @throws UnsupportedOperationException
     *             If the property cannot be changed to the given value for whatever
     *             reason.
     * @since 0.1
     */
    public void setWorldDefault(WorldRef world, float value) {
        Objects.requireNonNull(world, "world");
        checkForNaN(value);

        synchronized (mutationLock) {
            float[] worldValues = this.allWorldValues.get(world);
            if (worldValues == null) {
                // Make world-specific values possible
                worldValues = new float[1];
                this.allWorldValues.put(world, worldValues);
            }

            // Default value is stored in last slot
            worldValues[worldValues.length - 1] = value;
        }
    }

    @Override
    public String toString() {
        return "FloatProperty[" + this.name.toString() + "]";
    }
}
