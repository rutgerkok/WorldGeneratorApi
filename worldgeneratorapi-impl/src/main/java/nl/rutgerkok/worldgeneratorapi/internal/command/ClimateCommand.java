package nl.rutgerkok.worldgeneratorapi.internal.command;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;

import nl.rutgerkok.worldgeneratorapi.ClimateSampler;
import nl.rutgerkok.worldgeneratorapi.ClimateSampler.ClimatePoint;

/**
 * Shows the climate (temperature, continentalness, etc.) of the location of the
 * player.
 *
 */
public final class ClimateCommand extends WorldGeneratorApiCommand {

    private final Function<WorldInfo, ClimateSampler> getSampler;

    public ClimateCommand(Function<WorldInfo, ClimateSampler> getSampler) {
        this.getSampler = Objects.requireNonNull(getSampler, "getSampler");
    }

    @Override
    String getSyntax(String label) {
        return "";
    }

    @Override
    boolean onCommand(CommandSender sender, String label, String[] args) {
        if (args.length != 0) {
            return false;
        }

        if (sender instanceof Player player) {
            Location playerLocation = player.getLocation();

            ClimateSampler sampler = getSampler.apply(playerLocation.getWorld());
            ClimatePoint point = sampler.getClimatePoint(playerLocation.getBlockX(), playerLocation
                    .getBlockY(), playerLocation.getBlockZ());

            DecimalFormat f = new DecimalFormat("0.000");
            sender.sendMessage(BASE_COLOR + "Erosion: " + VALUE_COLOR + f.format(point.getErosion())
                    + BASE_COLOR + "  Continentalness: " + VALUE_COLOR + f.format(point.getContinentalness())
                    + BASE_COLOR + "  Weirdness: " + VALUE_COLOR + f.format(point.getWeirdness())
                    + BASE_COLOR + "  Temperature: " + VALUE_COLOR + f.format(point.getTemperature())
                    + BASE_COLOR + "  Humidity: " + VALUE_COLOR + f.format(point.getHumidity())
                    + BASE_COLOR + "  Factor: " + VALUE_COLOR + f.format(point.getInitialDensity())
                    + BASE_COLOR + "  Offset: " + VALUE_COLOR + f.format(point.getFinalDensity())
                    + BASE_COLOR + "  Jaggedness: " + VALUE_COLOR + f.format(point.getRidges()));
            return true;
        }

        sender.sendMessage(ERROR_COLOR + "Only players can use this command.");
        return true;
    }

    @Override
    List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        return List.of();
    }

}
