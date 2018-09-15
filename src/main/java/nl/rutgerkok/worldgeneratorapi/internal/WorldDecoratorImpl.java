package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.EnumSet;
import java.util.List;
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
import net.minecraft.server.v1_13_R2.WorldGenStage;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator.GeneratingChunk;
import nl.rutgerkok.worldgeneratorapi.decoration.BaseDecorationType;
import nl.rutgerkok.worldgeneratorapi.decoration.Decoration;
import nl.rutgerkok.worldgeneratorapi.decoration.DecorationArea;
import nl.rutgerkok.worldgeneratorapi.decoration.DecorationType;
import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;

public final class WorldDecoratorImpl implements WorldDecorator {

    private final Map<DecorationType, List<Decoration>> customDecorations = new ConcurrentHashMap<>();
    private final Set<DecorationType> disabledDecorations = EnumSet.noneOf(DecorationType.class);
    private final Map<BaseDecorationType, List<BaseChunkGenerator>> customBaseDecorations = new ConcurrentHashMap<>();
    private final Set<BaseDecorationType> disabledBaseDecorations = EnumSet.noneOf(BaseDecorationType.class);

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
            DecorationType type = DecorationType.all().get(decorationStage.ordinal());

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
