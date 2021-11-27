package nl.rutgerkok.worldgeneratorapi.internal.command;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.bukkit.Color;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockVector;
import org.bukkit.util.StringUtil;

public class BiomeMapCommand extends WorldGeneratorApiCommand {

    private class BiomeMapper {
        private final int minX;
        private final int minZ;
        private final int width;
        private final int height;
        private final int pixelToBlockScale;
        private final int y;

        private final BiomeProvider biomeProvider;
        private final WorldInfo worldInfo;
        private final File outputFile;

        /**
         * Progress on a scale of 0 to 1.
         */
        private volatile float progress;

        private BiomeMapper(WorldInfo worldInfo, BiomeProvider biomeProvider,
                BlockVector min, int pixelWidth, int pixelHeight, int pixelToBlockScale,
                File outputFile) {
            this.worldInfo = Objects.requireNonNull(worldInfo, "worldInfo");
            this.biomeProvider = Objects.requireNonNull(biomeProvider, "biomeProvider");

            this.minX = min.getBlockX();
            this.y = min.getBlockY();
            this.minZ = min.getBlockZ();
            this.width = pixelWidth;
            this.height = pixelHeight;
            this.pixelToBlockScale = pixelToBlockScale;

            this.outputFile = Objects.requireNonNull(outputFile, "outputFile");
        }

        private void run() throws IOException {
            BufferedImage biomeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            int pixelsDone = 0;
            double totalPixels = this.width * this.height;
            for (int i = 0; i < width; i++ ) {
                for (int j = 0; j < height; j++) {
                    int x = this.minX + i * pixelToBlockScale;
                    int z = this.minZ + j * pixelToBlockScale;
                    Biome biome = this.biomeProvider.getBiome(worldInfo, x, y, z);
                    Color color = biomeColors.getOrDefault(biome, Color.BLACK);
                    biomeImage.setRGB(i, j, color.asRGB());

                    // Multiply the progress by 0.98, as last 2% are reserved for writing the image
                    this.progress = (float) ((pixelsDone / totalPixels) * 0.98);
                    pixelsDone++;
                }
            }
            ImageIO.write(biomeImage, "png", outputFile);
            this.progress = 1; // Done!
        }
    }

    private class MapArguments {
        Optional<World> mapWorld = Optional.empty();
        OptionalInt width = OptionalInt.empty();
        OptionalInt height = OptionalInt.empty();
        OptionalInt scale = OptionalInt.empty();
        OptionalInt centerX = OptionalInt.empty();
        OptionalInt centerZ = OptionalInt.empty();
        OptionalInt y = OptionalInt.empty();

        List<String> getRemainingParameters() {
            List<String> remainingParameters = new ArrayList<>();
            if (this.mapWorld.isEmpty()) {
                remainingParameters.add("-m");
            }
            if (this.width.isEmpty()) {
                remainingParameters.add("-w");
            }
            if (this.height.isEmpty()) {
                remainingParameters.add("-h");
            }
            if (this.scale.isEmpty()) {
                remainingParameters.add("-s");
            }
            if (this.centerX.isEmpty()) {
                remainingParameters.add("-x");
            }
            if (this.y.isEmpty()) {
                remainingParameters.add("-y");
            }
            if (this.centerZ.isEmpty()) {
                remainingParameters.add("-z");
            }
            return remainingParameters;
        }

        public void guessRemaining(CommandSender sender) {
            if (this.width.isEmpty()) {
                this.width = OptionalInt.of(1024);
            }
            if (this.height.isEmpty()) {
                this.height = this.width;
            }
            if (this.scale.isEmpty()) {
                this.scale = OptionalInt.of(8);
            }
            if (this.y.isEmpty()) {
                this.y = OptionalInt.of(256);
            }
            if (sender instanceof Player player) {
                if (this.centerX.isEmpty()) {
                    this.centerX = OptionalInt.of(player.getLocation().getBlockX());
                }
                if (this.centerZ.isEmpty()) {
                    this.centerZ = OptionalInt.of(player.getLocation().getBlockZ());
                }
                if (this.mapWorld.isEmpty()) {
                    this.mapWorld = Optional.of(player.getWorld());
                }
            } else {
                if (this.centerX.isEmpty()) {
                    this.centerX = OptionalInt.of(0);
                }
                if (this.centerZ.isEmpty()) {
                    this.centerZ = OptionalInt.of(0);
                }
                if (this.mapWorld.isEmpty()) {
                    this.mapWorld = Optional.of(sender.getServer().getWorlds().get(0));
                }
            }
        }

