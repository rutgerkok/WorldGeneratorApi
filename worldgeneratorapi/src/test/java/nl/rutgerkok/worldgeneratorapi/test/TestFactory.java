package nl.rutgerkok.worldgeneratorapi.test;

import java.util.Collections;

import javax.annotation.Nonnull;

import org.bukkit.World;
import org.mockito.Mockito;

public class TestFactory {

    /**
     * Returns {@code null} that is annotated {@link Nonnull}, as to confuse the
     * type checker.
     *
     * Say that your annotation type checker forbids testing
     * {@code someMethod(null)}, then you can use this method to call
     * {@code someMethod(unsafeNull())}.
     *
     * @return {@code null}, but annotated as {@link Nonnull}
     */
    @SuppressWarnings("unchecked") // It is unsafe, that is the purpose
    public static <T> T unsafeNull() {
        return (T) Collections.EMPTY_MAP.get("some string");
    }

    public static World world(String name) {
        World world = Mockito.mock(World.class);
        Mockito.when(world.getName()).thenReturn(name);

        return world;
    }
}
