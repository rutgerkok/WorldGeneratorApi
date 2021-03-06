package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_17_R1.block.data.CraftBlockData;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class ChunkDataImpl implements ChunkData {
    private final ChunkAccess internal;

    private final int xOffset;
    private final int zOffset;
    private final MutableBlockPos reusableBlockPos = new MutableBlockPos();

    ChunkDataImpl(ChunkAccess internal) {
        this.internal = internal;

        this.xOffset = internal.getPos().x * 16;
        this.zOffset = internal.getPos().z * 16;
    }

    @Override
    public BlockData getBlockData(int x, int y, int z) {
        reusableBlockPos.set(xOffset + x, y, zOffset + z);
        return CraftBlockData.fromData(internal.getBlockState(reusableBlockPos));
    }

    @Override
    @Deprecated
    public byte getData(int x, int y, int z) {
        throw new UnsupportedOperationException("block data bytes are deprecated, use the BlockData class");
    }

    public ChunkAccess getHandle() {
        return this.internal;
    }

    @Override
    public int getMaxHeight() {
        return this.internal.getMaxBuildHeight();
    }

    @Override
    public int getMinHeight() {
        return internal.getMinBuildHeight();
    }

    @Override
    public Material getType(int x, int y, int z) {
        return getBlockData(x, y, z).getMaterial();
    }

    @Override
    @Deprecated
    public org.bukkit.material.MaterialData getTypeAndData(int x, int y, int z) {
        throw new UnsupportedOperationException("MaterialData is deprecated, use BlockData instead");
    }

    @Override
    public void setBlock(int x, int y, int z, BlockData blockData) {
        setBlock(x, y, z, ((CraftBlockData) blockData).getState());
    }

    private void setBlock(int x, int y, int z, BlockState blockData) {
        reusableBlockPos.c(xOffset + x, y, zOffset + z);
        internal.setBlockState(reusableBlockPos, blockData, false);
    }

    @Override
    public void setBlock(int x, int y, int z, Material material) {
        setBlock(x, y, z, material.createBlockData());
    }

    @Override
    @Deprecated
    public void setBlock(int x, int y, int z, org.bukkit.material.MaterialData material) {
        throw new UnsupportedOperationException("MaterialData is deprecated, use BlockData");
    }

    @Override
    public void setRegion(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, BlockData blockData) {
        BlockState mcBlockData = ((CraftBlockData) blockData).getState();
        for (int y = yMin; y < yMax; y++) {
            for (int x = xMin; x < xMax; x++) {
                for (int z = zMin; z < zMax; z++) {
                    setBlock(x, y, z, mcBlockData);
                }
            }
        }
    }

    @Override
    public void setRegion(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, Material material) {
        setRegion(xMin, yMin, zMin, xMax, yMax, zMax, material.createBlockData());
    }

    @Override
    @Deprecated
    public void setRegion(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax,
            org.bukkit.material.MaterialData material) {
        throw new UnsupportedOperationException("MaterialData is deprecated, use BlockData");
    }
}