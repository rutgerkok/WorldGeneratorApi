package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.decoration.BaseDecorationType;
import nl.rutgerkok.worldgeneratorapi.decoration.Decoration;
import nl.rutgerkok.worldgeneratorapi.decoration.DecorationType;
import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.InjectedChunkGenerator.GeneratingChunkImpl;

public final class WorldDecoratorImpl implements WorldDecorator {

    private static final GenerationStep.Decoration[] GENERATION_STEP_DECORATION = GenerationStep.Decoration.values();
    private final Map<DecorationType, List<Decoration>> customDecorations = new ConcurrentHashMap<>();
    private final Set<DecorationType> disabledDecorations = EnumSet.noneOf(DecorationType.class);

    private final Map<BaseDecorationType, List<BaseChunkGenerator>> customBaseDecorations = new ConcurrentHashMap<>();
    private final Set<BaseDecorationType> disabledBaseDecorations = EnumSet.noneOf(BaseDecorationType.class);

    /**
     * Villages, strongholds, etc. by generation step.
     */
    private final Map<Object, List<StructureFeature<?>>> structuresByStep;

    public WorldDecoratorImpl() {
        this.structuresByStep = Registry.STRUCTURE_FEATURE.stream()
                .collect(Collectors.groupingBy(feature -> feature.step().ordinal()));
    }

    public void generate(Biome biome,  StructureFeatureManager var0, final ChunkGenerator var1, final WorldGenRegion var2,
            final long var3, final WorldgenRandom var5, final BlockPos var6) {
        // Copied from Biome.generate(...)
        // Modifications marked with // WorldGeneratorApi

        final List<List<Supplier<ConfiguredFeature<?, ?>>>> var11 = biome.getGenerationSettings()
                .features();
        final Registry<ConfiguredFeature<?, ?>> var12 = var2.registryAccess()
                .registryOrThrow(Registry.CONFIGURED_FEATURE_REGISTRY);
        final Registry<StructureFeature<?>> var13 = var2.registryAccess()
                .registryOrThrow(Registry.STRUCTURE_FEATURE_REGISTRY);
        for (int var14 = GENERATION_STEP_DECORATION.length, var15 = 0; var15 < var14; ++var15) {
            // WorldGeneratorApi start
            DecorationType decorationType = this.toDecorationType(GENERATION_STEP_DECORATION[var15]);
            if (this.disabledDecorations.contains(decorationType)) {
                // This type is disabled, don't do anything except spawning the custom
                // decorations
                this.spawnCustomDecorations(decorationType, var2, var5);
                continue;
            }
            // WorldGeneratorApi end

            int var16 = 0;
            if (var0.shouldGenerateFeatures()) {
                final List<StructureFeature<?>> var17 = this.structuresByStep.getOrDefault(var15,
                        Collections.emptyList());
                for (final StructureFeature<?> var18 : var17) {
                    var5.setFeatureSeed(var3, var16, var15);
                    final int var19 = SectionPos.blockToSectionCoord(var6.getX());
                    final int var20 = SectionPos.blockToSectionCoord(var6.getZ());
                    final int var21 = SectionPos.sectionToBlockCoord(var19);
                    final int var22 = SectionPos.sectionToBlockCoord(var20);

                    final Supplier<String> var23 = () -> {
                        return var13.getResourceKey(var18).map(Object::toString).orElseGet(var13::toString);
                    };
                    try {
                        final int var24 = var2.getMinBuildHeight() + 1;
                        final int var25 = var2.getMaxBuildHeight() - 1;
                        var2.setCurrentlyGenerating(var23);
                        final int n = var21;
                        final int n2 = var24;
                        final int n3 = var22;
                        var0.startsForFeature(SectionPos.of(var6), var18)
                                .forEach(var10 -> var10.placeInChunk((WorldGenLevel) var2, var0, var1, (Random) var5,
                                        new BoundingBox(n, n2, n3, n + 15, var25, n3 + 15),
                                        new ChunkPos(var19, var20)));
                    } catch (Exception var27) {
                        final CrashReport var26 = CrashReport.forThrowable(var27, "Feature placement");
                        final CrashReportCategory addCategory = var26.addCategory("Feature");
                        final String s = "Description";
                        final Supplier<String> obj2 = var23;
                        Objects.requireNonNull(obj2);
                        addCategory.setDetail(s, obj2::get);
                        throw new ReportedException(var26);
                    }
                    ++var16;
                }
            }
            if (var11.size() > var15) {
                for (final Supplier<ConfiguredFeature<?, ?>> var28 : var11.get(var15)) {
                    final ConfiguredFeature<?, ?> var29 = var28.get();

                    final Supplier<String> var30 = () -> {
                        return var12.getResourceKey(var29).map(Object::toString).orElseGet(var29::toString);
                    };
                    var5.setFeatureSeed(var3, var16, var15);
                    try {
                        var2.setCurrentlyGenerating(var30);
                        var29.place(var2, var1, var5, var6);
                    } catch (Exception var32) {
                        final CrashReport var31 = CrashReport.forThrowable(var32, "Feature placement");
                        final CrashReportCategory addCategory2 = var31.addCategory("Feature");
                        final String s2 = "Description";
                        final Supplier<String> obj4 = var30;
                        Objects.requireNonNull(obj4);
                        addCategory2.setDetail(s2, obj4::get);
                        throw new ReportedException(var31);
                    }
                    ++var16;
                }
            }

            // WorldGeneratorApi
            this.spawnCustomDecorations(decorationType, var2, var5);
        }
        var2.setCurrentlyGenerating(null);
    }

