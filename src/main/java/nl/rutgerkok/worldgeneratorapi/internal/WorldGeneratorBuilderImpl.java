package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Objects;
import java.util.function.Function;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldGeneratorBuilder;
import nl.rutgerkok.worldgeneratorapi.WorldRef;

final class WorldGeneratorBuilderImpl implements WorldGeneratorBuilder {

    private final WorldRef world;
    private final WorldGeneratorApiImpl worldGeneratorApi;
    private final Function<World, BaseChunkGenerator> baseChunkGenerator;

    WorldGeneratorBuilderImpl(WorldGeneratorApiImpl worldGeneratorApi, WorldRef world,
            Function<World, BaseChunkGenerator> base) {
        this.world = Objects.requireNonNull(world, "world");
        this.worldGeneratorApi = Objects.requireNonNull(worldGeneratorApi, "worldGeneratorApi");
        this.baseChunkGenerator = Objects.requireNonNull(base, "baseChunkGenerator");
    }

    @Override
    public ChunkGenerator create() {
        return new ChunkGeneratorImpl(worldGeneratorApi, world, baseChunkGenerator);
    }


}
