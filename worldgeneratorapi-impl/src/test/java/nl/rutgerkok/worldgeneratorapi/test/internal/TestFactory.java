package nl.rutgerkok.worldgeneratorapi.test.internal;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.mockito.Mockito;

import net.minecraft.server.v1_13_R2.DispenserRegistry;

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

    public static World world(String name) {
        World world = Mockito.mock(World.class);
        Mockito.when(world.getName()).thenReturn(name);
        Mockito.when(world.getSeed()).thenReturn(WORLD_SEED);
        Mockito.when(world.getSeaLevel()).thenReturn(SEA_LEVEL);

        return world;
    }

}
