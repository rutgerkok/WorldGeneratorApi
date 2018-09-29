package nl.rutgerkok.worldgeneratorapi.test;

import static org.bukkit.block.Biome.DESERT;
import static org.bukkit.block.Biome.FOREST;
import static org.bukkit.block.Biome.JUNGLE;
import static org.bukkit.block.Biome.PLAINS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.bukkit.block.Biome;
import org.junit.jupiter.api.Test;

import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;

public class BiomeGeneratorTest {

    @Test
    public void scaling() {
        BiomeGenerator biomeGenerator = new BiomeGenerator() {

            @Override
            public Biome[] getZoomedOutBiomes(int minX, int minZ, int xSize, int zSize) {
                if (xSize != 2 || zSize != 2) {
                    throw new UnsupportedOperationException();
                }
                return new Biome[] {
                        PLAINS, DESERT,
                        FOREST, JUNGLE };
            }
        };

        assertArrayEquals(new Biome[] {
                PLAINS, PLAINS, PLAINS, PLAINS, DESERT, DESERT, DESERT, DESERT,
                PLAINS, PLAINS, PLAINS, PLAINS, DESERT, DESERT, DESERT, DESERT,
                PLAINS, PLAINS, PLAINS, PLAINS, DESERT, DESERT, DESERT, DESERT,
                PLAINS, PLAINS, PLAINS, PLAINS, DESERT, DESERT, DESERT, DESERT,
                FOREST, FOREST, FOREST, FOREST, JUNGLE, JUNGLE, JUNGLE, JUNGLE,
                FOREST, FOREST, FOREST, FOREST, JUNGLE, JUNGLE, JUNGLE, JUNGLE,
                FOREST, FOREST, FOREST, FOREST, JUNGLE, JUNGLE, JUNGLE, JUNGLE,
                FOREST, FOREST, FOREST, FOREST, JUNGLE, JUNGLE, JUNGLE, JUNGLE },
                biomeGenerator.getBiomes(0, 0, 8, 8));
    }
}
