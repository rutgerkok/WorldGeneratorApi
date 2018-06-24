package nl.rutgerkok.worldgeneratorapi;

import org.bukkit.generator.ChunkGenerator;

public interface WorldGeneratorBuilder {

    /**
     * Creates a chunk generator using the world generator settings provided in this
     * class.
     *
     * @return A chunk generator.
     */
    ChunkGenerator create();

}
