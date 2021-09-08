package nl.rutgerkok.worldgeneratorapi.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.block.Biome;
import org.junit.jupiter.api.Test;

import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;

@Deprecated(forRemoval = true)
public class BiomeGeneratorTest {

    @Test
    public void defaultMethod() {
        BiomeGenerator biomeGenerator = new BiomeGenerator() {

            @Override
            public Biome getZoomedOutBiome(int x, int y, int z) {
                if (y < 20) {
                    return Biome.PLAINS;
                }
                return Biome.SNOWY_TUNDRA;
            }
        };

        assertEquals(Biome.PLAINS, biomeGenerator.getZoomedOutBiome(0, 0));
    }
}
