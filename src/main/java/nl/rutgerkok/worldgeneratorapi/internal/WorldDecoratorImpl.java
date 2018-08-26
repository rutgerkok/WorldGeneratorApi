package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;

import net.minecraft.server.v1_13_R2.BiomeBase;
import net.minecraft.server.v1_13_R2.BlockFalling;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.BlockPosition.MutableBlockPosition;
import net.minecraft.server.v1_13_R2.ChunkGenerator;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.RegionLimitedWorldAccess;
import net.minecraft.server.v1_13_R2.SeededRandom;
import net.minecraft.server.v1_13_R2.WorldGenStage;

import nl.rutgerkok.worldgeneratorapi.decoration.Decoration;
import nl.rutgerkok.worldgeneratorapi.decoration.DecorationArea;
import nl.rutgerkok.worldgeneratorapi.decoration.DecorationType;
import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;

final class WorldDecoratorImpl implements WorldDecorator {

    private static class DecorationAreaImpl implements DecorationArea {

        final RegionLimitedWorldAccess internal;
        private final MutableBlockPosition blockPos = new MutableBlockPosition();

        private DecorationAreaImpl(RegionLimitedWorldAccess internal) {
            this.internal = Objects.requireNonNull(internal, "internal");
        }

        @Override
        public Biome getBiome(int x, int z) {
            blockPos.c(x, 0, z);
            return CraftBlock.biomeBaseToBiome(internal.getBiome(blockPos));
        }

        @Override
        public Material getBlock(int x, int y, int z) {
            return getBlockData(x, y, z).getMaterial();
        }

        @Override
        public BlockData getBlockData(int x, int y, int z) {
            blockPos.c(x, y, z);
            return CraftBlockData.fromData(internal.getType(blockPos));
        }

        @Override
        public int getCenterX() {
            return internal.a() * 16;
        }

        @Override
        public int getCenterZ() {
            return internal.b() * 16;
        }

        @Override
        public void setBlock(int x, int y, int z, Material material) {
            setBlockData(x, y, z, material.createBlockData());
        }

        @Override
        public void setBlockData(int x, int y, int z, BlockData blockData) {
            BlockPosition position = new BlockPosition(x, y, z);
            IBlockData mcBlockData = ((CraftBlockData) blockData).getState();
            internal.setTypeAndData(position, mcBlockData, 2);
        }

    }

    private final Map<DecorationType, List<Decoration>> customDecorators = new ConcurrentHashMap<>();
    private final Set<DecorationType> disabledDecorations = EnumSet.noneOf(DecorationType.class);

    @Override
    public List<Decoration> getCustomDecorations(DecorationType type) {
        Objects.requireNonNull(type, "type");
        return customDecorators.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>());
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

    void spawnDecorations(ChunkGenerator<?> chunkGenerator, RegionLimitedWorldAccess populationArea) {
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
            List<Decoration> decorations = this.customDecorators.get(type);
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
