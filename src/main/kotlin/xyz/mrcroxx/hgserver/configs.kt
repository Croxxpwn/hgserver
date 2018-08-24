package xyz.mrcroxx.hgserver

import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import java.io.Serializable


@Configuration
@AutoConfigureAfter(RedisAutoConfiguration::class)
class RedisConfig {

    /*
    @Bean
    fun redisCacheTemplate(redisConnectionFactory: LettuceConnectionFactory): RedisTemplate<String, Serializable> {
        // TODO: miss type id when derializing kotlin data class
        val template = RedisTemplate<String, Serializable>()
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = GenericJackson2JsonRedisSerializer()
        template.setConnectionFactory(redisConnectionFactory)
        return template
    }
    */


}

@Configuration
class RestTemplateConfig {
    @Bean
    fun byteArrayHttpMessageConverter(): ByteArrayHttpMessageConverter {
        return ByteArrayHttpMessageConverter()
    }
}