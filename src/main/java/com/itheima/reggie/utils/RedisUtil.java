package com.itheima.reggie.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itheima.reggie.service.impl.DishServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 类原名：ConvertObjUtil
 * @author MathewTang
 * @date 2025/07/13 19:42
 */
@Component
public class RedisUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
    }

    /**
     * TODO: ${@link DishServiceImpl#getDetailByIdWithFlavor(Long)} L111
     *   为解决这样问题：java.util.LinkedHashMap cannot be cast to com.baomidou.mybatisplus.extension.plugins.pagination.Page
     *
     * @param obj {@link Object}
     * @param targetType {@link Class<T>}
     * @return {@link T}
     */
    public static <T> T convertObj(Object obj, Class<T> targetType) {
        return mapper.convertValue(obj, targetType);
    }

    /**
     * 删除指定前缀的缓存
     * @param prefix 键前缀 (如 "list")
     */
    public static void deleteKeysByPrefixAsync(final RedisTemplate<Object, Object> redisTemplate, final String prefix) {
        // 定义每次 SCAN 返回的最大数量
        int scanCount = 100;
        String pattern = prefix + "*";
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(scanCount).build();

        List<String> batchKeys = new ArrayList<>(scanCount);

        try (Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
                connection -> connection.scan(options))) {

            while (cursor.hasNext()) {
                batchKeys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                // 分批删除
                if (batchKeys.size() >= scanCount) {
                    performDelete(redisTemplate, batchKeys);
                    batchKeys.clear();
                }
            }

            // 删除剩余的 key
            if (!batchKeys.isEmpty()) {
                performDelete(redisTemplate, batchKeys);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error scanning keys with prefix: " + prefix, e);
        }
    }

    /**
     * TODO: 异步删除（高并发场景推荐）
     *
     * @param redisTemplate {@link Object}
     * @param keys {@link Object>}
     */
    private static void performDelete(RedisTemplate<Object, Object> redisTemplate, List<String> keys) {
        redisTemplate.execute((RedisCallback<Long>) connection -> {
            long deleted = 0;
            for (String key : keys) {
                deleted += connection.del(key.getBytes(StandardCharsets.UTF_8));
            }
            return deleted;
        });
    }
}
