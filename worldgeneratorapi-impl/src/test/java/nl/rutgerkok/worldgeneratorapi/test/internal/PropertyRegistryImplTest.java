package nl.rutgerkok.worldgeneratorapi.test.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Month;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import nl.rutgerkok.worldgeneratorapi.WorldRef;
import nl.rutgerkok.worldgeneratorapi.internal.PropertyRegistryImpl;
import nl.rutgerkok.worldgeneratorapi.property.FloatProperty;
import nl.rutgerkok.worldgeneratorapi.property.Property;
import nl.rutgerkok.worldgeneratorapi.property.PropertyRegistry;

public class PropertyRegistryImplTest {

    @BeforeAll
    public static void bootstrap() {
        TestFactory.activateTestServer();
    }

    @Test
    public void baseHeight() {
        WorldRef world = WorldRef.ofName("test");
        PropertyRegistry registry = new PropertyRegistryImpl();

        FloatProperty baseHeight = registry.getFloat(PropertyRegistry.BASE_HEIGHT, 0);
        assertEquals(0.125, baseHeight.get(world, Biome.DESERT));
        assertEquals(0.45, baseHeight.get(world, Biome.SNOWY_MOUNTAINS), 0.0001);
    }

    @Test
    public void customFloat() {
        NamespacedKey customKey = new NamespacedKey(TestFactory.plugin("TestPlugin"), "float");
        WorldRef world = WorldRef.ofName("test");
        PropertyRegistry registry = new PropertyRegistryImpl();

        FloatProperty property = registry.getFloat(customKey, 3);
        assertEquals(3, property.get(world, Biome.BIRCH_FOREST));
    }

    @Test
    public void customObject() {
        NamespacedKey customKey = new NamespacedKey(TestFactory.plugin("TestPlugin"), "object");
        WorldRef world = WorldRef.ofName("test");
        PropertyRegistry registry = new PropertyRegistryImpl();

        Property<Month> property = registry.getProperty(customKey, Month.AUGUST);
        assertEquals(Month.AUGUST, property.get(world, Biome.OCEAN));
        assertEquals(customKey, property.getKey());
    }

    @Test
    public void heightVariation() {
        WorldRef world = WorldRef.ofName("test");
        PropertyRegistry registry = new PropertyRegistryImpl();

        FloatProperty baseHeight = registry.getFloat(PropertyRegistry.HEIGHT_VARIATION, 0);
        assertEquals(0.05, baseHeight.get(world, Biome.DESERT), 0.0001);
        assertEquals(0.30, baseHeight.get(world, Biome.SNOWY_MOUNTAINS), 0.0001);
    }

    @Test
    public void temperature() {
        WorldRef world = WorldRef.ofName("test");
        PropertyRegistry registry = new PropertyRegistryImpl();

        FloatProperty temperature = registry.getFloat(PropertyRegistry.TEMPERATURE, 0);
        assertEquals(2, temperature.get(world, Biome.DESERT));
        assertEquals(0, temperature.get(world, Biome.SNOWY_MOUNTAINS));

        assertThrows(UnsupportedOperationException.class, () -> {
            temperature.setBiomeDefault(Biome.DESERT, 1.8f);
        });
    }

    @Test
    public void testWorldProperties() {
        PropertyRegistry registry = new PropertyRegistryImpl();
        FloatProperty seaLevel = registry.getFloat(PropertyRegistry.SEA_LEVEL, 0);
        Property<Long> seed = registry.getProperty(PropertyRegistry.WORLD_SEED, -1L);

        // Test the standard testing world
        WorldRef world = WorldRef.ofName("test");
        assertEquals(TestFactory.SEA_LEVEL, (int) seaLevel.get(world));
        assertEquals(TestFactory.WORLD_SEED, (long) seed.get(world));

        // Test non-existing world
        WorldRef nonExisting = WorldRef.ofName("non_existing");
        assertEquals(0, (int) seaLevel.get(nonExisting));
        assertEquals(-1L, (long) seed.get(nonExisting));

        // Cannot change seed of worlds
        assertThrows(UnsupportedOperationException.class, () -> seed.setWorldDefault(world, 100L));
    }

    @Test
    public void useWrongType() {
        NamespacedKey floatKey = NamespacedKey.minecraft("float");
        NamespacedKey longKey = NamespacedKey.minecraft("long");
        PropertyRegistry registry = new PropertyRegistryImpl();
        registry.getFloat(floatKey, 2);
        registry.getProperty(longKey, 5L);

        assertThrows(ClassCastException.class, () -> {
            registry.getProperty(floatKey, "test");
        });
        assertThrows(ClassCastException.class, () -> {
            registry.getProperty(longKey, "test");
        });
        assertThrows(ClassCastException.class, () -> {
            registry.getProperty(floatKey, "test");
        });
    }

    @Test
    public void wetness() {
        WorldRef world = WorldRef.ofName("test");
        PropertyRegistry registry = new PropertyRegistryImpl();

        FloatProperty wetness = registry.getFloat(PropertyRegistry.WETNESS, 0);
        assertEquals(0, wetness.get(world, Biome.DESERT));
        assertEquals(0.9, wetness.get(world, Biome.JUNGLE), 0.0001);
    }
}
