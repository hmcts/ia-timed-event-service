package uk.gov.hmcts.reform.timedevent.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class CaffeineConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.cache")
    public CacheNamesProperties cacheNamesProperties() {
        return new CacheNamesProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.cache.caffeine.spec")
    public Map<String, String> caffeineSpecs() {
        return new HashMap<>();
    }

    @Bean
    public CacheManager cacheManager(
            CacheNamesProperties cacheNamesProps,
            Map<String, String> caffeineSpecs
    ) {

        List<CaffeineCache> caches = cacheNamesProps.getCacheNames().stream()
                .map(name -> {
                    String spec = caffeineSpecs.get(name);
                    Caffeine<Object, Object> builder =
                            (spec == null) ? Caffeine.newBuilder() : Caffeine.from(spec);

                    return new CaffeineCache(name, builder.build());
                })
                .collect(Collectors.toList());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(caches);
        return manager;
    }

    public static class CacheNamesProperties {
        private List<String> cacheNames = new ArrayList<>();

        public List<String> getCacheNames() {
            return cacheNames;
        }

        public void setCacheNames(List<String> cacheNames) {
            this.cacheNames = cacheNames;
        }
    }
}
