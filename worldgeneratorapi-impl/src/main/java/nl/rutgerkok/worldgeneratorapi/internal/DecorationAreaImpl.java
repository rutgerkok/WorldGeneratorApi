package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBanner;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBarrel;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBeacon;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBeehive;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBell;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBlastFurnace;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBrewingStand;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftCampfire;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftChest;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftCommandBlock;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftComparator;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftCreatureSpawner;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftDaylightDetector;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftDispenser;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftDropper;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftEnchantingTable;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftEndGateway;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftEnderChest;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftFurnace;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftHopper;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftJigsaw;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftJukebox;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftLectern;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftShulkerBox;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftSign;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftSkull;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftSmoker;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftStructureBlock;
import org.bukkit.craftbukkit.v1_16_R1.block.data.CraftBlockData;

import net.minecraft.server.v1_16_R1.BlockPosition;
import net.minecraft.server.v1_16_R1.BlockPosition.MutableBlockPosition;
import net.minecraft.server.v1_16_R1.IBlockData;
import net.minecraft.server.v1_16_R1.IChunkAccess;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import net.minecraft.server.v1_16_R1.RegionLimitedWorldAccess;
import net.minecraft.server.v1_16_R1.TileEntity;
import net.minecraft.server.v1_16_R1.TileEntityBanner;
import net.minecraft.server.v1_16_R1.TileEntityBarrel;
import net.minecraft.server.v1_16_R1.TileEntityBeacon;
import net.minecraft.server.v1_16_R1.TileEntityBeehive;
import net.minecraft.server.v1_16_R1.TileEntityBell;
import net.minecraft.server.v1_16_R1.TileEntityBlastFurnace;
import net.minecraft.server.v1_16_R1.TileEntityBrewingStand;
import net.minecraft.server.v1_16_R1.TileEntityCampfire;
import net.minecraft.server.v1_16_R1.TileEntityChest;
import net.minecraft.server.v1_16_R1.TileEntityCommand;
import net.minecraft.server.v1_16_R1.TileEntityComparator;
import net.minecraft.server.v1_16_R1.TileEntityDispenser;
import net.minecraft.server.v1_16_R1.TileEntityDropper;
import net.minecraft.server.v1_16_R1.TileEntityEnchantTable;
import net.minecraft.server.v1_16_R1.TileEntityEndGateway;
import net.minecraft.server.v1_16_R1.TileEntityEnderChest;
import net.minecraft.server.v1_16_R1.TileEntityFurnace;
import net.minecraft.server.v1_16_R1.TileEntityHopper;
import net.minecraft.server.v1_16_R1.TileEntityJigsaw;
import net.minecraft.server.v1_16_R1.TileEntityJukeBox;
import net.minecraft.server.v1_16_R1.TileEntityLectern;
import net.minecraft.server.v1_16_R1.TileEntityLightDetector;
import net.minecraft.server.v1_16_R1.TileEntityMobSpawner;
import net.minecraft.server.v1_16_R1.TileEntityShulkerBox;
import net.minecraft.server.v1_16_R1.TileEntitySign;
import net.minecraft.server.v1_16_R1.TileEntitySkull;
import net.minecraft.server.v1_16_R1.TileEntitySmoker;
import net.minecraft.server.v1_16_R1.TileEntityStructure;
import nl.rutgerkok.worldgeneratorapi.decoration.DecorationArea;

class DecorationAreaImpl implements DecorationArea {

    final RegionLimitedWorldAccess internal;

    /**
     * Only one thread is working on a single decoration area, so no need to worry
     * about thread-safety for this mutable field.
     */
    private final MutableBlockPosition reusableBlockPos = new MutableBlockPosition();

    DecorationAreaImpl(RegionLimitedWorldAccess internal) {
        this.internal = Objects.requireNonNull(internal, "internal");
    }

    @Override
    public Biome getBiome(int x, int z) {
        reusableBlockPos.c(x, 0, z);
        return CraftBlock.biomeBaseToBiome(internal.getBiome(reusableBlockPos));
    }

    @Override
    public Material getBlock(int x, int y, int z) {
        return getBlockData(x, y, z).getMaterial();
    }

