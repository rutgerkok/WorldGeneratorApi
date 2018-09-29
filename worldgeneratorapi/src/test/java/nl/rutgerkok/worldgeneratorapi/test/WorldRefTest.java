package nl.rutgerkok.worldgeneratorapi.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Month;

import org.bukkit.World;
import org.junit.jupiter.api.Test;

import nl.rutgerkok.worldgeneratorapi.WorldRef;

public class WorldRefTest {

    @Test
    public void equality() {
        WorldRef one = WorldRef.ofName("Test");
        WorldRef two = WorldRef.ofName("test");

        assertEquals(one, one);
        assertEquals(one, two);
        assertEquals(one.hashCode(), two.hashCode());
    }

    @Test
    public void fromWorld() {
        World world = TestFactory.world("myTest");
        WorldRef worldRef = WorldRef.of(world);

        assertEquals(WorldRef.ofName("myTest"), worldRef);
    }

    @Test
    public void nameIsPreserved() {
        assertTrue("Test".equalsIgnoreCase(WorldRef.ofName("Test").getName()));
    }

    @Test
    public void nonEquality() {
        WorldRef one = WorldRef.ofName("some");
        WorldRef two = WorldRef.ofName("other");

        assertNotEquals(one, two);
        assertNotEquals(one, null);
        assertNotEquals(one, Month.DECEMBER);
    }

    @Test
    public void toStringContainsName() {
        assertTrue(WorldRef.ofName("foo").toString().contains("foo"));
    }
}
