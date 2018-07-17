package nl.rutgerkok.worldgeneratorapi.test;

import java.util.Collections;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.mockito.Mockito;

import net.minecraft.server.v1_13_R1.DispenserRegistry;

public class TestFactory {

    public static final long WORLD_SEED = 1234;
    public static final int SEA_LEVEL = 63;

    /**
     * Creates a dummy server with one world named "test".
     */
    public static void activateTestServer() {
        if (Bukkit.getServer() != null) {
            return;
        }
        DispenserRegistry.c();
        Server server = Mockito.mock(Server.class);
        Mockito.when(server.getLogger()).thenReturn(Logger.getLogger(TestFactory.class.getName()));
        Mockito.when(server.getWorld(Mockito.eq("test"))).thenAnswer(args -> world(args.getArgument(0)));
        Bukkit.setServer(server);
    }

    public static Plugin plugin(String name) {
        Plugin plugin = Mockito.mock(Plugin.class);
        Mockito.when(plugin.getName()).thenReturn(name);
        return plugin;
    }

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
        Mockito.when(world.getSeed()).thenReturn(WORLD_SEED);
        Mockito.when(world.getSeaLevel()).thenReturn(SEA_LEVEL);

        return world;
    }
}
