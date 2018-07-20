package nl.rutgerkok.worldgeneratorapi.decoration;

import java.util.List;

public interface WorldDecorator {

    /**
     * A list of all custom decorators of a world. Can be modified to add your own
     * custom decorators.
     *
     * @param type
     *            The decoration type to get the decorations for.
     *
     * @return A mutable list.
     */
    List<Decoration> getCustomDecorations(DecorationType type);

    /**
     * Sets whether the default Minecraft decorators of the given type are enabled.
     *
     * @param type
     *            The type.
     * @param enabled
     *            True if enabled, false otherwise.
     */
    void setDefaultDecoratorsEnabled(DecorationType type, boolean enabled);

    /**
     * Adds a custom decorator of the given type.
     *
     * @param type
     *            The type of decoration.
     * @param decorator
     *            The decorator.
     * @return This, for chaining.
     */
    default WorldDecorator withCustomDecoration(DecorationType type, Decoration decorator) {
        this.getCustomDecorations(type).add(decorator);
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
     */
    default WorldDecorator withoutDefaultDecorations(DecorationType type) {
        setDefaultDecoratorsEnabled(type, false);
        return this;
    }
}
