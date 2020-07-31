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
final class PutListeningMap<K, V> extends AbstractMap<K, V> {

    /**
     * The backing map, used for storage.
     */
    private final Map<K, V> backingMap;

    private final CopyOnWriteArrayList<BiConsumer<K, V>> eventListeners = new CopyOnWriteArrayList<>();

    PutListeningMap(Map<K, V> backingMap) {
        this.backingMap = Objects.requireNonNull(backingMap, "backingMap");
    }

    /**
     * Adds a listener that will receive all calls to {@link #put(Object, Object)}.
     *
     * @param listener
     *            The listener.
     */
    void addListener(BiConsumer<K, V> listener) {
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
        for (BiConsumer<K, V> listener : this.eventListeners) {
            listener.accept(key, value);
        }
        return returnValue;
    }

    @Override
    public V remove(Object key) {
        return backingMap.remove(key);
    }

}
