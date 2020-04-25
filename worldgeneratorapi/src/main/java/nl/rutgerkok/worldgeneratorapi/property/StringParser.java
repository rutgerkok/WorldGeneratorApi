package nl.rutgerkok.worldgeneratorapi.property;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

/**
 * Helper method to parse strings.
 *
 * @since 0.5
 */
public final class StringParser {

    /**
     * Parses the given string into an instance of the given class. Materials are
     * special-cased, for all others {@code clazz.valueOf(String)} is called.
     *
     * @param <T>
     *            The type of the class.
     * @param value
     *            The value to parse.
     * @param clazz
     *            The class.
     * @return The parsed vale.
     * @throws IllegalArgumentException
     *             If parsing fails.
     * @since 0.5
     */
    public static <T> T parse(String value, Class<T> clazz) throws IllegalArgumentException {
        if (clazz == Material.class) {
            Material material = Material.matchMaterial(value);
            if (material == null) {
                throw new IllegalArgumentException("unknown material: \"" + value + "\"");
            }
            return clazz.cast(material);
        }
        if (clazz == BlockData.class) {
            return clazz.cast(Bukkit.createBlockData(value));
        }
        try {
            Method method = clazz.getMethod("valueOf", String.class);
            return clazz.cast(method.invoke(null, value));
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("parsing a " + clazz.getSimpleName()
                    + " is not supported. Ask the developer of that class to add a static " + clazz.getSimpleName()
                    + ".valueOf(String) method");
        } catch (SecurityException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
            throw new IllegalArgumentException("could not parse \"" + value + "\" as a " + clazz.getSimpleName());
        }
    }

    private StringParser() {
        // No instances
    }
}
