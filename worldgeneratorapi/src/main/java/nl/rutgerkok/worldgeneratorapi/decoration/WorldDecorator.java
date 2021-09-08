package nl.rutgerkok.worldgeneratorapi.decoration;

import java.util.List;

import org.bukkit.generator.ChunkGenerator;

import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;

/**
 * Without any decorations, a Minecraft world would just contain stone, air and
 * water. Everything else is a decoration. This class can be used to add or
 * remove decorations from the world.
 *
 * @since 0.2
 * @deprecated Bukkit added various methods to {@link ChunkGenerator} to control
 *             vanilla decorations.
 */
@Deprecated(forRemoval = true)
public interface WorldDecorator {

    /**
     * Gets a list of all custom base decorators. This list can be modified to add
     * your own custom decorators.
     * <p>
     * "Base" decorations run earlier in terrain generation and operate on only a
     * single chunk. (Normal decorations operate on a 2x2 square of chunks, so that
     * they can place objects that cross chunk boundaries.)
     *
     * @param decorationType
     *            The type of base decoration.
     *
     * @return All custom base decorations of the given type.
     * @since 0.3
     */
    List<BaseChunkGenerator> getCustomBaseDecorations(BaseDecorationType decorationType);

    /**
     * Gets a list of all custom decorators of a world. This list can be modified to
     * add your own custom decorators.
     *
     * @param type
     *            The decoration type to get the decorations for.
     *
     * @return A mutable list.
     * @see #withCustomDecoration(DecorationType, Decoration)
     * @since 0.2
     */
    List<Decoration> getCustomDecorations(DecorationType type);

    /**
     * Sets whether the default Minecraft decorators of the given type are enabled.
     *
     * @param type
     *            The type.
     * @param enabled
     *            True if enabled, false otherwise.
     * @since 0.3
     */
    void setDefaultBaseDecoratorsEnabled(BaseDecorationType type, boolean enabled);

    /**
     * Sets whether the default Minecraft decorators of the given type are enabled.
     *
     * @param type
     *            The type.
     * @param enabled
     *            True if enabled, false otherwise.
     * @since 0.2
     */
    void setDefaultDecoratorsEnabled(DecorationType type, boolean enabled);

    /**
     * Adds a custom base decorator of the given type.
     * <p>
     * "Base" decorations run earlier in terrain generation and operate on only a
     * single chunk. (Normal decorations operate on a 2x2 square of chunks, so that
     * they can place objects that cross chunk boundaries.)
     *
     * @param type
     *            The type of decoration.
     * @param decorator
     *            The decorator.
     * @return This, for chaining.
     * @since 0.3
     */
    default WorldDecorator withCustomBaseDecoration(BaseDecorationType type, BaseChunkGenerator decorator) {
        getCustomBaseDecorations(type).add(decorator);
        return this;
    }

    /**
     * Adds a custom decorator of the given type.
     *
     * @param type
     *            The type of decoration.
     * @param decorator
     *            The decorator.
     * @return This, for chaining.
     * @since 0.2
     */
    default WorldDecorator withCustomDecoration(DecorationType type, Decoration decorator) {
        this.getCustomDecorations(type).add(decorator);
        return this;
    }

    /**
     * Disables <strong>all</strong> of Minecraft's decorations, so that you are
     * left with a world consisting of only stone, water and air. Does not affect
     * any custom decorations.
     *
     * @return This, for chaining.
     * @since 0.3
     */
    default WorldDecorator withoutAllDefaultDecorations() {
        for (DecorationType type : DecorationType.values()) {
            setDefaultDecoratorsEnabled(type, false);
        }
        for (BaseDecorationType type : BaseDecorationType.values()) {
            setDefaultBaseDecoratorsEnabled(type, false);
        }
        return this;
    }

    /**
     * Disables all default Minecraft base decorations of the given type. Equivalent
     * to calling
     * {@link #setDefaultBaseDecoratorsEnabled(BaseDecorationType, boolean)} with
     * {@code enabled} set to {@code false}.
     *
     * @param type
     *            The type of decoration.
     * @return This, for chaining.
     * @since 0.3
     */
    default WorldDecorator withoutDefaultBaseDecorations(BaseDecorationType type) {
        setDefaultBaseDecoratorsEnabled(type, false);
        return this;
    }

    /**
     * Disables all default Minecraft decorations of the given type. Equivalent to
     * calling {@link #setDefaultDecoratorsEnabled(DecorationType, boolean)} with
     * {@code enabled} set to {@code false}.
     *
     * @param type
     *            The type of decoration.
     * @return This, for chaining.
     * @since 0.2
     */
    default WorldDecorator withoutDefaultDecorations(DecorationType type) {
        setDefaultDecoratorsEnabled(type, false);
        return this;
    }
}
