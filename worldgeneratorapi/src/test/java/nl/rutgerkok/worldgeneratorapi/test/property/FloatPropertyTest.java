package nl.rutgerkok.worldgeneratorapi.test.property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.junit.jupiter.api.Test;

import nl.rutgerkok.worldgeneratorapi.WorldRef;
import nl.rutgerkok.worldgeneratorapi.property.FloatProperty;

public class FloatPropertyTest {

    @Test
    public void biomeDefault() {
        FloatProperty property = new FloatProperty(NamespacedKey.minecraft("test"), 0);
        WorldRef world = WorldRef.ofName("test");

        property.setBiomeDefault(Biome.DESERT, 2);
        property.setBiomeDefault(Biome.DESERT_HILLS, 6);
        assertEquals(0, property.get(world, Biome.SNOWY_BEACH));
        assertEquals(2, property.get(world, Biome.DESERT));
        assertEquals(6, property.get(world, Biome.DESERT_HILLS));
    }

    @Test
    public void biomeDefaultOverridesWorldDefault() {
        FloatProperty property = new FloatProperty(NamespacedKey.minecraft("test"), 0);
        WorldRef world = WorldRef.ofName("test");

        property.setWorldDefault(world, 5);
        property.setBiomeDefault(Biome.SNOWY_BEACH, 6);
        assertEquals(6, property.get(world, Biome.SNOWY_BEACH));

        property.setWorldDefault(world, 7);
        assertEquals(6, property.get(world, Biome.SNOWY_BEACH)); // Value must not have been changed
    }

    @Test
    public void biomeInWorldDefault() {
        FloatProperty property = new FloatProperty(NamespacedKey.minecraft("test"), 0);
        WorldRef world = WorldRef.ofName("some_world");
        WorldRef otherWorld = WorldRef.ofName("other_world");

        property.setBiomeDefault(Biome.SNOWY_BEACH, 3);
        property.setBiomeInWorldDefault(world, Biome.SNOWY_BEACH, 6);
        assertEquals(6, property.get(world, Biome.SNOWY_BEACH));
        assertEquals(3, property.get(otherWorld, Biome.SNOWY_BEACH));
    }

    @Test
    public void biomeInWorldDefaultOverridesWorldDefault() {
        FloatProperty property = new FloatProperty(NamespacedKey.minecraft("test"), 0);
        WorldRef world = WorldRef.ofName("test");

        property.setWorldDefault(world, 2);
        property.setBiomeInWorldDefault(world, Biome.SNOWY_BEACH, 6);
        property.setBiomeInWorldDefault(world, Biome.BIRCH_FOREST, 10);

        assertEquals(2, property.get(world, Biome.BEACH));
        assertEquals(6, property.get(world, Biome.SNOWY_BEACH));
        assertEquals(10, property.get(world, Biome.BIRCH_FOREST));
    }

    @Test
    public void cannotStoreNaN() {
        assertThrows(UnsupportedOperationException.class, () -> {
            new FloatProperty(NamespacedKey.minecraft("test"), Float.NaN);
        });
    }

    @Test
    public void defaultValueCanBeChanged() {
        FloatProperty property = new FloatProperty(NamespacedKey.minecraft("test"), 0);
        WorldRef world = WorldRef.ofName("test");

        assertEquals(0, property.get(world, Biome.SNOWY_BEACH));
        property.setDefault(3);
        assertEquals(3, property.get(world, Biome.SNOWY_BEACH));
    }

    @Test
    public void toStringContainsName() {
        FloatProperty property = new FloatProperty(NamespacedKey.minecraft("test"), 0);
        assertTrue(property.toString().contains("minecraft:test"));
    }

    @Test
    public void worldDefault() {
        FloatProperty property = new FloatProperty(NamespacedKey.minecraft("test"), 0);
        WorldRef world = WorldRef.ofName("test");

        property.setWorldDefault(world, 5);
        assertEquals(5, property.get(world));
        assertEquals(5, property.get(world, Biome.SNOWY_BEACH));
    }
}