    @Override
    public BlockData getBlockData(int x, int y, int z) {
        reusableBlockPos.c(x, y, z);
        return CraftBlockData.fromData(internal.getType(reusableBlockPos));
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        BlockPosition position = new BlockPosition(x, y, z);
        TileEntity tileEntity = internal.getTileEntity(position);
        Material material = CraftBlockData.fromData(internal.getType(position)).getMaterial();
        // This code is based on the following: (this code is similar, in that it
        // creates tile entities that have not been added to a world)
        // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/inventory/CraftMetaBlockState.java?until=3f0c333870ba74705e98d19322174d6f0c10c900&untilPath=src%2Fmain%2Fjava%2Forg%2Fbukkit%2Fcraftbukkit%2Finventory%2FCraftMetaBlockState.java#302
        // If new tile entities are added, don't forget to add them here
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
                return new CraftSign(material, (TileEntitySign) tileEntity);
            case CHEST:
            case TRAPPED_CHEST:
                return new CraftChest(material, (TileEntityChest) tileEntity);
            case FURNACE:
                return new CraftFurnace<>(material, (TileEntityFurnace) tileEntity);
            case DISPENSER:
                return new CraftDispenser(material, (TileEntityDispenser) tileEntity);
            case DROPPER:
                return new CraftDropper(material, (TileEntityDropper) tileEntity);
            case END_GATEWAY:
                return new CraftEndGateway(material, (TileEntityEndGateway) tileEntity);
            case HOPPER:
                return new CraftHopper(material, (TileEntityHopper) tileEntity);
            case SPAWNER:
                return new CraftCreatureSpawner(material, (TileEntityMobSpawner) tileEntity);
            case JUKEBOX:
                return new CraftJukebox(material, (TileEntityJukeBox) tileEntity);
            case BREWING_STAND:
                return new CraftBrewingStand(material, (TileEntityBrewingStand) tileEntity);
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
                return new CraftSkull(material, (TileEntitySkull) tileEntity);
            case COMMAND_BLOCK:
            case REPEATING_COMMAND_BLOCK:
            case CHAIN_COMMAND_BLOCK:
                return new CraftCommandBlock(material, (TileEntityCommand) tileEntity);
            case BEACON:
                return new CraftBeacon(material, (TileEntityBeacon) tileEntity);
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
                return new CraftBanner(material, (TileEntityBanner) tileEntity);
            case STRUCTURE_BLOCK:
                return new CraftStructureBlock(material, (TileEntityStructure) tileEntity);
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
                return new CraftShulkerBox(material, (TileEntityShulkerBox) tileEntity);
            case ENCHANTING_TABLE:
                return new CraftEnchantingTable(material, (TileEntityEnchantTable) tileEntity);
            case ENDER_CHEST:
                return new CraftEnderChest(material, (TileEntityEnderChest) tileEntity);
            case DAYLIGHT_DETECTOR:
                return new CraftDaylightDetector(material, (TileEntityLightDetector) tileEntity);
            case COMPARATOR:
                return new CraftComparator(material, (TileEntityComparator) tileEntity);
            case BARREL:
                return new CraftBarrel(material, (TileEntityBarrel) tileEntity);
            case BELL:
                return new CraftBell(material, (TileEntityBell) tileEntity);
            case BLAST_FURNACE:
                return new CraftBlastFurnace(material, (TileEntityBlastFurnace) tileEntity);
            case CAMPFIRE:
                return new CraftCampfire(material, (TileEntityCampfire) tileEntity);
            case JIGSAW:
                return new CraftJigsaw(material, (TileEntityJigsaw) tileEntity);
            case LECTERN:
                return new CraftLectern(material, (TileEntityLectern) tileEntity);
            case SMOKER:
                return new CraftSmoker(material, (TileEntitySmoker) tileEntity);
            case BEE_NEST:
            case BEEHIVE:
                return new CraftBeehive(material, (TileEntityBeehive) tileEntity);
            default:
                return new CraftBlockState(material);
        }
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

    @Override
    public void setBlockState(int x, int y, int z, BlockState blockState) {
        BlockPosition position = new BlockPosition(x, y, z);

        // Update basic material
        IBlockData mcBlockData = ((CraftBlockState) blockState).getHandle();
        internal.setTypeAndData(position, mcBlockData, 2);

        // Update TileEntity data
        if (blockState instanceof CraftBlockEntityState) {
            IChunkAccess chunk = internal.z(position);
            NBTTagCompound tag = ((CraftBlockEntityState<?>) blockState).getSnapshotNBT();
            TileEntity tileEntity = TileEntity.create(mcBlockData, tag);
            chunk.setTileEntity(position, tileEntity);
        }
    }

}