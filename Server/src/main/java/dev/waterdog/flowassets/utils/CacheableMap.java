/*
 * Copyright 2022 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.flowassets.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

public class CacheableMap<K, V> implements Map<K, V> {

    private static final ScheduledExecutorService defaultExecutor = Executors.newSingleThreadScheduledExecutor();

    private final Map<K, Entry<V>> backedMap;
    private final ScheduledExecutorService executor;

    private final int timeout;
    private final TimeUnit unit;
    private final BiConsumer<K, V> expiredConsumer;

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    public CacheableMap(Map<K, Entry<V>> backedMap, ScheduledExecutorService executor, int timeout, TimeUnit unit, BiConsumer<K, V> expiredConsumer) {
        this.backedMap = backedMap;
        this.executor = executor;
        this.timeout = timeout;
        this.unit = unit;
        this.expiredConsumer = expiredConsumer;
    }

    private Entry<V> createEntry(K key, V value) {
        ScheduledFuture<?> future = this.executor.schedule(() -> this.onTimeout(key), this.timeout, this.unit);
        return new Entry<>(value, System.currentTimeMillis(), future);
    }

    private void onTimeout(K key) {
        Entry<V> value = this.backedMap.remove(key);
        if (value != null && this.expiredConsumer != null) {
            this.expiredConsumer.accept(key, value.getValue());
        }
    }

    @Override
    public int size() {
        return this.backedMap.size();
    }

    @Override
    public boolean isEmpty() {
        return this.backedMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.backedMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.backedMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        Entry<V> value = this.backedMap.get(key);
        return value == null ? null : value.getValue();
    }

    @Override
    public V put(K key, V value) {
        Entry<V> entry = this.backedMap.put(key, this.createEntry(key, value));
        if (entry != null) {
            entry.getFuture().cancel(true);
            return entry.getValue();
        }
        return null;
    }

    @Override
    public V remove(Object key) {
        Entry<V> entry = this.backedMap.remove(key);
        if (entry != null) {
            entry.getFuture().cancel(true);
            return entry.getValue();
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        this.backedMap.forEach((key, val) -> this.remove(key));
    }

    @Override
    public Set<K> keySet() {
        return this.backedMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {

            @Override
            public Iterator<V> iterator() {
                Iterator<Map.Entry<K, Entry<V>>> iterator = CacheableMap.this.backedMap.entrySet().iterator();
                return new AbstractIterator<V>() {

                    @Override
                    protected V computeNext() {
                        if (!iterator.hasNext()) {
                            return this.endOfData();
                        }

                        return iterator.next().getValue().getValue();
                    }
                };
            }

            @Override
            public int size() {
                return CacheableMap.this.backedMap.size();
            }
        };
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entries = new HashSet<>();
        for (Map.Entry<K, Entry<V>> entry : this.backedMap.entrySet()) {
            entries.add(new Map.Entry<K, V>() {
                @Override
                public K getKey() {
                    return entry.getKey();
                }

                @Override
                public V getValue() {
                    return entry.getValue().getValue();
                }

                @Override
                public V setValue(V value) {
                    Entry<V> val = entry.setValue(createEntry(this.getKey(), value));
                    if (val != null) {
                        val.getFuture().cancel(true);
                        return val.getValue();
                    }

                    return null;
                }
            });
        }

        return entries;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CacheableMap<?,?> map)) {
            return false;
        }

        return this.backedMap.equals(map.backedMap);
    }

    @Override
    public int hashCode() {
        return this.backedMap.hashCode();
    }

    @Setter
    @Accessors(fluent = true)
    public static class Builder<K, V> {
        private Map<K,  Entry<V>> backedMap = new ConcurrentHashMap<>();
        private ScheduledExecutorService executor = defaultExecutor;
        private int timeout;
        private TimeUnit unit;
        private BiConsumer<K, V> expiredConsumer;

        public CacheableMap<K, V> build() {
            if (this.timeout == 0 || this.unit == null) {
                throw new IllegalStateException("No timeout defined!");
            }
            return new CacheableMap<>(this.backedMap, this.executor, this.timeout, this.unit, this.expiredConsumer);
        }
    }

    @Getter
    @AllArgsConstructor
    private static class Entry<V> {
        private final V value;
        private final long insertTime;
        private final ScheduledFuture<?> future;
    }

    private abstract static class AbstractIterator<T> implements Iterator<T> {
        private enum State {
            READY,
            NOT_READY,
            DONE,
            FAILED,
        }

        private State state = State.NOT_READY;
        private T next;

        protected abstract T computeNext();


        protected final T endOfData() {
            this.state = State.DONE;
            return null;
        }

        @Override
        public final boolean hasNext() {
            if (this.state == State.FAILED) {
                throw new IllegalStateException();
            }

            switch (this.state) {
                case DONE:
                    return false;
                case READY:
                    return true;
                default:
            }
            return tryToComputeNext();
        }

        private boolean tryToComputeNext() {
            this.state = State.FAILED;
            this.next = computeNext();
            if (this.state != State.DONE) {
                this.state = State.READY;
                return true;
            }
            return false;
        }

        @Override
        public final T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            this.state = State.NOT_READY;
            T result = this.next;
            this.next = null;
            return result;
        }

        public final T peek() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            return this.next;
        }
    }
}
