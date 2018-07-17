package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_13_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R1.block.CraftBlock;

import net.minecraft.server.v1_13_R1.BiomeBase;
import net.minecraft.server.v1_13_R1.Biomes;

import nl.rutgerkok.worldgeneratorapi.WorldGeneratorApi;
import nl.rutgerkok.worldgeneratorapi.WorldRef;
import nl.rutgerkok.worldgeneratorapi.property.FloatProperty;
import nl.rutgerkok.worldgeneratorapi.property.Property;
import nl.rutgerkok.worldgeneratorapi.property.PropertyRegistry;

/**
 * Default implementation of {@link PropertyRegistry}. Use
 * {@link WorldGeneratorApi#getPropertyRegistry()} to get an instance.
 *
 */
public final class PropertyRegistryImpl implements PropertyRegistry {

    private final Map<NamespacedKey, Keyed> properties = new HashMap<>();

    public PropertyRegistryImpl() {
        addMinecraftBiomeFloatProperty(TEMPERATURE, BiomeBase::getTemperature);
        addMinecraftBiomeFloatProperty(WETNESS, BiomeBase::getHumidity);
        addMinecraftBiomeFloatProperty(BASE_HEIGHT, BiomeBase::h);
        addMinecraftBiomeFloatProperty(HEIGHT_VARIATION, BiomeBase::l);
        addMinecraftWorldProperty(WORLD_SEED, world -> (Long) world.getSeed(), -1L);
        addMinecraftWorldFloatProperty(SEA_LEVEL, world -> (float) world.getSeaLevel(), (world, level) -> {
            ((CraftWorld) world).getHandle().b(level.intValue());
            if (world.getSeaLevel() != level.intValue()) {
                throw new UnsupportedOperationException("Failed to set sea level to " + level.intValue());
            }
        });
    }

    private void addMinecraftBiomeFloatProperty(NamespacedKey name, Function<BiomeBase, Float> value) {
        FloatProperty property = new FloatProperty(name, value.apply(Biomes.b)) {

            @Override
            public float getBiomeDefault(Biome biome) {
                BiomeBase base = CraftBlock.biomeToBiomeBase(biome);
                return value.apply(base);
            }

            @Override
            public void setBiomeDefault(Biome biome, float value) {
                throw new UnsupportedOperationException(
                        "Cannot change " + getKey().getKey() + " globally for biomes. Try"
                                + " setting it per-world instead.");
            }

        };
        this.properties.put(name, property);
    }

    private void addMinecraftWorldFloatProperty(NamespacedKey name, Function<World, Float> value,
            BiConsumer<World, Float> setter) {
        FloatProperty property = new FloatProperty(name, 0f) {

            @Override
            public float getWorldDefault(WorldRef worldRef) {
                World world = Bukkit.getWorld(worldRef.getName());
                if (world == null) {
                    return Float.NaN;
                }
                return value.apply(world);
            }

            @Override
            public void setWorldDefault(WorldRef worldRef, float value) {
                World world = Bukkit.getWorld(worldRef.getName());
                if (world == null) {
                    throw new UnsupportedOperationException(
                            "Cannot change sea level yet, world " + worldRef.getName() + " is not loaded");
                }
                setter.accept(world, value);
            }

        };
        this.properties.put(name, property);
    }

    private <T> void addMinecraftWorldProperty(NamespacedKey name, Function<World, T> value, T defaultValue) {
        Property<T> property = new Property<T>(name, defaultValue) {

            @Override
            public @Nullable T getWorldDefault(WorldRef worldRef) {
                World world = Bukkit.getWorld(worldRef.getName());
                if (world == null) {
                    return null;
                }
                return value.apply(world);
            }

            @Override
            public void setWorldDefault(WorldRef world, T value) {
                throw new UnsupportedOperationException(
                        "Cannot change " + getKey().getKey() + " for worlds.");
            }

        };
        this.properties.put(name, property);
    }

    @Override
    public FloatProperty getFloat(NamespacedKey name, float defaultValue) {
        return (FloatProperty) properties.computeIfAbsent(name, n -> new FloatProperty(name, defaultValue));
    }

    @Override
    public <T> Property<T> getProperty(NamespacedKey name, T defaultValue) {
        @SuppressWarnings("unchecked") // Will be checked on lines following
        Property<T> property = (Property<T>) properties.computeIfAbsent(name, n -> new Property<>(name, defaultValue));
        if (!property.getDefault().getClass().equals(defaultValue.getClass())) {
            throw new ClassCastException("Cannot cast Property<" + property.getDefault().getClass().getSimpleName()
                    + "> to Property<" + defaultValue.getClass().getSimpleName() + ">");
        }
        return property;
    }
}
