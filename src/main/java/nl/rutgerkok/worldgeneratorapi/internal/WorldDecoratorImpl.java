package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.BitSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.server.v1_13_R2.BiomeBase;
import net.minecraft.server.v1_13_R2.BlockFalling;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.ChunkGenerator;
import net.minecraft.server.v1_13_R2.RegionLimitedWorldAccess;
import net.minecraft.server.v1_13_R2.SeededRandom;
import net.minecraft.server.v1_13_R2.WorldGenCarverWrapper;
import net.minecraft.server.v1_13_R2.WorldGenFeatureConfiguration;
import net.minecraft.server.v1_13_R2.WorldGenStage;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator.GeneratingChunk;
import nl.rutgerkok.worldgeneratorapi.decoration.BaseDecorationType;
import nl.rutgerkok.worldgeneratorapi.decoration.Decoration;
import nl.rutgerkok.worldgeneratorapi.decoration.DecorationArea;
import nl.rutgerkok.worldgeneratorapi.decoration.DecorationType;
import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;

public final class WorldDecoratorImpl implements WorldDecorator {

    private static final Map<WorldGenStage.Decoration, DecorationType> DECORATION_TRANSLATION;
    private static final Map<WorldGenStage.Features, DecorationType> CARVER_TRANSLATION;

    static {
        DECORATION_TRANSLATION = new EnumMap<>(WorldGenStage.Decoration.class);
        for (WorldGenStage.Decoration type : WorldGenStage.Decoration.values()) {
            DECORATION_TRANSLATION.put(type, DecorationType.valueOf(type.name()));
        }

        CARVER_TRANSLATION = new EnumMap<>(WorldGenStage.Features.class);
        for (WorldGenStage.Features type : WorldGenStage.Features.values()) {
            CARVER_TRANSLATION.put(type, DecorationType.valueOf("CARVING_" + type.name()));
        }
    }

    private final Map<DecorationType, List<Decoration>> customDecorations = new ConcurrentHashMap<>();
    private final Set<DecorationType> disabledDecorations = EnumSet.noneOf(DecorationType.class);

    private final Map<BaseDecorationType, List<BaseChunkGenerator>> customBaseDecorations = new ConcurrentHashMap<>();
    private final Set<BaseDecorationType> disabledBaseDecorations = EnumSet.noneOf(BaseDecorationType.class);

    @Override
    public List<BaseChunkGenerator> getCustomBaseDecorations(BaseDecorationType type) {
        Objects.requireNonNull(type, "type");
        return customBaseDecorations.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>());
    }

    @Override
    public List<Decoration> getCustomDecorations(DecorationType type) {
        Objects.requireNonNull(type, "type");
        return customDecorations.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>());
    }


    public boolean isDefaultEnabled(BaseDecorationType type) {
        return !this.disabledBaseDecorations.contains(type);
    }

    @Override
    public void setDefaultBaseDecoratorsEnabled(BaseDecorationType type, boolean enabled) {
        Objects.requireNonNull(type, "type");
        if (enabled) {
            this.disabledBaseDecorations.remove(type);
        } else {
            this.disabledBaseDecorations.add(type);
        }
    }

    @Override
    public void setDefaultDecoratorsEnabled(DecorationType type, boolean enabled) {
        Objects.requireNonNull(type, "type");
        if (enabled) {
            this.disabledDecorations.remove(type);
        } else {
            this.disabledDecorations.add(type);
        }
    }

    @SuppressWarnings("deprecation")
    public void spawnCarvers(RegionLimitedWorldAccess world, WorldGenStage.Features stage, SeededRandom seededrandom) {
        DecorationType decorationType = CARVER_TRANSLATION.get(stage);
        if (!this.disabledDecorations.contains(decorationType)) {
            // Spawn default carvers (code based on ChunkGeneratorAbstract.addFeatures)
            int chunkX = world.a();
            int chunkZ = world.b();
            BitSet bitset = world.b(chunkX, chunkZ).a(stage);
            for (int lookingChunkX = chunkX - 8; lookingChunkX <= chunkX + 8; ++lookingChunkX) {
                for (int lookingChunkZ = chunkZ - 8; lookingChunkZ <= chunkZ + 8; ++lookingChunkZ) {
                    List<WorldGenCarverWrapper<?>> list = world.getChunkProvider().getChunkGenerator()
                            .getWorldChunkManager()
                            .getBiome(new BlockPosition(lookingChunkX * 16, 0, lookingChunkZ * 16), null)
                            .a(stage);
                    ListIterator<WorldGenCarverWrapper<?>> listiterator = list.listIterator();
                    while (listiterator.hasNext()) {
                        int i2 = listiterator.nextIndex();
                        WorldGenCarverWrapper<?> worldgencarverwrapper = listiterator.next();
                        seededrandom.c(world.getMinecraftWorld().getSeed() + i2, lookingChunkX, lookingChunkZ);
                        if (worldgencarverwrapper.a(world, seededrandom, lookingChunkX, lookingChunkZ,
                                WorldGenFeatureConfiguration.e)) {
                            worldgencarverwrapper.a(world, seededrandom, lookingChunkX, lookingChunkZ, chunkX, chunkZ,
                                    bitset, WorldGenFeatureConfiguration.e);
                        }
                    }
                }
            }
        }

        // Spawn custom carvers
        List<Decoration> decorations = this.customDecorations.get(decorationType);
        if (decorations == null) {
            return;
        }
        DecorationArea decorationArea = new DecorationAreaImpl(world);
        for (Decoration decoration : decorations) {
            decoration.decorate(decorationArea, seededrandom);
        }
    }

    public void spawnCustomBaseDecorations(BaseDecorationType type, GeneratingChunk chunk) {
        // Generates custom decorations. Should be called after the default decorations
        // have been spawned for that type

        List<BaseChunkGenerator> decorations = this.customBaseDecorations.get(type);
        if (decorations != null) {
            for (BaseChunkGenerator decoration : decorations) {
                decoration.setBlocksInChunk(chunk);
            }
        }
    }

    public void spawnDecorations(ChunkGenerator<?> chunkGenerator, RegionLimitedWorldAccess populationArea) {
        BlockFalling.instaFall = true;
        int i = populationArea.a();
        int j = populationArea.b();
        int k = i * 16;
        int l = j * 16;
        BlockPosition blockposition = new BlockPosition(k, 0, l);
        BiomeBase biomebase = populationArea.b(i + 1, j + 1).getBiomeIndex()[0];
        SeededRandom seededrandom = new SeededRandom();
        DecorationArea decorationArea = new DecorationAreaImpl(populationArea);
        long chunkSeed = seededrandom.a(populationArea.getSeed(), k, l);
        for (WorldGenStage.Decoration decorationStage : WorldGenStage.Decoration.values()) {
            DecorationType type = DECORATION_TRANSLATION.get(decorationStage);

            // Spawn default decorations
            if (!this.disabledDecorations.contains(type)) {
                biomebase.a(decorationStage, chunkGenerator, populationArea, chunkSeed, seededrandom, blockposition);
            }

            // Spawn custom decorations
            List<Decoration> decorations = this.customDecorations.get(type);
            if (decorations == null) {
                continue;
            }
            int decorationIndex = 0;
            for (Decoration decoration : decorations) {
                if (decoration == null) {
                    continue;
                }
                seededrandom.b(chunkSeed, decorationIndex, type.ordinal());
                decoration.decorate(decorationArea, seededrandom);
                decorationIndex++;
            }
        }
        BlockFalling.instaFall = false;
    }

}