    @Override
    public List<BaseChunkGenerator> getCustomBaseDecorations(BaseDecorationType type) {
        Objects.requireNonNull(type, "type");
        return customBaseDecorations.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>());
    }

    @Override
    public List<Decoration> getCustomDecorations(DecorationType type) {
        Objects.requireNonNull(type, "type");
        return customDecorations.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>());
    }

    public boolean isDefaultEnabled(BaseDecorationType type) {
        return !this.disabledBaseDecorations.contains(type);
    }

    @Override
    public void setDefaultBaseDecoratorsEnabled(BaseDecorationType type, boolean enabled) {
        Objects.requireNonNull(type, "type");
        if (enabled) {
            this.disabledBaseDecorations.remove(type);
        } else {
            this.disabledBaseDecorations.add(type);
        }
    }

    @Override
    public void setDefaultDecoratorsEnabled(DecorationType type, boolean enabled) {
        Objects.requireNonNull(type, "type");
        if (enabled) {
            this.disabledDecorations.remove(type);
        } else {
            this.disabledDecorations.add(type);
        }
    }

    public void spawnCustomBaseDecorations(BaseDecorationType type, GeneratingChunkImpl chunk) {
        // Generates custom decorations. Should be called after the default decorations
        // have been spawned for that type

        List<BaseChunkGenerator> decorations = this.customBaseDecorations.get(type);
        if (decorations != null) {
            for (BaseChunkGenerator decoration : decorations) {
                decoration.setBlocksInChunk(chunk);
            }
        }
    }

    private void spawnCustomDecorations(DecorationType decorationType, WorldGenRegion var2, Random random) {
        DecorationAreaImpl area = new DecorationAreaImpl(var2, var2.getCenter());
        for (Decoration decoration : this.customDecorations.getOrDefault(decorationType, Collections.emptyList())) {
            decoration.decorate(area, random);
        }
    }

    /**
     * Simple translation from Minecraft Carving GenerationStep to our
     * BaseDecorationType.
     *
     * @param carvingStep
     *            The carving step.
     * @return The BaseDecorationType.
     */
    public BaseDecorationType toBaseDecorationType(GenerationStep.Carving carvingStep) {
        return switch (carvingStep) {
            case AIR -> BaseDecorationType.CARVING_AIR;
            case LIQUID -> BaseDecorationType.CARVING_LIQUID;
        };
    }

    private DecorationType toDecorationType(GenerationStep.Decoration decorationStep) {
        return switch (decorationStep) {
            case LAKES -> DecorationType.LAKES;
            case LOCAL_MODIFICATIONS -> DecorationType.LOCAL_MODIFICATIONS;
            case RAW_GENERATION -> DecorationType.RAW_GENERATION;
            case STRONGHOLDS -> DecorationType.STRONGHOLDS;
            case SURFACE_STRUCTURES -> DecorationType.SURFACE_STRUCTURES;
            case TOP_LAYER_MODIFICATION -> DecorationType.TOP_LAYER_MODIFICATION;
            case UNDERGROUND_DECORATION -> DecorationType.UNDERGROUND_DECORATION;
            case UNDERGROUND_ORES -> DecorationType.UNDERGROUND_ORES;
            case UNDERGROUND_STRUCTURES -> DecorationType.UNDERGROUND_STRUCTURES;
            case VEGETAL_DECORATION -> DecorationType.VEGETAL_DECORATION;
        };
    }

}
