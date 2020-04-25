package nl.rutgerkok.worldgeneratorapi.property;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;

import nl.rutgerkok.worldgeneratorapi.WorldRef;

/**
 * Some abstract property, which can take world-specific or biome-specific
 * values.
 * <p>
 * This class is thread-safe. However, most getters in this class can produce
 * slightly outdated values if another thread is currently modifying those
 * values.
 *
 * @param <T>
 *            The type to be stored.
 * @see FloatProperty
 * @since 0.1
 */
public class Property<T> extends AbstractProperty {

    /**
     * A list that cannot be grown, has a size of 1, but can be modified.
     *
     * @param value
     *            The single value, may be null.
     * @return The list.
     */
    private static <T> List<T> singletonArrayList(@Nullable T value) {
        @SuppressWarnings("unchecked")
        T[] singleArray = (T[]) new Object[1];
        singleArray[0] = value;
        return Arrays.asList(singleArray);
    }

    private final Map<WorldRef, List<T>> allWorldValues = new ConcurrentHashMap<>();
    private List<T> defaultValues = singletonArrayList(null);

    /**
     * Only one thread may modify the class at a time. This is necessary. Imagine
     * that this lock wasn't there, and one method (say
     * {@link #setBiomeDefault(Biome, Object)} needs to replace an array, while
     * another method changes a value in the old array (say
     * {@link #setDefault(Object)}): then the second method may silently fail.
     *
     * <p>
     * For reading values no lock is necessary. If a getter runs at the same time as
     * a setter, then the getter may return a now slightly outdated value. This
     * shouldn't be a problem in practice. There is one other risk though: lists
     * normally aren't thread safe. We counter that risk by only using
     * {@link Arrays#asList(Object...)} as a list, which cannot be resized. So to
     * resize the list, the old list needs to be replaced by a new one. Getters take
     * a local reference to any list they use, so they can continue working on that
     * list, even if another thread provides a new list.
     */
    private final Object mutationLock = new Object();

    public Property(NamespacedKey name, T defaultValue) {
        super(name);
        setDefault(defaultValue);
    }

    /**
     * Gets the property, ignoring biome-specific values.
     *
     * @param world
     *            The world.
     * @return The value of the property.
     * @since 0.1
     */
    public final T get(WorldRef world) {
        T value = getWorldDefault(world);
        if (value != null) {
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
     * @since 0.1
     */
    public final T get(WorldRef world, Biome biome) {
        T value = getBiomeInWorldDefault(world, biome);
        if (value != null) {
            return value;
        }

        value = getBiomeDefault(biome);
        if (value != null) {
            return value;
        }

        return get(world);
    }

    /**
     * Gets the default value explicitly specified for a given biome. May return
     * {@code null}, in which case the call is driven to
     * {@link #getWorldDefault(WorldRef)}.
     *
     * @param biome
     *            The biome.
     * @return The default value.
     */
    protected @Nullable T getBiomeDefault(Biome biome) {
        List<T> defaultValues = this.defaultValues;
        if (defaultValues.size() == 1) {
            return null;
        }
        return defaultValues.get(biome.ordinal());
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
    protected @Nullable T getBiomeInWorldDefault(WorldRef world, Biome biome) {
        List<T> worldValues = allWorldValues.get(world);
        if (worldValues == null || worldValues.size() == 1) {
            return null;
        }
        return worldValues.get(biome.ordinal());
    }

    /**
     * Gets a default value. May not be null.
     *
     * @return A default value.
     * @since 0.1
     */
    public T getDefault() {
        List<T> defaultValues = this.defaultValues;
        return defaultValues.get(defaultValues.size() - 1);
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
            T biomeDefault = this.getBiomeDefault(biome.get());
            if (biomeDefault != null) {
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
    protected @Nullable T getWorldDefault(WorldRef world) {
        List<T> worldValues = allWorldValues.get(world);
        if (worldValues != null) {
            return worldValues.get(worldValues.size() - 1);
        }
        return null;
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
    @SuppressWarnings("unchecked") // List is guarded
    public void setBiomeDefault(Biome biome, T value) {
        Objects.requireNonNull(biome, "biome");
        Objects.requireNonNull(value, "value");

        synchronized (mutationLock) {
            if (defaultValues.size() == 1) {
                // Make biome-specific default values possible
                T globalDefault = defaultValues.get(0);
                defaultValues = (List<T>) Arrays.asList(new Object[Biome.values().length + 1]);

                // Restore all-default value, always stored in last slot
                defaultValues.set(defaultValues.size() - 1, globalDefault);
            }

            defaultValues.set(biome.ordinal(), value);
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
    @SuppressWarnings("unchecked") // Safe, as list is guarded
    public void setBiomeInWorldDefault(WorldRef world, Biome biome, T value) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(biome, "biome");
        Objects.requireNonNull(value, "value");

        synchronized (mutationLock) {
            List<T> worldValues = this.allWorldValues.get(world);
            if (worldValues == null) {
                worldValues = (List<T>) Arrays.asList(new Object[Biome.values().length + 1]);
                this.allWorldValues.put(world, worldValues);
            } else if (worldValues.size() == 1) {
                T worldDefault = worldValues.get(0);
                worldValues = (List<T>) Arrays.asList(new Object[Biome.values().length + 1]);
                worldValues.set(worldValues.size() - 1, worldDefault);
                this.allWorldValues.put(world, worldValues);
            }
            worldValues.set(biome.ordinal(), value);
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
    public void setDefault(T value) {
        Objects.requireNonNull(value, "value");
        synchronized (mutationLock) {
            defaultValues.set(defaultValues.size() - 1, value);
        }
    }

    @Override
    public void setStringValue(Optional<WorldRef> worldRef, Optional<Biome> biome, String string)
            throws IllegalArgumentException, UnsupportedOperationException {
        @SuppressWarnings("unchecked") // Safe, default value of of type T
        Class<? extends T> requiredClass = (Class<? extends T>) this.getDefault().getClass();
        T value = StringParser.parse(string, requiredClass);
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
    public void setWorldDefault(WorldRef world, T value) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(value, "value");

        synchronized (mutationLock) {
            List<T> worldValues = this.allWorldValues.get(world);
            if (worldValues == null) {
                // Make world-specific values possible
                worldValues = singletonArrayList(value);
                this.allWorldValues.put(world, worldValues);
                return;
            }

            // Default value is stored in last slot
            worldValues.set(worldValues.size() - 1, value);
        }
    }

    @Override
    public String toString() {
        return "Property[" + this.name.toString() + "]";
    }
}
