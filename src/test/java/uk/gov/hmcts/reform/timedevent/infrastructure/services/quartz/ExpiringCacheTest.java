package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ExpiringCacheTest {

    @Test
    void shouldReturnValueIfNotExpired() {
        ExpiringCache<String, String> cache = new ExpiringCache<>();
        cache.put("k1", "v1", 5000);

        Optional<String> result = cache.get("k1");

        assertTrue(result.isPresent());
        assertEquals("v1", result.get());
    }

    @Test
    void shouldReturnEmptyIfExpired() throws InterruptedException {
        ExpiringCache<String, String> cache = new ExpiringCache<>();
        cache.put("k1", "v1", 50);

        Thread.sleep(60); // allow TTL to expire

        Optional<String> result = cache.get("k1");

        assertFalse(result.isPresent(), "Expired key should return empty");
    }

    @Test
    void shouldRemoveExpiredEntryOnGet() throws InterruptedException {
        ExpiringCache<String, String> cache = new ExpiringCache<>();
        cache.put("k1", "v1", 50);

        Thread.sleep(60);

        cache.get("k1");

        Optional<String> result = cache.get("k1");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldOverwriteExistingValue() {
        ExpiringCache<String, String> cache = new ExpiringCache<>();

        cache.put("k1", "v1", 5000);
        cache.put("k1", "v2", 5000);

        Optional<String> result = cache.get("k1");

        assertTrue(result.isPresent());
        assertEquals("v2", result.get(), "New value should overwrite the old one");
    }

    @Test
    void shouldHandleConcurrentAccess() throws InterruptedException, ExecutionException {
        ExpiringCache<Integer, Integer> cache = new ExpiringCache<>();

        ExecutorService executor = Executors.newFixedThreadPool(10);

        Callable<Void> writeTask = () -> {
            for (int i = 0; i < 1000; i++) {
                cache.put(i, i, 5000);
            }
            return null;
        };

        Callable<Void> readTask = () -> {
            for (int i = 0; i < 1000; i++) {
                cache.get(i);
            }
            return null;
        };

        Future<?> f1 = executor.submit(writeTask);
        Future<?> f2 = executor.submit(readTask);
        Future<?> f3 = executor.submit(writeTask);

        f1.get();
        f2.get();
        f3.get();
        executor.shutdown();

        // Should still be consistent, no exceptions during concurrent access
        assertTrue(true);
    }

    @Test
    void shouldReturnEmptyForUnknownKeys() {
        ExpiringCache<String, String> cache = new ExpiringCache<>();
        Optional<String> result = cache.get("missing");

        assertTrue(result.isEmpty());
    }
}
