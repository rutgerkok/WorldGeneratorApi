package nl.rutgerkok.worldgeneratorapi.internal.recording;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.decoration.BaseDecorationType;
import nl.rutgerkok.worldgeneratorapi.decoration.Decoration;
import nl.rutgerkok.worldgeneratorapi.decoration.DecorationType;
import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;

@Deprecated(forRemoval = true)
final class RecordingWorldDecoratorImpl implements WorldDecorator {

    private final WorldDecorator internal;

    private final Map<BaseDecorationType, List<BaseChunkGenerator>> customBaseDecorations = new EnumMap<>(
            BaseDecorationType.class);
    private final Map<DecorationType, List<Decoration>> customDecorations = new EnumMap<>(
            DecorationType.class);
    private final Map<DecorationType, Boolean> minecraftDecorations = new EnumMap<>(DecorationType.class);
    private final Map<BaseDecorationType, Boolean> minecraftBaseDecorations = new EnumMap<>(BaseDecorationType.class);


    public RecordingWorldDecoratorImpl(WorldDecorator internal) {
        this.internal = Objects.requireNonNull(internal, "internal");
    }

    void applyTo(WorldDecorator decorator) {
        // Sync up all lists
        this.customBaseDecorations.forEach((type, decorations) -> {
            List<BaseChunkGenerator> list = decorator.getCustomBaseDecorations(type);
            list.clear();
            list.addAll(decorations);
        });
        this.customDecorations.forEach((type, decorations) -> {
            List<Decoration> list = decorator.getCustomDecorations(type);
            list.clear();
            list.addAll(decorations);
        });

        // Reapply vanilla modifications
        this.minecraftBaseDecorations.forEach((type, isEnabled) -> {
            decorator.setDefaultBaseDecoratorsEnabled(type, isEnabled);
        });
        this.minecraftDecorations.forEach((type, isEnabled) -> {
            decorator.setDefaultDecoratorsEnabled(type, isEnabled);
        });
    }

    @Override
    public List<BaseChunkGenerator> getCustomBaseDecorations(BaseDecorationType decorationType) {
        return this.customBaseDecorations.computeIfAbsent(decorationType, d -> {
            return new ArrayList<>(this.internal.getCustomBaseDecorations(decorationType));
        });
    }

    @Override
    public List<Decoration> getCustomDecorations(DecorationType decorationType) {
        return this.customDecorations.computeIfAbsent(decorationType, d -> {
            return new ArrayList<>(this.internal.getCustomDecorations(decorationType));
        });
    }

    @Override
    public void setDefaultBaseDecoratorsEnabled(BaseDecorationType type, boolean enabled) {
        this.minecraftBaseDecorations.put(type, enabled);
    }

    @Override
    public void setDefaultDecoratorsEnabled(DecorationType type, boolean enabled) {
        this.minecraftDecorations.put(type, enabled);
    }

}
