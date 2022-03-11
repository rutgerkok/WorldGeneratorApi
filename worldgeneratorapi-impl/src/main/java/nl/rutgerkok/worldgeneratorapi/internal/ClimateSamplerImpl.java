package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import nl.rutgerkok.worldgeneratorapi.ClimateSampler;

public class ClimateSamplerImpl implements ClimateSampler {

    private static class ClimateSamplerClimatePoint implements ClimatePoint {

        protected final Climate.Sampler climateSampler;
        protected final DensityFunction.SinglePointContext position;

        public ClimateSamplerClimatePoint(Climate.Sampler climateSampler, int blockX, int blockY, int blockZ) {
            this.position = new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
            this.climateSampler = Objects.requireNonNull(climateSampler, "targetPoint");
        }

        @Override
        public float getContinentalness() {
            return (float) climateSampler.continentalness().compute(position);
        }

        @Override
        public float getDepth() {
            return (float) climateSampler.depth().compute(position);
        }

        @Override
        public float getErosion() {
            return (float) climateSampler.erosion().compute(position);
        }

        @Override
        public float getFinalDensity() {
            return 0;
        }

        @Override
        public float getHumidity() {
            return (float) climateSampler.humidity().compute(position);
        }

        @Override
        public float getInitialDensity() {
            return 0;
        }

        @Override
        public float getRidges() {
            return 0;
        }

        @Override
        public float getTemperature() {
            return (float) climateSampler.temperature().compute(position);
        }

        @Override
        public float getWeirdness() {
            return (float) climateSampler.weirdness().compute(position);
        }

    }

    private static class NoiseRouterClimatePoint extends ClimateSamplerClimatePoint {
        private final NoiseRouter noiseRouter;

        public NoiseRouterClimatePoint(Climate.Sampler climateSampler, NoiseRouter noiseRouter, int blockX, int blockY,
                int blockZ) {
            super(climateSampler, blockX, blockY, blockZ);
            this.noiseRouter = Objects.requireNonNull(noiseRouter, "noiseRouter");
        }

        @Override
        public float getFinalDensity() {
            return (float) noiseRouter.finalDensity().compute(position);
        }

        @Override
        public float getInitialDensity() {
            return (float) noiseRouter.initialDensityWithoutJaggedness().compute(position);
        }

        @Override
        public float getRidges() {
            return (float) noiseRouter.ridges().compute(position);
        }
    }

    private final Climate.Sampler internal;
    private final NoiseRouter noiseRouterOrNull;

    public ClimateSamplerImpl(Climate.Sampler climateSampler, @Nullable NoiseRouter noiseRouterOrNull) {
        this.internal = Objects.requireNonNull(climateSampler, "climateSampler");
        this.noiseRouterOrNull = noiseRouterOrNull;
    }

    @Override
    public ClimatePoint getClimatePoint(int x, int y, int z) {
        if (this.noiseRouterOrNull == null) {
            return new ClimateSamplerClimatePoint(internal, x, y, z);
        }
        return new NoiseRouterClimatePoint(internal, noiseRouterOrNull, x, y, z);
    }

}
