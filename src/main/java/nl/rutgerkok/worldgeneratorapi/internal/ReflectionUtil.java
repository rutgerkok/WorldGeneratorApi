package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.NoSuchElementException;

final class ReflectionUtil {

    /**
     * Gets a field from an object.
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
    static Field getFieldOfType(Object on, Class<?> typeOfField) {
        for (Field field : on.getClass().getDeclaredFields()) {
            if (field.getType().equals(typeOfField)) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchElementException("No field on " + on.getClass().getSimpleName()
                + " of type " + typeOfField);
    }
}