        public void parse(Server server, String[] args) throws ParseException {
            for (int i = 0; i < args.length - 1; i += 2) {
                String argName = args[i];
                String argValue = args[i + 1];
                if (!getRemainingParameters().contains(argName)) {
                    throw new ParseException("Unknown/duplicate argument name", 0);
                }
                switch (argName.toLowerCase(Locale.ROOT)) {
                    case "-m" -> this.mapWorld = parseWorld(server, argValue);
                    case "-w" -> this.width = parseInt(argValue);
                    case "-h" -> this.height = parseInt(argValue);
                    case "-s" -> this.scale = parseInt(argValue);
                    case "-x" -> this.centerX = parseInt(argValue);
                    case "-y" -> this.y = parseInt(argValue);
                    case "-z" -> this.centerZ = parseInt(argValue);
                }
            }
            if (args.length % 2 != 0) {
                throw new ParseException("Invalid number of argument", 0);
            }
        }

        private OptionalInt parseInt(String argValue) throws ParseException {
            try {
                return OptionalInt.of(Integer.parseInt(argValue));
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid number: " + argValue, 0);
            }
        }

        private Optional<World> parseWorld(Server server, String argValue) throws ParseException {
            World world = server.getWorld(argValue);
            if (world == null) {
                throw new ParseException("Unknown world: " + argValue, 0);
            }
            return Optional.of(world);
        }

        private String s(OptionalInt value) {
            if (value.isEmpty()) {
                return "?";
            }
            return String.valueOf(value.orElseThrow());
        }

        @Override
        public String toString() {
            return "-m " + this.mapWorld.map(World::getName).orElse("?") + " -w " + s(this.width)
                    + " -h " + s(this.height) + " -s " + s(this.scale) + " -x " + s(this.centerX) + " -y " + s(this.y)
                    + " -z " + s(this.centerZ);
        }
    }

    private final Map<Biome, Color> biomeColors = new HashMap<>();
    private final Function<World, BiomeProvider> getBiomeProvider;
    private final Plugin plugin;

