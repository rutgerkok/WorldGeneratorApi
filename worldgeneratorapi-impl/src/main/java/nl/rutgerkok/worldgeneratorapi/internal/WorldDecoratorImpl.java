package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import net.minecraft.server.v1_16_R3.BiomeBase;
import net.minecraft.server.v1_16_R3.BiomeManager;
import net.minecraft.server.v1_16_R3.BiomeSettingsGeneration;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R3.ChunkGenerator;
import net.minecraft.server.v1_16_R3.CrashReport;
import net.minecraft.server.v1_16_R3.IChunkAccess;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.ProtoChunk;
import net.minecraft.server.v1_16_R3.RegionLimitedWorldAccess;
import net.minecraft.server.v1_16_R3.ReportedException;
import net.minecraft.server.v1_16_R3.SectionPosition;
import net.minecraft.server.v1_16_R3.SeededRandom;
import net.minecraft.server.v1_16_R3.StructureBoundingBox;
import net.minecraft.server.v1_16_R3.StructureGenerator;
import net.minecraft.server.v1_16_R3.StructureManager;
import net.minecraft.server.v1_16_R3.WorldChunkManager;
import net.minecraft.server.v1_16_R3.WorldGenCarverWrapper;
import net.minecraft.server.v1_16_R3.WorldGenFeatureConfigured;
import net.minecraft.server.v1_16_R3.WorldGenStage;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator.GeneratingChunk;
import nl.rutgerkok.worldgeneratorapi.decoration.BaseDecorationType;
import nl.rutgerkok.worldgeneratorapi.decoration.Decoration;
import nl.rutgerkok.worldgeneratorapi.decoration.DecorationArea;
import nl.rutgerkok.worldgeneratorapi.decoration.DecorationType;
import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.InjectedChunkGenerator.GeneratingChunkImpl;

public final class WorldDecoratorImpl implements WorldDecorator {

    private static final DecorationType[] DECORATION_TRANSLATION;
    private static final Map<WorldGenStage.Features, BaseDecorationType> CARVER_TRANSLATION;
    private static final Field BIOME_DECORATIONS_FIELD;
    private static final Field BIOME_SETTINGS_FIELD;

    static {
        WorldGenStage.Decoration[] vanillaArray = WorldGenStage.Decoration.values();
        DECORATION_TRANSLATION = new DecorationType[vanillaArray.length];
        for (int i = 0; i < vanillaArray.length; i++) {
            DECORATION_TRANSLATION[i] = DecorationType.valueOf(vanillaArray[i].name());
        }

        CARVER_TRANSLATION = new EnumMap<>(WorldGenStage.Features.class);
        for (WorldGenStage.Features type : WorldGenStage.Features.values()) {
            CARVER_TRANSLATION.put(type, BaseDecorationType.valueOf("CARVING_" + type.name()));
        }

        BIOME_DECORATIONS_FIELD = ReflectionUtil.getFieldOfType(BiomeBase.class, Map.class);
        BIOME_DECORATIONS_FIELD.setAccessible(true);
        BIOME_SETTINGS_FIELD = ReflectionUtil.getFieldOfType(BiomeBase.class, BiomeSettingsGeneration.class);
        BIOME_SETTINGS_FIELD.setAccessible(true);
    }

    private final Map<DecorationType, List<Decoration>> customDecorations = new ConcurrentHashMap<>();
    private final Set<DecorationType> disabledDecorations = EnumSet.noneOf(DecorationType.class);

    private final Map<BaseDecorationType, List<BaseChunkGenerator>> customBaseDecorations = new ConcurrentHashMap<>();
    private final Set<BaseDecorationType> disabledBaseDecorations = EnumSet.noneOf(BaseDecorationType.class);

    @SuppressWarnings({ "unchecked", "rawtypes" }) // Decompiled code
    public void a(BiomeBase biomeBase, StructureManager var0, ChunkGenerator var1, RegionLimitedWorldAccess var2,
            long var3, SeededRandom var5, BlockPosition var6) throws IllegalAccessException {
        // Adapted from the same method in BiomeBase
        BiomeSettingsGeneration k = (BiomeSettingsGeneration) BIOME_SETTINGS_FIELD.get(biomeBase);
        Map g = (Map) BIOME_DECORATIONS_FIELD.get(biomeBase);
        DecorationArea decorationArea = new DecorationAreaImpl(var2);

        // Start of original method
        List<List<Supplier<WorldGenFeatureConfigured<?, ?>>>> var7 = k.c();
        int var8 = WorldGenStage.Decoration.values().length;

        for (int var9 = 0; var9 < var8; ++var9) {
            // Start of modifications
            DecorationType apiType = DECORATION_TRANSLATION[var9];
            // Spawn custom decorations
            List<Decoration> decorations = this.customDecorations.get(apiType);
            if (decorations != null) {
                int decorationIndex = 0;
                for (Decoration decoration : decorations) {
                    if (decoration == null) {
                        continue;
                    }
                    var5.b(var3, decorationIndex, apiType.ordinal());
                    decoration.decorate(decorationArea, var5);
                    decorationIndex++;
                }
            }
            if (this.disabledDecorations.contains(apiType)) {
                continue; // Skip vanilla decorations when requested
            }
            // End of modifications

            int var10 = 0;
            if (var0.a()) {
                List<StructureGenerator<?>> var11 = (List) g.getOrDefault(var9, Collections.emptyList());

                for (StructureGenerator<?> var13 : var11) {
                    var10++;
                    var5.b(var3, var10, var9);
                    int var14 = var6.getX() >> 4;
                    int var15 = var6.getZ() >> 4;
                    int var16 = var14 << 4;
                    int var17 = var15 << 4;

                    try {
                        var0.a(SectionPosition.a(var6), var13).forEach((var8x) -> {
                            var8x.a(var2, var0, var1, var5,
                                    new StructureBoundingBox(var16, var17, var16 + 15, var17 + 15),
                                    new ChunkCoordIntPair(var14, var15));
                        });
                    } catch (Exception var21) {
                        CrashReport var19 = CrashReport.a(var21, "Feature placement");
                        var19.a("Feature").a("Id", IRegistry.STRUCTURE_FEATURE.getKey(var13)).a("Description", () -> {
                            return var13.toString();
                        });
                        throw new ReportedException(var19);
                    }
                }
            }

            if (var7.size() > var9) {
                for (Iterator<Supplier<WorldGenFeatureConfigured<?, ?>>> var23 = var7.get(var9).iterator(); var23.hasNext(); ++var10) {
                    Supplier<WorldGenFeatureConfigured<?, ?>> var12 = var23.next();
                    WorldGenFeatureConfigured<?, ?> var13 = var12.get();
                    var5.b(var3, var10, var9);

                    try {
                        var13.a(var2, var1, var5, var6);
                    } catch (Exception var22) {
                        CrashReport var15 = CrashReport.a(var22, "Feature placement");
                        var15.a("Feature").a("Id", IRegistry.FEATURE.getKey(var13.e)).a("Config", var13.f)
                                .a("Description", () -> {
                                    return var13.e.toString();
                                });
                        throw new ReportedException(var15);
                    }
                }
            }
        }

    }

