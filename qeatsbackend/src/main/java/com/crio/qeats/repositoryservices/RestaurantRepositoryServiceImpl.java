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
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
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
import java.util.Collections;
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
import org.springframework.scheduling.annotation.Async;
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
  private ItemRepository itemRepository;

  @Autowired
  private MenuRepository menuRepository;
  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  @Autowired
  private RedisConfiguration redisConfiguration;
  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  @Override
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


 

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search query.
  @Override
public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
    String searchString, LocalTime currentTime, Double servingRadiusInKms) {
          // Find exact matches first
    Optional<List<RestaurantEntity>> exactMatchesOptional = restaurantRepository.findRestaurantsByNameExact(searchString);//nameexact
    List<RestaurantEntity> exactMatches = exactMatchesOptional.orElseGet(Collections::emptyList);

    // Find partial matches
    Optional<List<RestaurantEntity>> partialMatchesOptional = restaurantRepository.findRestaurantsByName(searchString);
    List<RestaurantEntity> partialMatches = partialMatchesOptional.orElseGet(Collections::emptyList);

    // Combine exact and partial matches
    List<RestaurantEntity> matchingRestaurants = new ArrayList<>(exactMatches);
    partialMatches.removeAll(exactMatches); // Remove exact matches from partial matches to avoid duplicates
    matchingRestaurants.addAll(partialMatches);

    // Filter restaurants by serving radius and open status
    List<Restaurant> filteredRestaurants = matchingRestaurants.stream()
        .filter(restaurantEntity -> isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude, servingRadiusInKms))
        .map(restaurantEntity -> modelMapperProvider.get().map(restaurantEntity, Restaurant.class))
        .collect(Collectors.toList());

    return filteredRestaurants;

}


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
//   @Override
// public List<Restaurant> findRestaurantsByAttributes(
//     Double latitude, Double longitude,
//     String searchString, LocalTime currentTime, Double servingRadiusInKms) {

//     // Retrieve the list of matching restaurants from the repository
//     Optional<List<RestaurantEntity>> matchingRestaurantsOptional = restaurantRepository
//             .findByAttributesContainingIgnoreCase(searchString);

//     // If the optional is present, get the list of entities; otherwise, return an empty list
//     List<RestaurantEntity> matchingRestaurants = matchingRestaurantsOptional.orElseGet(Collections::emptyList);

// // for(RestaurantEntity r:matchingRestaurants)
// // {
// //     System.out.print("{ "+r.getId()+"}  ");
// // }
// System.out.println("{{{{{}}}}}}}->"+matchingRestaurants.size());
//     // Filter restaurants by serving radius and open status
//     List<Restaurant> filteredRestaurants = matchingRestaurants.stream()
//         .filter(restaurantEntity -> isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude, servingRadiusInKms))
//         .map(restaurantEntity -> modelMapperProvider.get().map(restaurantEntity, Restaurant.class))
//         .collect(Collectors.toList());
//         // List<Restaurant> filteredRestaurants = matchingRestaurants.stream()
//         // .map(restaurantEntity -> modelMapperProvider.get().map(restaurantEntity, Restaurant.class))
//         // .collect(Collectors.toList());
//         System.out.println("{{{{{}}}}}}}->"+filteredRestaurants.size());

//     return filteredRestaurants;
// }

