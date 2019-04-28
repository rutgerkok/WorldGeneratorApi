package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class ReflectionUtil {

    /**
     * Gets a field from an object. The field will be made accessible.
     *
     * @param on
     *            The object.
     * @param name
     *            The name of the field.
     * @return The field.
     * @throws NoSuchElementException
     *             If the object does not contain a field of the type
     *             {@code typeOfField}.
     */
    public static Field getFieldByName(Object on, String name) {
        Objects.requireNonNull(name, "name");
        Class<?> clazz = on.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {

            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchElementException("No field on " + on.getClass().getSimpleName()
                + " of name " + name);
    }

    /**
     * Gets a field from a class. The field will be made accessible.
     *
     * @param onClazz
     *            The class that declares the field..
     * @param typeOfField
     *            The type of the field.
     * @return The field.
     * @throws NoSuchElementException
     *             If the object does not contain a field of the type
     *             {@code typeOfField}.
     */
    public static Field getFieldOfType(Class<?> onClazz, Class<?> typeOfField) {
        Objects.requireNonNull(onClazz, "onClazz");

        Class<?> clazz = onClazz;
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType().equals(typeOfField)) {
                    field.setAccessible(true);
                    return field;
                }
            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchElementException("No field on " + onClazz.getSimpleName()
                + " of type " + typeOfField);
    }

    /**
     * Gets a field from an object. The field will be made accessible.
     *
     * @param on
     *            The object.
     * @param typeOfField
     *            The type of the field.
     * @return The field.
     * @throws NoSuchElementException
     *             If the object does not contain a field of the type
     *             {@code typeOfField}.
     */
    public static Field getFieldOfType(Object on, Class<?> typeOfField) {
        Objects.requireNonNull(typeOfField, "typeOfField");

        return getFieldOfType(on.getClass(), typeOfField);
    }
}
