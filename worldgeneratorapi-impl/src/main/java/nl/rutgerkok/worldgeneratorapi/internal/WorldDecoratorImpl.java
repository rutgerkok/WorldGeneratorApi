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

import net.minecraft.server.v1_14_R1.BiomeBase;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_14_R1.ChunkGenerator;
import net.minecraft.server.v1_14_R1.IChunkAccess;
import net.minecraft.server.v1_14_R1.RegionLimitedWorldAccess;
import net.minecraft.server.v1_14_R1.SeededRandom;
import net.minecraft.server.v1_14_R1.WorldGenCarverWrapper;
import net.minecraft.server.v1_14_R1.WorldGenStage;
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

    private BiomeBase getCarvingBiome(IChunkAccess ichunkaccess) {
        return ichunkaccess.getBiome(BlockPosition.ZERO);
    }

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

    public void spawnCarvers(IChunkAccess ichunkaccess, WorldGenStage.Features stage, int seaLevel, long seed) {
        SeededRandom seededrandom = new SeededRandom(seed);
        DecorationType decorationType = CARVER_TRANSLATION.get(stage);
        if (!this.disabledDecorations.contains(decorationType)) {
            // Spawn default carvers (code based on ChunkGenerator.doCarving)
            ChunkCoordIntPair chunkcoordintpair = ichunkaccess.getPos();
            int i = chunkcoordintpair.x;
            int j = chunkcoordintpair.z;
            BitSet bitset = ichunkaccess.a(stage);

            for (int k = i - 8; k <= i + 8; ++k) {
                for (int l = j - 8; l <= j + 8; ++l) {
                    List<WorldGenCarverWrapper<?>> list = this.getCarvingBiome(ichunkaccess).a(stage);
                    ListIterator<WorldGenCarverWrapper<?>> listiterator = list.listIterator();

                    while (listiterator.hasNext()) {
                        int i1 = listiterator.nextIndex();
                        WorldGenCarverWrapper<?> worldgencarverwrapper = listiterator.next();
                        seededrandom.c(seed + i1, k, l);
                        if (worldgencarverwrapper.a(seededrandom, k, l)) {
                            worldgencarverwrapper.a(ichunkaccess, seededrandom, seaLevel, k, l, i, j, bitset);
                        }
                    }
                }
            }
        }

        // Spawn custom carvers
        // TODO re-enable now that it operates
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
        int i = populationArea.a();
        int j = populationArea.b();
        int k = i * 16;
        int l = j * 16;
        BlockPosition blockposition = new BlockPosition(k, 0, l);
        BiomeBase biomebase = populationArea.getChunkAt(i + 1, j + 1).getBiomeIndex()[0];
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
    }

}
