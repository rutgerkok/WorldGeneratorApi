package nl.rutgerkok.worldgeneratorapi.test.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bukkit.block.Biome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.v1_16_R2.RegistryGeneration;
import net.minecraft.server.v1_16_R2.WorldChunkManager;
import net.minecraft.server.v1_16_R2.WorldChunkManagerOverworld;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.BiomeGeneratorImpl;
import nl.rutgerkok.worldgeneratorapi.internal.InjectedBiomeGenerator;

public class BiomeGeneratorImplTest {

    @BeforeAll
    public static void bootstrap() {
        TestFactory.activateTestServer();
    }

    @Test
    public void isGetStructureBiomesInSync() {
        // Create vanilla biome generator
        WorldChunkManager worldChunkManager = new WorldChunkManagerOverworld(10, false, false,
                RegistryGeneration.WORLDGEN_BIOME);

        // Check the structures
        BiomeGeneratorImpl biomeGenerator = new BiomeGeneratorImpl(worldChunkManager);
        assertEquals(BiomeGenerator.VANILLA_OVERWORLD_STRUCTURE_BIOMES, biomeGenerator.getStructureBiomes());
    }

    @Test
    public void noDoubleWrapping() {
        BiomeGenerator ours = (x, y, z) -> Biome.PLAINS;

        assertThrows(IllegalArgumentException.class, () -> new BiomeGeneratorImpl(new InjectedBiomeGenerator(ours)));
    }
}