@Override
public List<Restaurant> findRestaurantsByAttributes(Double latitude, Double longitude,String searchString, LocalTime currentTime, Double servingRadiusInKms) 
{
  Pattern patterns = Pattern.compile(searchString,Pattern.CASE_INSENSITIVE);
  Query query = new Query();
//   System.out.println("size->"+patterns.size());

   query.addCriteria(
   Criteria.where("attributes").regex(patterns));
  
   List<RestaurantEntity> restaurantEntityList= mongoTemplate.find(query, RestaurantEntity.class);
   List<Restaurant> restaurantList = new ArrayList<>();
   ModelMapper modelMapper = modelMapperProvider.get();
   System.out.println("  before->>"+restaurantEntityList.size());
  for (RestaurantEntity restaurantEntity : restaurantEntityList)
   {
    if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime,latitude, longitude, servingRadiusInKms))
     {
       restaurantList.add(modelMapper.map(restaurantEntity,Restaurant.class));
     }
   }
   System.out.print("  after->>>"+restaurantList.size());
   return restaurantList;
}
/*public List<Restaurant> findRestaurantsByAttributes(Double latitude, Double longitude,String searchString, LocalTime currentTime, Double servingRadiusInKms) 
{
  List<Pattern> patterns = Arrays
  .stream(searchString.split(" "))
   .map(attr -> Pattern.compile(attr, Pattern.CASE_INSENSITIVE))
    .collect(Collectors.toList());
  Query query = new Query();
  System.out.println("size->"+patterns.size());
  for (Pattern pattern : patterns) 
  {
   query.addCriteria(
   Criteria.where("attributes").regex(pattern));
  }
   List<RestaurantEntity> restaurantEntityList= mongoTemplate.find(query, RestaurantEntity.class);
   List<Restaurant> restaurantList = new ArrayList<>();
   ModelMapper modelMapper = modelMapperProvider.get();
   System.out.println("  before->>"+restaurantEntityList.size());
  for (RestaurantEntity restaurantEntity : restaurantEntityList)
   {
    if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime,latitude, longitude, servingRadiusInKms))
     {
       restaurantList.add(modelMapper.map(restaurantEntity,Restaurant.class));
     }
   }
   System.out.print("  after->>>"+restaurantList.size());
   return restaurantList;
} */

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose names form a complete or partial match
  // with the search query.

  @Override
  public List<Restaurant> findRestaurantsByItemName(
    Double latitude, Double longitude,
    String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    // Query items from ItemRepository based on item name
    Optional<List<ItemEntity>> matchingItemsExactOptional = itemRepository.findItemsByNameExact(searchString);
    Optional<List<ItemEntity>> matchingItemsInExactOptional = itemRepository.findItemsByNameInexact(searchString);

    List<ItemEntity> combinedMatchingItems = new ArrayList<>();

    // If the optional for exact matches is present, add its content to the combined list
    matchingItemsExactOptional.ifPresent(combinedMatchingItems::addAll);
    
    // If the optional for inexact matches is present, add its content to the combined list
    matchingItemsInExactOptional.ifPresent(combinedMatchingItems::addAll);

    // Get the restaurant IDs associated with matching items from MenuRepository
    List<String> itemIds = combinedMatchingItems.stream()
        .map(ItemEntity::getId)
        .collect(Collectors.toList());

    // Retrieve the list of matching menus from the repository
    Optional<List<MenuEntity>> optionalMatchingMenus = menuRepository.findMenusByItemsItemIdIn(itemIds);
    
    // If the optional is present, get the list of menus; otherwise, return an empty list
    List<MenuEntity> matchingMenus = optionalMatchingMenus.orElseGet(Collections::emptyList);

    // Get the restaurant IDs associated with matching menus
    Set<String> restaurantIds = matchingMenus.stream()
        .map(MenuEntity::getRestaurantId)
        .collect(Collectors.toSet());

 
     // Retrieve the list of matching restaurants from the repository
     Optional<List<RestaurantEntity>> optionalMatchingRestaurants = restaurantRepository.findByIdIn(restaurantIds);
    
     // If the optional is present, get the list of restaurants; otherwise, return an empty list
     List<RestaurantEntity> matchingRestaurants = optionalMatchingRestaurants.orElseGet(Collections::emptyList);

    // Filter restaurants by serving radius and open status
    List<Restaurant> filteredRestaurants = matchingRestaurants.stream()
        .filter(restaurantEntity -> isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude, servingRadiusInKms))
        .map(restaurantEntity -> modelMapperProvider.get().map(restaurantEntity, Restaurant.class))
        .collect(Collectors.toList());

    return filteredRestaurants;
}

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
  
        List<ItemEntity> matchingItems = itemRepository.findByAttributesContainingIgnoreCase(searchString);

        // Get the IDs of the items with matching attributes
        List<String> itemIds = matchingItems.stream()
            .map(ItemEntity::getId)
            .collect(Collectors.toList());
    
        // Query menus from MongoDB based on item IDs
        Optional<List<MenuEntity>> optionalMatchingMenus = menuRepository.findMenusByItemsItemIdIn(itemIds);
        
        // Check if any matching menus were found
        if (optionalMatchingMenus.isPresent()) {
            // Extract the list of matching menus
            List<MenuEntity> matchingMenus = optionalMatchingMenus.get();
            
            // Extract the restaurant IDs from the matching menus
            Set<String> restaurantIds = matchingMenus.stream()
                .map(MenuEntity::getRestaurantId)
                .collect(Collectors.toSet());

      // Retrieve the list of matching restaurants from the repository
      Optional<List<RestaurantEntity>> optionalMatchingRestaurants = restaurantRepository.findByIdIn(restaurantIds);
    
      // If the optional is present, get the list of restaurants; otherwise, return an empty list
      List<RestaurantEntity> matchingRestaurants = optionalMatchingRestaurants.orElseGet(Collections::emptyList);
            // Filter restaurants by serving radius and open status
            List<Restaurant> filteredRestaurants = matchingRestaurants.stream()
                .filter(restaurantEntity -> isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude, servingRadiusInKms))
                .map(restaurantEntity -> modelMapperProvider.get().map(restaurantEntity, Restaurant.class))
                .collect(Collectors.toList());
           

            return filteredRestaurants;
        } else {
            return Collections.emptyList(); // Return an empty list if no matching menus were found
        }
  }





  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   * @return boolean True if restaurant falls within serving radius and is open, false otherwise
   */
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

