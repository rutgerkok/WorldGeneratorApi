package nl.rutgerkok.worldgeneratorapi.test.property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.junit.jupiter.api.Test;

import nl.rutgerkok.worldgeneratorapi.WorldRef;
import nl.rutgerkok.worldgeneratorapi.property.Property;
import nl.rutgerkok.worldgeneratorapi.test.TestFactory;

public class PropertyTest {

    @Test
    public void biomeDefault() {
        Property<String> property = new Property<>(NamespacedKey.minecraft("test"), "zero");
        WorldRef world = WorldRef.ofName("test");

        property.setBiomeDefault(Biome.DESERT, "two");
        property.setBiomeDefault(Biome.DESERT_HILLS, "six");
        assertEquals("zero", property.get(world, Biome.SNOWY_BEACH));
        assertEquals("two", property.get(world, Biome.DESERT));
        assertEquals("six", property.get(world, Biome.DESERT_HILLS));
    }

    @Test
    public void biomeDefaultOverridesWorldDefault() {
        Property<String> property = new Property<>(NamespacedKey.minecraft("test"), "zero");
        WorldRef world = WorldRef.ofName("test");

        property.setWorldDefault(world, "five");
        property.setBiomeDefault(Biome.SNOWY_BEACH, "six");
        assertEquals("six", property.get(world, Biome.SNOWY_BEACH));

        property.setWorldDefault(world, "seven");
        assertEquals("six", property.get(world, Biome.SNOWY_BEACH)); // Value must not have been changed
    }

    @Test
    public void biomeInWorldDefault() {
        Property<String> property = new Property<>(NamespacedKey.minecraft("test"), "zero");
        WorldRef world = WorldRef.ofName("some_world");
        WorldRef otherWorld = WorldRef.ofName("other_world");

        property.setBiomeDefault(Biome.SNOWY_BEACH, "three");
        property.setBiomeInWorldDefault(world, Biome.SNOWY_BEACH, "six");
        assertEquals("six", property.get(world, Biome.SNOWY_BEACH));
        assertEquals("three", property.get(otherWorld, Biome.SNOWY_BEACH));
    }

    @Test
    public void biomeInWorldDefaultOverridesWorldDefault() {
        Property<String> property = new Property<>(NamespacedKey.minecraft("test"), "zero");
        WorldRef world = WorldRef.ofName("test");

        property.setWorldDefault(world, "two");
        property.setBiomeInWorldDefault(world, Biome.SNOWY_BEACH, "six");
        property.setBiomeInWorldDefault(world, Biome.BIRCH_FOREST, "ten");

        assertEquals("two", property.get(world, Biome.BEACH));
        assertEquals("six", property.get(world, Biome.SNOWY_BEACH));
        assertEquals("ten", property.get(world, Biome.BIRCH_FOREST));
    }

    @Test
    public void cannotStoreNull() {
        assertThrows(NullPointerException.class, () -> {
            new Property<>(NamespacedKey.minecraft("test"), TestFactory.unsafeNull());
        });
    }

    @Test
    public void defaultValueCanBeChanged() {
        Property<String> property = new Property<>(NamespacedKey.minecraft("test"), "zero");
        WorldRef world = WorldRef.ofName("test");

        assertEquals("zero", property.get(world, Biome.SNOWY_BEACH));
        property.setDefault("three");
        assertEquals("three", property.get(world, Biome.SNOWY_BEACH));
    }

    @Test
    public void toStringContainsName() {
        Property<String> property = new Property<>(NamespacedKey.minecraft("test"), "zero");
        assertTrue(property.toString().contains("minecraft:test"));
    }

    @Test
    public void worldDefault() {
        Property<String> property = new Property<>(NamespacedKey.minecraft("test"), "zero");
        WorldRef world = WorldRef.ofName("test");

        property.setWorldDefault(world, "five");
        assertEquals("five", property.get(world));
        assertEquals("five", property.get(world, Biome.SNOWY_BEACH));
    }
}
