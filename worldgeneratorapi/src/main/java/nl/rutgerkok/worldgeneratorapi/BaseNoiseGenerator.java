package nl.rutgerkok.worldgeneratorapi;

public interface BaseNoiseGenerator {
    /**
     * Represents the noise calculator.
     * 
     * @param buffer
     * @param x
     * @param z
     */
    void getNoise(BiomeGenerator biomeGenerator, double[] buffer, int x, int z);
}
