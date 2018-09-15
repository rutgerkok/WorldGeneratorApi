package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBanner;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBeacon;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBrewingStand;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftChest;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftCommandBlock;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftComparator;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftCreatureSpawner;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftDaylightDetector;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftDispenser;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftDropper;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftEnchantingTable;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftEndGateway;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftEnderChest;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftFurnace;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftHopper;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftJukebox;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftShulkerBox;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftSign;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftSkull;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftStructureBlock;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;

import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.IChunkAccess;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.RegionLimitedWorldAccess;
import net.minecraft.server.v1_13_R2.TileEntity;
import net.minecraft.server.v1_13_R2.TileEntityBanner;
import net.minecraft.server.v1_13_R2.TileEntityBeacon;
import net.minecraft.server.v1_13_R2.TileEntityBrewingStand;
import net.minecraft.server.v1_13_R2.TileEntityChest;
import net.minecraft.server.v1_13_R2.TileEntityCommand;
import net.minecraft.server.v1_13_R2.TileEntityComparator;
import net.minecraft.server.v1_13_R2.TileEntityDispenser;
import net.minecraft.server.v1_13_R2.TileEntityDropper;
import net.minecraft.server.v1_13_R2.TileEntityEnchantTable;
import net.minecraft.server.v1_13_R2.TileEntityEndGateway;
import net.minecraft.server.v1_13_R2.TileEntityEnderChest;
import net.minecraft.server.v1_13_R2.TileEntityFurnace;
import net.minecraft.server.v1_13_R2.TileEntityHopper;
import net.minecraft.server.v1_13_R2.TileEntityJukeBox;
import net.minecraft.server.v1_13_R2.TileEntityLightDetector;
import net.minecraft.server.v1_13_R2.TileEntityMobSpawner;
import net.minecraft.server.v1_13_R2.TileEntityShulkerBox;
import net.minecraft.server.v1_13_R2.TileEntitySign;
import net.minecraft.server.v1_13_R2.TileEntitySkull;
import net.minecraft.server.v1_13_R2.TileEntityStructure;
import net.minecraft.server.v1_13_R2.BlockPosition.MutableBlockPosition;
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
        // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/inventory/CraftMetaBlockState.java?at=421c1728c81e2f729dff88da2ac96535d2b8e5e8#227
        // If new tile entities are added, don't forget to add them here
        switch (material) {
            case SIGN:
            case WALL_SIGN:
                return new CraftSign(material, (TileEntitySign) tileEntity);
            case CHEST:
            case TRAPPED_CHEST:
                return new CraftChest(material, (TileEntityChest) tileEntity);
            case FURNACE:
                return new CraftFurnace(material, (TileEntityFurnace) tileEntity);
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
            IChunkAccess chunk = internal.y(position);
            NBTTagCompound tag = ((CraftBlockEntityState<?>) blockState).getSnapshotNBT();
            TileEntity tileEntity = TileEntity.create(tag);
            chunk.a(position, tileEntity);
        }
    }

}