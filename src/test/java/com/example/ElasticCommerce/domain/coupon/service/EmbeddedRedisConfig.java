package com.example.ElasticCommerce.domain.coupon.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * 테스트 실행 시 임베디드 Redis 서버를 자동으로 띄우고,
 * RedisTemplate 빈을 등록하는 설정입니다.
 */
@TestConfiguration
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    /**
     * @PostConstruct: 컨텍스트 생성 후 호출되어 임베디드 Redis를 시작합니다.
     */
    @PostConstruct
    public void startRedis() throws IOException {
        // 기본 포트 6379로 임베디드 Redis 서버 띄우기
        this.redisServer = new RedisServer(6379);
        redisServer.start();
    }

    /**
     * @PreDestroy: 컨텍스트 종료 직전에 호출되어 Redis를 중지합니다.
     */
    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    /**
     * LettuceConnectionFactory 빈 등록 → localhost:6379 임베디드 Redis에 연결합니다.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("localhost", 6379);
    }

    /**
     * RedisTemplate<String, String> 빈 등록:
     * key/value 모두 StringRedisSerializer 로 직렬화/역직렬화합니다.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}
