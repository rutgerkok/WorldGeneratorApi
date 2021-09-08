package nl.rutgerkok.worldgeneratorapi.test.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.OverworldBiomeSource;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.BiomeGeneratorImpl;
import nl.rutgerkok.worldgeneratorapi.internal.InjectedBiomeGenerator;

@Deprecated
public class BiomeGeneratorImplTest {

    @BeforeAll
    public static void bootstrap() {
        TestFactory.activateTestServer();
    }

    @Test
    public void isGetStructureBiomesInSync() {
        // Create vanilla biome generator
        Registry<Biome> biomeRegistry = RegistryAccess.builtin().registryOrThrow(Registry.BIOME_REGISTRY);
        BiomeSource worldChunkManager = new OverworldBiomeSource(10, false, false, biomeRegistry);

        // Check the structures
        BiomeGeneratorImpl biomeGenerator = new BiomeGeneratorImpl(biomeRegistry, worldChunkManager);
        assertEquals(BiomeGenerator.VANILLA_OVERWORLD_STRUCTURE_BIOMES, biomeGenerator.getStructureBiomes());
    }

    @Test
    public void noDoubleWrapping() {
        BiomeGenerator ours = (x, y, z) -> org.bukkit.block.Biome.PLAINS;
        Registry<Biome> biomeRegistry = RegistryAccess.builtin().registryOrThrow(Registry.BIOME_REGISTRY);

        assertThrows(IllegalArgumentException.class,
                () -> new BiomeGeneratorImpl(biomeRegistry,
                        new InjectedBiomeGenerator(biomeRegistry, ours)));
    }
}
