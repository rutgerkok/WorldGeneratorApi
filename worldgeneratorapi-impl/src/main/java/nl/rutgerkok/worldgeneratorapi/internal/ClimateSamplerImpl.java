package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.TargetPoint;
import net.minecraft.world.level.levelgen.NoiseSampler;
import net.minecraft.world.level.levelgen.TerrainInfo;
import net.minecraft.world.level.levelgen.blending.Blender;
import nl.rutgerkok.worldgeneratorapi.ClimateSampler;

public class ClimateSamplerImpl implements ClimateSampler {

    private class ClimatePointImpl implements ClimatePoint {

        private final TargetPoint targetPoint;
        private final @Nullable TerrainInfo terrainInfoOrNull;

        public ClimatePointImpl(TargetPoint targetPoint, @Nullable TerrainInfo terrainInfoOrNull) {
            this.targetPoint = Objects.requireNonNull(targetPoint, "targetPoint");
            this.terrainInfoOrNull = terrainInfoOrNull;
        }

        @Override
        public float getContinentalness() {
            return Climate.unquantizeCoord(targetPoint.continentalness());
        }

        @Override
        public float getErosion() {
            return Climate.unquantizeCoord(targetPoint.erosion());
        }

        @Override
        public float getFactor() {
            if (this.terrainInfoOrNull == null) {
                return 0;
            }
            return (float) this.terrainInfoOrNull.factor();
        }

        @Override
        public float getHumidity() {
            return Climate.unquantizeCoord(targetPoint.humidity());
        }

        @Override
        public float getJaggedness() {
            if (this.terrainInfoOrNull == null) {
                return 0;
            }
            return (float) this.terrainInfoOrNull.jaggedness();
        }

        @Override
        public float getOffset() {
            if (this.terrainInfoOrNull == null) {
                return 0;
            }
            return (float) this.terrainInfoOrNull.offset();
        }

        @Override
        public float getTemperature() {
            return Climate.unquantizeCoord(targetPoint.temperature());
        }

        @Override
        public float getWeirdness() {
            return Climate.unquantizeCoord(targetPoint.weirdness());
        }

    }

    private final Climate.Sampler internal;

    public ClimateSamplerImpl(Climate.Sampler climateSampler) {
        this.internal = Objects.requireNonNull(climateSampler, "climateSampler");
    }

    @Override
    public ClimatePoint getClimatePoint(int x, int y, int z) {
        TargetPoint targetPoint = internal.sample(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
        if (internal instanceof NoiseSampler noiseSampler) {
            TerrainInfo terrainInfo = noiseSampler
                    .terrainInfo(x, z, targetPoint.continentalness(), targetPoint.weirdness(), targetPoint
                            .erosion(), Blender.empty());
            return new ClimatePointImpl(targetPoint, terrainInfo);
        }
        return new ClimatePointImpl(targetPoint, null);
    }

}
