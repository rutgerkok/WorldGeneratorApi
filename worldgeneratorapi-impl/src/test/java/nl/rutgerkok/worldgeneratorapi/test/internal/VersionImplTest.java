package nl.rutgerkok.worldgeneratorapi.test.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nl.rutgerkok.worldgeneratorapi.Version;
import nl.rutgerkok.worldgeneratorapi.internal.VersionImpl;

public class VersionImplTest {

    @Test
    public void devRelease() {
        Version version = new VersionImpl("2.2-SNAPSHOT");

        assertTrue(version.isCompatibleWith(2, 2));
        assertTrue(version.isCompatibleWith(2, 1));
        assertFalse(version.isCompatibleWith(2, 3));
    }

    @Test
    public void pointRelease() {
        Version version = new VersionImpl("2.1");

        assertFalse(version.isCompatibleWith(0, 8));
        assertFalse(version.isCompatibleWith(1, 2));
        assertFalse(version.isCompatibleWith(1, 4));
        assertFalse(version.isCompatibleWith(1, 5));
        assertTrue(version.isCompatibleWith(2, 0));
        assertTrue(version.isCompatibleWith(2, 1));
        assertFalse(version.isCompatibleWith(2, 2));

    }

    @Test
    public void string() {
        assertEquals("1.4.2-SNAPSHOT", new VersionImpl("1.4.2-SNAPSHOT").toString());
    }

    @Test
    public void twoPointRelease() {
        Version version = new VersionImpl("2.4.2");

        assertTrue(version.isCompatibleWith(2, 2));
        assertTrue(version.isCompatibleWith(2, 4));
        assertFalse(version.isCompatibleWith(2, 5));
    }
}