    public BiomeMapCommand(Plugin plugin, Function<World, BiomeProvider> getBiomeProvider) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.getBiomeProvider = Objects.requireNonNull(getBiomeProvider, "getBiomeProvider");
        registerBiomeColors();
    }

    private void addBiome(Biome biome, Color color) {
        if (this.biomeColors.put(biome, color) != null) {
            throw new IllegalArgumentException("Duplicate color for " + biome);
        }
    }

    @Override
    String getSyntax(String label) {
        return "[-m <world name>] [-w <width>] [-h <height] [-s <scale>] [-x <center x>] [-y <map y>] [-z <center z]";
    }

    @Override
    boolean onCommand(CommandSender sender, String label, String[] args) {
        MapArguments mapArguments = new MapArguments();
        try {
            mapArguments.parse(sender.getServer(), args);
        } catch (ParseException e) {
            sender.sendMessage(ERROR_COLOR + "Cannot understand your input: " + e.getMessage());
            return true;
            }
        mapArguments.guessRemaining(sender);

        World world = mapArguments.mapWorld.orElseThrow();
        BiomeProvider biomeProvider = getBiomeProvider.apply(world);
        BlockVector min = new BlockVector(
                mapArguments.centerX.orElseThrow()
                        - mapArguments.width.orElseThrow() / 2 * mapArguments.scale.orElseThrow(),
                mapArguments.y.orElseThrow(),
                mapArguments.centerZ.orElseThrow()
                        - mapArguments.height.orElseThrow() / 2 * mapArguments.scale.orElseThrow());
        File outputFile = new File(world.getName() + "_biomes.png");

        BiomeMapper mapper = new BiomeMapper(world, biomeProvider, min, mapArguments.width.orElseThrow(),
                mapArguments.height.orElseThrow(), mapArguments.scale.orElseThrow(), outputFile);
        sender.sendMessage(BASE_COLOR + "Creating map with settings " + VALUE_COLOR + mapArguments.toString()
                + BASE_COLOR + ".");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                mapper.run();
                sender.sendMessage(BASE_COLOR + "Map written to " + VALUE_COLOR + outputFile.getAbsolutePath()
                        + BASE_COLOR + ".");
            } catch (IOException e) {
                sender.sendMessage(ERROR_COLOR + "Failed to write " + VALUE_COLOR + outputFile.getAbsolutePath()
                        + ERROR_COLOR + ": " + e.getMessage() + ".");
            }
        });

        // Show progress updates
        BukkitTask[] progressUpdater = new BukkitTask[1]; // Array hack so that we can access this from the // lambda
        progressUpdater[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            float progress = mapper.progress;
            if (progress >= 1) {
                progressUpdater[0].cancel();
                return;
            }

            float progressDisplay = Math.round(progress * 1000) / 10;
            sender.sendMessage(BASE_COLOR + "Map completion: " + progressDisplay + "%");
        }, 0, 40);
        return true;
    }

    @Override
    List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            // Suggest a first parameter
            return new MapArguments().getRemainingParameters();
        }

        // Try to complete the last paramter
        String[] beforeLast = Arrays.copyOf(args, args.length -1);
        if (beforeLast.length % 2 == 0) {
            MapArguments mapArguments = new MapArguments();
            try {
                mapArguments.parse(sender.getServer(), beforeLast);
            } catch (ParseException e) {
                return List.of(); // Parsing failed, no suggestions
            }
            List<String> completions = mapArguments.getRemainingParameters();
            return StringUtil.copyPartialMatches(args[args.length - 1], completions, new ArrayList<>());
        }

        // Nothing to complete
        return List.of();
    }

    private void registerBiomeColors() {
        // Colors from
        // https://twitter.com/henrikkniberg/status/1429700450807341057
        // https://github.com/toolbox4minecraft/amidst/blob/86ffd6004a3ab2a88b9325de420a596665f0df75/biome/test.json

        addBiome(Biome.OCEAN, rgb(0, 0, 112));
        addBiome(Biome.PLAINS, rgb(141, 179, 96));
        addBiome(Biome.DESERT, rgb(250, 148, 24));
        addBiome(Biome.WINDSWEPT_HILLS, rgb(96, 96, 96)); // Original Extreme Hills, later Mountains
        addBiome(Biome.FOREST, rgb(5, 102, 33));
        addBiome(Biome.TAIGA, rgb(11, 2, 89));
        addBiome(Biome.SWAMP, rgb(7, 249, 178));
        addBiome(Biome.RIVER, rgb(0, 0, 255));
        addBiome(Biome.NETHER_WASTES, rgb(255, 0, 0));
        addBiome(Biome.THE_END, rgb(128, 128, 255));
        addBiome(Biome.FROZEN_OCEAN, rgb(112, 112, 214));
        addBiome(Biome.FROZEN_RIVER, rgb(160, 160, 255));
        addBiome(Biome.SNOWY_PLAINS, rgb(255, 255, 255)); // SNOWY_TUNDRA
        // addBiome(Biome.SNOWY_MOUNTAINS, rgb(160, 160, 160));
        addBiome(Biome.MUSHROOM_FIELDS, rgb(255, 0, 255));
        // addBiome(Biome.MUSHROOM_FIELD_SHORE, rgb(160, 0, 255));
        addBiome(Biome.BEACH, rgb(250, 222, 85));
        // addBiome(Biome.DESERT_HILLS, rgb(210, 95, 18));
        // addBiome(Biome.WOODED_HILLS, rgb(34, 85, 28));
        // addBiome(Biome.TAIGA_HILLS, rgb(22, 57, 51));
        // addBiome(Biome.MOUNTAIN_EDGE, rgb(114, 120, 154));
        addBiome(Biome.JUNGLE, rgb(83, 123, 9));
        // addBiome(Biome.JUNGLE_HILLS, rgb(44, 66, 5));
        addBiome(Biome.SPARSE_JUNGLE, rgb(98, 139, 23));
        addBiome(Biome.DEEP_OCEAN, rgb(0, 0, 48));
        addBiome(Biome.STONY_SHORE, rgb(162, 162, 132));
        addBiome(Biome.SNOWY_BEACH, rgb(250, 240, 192));
        addBiome(Biome.BIRCH_FOREST, rgb(48, 116, 68));
        // addBiome(Biome.BIRCH_FOREST_HILLS, rgb(31, 5, 50));
        addBiome(Biome.DARK_FOREST, rgb(64, 81, 26));
        addBiome(Biome.SNOWY_TAIGA, rgb(49, 85, 74));
        // addBiome(Biome.SNOWY_TAIGA_HILLS, rgb(36, 63, 54));
        addBiome(Biome.OLD_GROWTH_PINE_TAIGA, rgb(89, 102, 81)); // Was GIANT_TREE_TAIGA
        // addBiome(Biome.GIANT_TREE_TAIGA_HILLS, rgb(69, 7, 62));
        addBiome(Biome.WINDSWEPT_FOREST, rgb(80, 112, 80));
        addBiome(Biome.SAVANNA, rgb(189, 18, 95));
        addBiome(Biome.SAVANNA_PLATEAU, rgb(167, 157, 100));
        addBiome(Biome.BADLANDS, rgb(217, 69, 21));
        addBiome(Biome.WOODED_BADLANDS, rgb(17, 151, 101)); // Was WOODED_BADLANDS_PLATEAU
        // addBiome(Biome.BADLANDS_PLATEAU, rgb(202, 140, 101));
        addBiome(Biome.SMALL_END_ISLANDS, rgb(128, 128, 255));
        addBiome(Biome.END_MIDLANDS, rgb(128, 128, 255));
        addBiome(Biome.END_HIGHLANDS, rgb(128, 128, 255));
        addBiome(Biome.END_BARRENS, rgb(128, 128, 255));
        addBiome(Biome.WARM_OCEAN, rgb(0, 0, 172));
        addBiome(Biome.LUKEWARM_OCEAN, rgb(0, 0, 144));
        addBiome(Biome.COLD_OCEAN, rgb(32, 32, 112));
        // addBiome(Biome.DEEP_WARM_OCEAN, rgb(0, 0, 80));
        addBiome(Biome.DEEP_LUKEWARM_OCEAN, rgb(0, 0, 64));
        addBiome(Biome.DEEP_COLD_OCEAN, rgb(32, 32, 56));
        addBiome(Biome.DEEP_FROZEN_OCEAN, rgb(64, 64, 144));
        addBiome(Biome.THE_VOID, rgb(0, 0, 0));
        addBiome(Biome.SUNFLOWER_PLAINS, rgb(181, 219, 136));
        // addBiome(Biome.DESERT_LAKES, rgb(255, 188, 64));
        addBiome(Biome.WINDSWEPT_GRAVELLY_HILLS, rgb(136, 136, 136)); // Was GRAVELLY_MOUNTAINS
        addBiome(Biome.FLOWER_FOREST, rgb(45, 142, 73));
        // addBiome(Biome.TAIGA_MOUNTAINS, rgb(51, 142, 19));
        // addBiome(Biome.SWAMP_HILLS, rgb(47, 255, 18));
        addBiome(Biome.ICE_SPIKES, rgb(180, 20, 220));
        // addBiome(Biome.MODIFIED_JUNGLE, rgb(123, 13, 49));
        // addBiome(Biome.MODIFIED_JUNGLE_EDGE, rgb(138, 179, 63));
        addBiome(Biome.OLD_GROWTH_BIRCH_FOREST, rgb(88, 156, 108)); // Was TALL_BIRCH_FOREST
        // addBiome(Biome.TALL_BIRCH_HILLS, rgb(71, 15, 90));
        // addBiome(Biome.DARK_FOREST_HILLS, rgb(104, 121, 66));
        // addBiome(Biome.SNOWY_TAIGA_MOUNTAINS, rgb(89, 125, 114));
        addBiome(Biome.OLD_GROWTH_SPRUCE_TAIGA, rgb(129, 142, 121)); // Was GIANT_SPRUCE_TAIGA
        // addBiome(Biome.GIANT_SPRUCE_TAIGA_HILLS, rgb(109, 119, 102));
        // addBiome(Biome.MODIFIED_GRAVELLY_MOUNTAINS, rgb(120, 52, 120));
        addBiome(Biome.WINDSWEPT_SAVANNA, rgb(229, 218, 135)); // Was SHATTERED_SAVANNA
        // addBiome(Biome.SHATTERED_SAVANNA_PLATEAU, rgb(207, 197, 140));
        addBiome(Biome.ERODED_BADLANDS, rgb(255, 109, 61));
        // addBiome(Biome.MODIFIED_WOODED_BADLANDS_PLATEAU, rgb(216, 191, 141));
        addBiome(Biome.BAMBOO_JUNGLE, rgb(118, 142, 20));
        // addBiome(Biome.BAMBOO_JUNGLE_HILLS, rgb(59, 71, 10));
        addBiome(Biome.SOUL_SAND_VALLEY, rgb(82, 41, 33));
        addBiome(Biome.CRIMSON_FOREST, rgb(221, 8, 8));
        addBiome(Biome.WARPED_FOREST, rgb(73, 144, 123));
        addBiome(Biome.BASALT_DELTAS, rgb(45, 52, 54));
        addBiome(Biome.DRIPSTONE_CAVES, rgb(89, 62, 42));
        addBiome(Biome.LUSH_CAVES, rgb(137, 232, 79));
        addBiome(Biome.MEADOW, rgb(71, 15, 90)); // Copied from TALL_BIRCH_HILLS
        addBiome(Biome.GROVE, rgb(89, 125, 114)); // Copied from SNOWY_TAIGA_MOUNTAINS
        addBiome(Biome.SNOWY_SLOPES, rgb(205, 205, 229));
        addBiome(Biome.FROZEN_PEAKS, rgb(160, 160, 160)); // Copied from SNOWY_MOUNTAINS
        addBiome(Biome.JAGGED_PEAKS, rgb(20, 94, 97));
        addBiome(Biome.STONY_PEAKS, rgb(120, 52, 120)); // Copied from MODIFIED_GRAVELLY_MOUNTAINS
        addBiome(Biome.CUSTOM, rgb(0, 0, 0));
    }

    private Color rgb(int r, int g, int b) {
        return Color.fromRGB(r, g, b);
    }

}
