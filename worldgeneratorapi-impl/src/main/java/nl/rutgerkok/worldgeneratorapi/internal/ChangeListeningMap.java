package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Forwards all calls to the given map. Fires the event listeners when
 * {@link #put(Object, Object)} (or a variant of it) is called.
 *
 * @param <K>
 *            Type of the key.
 * @param <V>
 *            Type of the value.
 */
final class ChangeListeningMap<K, V> extends AbstractMap<K, V> {

    /**
     * The backing map, used for storage.
     */
    private final Map<K, V> backingMap;

    private final CopyOnWriteArrayList<BiConsumer<V, Boolean>> eventListeners = new CopyOnWriteArrayList<>();

    ChangeListeningMap(Map<K, V> backingMap) {
        this.backingMap = Objects.requireNonNull(backingMap, "backingMap");
    }

    /**
     * Adds a listener that will receive all calls to {@link #put(Object, Object)}
     * and {@link #remove(Object)}.
     *
     * @param listener
     *            The listener.
     */
    void addListener(BiConsumer<V, Boolean> listener) {
        Objects.requireNonNull(listener, "listener");
        this.eventListeners.add(listener);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return backingMap.entrySet();
    }

    @Override
    public V get(Object key) {
        return backingMap.get(key);
    }

    @Override
    public V put(K key, V value) {
        V returnValue = backingMap.put(key, value);
        if (returnValue != null) {
            // A values is being removed
            for (BiConsumer<V, Boolean> listener : this.eventListeners) {
                listener.accept(returnValue, false);
            }
        }
        for (BiConsumer<V, Boolean> listener : this.eventListeners) {
            listener.accept(value, true);
        }
        return returnValue;
    }

    @Override
    public V remove(Object key) {
        V oldValue = backingMap.remove(key);
        if (oldValue != null) {
            for (BiConsumer<V, Boolean> listener : this.eventListeners) {
                listener.accept(oldValue, false);
            }
        }
        return oldValue;
    }

}
