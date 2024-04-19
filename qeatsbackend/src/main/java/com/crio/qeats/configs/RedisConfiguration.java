 
package com.crio.qeats.configs;

import java.time.Duration;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import lombok.Data;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Data
@Component
public class RedisConfiguration {


  public static final String redisHost = "localhost";

  // Amount of time after which the redis entries should expire.
  public static final int REDIS_ENTRY_EXPIRY_IN_SECONDS = 3600;

  // TIP(MODULE_RABBITMQ): RabbitMQ related configs.
  public static final String EXCHANGE_NAME = "rabbitmq-exchange";
  public static final String QUEUE_NAME = "rabbitmq-queue";
  public static final String ROUTING_KEY = "qeats.postorder";


  private int redisPort;
  private JedisPool jedisPool; 


  @Value("${spring.redis.port}")
  public void setRedisPort(int port) {
    System.out.println("setting up redis port to " + port);
    redisPort = port;
  }

@Bean
public JedisConnectionFactory jedisConnectionFactory() {
    RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
    redisStandaloneConfiguration.setHostName("localhost");
    redisStandaloneConfiguration.setPort(redisPort);

    return new JedisConnectionFactory(redisStandaloneConfiguration);
}

@Bean
public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(jedisConnectionFactory());
    return redisTemplate;
}

  @PostConstruct
  public void initCache() {
    JedisPoolConfig poolConfig = buildPoolConfig();
    try {
    jedisPool = new JedisPool(poolConfig, redisHost, redisPort);
    } catch(Exception e)
    {
      e.printStackTrace();
    }
  }


 
  public boolean isCacheAvailable() {

    if(jedisPool==null)
    {
      return false;
    }
    try(Jedis jedis=this.getJedisPool().getResource())
    {
      return true;
    }
    catch(Exception e)
    {
      return false;
    }
  }


  public void destroyCache() {
    if (jedisPool != null) {
      jedisPool.getResource().flushAll();
      jedisPool.destroy();
      jedisPool=null;
    }
  }

public JedisPool getJedisPool()
{
  if(jedisPool!=null)
  {
    return jedisPool;
  }
  try{
    final JedisPoolConfig poolConfig=buildPoolConfig();
    jedisPool=new JedisPool(poolConfig,redisHost,redisPort);
  }
  catch(Exception e)
  {
   e.printStackTrace();
  }
  return jedisPool;
}
private JedisPoolConfig buildPoolConfig() {
  final JedisPoolConfig poolConfig = new JedisPoolConfig();
  poolConfig.setMaxTotal(128);
  poolConfig.setMaxIdle(128);
  poolConfig.setMinIdle(16);
  poolConfig.setTestOnBorrow(true);
  poolConfig.setTestOnReturn(true);
  poolConfig.setTestWhileIdle(true);
  poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
  poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
  poolConfig.setNumTestsPerEvictionRun(3);
  poolConfig.setBlockWhenExhausted(true);
  return poolConfig;
}

}

