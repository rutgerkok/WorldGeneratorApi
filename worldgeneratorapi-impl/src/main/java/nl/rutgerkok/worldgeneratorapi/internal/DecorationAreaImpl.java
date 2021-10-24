package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlockStates;
import org.bukkit.craftbukkit.v1_17_R1.block.data.CraftBlockData;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.rutgerkok.worldgeneratorapi.decoration.DecorationArea;

@Deprecated
class DecorationAreaImpl implements DecorationArea {

    final WorldGenRegion region;
    final ChunkPos chunkPos;

    /**
     * Only one thread is working on a single decoration area, so no need to worry
     * about thread-safety for this mutable field.
     */
    private final MutableBlockPos reusableBlockPos = new MutableBlockPos();

    DecorationAreaImpl(WorldGenRegion region, ChunkPos chunkPos) {
        this.region = Objects.requireNonNull(region, "region");
        this.chunkPos = Objects.requireNonNull(chunkPos, "chunkPos");
    }

    @Override
    public org.bukkit.block.Biome getBiome(int x, int z) {
        reusableBlockPos.set(x, 0, z);
        Registry<Biome> biomeRegistry = this.region.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        return CraftBlock.biomeBaseToBiome(biomeRegistry, region.getBiome(reusableBlockPos));
    }

    @Override
    public Material getBlock(int x, int y, int z) {
        return getBlockData(x, y, z).getMaterial();
    }

    @Override
    public BlockData getBlockData(int x, int y, int z) {
        reusableBlockPos.set(x, y, z);
        return CraftBlockData.fromData(region.getBlockState(reusableBlockPos));
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        BlockPos position = new BlockPos(x, y, z);

        BlockEntity tileEntity = region.getBlockEntity(position);
        Material material = CraftBlockData.fromData(region.getBlockState(position)).getMaterial();
        CompoundTag tag = tileEntity.getUpdateTag();

        return CraftBlockStates.getBlockState(position, material, tag);
    }

    @Override
    public int getCenterX() {
        // Center of 2x2 chunks, so min of this chunk
        return chunkPos.getMinBlockX();
    }

    @Override
    public int getCenterZ() {
        // Center of 2x2 chunks, so min of this chunk
        return chunkPos.getMinBlockZ();
    }

    @Override
    public int getHighestBlockYAt(int x, int z) {
        return this.region.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }

    @Override
    public void setBlock(int x, int y, int z, Material material) {
        setBlockData(x, y, z, material.createBlockData());
    }

    @Override
    public void setBlockData(int x, int y, int z, BlockData blockData) {
        BlockPos position = new BlockPos(x, y, z);
        net.minecraft.world.level.block.state.BlockState mcBlockData = ((CraftBlockData) blockData).getState();
        region.setBlock(position, mcBlockData, 2);
    }

    @Override
    public void setBlockState(int x, int y, int z, BlockState blockState) {
        BlockPos position = new BlockPos(x, y, z);

        // Update basic material
        net.minecraft.world.level.block.state.BlockState mcBlockData = ((CraftBlockState) blockState).getHandle();
        region.setBlock(position, mcBlockData, 2);

        // Update BlockEntity data
        if (blockState instanceof CraftBlockEntityState) {
            ChunkAccess chunk = region.getChunk(position);
            CompoundTag tag = ((CraftBlockEntityState<?>) blockState).getSnapshotNBT();
            BlockEntity tileEntity = BlockEntity.loadStatic(position, mcBlockData, tag);
            chunk.setBlockEntity(tileEntity);
        }
    }

    @Override
    public <T extends Entity> T spawnEntity(Class<T> entityClass, double x, double y, double z)
            throws IllegalArgumentException {
        // Inspired on
        // https://github.com/PaperMC/Paper/blob/c4c6e26c00665989d3c1c82fb115a8f7f8de659b/patches/server/0714-Add-Feature-Generation-API.patch
        Objects.requireNonNull(entityClass, "entityClass");

        CraftWorld world = this.region.getMinecraftWorld().getWorld();
        net.minecraft.world.entity.Entity entity = world.createEntity(new Location(world, x, y, z), entityClass);
        if (entity == null) {
            throw new IllegalArgumentException("No entity for " + entityClass);
        }
        if (entity instanceof Mob) {
            ((Mob) entity).finalizeSpawn(this.region, this.region
                    .getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.COMMAND, (SpawnGroupData) null, null);
        }

        // SpawnReason is unused by WorldGenRegion
        this.region.addEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);

        // Should be safe, see world.createEntity
        @SuppressWarnings("unchecked")
        T bukkitEntity = (T) entity.getBukkitEntity();
        return bukkitEntity;
    }

}
