package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
public class ExpiringCache<K, V> {

    private static class CacheEntry<V> {
        V value;
        Instant expiry;

        CacheEntry(V value, long ttlMillis) {
            this.value = value;
            this.expiry = Instant.now().plusMillis(ttlMillis);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }

    private final Map<K, CacheEntry<V>> store = new ConcurrentHashMap<>();

    public void put(K key, V value, long ttlMillis) {
        log.info("-------------Putting key: {} with value: {} into store", key, value);
        store.put(key, new CacheEntry<>(value, ttlMillis));
    }

    public Optional<V> get(K key) {
        CacheEntry<V> entry = store.get(key);
        if (entry == null || entry.isExpired()) {
            store.remove(key);
            log.info("-------------Getting empty value key: {}", key);
            return Optional.empty();
        }
        log.info("-------------Getting non-empty value key: {}", key);
        return Optional.of(entry.value);
    }
}
