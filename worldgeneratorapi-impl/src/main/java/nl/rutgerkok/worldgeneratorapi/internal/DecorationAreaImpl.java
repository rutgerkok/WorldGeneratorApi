package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBanner;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBarrel;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBeacon;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBeehive;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBell;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlastFurnace;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBrewingStand;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftCampfire;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftChest;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftCommandBlock;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftComparator;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftCreatureSpawner;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftDaylightDetector;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftDispenser;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftDropper;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftEnchantingTable;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftEndGateway;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftEnderChest;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftFurnaceFurnace;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftHopper;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftJigsaw;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftJukebox;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftLectern;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftSculkSensor;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftShulkerBox;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftSign;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftSkull;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftSmoker;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftStructureBlock;
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
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.DaylightDetectorBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
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
        // This code is based on the following: (this code is similar, in that it
        // creates block entities that have not been added to a world)
        // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/inventory/CraftMetaBlockState.java?until=622cf6111905e787add191b9a88d62656439ef31&untilPath=src%2Fmain%2Fjava%2Forg%2Fbukkit%2Fcraftbukkit%2Finventory%2FCraftMetaBlockState.java#313
        // If new block entities are added, don't forget to add them here
        switch (material) {
            case ACACIA_SIGN:
            case ACACIA_WALL_SIGN:
            case BIRCH_SIGN:
            case BIRCH_WALL_SIGN:
            case CRIMSON_SIGN:
            case CRIMSON_WALL_SIGN:
            case DARK_OAK_SIGN:
            case DARK_OAK_WALL_SIGN:
            case JUNGLE_SIGN:
            case JUNGLE_WALL_SIGN:
            case OAK_SIGN:
            case OAK_WALL_SIGN:
            case SPRUCE_SIGN:
            case SPRUCE_WALL_SIGN:
            case WARPED_SIGN:
            case WARPED_WALL_SIGN:
                return new CraftSign(material, (SignBlockEntity) tileEntity);
            case CHEST:
            case TRAPPED_CHEST:
                return new CraftChest(material, (ChestBlockEntity) tileEntity);
            case FURNACE:
                return new CraftFurnaceFurnace(material, (FurnaceBlockEntity) tileEntity);
            case DISPENSER:
                return new CraftDispenser(material, (DispenserBlockEntity) tileEntity);
            case DROPPER:
                return new CraftDropper(material, (DropperBlockEntity) tileEntity);
            case END_GATEWAY:
                return new CraftEndGateway(material, (TheEndGatewayBlockEntity) tileEntity);
            case HOPPER:
                return new CraftHopper(material, (HopperBlockEntity) tileEntity);
            case SPAWNER:
                return new CraftCreatureSpawner(material, (SpawnerBlockEntity) tileEntity);
            case JUKEBOX:
                return new CraftJukebox(material, (JukeboxBlockEntity) tileEntity);
            case BREWING_STAND:
                return new CraftBrewingStand(material, (BrewingStandBlockEntity) tileEntity);
            case CREEPER_HEAD:
            case CREEPER_WALL_HEAD:
            case DRAGON_HEAD:
            case DRAGON_WALL_HEAD:
            case PLAYER_HEAD:
            case PLAYER_WALL_HEAD:
            case SKELETON_SKULL:
            case SKELETON_WALL_SKULL:
            case WITHER_SKELETON_SKULL:
            case WITHER_SKELETON_WALL_SKULL:
            case ZOMBIE_HEAD:
            case ZOMBIE_WALL_HEAD:
                return new CraftSkull(material, (SkullBlockEntity) tileEntity);
            case COMMAND_BLOCK:
            case REPEATING_COMMAND_BLOCK:
            case CHAIN_COMMAND_BLOCK:
                return new CraftCommandBlock(material, (CommandBlockEntity) tileEntity);
            case BEACON:
                return new CraftBeacon(material, (BeaconBlockEntity) tileEntity);
            case BLACK_BANNER:
            case BLACK_WALL_BANNER:
            case BLUE_BANNER:
            case BLUE_WALL_BANNER:
            case BROWN_BANNER:
            case BROWN_WALL_BANNER:
            case CYAN_BANNER:
            case CYAN_WALL_BANNER:
            case GRAY_BANNER:
            case GRAY_WALL_BANNER:
            case GREEN_BANNER:
            case GREEN_WALL_BANNER:
            case LIGHT_BLUE_BANNER:
            case LIGHT_BLUE_WALL_BANNER:
            case LIGHT_GRAY_BANNER:
            case LIGHT_GRAY_WALL_BANNER:
            case LIME_BANNER:
            case LIME_WALL_BANNER:
            case MAGENTA_BANNER:
            case MAGENTA_WALL_BANNER:
            case ORANGE_BANNER:
            case ORANGE_WALL_BANNER:
            case PINK_BANNER:
            case PINK_WALL_BANNER:
            case PURPLE_BANNER:
            case PURPLE_WALL_BANNER:
            case RED_BANNER:
            case RED_WALL_BANNER:
            case WHITE_BANNER:
            case WHITE_WALL_BANNER:
            case YELLOW_BANNER:
            case YELLOW_WALL_BANNER:
                return new CraftBanner(material, (BannerBlockEntity) tileEntity);
            case STRUCTURE_BLOCK:
                return new CraftStructureBlock(material, (StructureBlockEntity) tileEntity);
            case SHULKER_BOX:
            case WHITE_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case RED_SHULKER_BOX:
            case BLACK_SHULKER_BOX:
                return new CraftShulkerBox(material, (ShulkerBoxBlockEntity) tileEntity);
            case ENCHANTING_TABLE:
                return new CraftEnchantingTable(material, (EnchantmentTableBlockEntity) tileEntity);
            case ENDER_CHEST:
                return new CraftEnderChest(material, (EnderChestBlockEntity) tileEntity);
            case DAYLIGHT_DETECTOR:
                return new CraftDaylightDetector(material, (DaylightDetectorBlockEntity) tileEntity);
            case COMPARATOR:
                return new CraftComparator(material, (ComparatorBlockEntity) tileEntity);
            case BARREL:
                return new CraftBarrel(material, (BarrelBlockEntity) tileEntity);
            case BELL:
                return new CraftBell(material, (BellBlockEntity) tileEntity);
            case BLAST_FURNACE:
                return new CraftBlastFurnace(material, (BlastFurnaceBlockEntity) tileEntity);
            case CAMPFIRE:
                return new CraftCampfire(material, (CampfireBlockEntity) tileEntity);
            case JIGSAW:
                return new CraftJigsaw(material, (JigsawBlockEntity) tileEntity);
            case LECTERN:
                return new CraftLectern(material, (LecternBlockEntity) tileEntity);
            case SMOKER:
                return new CraftSmoker(material, (SmokerBlockEntity) tileEntity);
            case BEE_NEST:
            case BEEHIVE:
                return new CraftBeehive(material, (BeehiveBlockEntity) tileEntity);
            case SCULK_SENSOR:
                return new CraftSculkSensor(material, (SculkSensorBlockEntity) tileEntity);
            default:
                return new CraftBlockState(material);
        }
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