    private BiomeBase getBiome(BiomeManager biomeManager, BlockPosition blockPosition) {
        return biomeManager.a(blockPosition);
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

    public void spawnCarvers(BiomeManager biomeManager, GeneratingChunkImpl chunk, WorldGenStage.Features stage,
            int seaLevel, long seed) {
        IChunkAccess ichunkaccess = chunk.internal;
        SeededRandom seededrandom = new SeededRandom(seed);
        BaseDecorationType decorationType = CARVER_TRANSLATION.get(stage);
        if (!this.disabledBaseDecorations.contains(decorationType)) {
            // Spawn default carvers (code based on ChunkGenerator.doCarving)
            ChunkCoordIntPair chunkcoordintpair = ichunkaccess.getPos();
            int i = chunkcoordintpair.x;
            int j = chunkcoordintpair.z;
            BiomeSettingsGeneration biomeConfig = this.getBiome(biomeManager, chunkcoordintpair.l()).e();
            BitSet bitset = ((ProtoChunk) ichunkaccess).b(stage);

            for (int k = i - 8; k <= i + 8; ++k) {
                for (int l = j - 8; l <= j + 8; ++l) {
                    List<Supplier<WorldGenCarverWrapper<?>>> list = biomeConfig.a(stage);
                    ListIterator<Supplier<WorldGenCarverWrapper<?>>> listiterator = list.listIterator();

                    while (listiterator.hasNext()) {
                        int i1 = listiterator.nextIndex();
                        WorldGenCarverWrapper<?> worldgencarverwrapper = listiterator.next().get();

                        seededrandom.c(seed + i1, k, l);
                        if (worldgencarverwrapper.a(seededrandom, k, l)) {
                            worldgencarverwrapper.a(ichunkaccess, (blockposition) -> {
                                return this.getBiome(biomeManager, blockposition);
                            }, seededrandom, seaLevel, k, l, i, j, bitset);
                        }
                    }
                }
            }
        }

        // Spawn custom carvers
        List<BaseChunkGenerator> carvers = this.customBaseDecorations.get(decorationType);
        if (carvers == null) {
            return;
        }
        for (BaseChunkGenerator carver : carvers) {
            carver.setBlocksInChunk(chunk);
        }
    }

    public void spawnCustomBaseDecorations(BaseDecorationType type, GeneratingChunk chunk) {
        // Generates custom decorations. Should be called after the default decorations
        // have been spawned for that type

        List<BaseChunkGenerator> decorations = this.customBaseDecorations.get(type);
        if (decorations != null) {
            for (BaseChunkGenerator decoration : decorations) {
                decoration.setBlocksInChunk(chunk);
            }
        }
    }

    public void spawnDecorations(ChunkGenerator chunkGenerator, WorldChunkManager worldChunkManager,
            StructureManager structureManager, RegionLimitedWorldAccess populationArea) {
        // Copied from ChunkGeneratorAbstract - modified to call own biome decorator
        int i = populationArea.a();
        int j = populationArea.b();
        int k = i * 16;
        int l = j * 16;
        BlockPosition blockposition = new BlockPosition(k, 0, l);
        BiomeBase biomebase = worldChunkManager.getBiome((i << 2) + 2, 2, (j << 2) + 2);

        SeededRandom seededrandom = new SeededRandom();
        long i1 = seededrandom.a(populationArea.getSeed(), k, l);

        try {
            a(biomebase, structureManager, chunkGenerator, populationArea, i1, seededrandom, blockposition);
        } catch (Exception var14) {
            CrashReport crashreport = CrashReport.a(var14, "Biome decoration");
            crashreport.a("Generation").a("CenterX", i).a("CenterZ", j).a("Seed", i1).a("Biome", biomebase);
            throw new ReportedException(crashreport);
        }
    }

}
