package io.dataease.utils;


import io.dataease.cache.DECacheService;
import io.dataease.constant.CacheConstant;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class CacheUtils {

    private static DECacheService<Object> deCacheService;

    static {
        getService();
    }

    @SuppressWarnings("unchecked")
    private static DECacheService<Object> getService() {
        if (ObjectUtils.isEmpty(deCacheService)) {
            deCacheService = CommonBeanFactory.getBean(DECacheService.class);
        }
        return deCacheService;
    }

    public static void put(String cacheName, String key, Object val) {
        deCacheService.put(cacheName, key, val, 8L, TimeUnit.HOURS);
    }

    public static void put(String cacheName, String key, Object val, Long expTime, TimeUnit unit) {
        deCacheService.put(cacheName, key, val, expTime, unit);
    }

    public static Object get(String cacheName, String key) {
        return deCacheService.get(cacheName, key);
    }

    public static Boolean keyExist(String cacheName, String key) {
        return deCacheService.keyExist(cacheName, key);
    }

    public static void keyRemove(String cacheName, String key) {
        deCacheService.keyRemove(cacheName, key);
    }

    public static void remove(String cacheName, String key, Consumer<Object> consumer) {
        deCacheService.keyRemove(cacheName, key);
        consumer.accept(null);
        DelayQueueUtils.execute(IDUtils.randomID(16), () -> {
            deCacheService.keyRemove(cacheName, key);
        }, 1L);
    }

    public static void remove(String[] cacheNames, String key, Consumer<Object> consumer) {
        Arrays.stream(cacheNames).forEach(cacheName -> deCacheService.keyRemove(cacheName, key));
        consumer.accept(null);
        DelayQueueUtils.execute(IDUtils.randomID(16), () -> {
            Arrays.stream(cacheNames).forEach(cacheName -> deCacheService.keyRemove(cacheName, key));
        }, 1L);

    }

    public static void remove(String cacheName, List<String> keys, Consumer<Object> consumer) {
        keys.forEach(key -> deCacheService.keyRemove(cacheName, key));
        consumer.accept(null);
        DelayQueueUtils.execute(IDUtils.randomID(16), () -> {
            keys.forEach(key -> deCacheService.keyRemove(cacheName, key));
        }, 1L);
    }

    public static void remove(String[] cacheNames, List<String> keys, Consumer<Object> consumer) {
        Arrays.stream(cacheNames).forEach(cacheName -> {
            keys.forEach(key -> deCacheService.keyRemove(cacheName, key));
        });
        consumer.accept(null);
        DelayQueueUtils.execute(IDUtils.randomID(16), () -> {
            Arrays.stream(cacheNames).forEach(cacheName -> {
                keys.forEach(key -> deCacheService.keyRemove(cacheName, key));
            });
        }, 1L);
    }

    public static void evictUserPermissionCaches(Long userId) {
        if (userId == null) return;
        String key = String.valueOf(userId);
        deCacheService.keyRemove(CacheConstant.UserCacheConstant.USER_ROLES_CACHE, key);
        deCacheService.keyRemove(CacheConstant.UserCacheConstant.USER_BUSI_PERS_CACHE, key);
        deCacheService.keyRemove(CacheConstant.RoleCacheConstant.ROLE_BUSI_PERS_CACHE, key);
    }
}
