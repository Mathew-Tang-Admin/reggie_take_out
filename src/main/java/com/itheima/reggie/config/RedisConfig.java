package com.itheima.reggie.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itheima.reggie.entity.Dish;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;

@Configuration
public class RedisConfig extends CachingConfigurerSupport {

    @Value("${spring.cache.redis.time-to-live}")
    private Long timeToLive;

    @Bean("dishRedisCacheManager")
    public RedisCacheManager userRedisCacheManager(RedisConnectionFactory factory) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // 支持 LocalDateTime

        Jackson2JsonRedisSerializer<Dish> redisSerializer = new Jackson2JsonRedisSerializer<>(Dish.class);
        redisSerializer.setObjectMapper(mapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(timeToLive)) // 硬编码设置 TTL
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * TODO:
     *     ${@code @Primary} 设置为默认的Redis管理器
     *     ${@code @CacheEvict(cacheManager = "", value = "userCache", key = "#id")} 或者可以这么使用
     *
     * @param factory {@link RedisConnectionFactory}
     * @return {@link RedisCacheManager}
     */
    @Bean
    @Primary
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(timeToLive)) // 硬编码设置 TTL
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer()));

        return RedisCacheManager.builder(factory).cacheDefaults(config).build();
    }

    private RedisSerializer<String> keySerializer() {
        return new StringRedisSerializer();
    }

    private RedisSerializer<Object> valueSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // 支持 LocalDateTime
        // 开启默认类型信息（这样反序列化时就能还原成具体类型）
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        // 在SetMealController#getDetail中会有问题 （因为那里有基础类型嵌套）
        Jackson2JsonRedisSerializer<Object> redisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        redisSerializer.setObjectMapper(mapper);
        return redisSerializer;


        // 默认处理多态的json序列化器 【这个就 <==> mapper,null 的构造方法】
        /* GenericJackson2JsonRedisSerializer redisSerializer = new GenericJackson2JsonRedisSerializer(mapper);
        return redisSerializer; */

    }

    /**
     * TODO:
     *     DishController中旧版手动管理的实现方式
     * @param factory {@link RedisConnectionFactory}
     * @return {@link RedisTemplate<Object,Object>}
     */
    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);

        // 设置 key 和 hashKey 使用 String 序列化
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // 创建 ObjectMapper 并注册 JavaTimeModule
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // 设置 value 和 hashValue 使用 JSON 序列化
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        // 开启默认类型信息（这样反序列化时就能还原成具体类型）
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper); // 关键步骤：注入带 JavaTimeModule 的 ObjectMapper

        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}