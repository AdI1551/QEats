/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Primary
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {
  private static final Logger log = LoggerFactory.getLogger(RestaurantRepositoryService.class);



  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private RestaurantRepository restaurantRepository;
  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  @Autowired
  private RedisConfiguration redisConfiguration;
  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude,
  LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurants = new ArrayList<>();
    LocalTime roundedTime = currentTime.truncatedTo(ChronoUnit.SECONDS);
    log.info("CURRENT--TIME=={}",roundedTime);

    int geoHashPrecison=7;
    String cacheKey=GeoHash.geoHashStringWithCharacterPrecision(latitude, longitude, geoHashPrecison);
log.info("the value of catchkey={}",cacheKey);
    try (Jedis jedis = redisConfiguration.getJedisPool().getResource()) {
        String cachedResult = jedis.get(cacheKey);
        if (cachedResult != null) {
            restaurants = new ObjectMapper().readValue(cachedResult, new TypeReference<List<Restaurant>>() {});
            log.info("Found cached result for key: {}", cacheKey);
            return restaurants;
        }
    } catch (IOException e) {
        log.error("Error reading cached result for key: {}", cacheKey, e);
    }


    restaurants = findAllRestaurantsFromMongo(latitude, longitude, currentTime, servingRadiusInKms);

    // Cache the result
    try (Jedis jedis = redisConfiguration.getJedisPool().getResource()) {
        String jsonResult = new ObjectMapper().writeValueAsString(restaurants);
        jedis.setex(cacheKey, GlobalConstants.REDIS_ENTRY_EXPIRY_IN_SECONDS, jsonResult);
        log.info("Cached result for key: {}", cacheKey);
    } catch (JsonProcessingException e) {
        log.error("Error caching result for key: {}", cacheKey, e);
    }

    return restaurants;
}

private List<Restaurant> findAllRestaurantsFromMongo(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {
List<Restaurant> restaurants = new ArrayList<>();
List<RestaurantEntity> allRestaurants = restaurantRepository.findAll();
for (RestaurantEntity res : allRestaurants) {
if (isRestaurantCloseByAndOpen(res, currentTime, latitude, longitude, servingRadiusInKms)) {
restaurants.add(modelMapperProvider.get().map(res, Restaurant.class));
}
}
return restaurants;
}


  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude,
          restaurantEntity.getLatitude(), restaurantEntity.getLongitude())
          < servingRadiusInKms;
    }

    return false;
  }
 



}

