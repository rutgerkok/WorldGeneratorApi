package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.decoration.BaseDecorationType;
import nl.rutgerkok.worldgeneratorapi.decoration.Decoration;
import nl.rutgerkok.worldgeneratorapi.decoration.DecorationType;
import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.InjectedChunkGenerator.GeneratingChunkImpl;

public final class WorldDecoratorImpl implements WorldDecorator {

    private static final DecorationType[] DECORATION_TRANSLATION;
    private static final Map<GenerationStep.Carving, BaseDecorationType> CARVER_TRANSLATION;
    private static final Field BIOME_DECORATIONS_FIELD;
    private static final Field BIOME_SETTINGS_FIELD;

    static {
        GenerationStep.Decoration[] vanillaArray = GenerationStep.Decoration.values();
        DECORATION_TRANSLATION = new DecorationType[vanillaArray.length];
        for (int i = 0; i < vanillaArray.length; i++) {
            DECORATION_TRANSLATION[i] = DecorationType.valueOf(vanillaArray[i].name());
        }

        CARVER_TRANSLATION = new EnumMap<>(GenerationStep.Carving.class);
        for (GenerationStep.Carving type : GenerationStep.Carving.values()) {
            CARVER_TRANSLATION.put(type, BaseDecorationType.valueOf("CARVING_" + type.name()));
        }

        BIOME_DECORATIONS_FIELD = ReflectionUtil.getFieldOfType(Biome.class, Map.class);
        BIOME_DECORATIONS_FIELD.setAccessible(true);
        BIOME_SETTINGS_FIELD = ReflectionUtil.getFieldOfType(Biome.class, BiomeGenerationSettings.class);
        BIOME_SETTINGS_FIELD.setAccessible(true);
    }

    private final Map<DecorationType, List<Decoration>> customDecorations = new ConcurrentHashMap<>();
    private final Set<DecorationType> disabledDecorations = EnumSet.noneOf(DecorationType.class);

    private final Map<BaseDecorationType, List<BaseChunkGenerator>> customBaseDecorations = new ConcurrentHashMap<>();
    private final Set<BaseDecorationType> disabledBaseDecorations = EnumSet.noneOf(BaseDecorationType.class);


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

}
