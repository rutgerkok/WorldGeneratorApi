package nl.rutgerkok.worldgeneratorapi.internal;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.bukkit.Color;

import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseMap {

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 1024;
    private static final int SCALE = 8;

    private static Color getColor(double noiseValue) {
        if (noiseValue < -1.0445810959740656) {
            return Color.FUCHSIA;
        }
        noiseValue -= 0.10983337962105744;
        int intensity = (int) Math.abs(noiseValue * 200);
        if (intensity > 255) {
            intensity = 255;
        }
        if (noiseValue < 0) {
            return Color.fromRGB(0, 0, intensity);
        }
        return Color.fromRGB(0, intensity, 0);
    }

    public static void main(String... args) throws IOException {
        RandomSource random = new XoroshiroRandomSource(1);
        NormalNoise noiseFunction = NormalNoise
                .create(random, -11, new double[] { 0.5, 1.8, 2.0, 4.0, 4.0, 4.0, 2.0, 2.0, 2.0, 2.0 });
        BufferedImage biomeImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                double noiseValue = noiseFunction.getValue(i * SCALE, 20, j * SCALE);
                Color color = getColor(noiseValue);
                biomeImage.setRGB(i, j, color.asRGB());
            }
        }

        File file = new File("C:\\Users\\Rutger\\Desktop\\map.png");
        ImageIO.write(biomeImage, "png", file);
        Desktop.getDesktop().open(file);
    }
}
