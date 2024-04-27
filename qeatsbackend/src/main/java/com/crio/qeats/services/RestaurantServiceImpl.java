
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.modelmapper.internal.bytebuddy.asm.Advice.Return;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
// @Log4j2
public class RestaurantServiceImpl implements RestaurantService {
  private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);
  private final Double PEAK_HOURS_SERVING_RADIUS_IN_KMS = 3.0;
  private final Double NORMAL_HOURS_SERVING_RADIUS_IN_KMS = 5.0;
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;


  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
    double serviceRadius = getServiceRadius(currentTime);
    List<Restaurant> restaurants = restaurantRepositoryService.findAllRestaurantsCloseBy(
        getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(),
        currentTime, serviceRadius);
    return new GetRestaurantsResponse(restaurants);
  }
 // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
        GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
          System.out.print("entered  {findRestaurantsBySearchQuery} here \n with lat="+getRestaurantsRequest.getLatitude()+" long="+getRestaurantsRequest.getLongitude()+" searchfor"+getRestaurantsRequest.getSearchFor());
         Long start=System.currentTimeMillis();
          Double serviceRadius;
          List<Restaurant> allRestaurants = new ArrayList<>();
          if(getRestaurantsRequest.getSearchFor().equals("")){return new GetRestaurantsResponse(allRestaurants);}
      
          // Determine service radius based on peak or normal hours
          if (isInPeakHours(currentTime)) {
              serviceRadius = PEAK_HOURS_SERVING_RADIUS_IN_KMS;
          } else {
              serviceRadius = NORMAL_HOURS_SERVING_RADIUS_IN_KMS;
          }
      
          // Retrieve restaurants by name (exact and partial matches)
          List<Restaurant> restaurantsByName = restaurantRepositoryService
              .findRestaurantsByName(getRestaurantsRequest.getLatitude(), 
                                      getRestaurantsRequest.getLongitude(), 
                                      getRestaurantsRequest.getSearchFor(), 
                                      currentTime, 
                                      serviceRadius);
      
          // Retrieve restaurants by attributes (cuisines)
          List<Restaurant> restaurantsByAttributes = restaurantRepositoryService
              .findRestaurantsByAttributes(getRestaurantsRequest.getLatitude(), 
                                           getRestaurantsRequest.getLongitude(), 
                                           getRestaurantsRequest.getSearchFor(), 
                                           currentTime, 
                                           serviceRadius);
      
          // Retrieve restaurants by item name
          List<Restaurant> restaurantsByItemName = restaurantRepositoryService
              .findRestaurantsByItemName(getRestaurantsRequest.getLatitude(), 
                                         getRestaurantsRequest.getLongitude(), 
                                         getRestaurantsRequest.getSearchFor(), 
                                         currentTime, 
                                         serviceRadius);
      
          // Retrieve restaurants by item attributes
          List<Restaurant> restaurantsByItemAttributes = restaurantRepositoryService
              .findRestaurantsByItemAttributes(getRestaurantsRequest.getLatitude(), 
                                               getRestaurantsRequest.getLongitude(), 
                                               getRestaurantsRequest.getSearchFor(), 
                                               currentTime, 
                                               serviceRadius);
      
          // Combine results from all sources
          allRestaurants.addAll(restaurantsByName);
          allRestaurants.addAll(restaurantsByAttributes);
          allRestaurants.addAll(restaurantsByItemName);
          allRestaurants.addAll(restaurantsByItemAttributes);
      
          // Apply ordering rules (not implemented here)
      
          // Remove duplicates from the list (if any)
          Set<String> uniqueRestaurantIds = new HashSet<>();
          List<Restaurant> uniqueRestaurants = new ArrayList<>();
          for (Restaurant restaurant : allRestaurants) {
              if (!uniqueRestaurantIds.contains(restaurant.getRestaurantId())) {
                  uniqueRestaurantIds.add(restaurant.getRestaurantId());
                  uniqueRestaurants.add(restaurant);
              }
          }
          Long end=System.currentTimeMillis();
         System.out.println("the time taken by serachbyquery NO Mt->>"+(end-start));
          // Create and return the response object
          return new GetRestaurantsResponse(uniqueRestaurants);
  }

  private double getServiceRadius(LocalTime currentTime) {
    if (isInPeakHours(currentTime)) {
      return PEAK_HOURS_SERVING_RADIUS_IN_KMS;
    } else {
      return NORMAL_HOURS_SERVING_RADIUS_IN_KMS;
    }
  }

  private boolean isInPeakHours(LocalTime currentTime) {
    LocalTime peakStartTime1 = LocalTime.of(8, 0);
    LocalTime peakEndTime1 = LocalTime.of(10, 0);
    LocalTime peakStartTime2 = LocalTime.of(13, 0);
    LocalTime peakEndTime2 = LocalTime.of(14, 0);
    LocalTime peakStartTime3 = LocalTime.of(19, 0);
    LocalTime peakEndTime3 = LocalTime.of(21, 0);
    return (currentTime.equals(peakStartTime1) || currentTime.isAfter(peakStartTime1)) &&
    (currentTime.equals(peakEndTime1) || currentTime.isBefore(peakEndTime1)) ||
    (currentTime.equals(peakStartTime2) || currentTime.isAfter(peakStartTime2)) &&
    (currentTime.equals(peakEndTime2) || currentTime.isBefore(peakEndTime2)) ||
    (currentTime.equals(peakStartTime3) || currentTime.isAfter(peakStartTime3)) &&
    (currentTime.equals(peakEndTime3) || currentTime.isBefore(peakEndTime3));
  }

 
 



  

  // TODO: CRIO_TASK_MODULE_MULTITHREADING
  // Implement multi-threaded version of RestaurantSearch.
  // Implement variant of findRestaurantsBySearchQuery which is at least 1.5x time faster than
  // findRestaurantsBySearchQuery.
  // @Override
  // public GetRestaurantsResponse findRestaurantsBySearchQueryMt(
  //     GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
  //       System.out.print("entered  {findRestaurantsBySearchQueryMTTTTTTT} here \n with lat="+getRestaurantsRequest.getLatitude()+" long="+getRestaurantsRequest.getLongitude()+" searchfor"+getRestaurantsRequest.getSearchFor());
  //        long start=System.currentTimeMillis();
  //       Double serviceRadius;
  //       List<Restaurant> allRestaurants = new ArrayList<>();
  //       if(getRestaurantsRequest.getSearchFor().equals("")){return new GetRestaurantsResponse(allRestaurants);}
    
  //       // Determine service radius based on peak or normal hours
  //       if (isInPeakHours(currentTime)) {
  //           serviceRadius = PEAK_HOURS_SERVING_RADIUS_IN_KMS;
  //       } else {
  //           serviceRadius = NORMAL_HOURS_SERVING_RADIUS_IN_KMS;
  //       }
    
  //       // Retrieve restaurants by name (exact and partial matches)
  //       CompletableFuture<List<Restaurant>> restaurantsByName =CompletableFuture.supplyAsync(()-> restaurantRepositoryService
  //           .findRestaurantsByName(getRestaurantsRequest.getLatitude(), 
  //                                   getRestaurantsRequest.getLongitude(), 
  //                                   getRestaurantsRequest.getSearchFor(), 
  //                                   currentTime, 
  //                                   serviceRadius));
    
  //       // Retrieve restaurants by attributes (cuisines)
  //       CompletableFuture<List<Restaurant>> restaurantsByAttributes = CompletableFuture.supplyAsync(()->restaurantRepositoryService
  //           .findRestaurantsByAttributes(getRestaurantsRequest.getLatitude(), 
  //                                        getRestaurantsRequest.getLongitude(), 
  //                                        getRestaurantsRequest.getSearchFor(), 
  //                                        currentTime, 
  //                                        serviceRadius));
    
  //       // Retrieve restaurants by item name
  //       CompletableFuture<List<Restaurant>> restaurantsByItemName = CompletableFuture.supplyAsync(()->restaurantRepositoryService
  //           .findRestaurantsByItemName(getRestaurantsRequest.getLatitude(), 
  //                                      getRestaurantsRequest.getLongitude(), 
  //                                      getRestaurantsRequest.getSearchFor(), 
  //                                      currentTime, 
  //                                      serviceRadius));
    
  //       // Retrieve restaurants by item attributes
  //       CompletableFuture<List<Restaurant>> restaurantsByItemAttributes =CompletableFuture.supplyAsync(()->restaurantRepositoryService
  //           .findRestaurantsByItemAttributes(getRestaurantsRequest.getLatitude(), 
  //                                            getRestaurantsRequest.getLongitude(), 
  //                                            getRestaurantsRequest.getSearchFor(), 
  //                                            currentTime, 
  //                                            serviceRadius));
    
  //       // Combine results from all sources
  //       CompletableFuture<List<Restaurant>>combinedRestaurants=CompletableFuture.allOf(restaurantsByName,restaurantsByAttributes,restaurantsByItemName,restaurantsByItemAttributes);
  //       try {
  //         allRestaurants.addAll(restaurantsByName.get());
  //       } catch (InterruptedException | ExecutionException e) {
  //         // TODO Auto-generated catch block
  //         e.printStackTrace();
  //       }
  //       try {
  //         allRestaurants.addAll(restaurantsByAttributes.get());
  //       } catch (InterruptedException | ExecutionException e) {
  //         // TODO Auto-generated catch block
  //         e.printStackTrace();
  //       }
  //       try {
  //         allRestaurants.addAll(restaurantsByItemName.get());
  //       } catch (InterruptedException | ExecutionException e) {
  //         // TODO Auto-generated catch block
  //         e.printStackTrace();
  //       }
  //       try {
  //         allRestaurants.addAll(restaurantsByItemAttributes.get());
  //       } catch (InterruptedException | ExecutionException e) {
  //         // TODO Auto-generated catch block
  //         e.printStackTrace();
  //       }
    
  //       // Apply ordering rules (not implemented here)
    
  //       // Remove duplicates from the list (if any)
  //       Set<String> uniqueRestaurantIds = new HashSet<>();
  //       List<Restaurant> uniqueRestaurants = new ArrayList<>();
  //       for (Restaurant restaurant : allRestaurants) {
  //           if (!uniqueRestaurantIds.contains(restaurant.getRestaurantId())) {
  //               uniqueRestaurantIds.add(restaurant.getRestaurantId());
  //               uniqueRestaurants.add(restaurant);
  //           }
  //       }
  //       long end=System.currentTimeMillis();
  //       System.out.println("time taken to completesearchbyqueryMT->"+(end-start));
  //       // Create and return the response object
  //       return new GetRestaurantsResponse(uniqueRestaurants);
  //   //  return null;
  // }
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQueryMt(
        GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
          System.out.print("entered  {findRestaurantsBySearchQuerymt} here \n with lat="+getRestaurantsRequest.getLatitude()+" long="+getRestaurantsRequest.getLongitude()+" searchfor"+getRestaurantsRequest.getSearchFor()+"\n");
         long start=System.currentTimeMillis();
          Double serviceRadius;
          Double latitude=getRestaurantsRequest.getLatitude();
          Double longitude=getRestaurantsRequest.getLongitude();
          String searchString=getRestaurantsRequest.getSearchFor();
          List<Restaurant> allRestaurants = new ArrayList<>();
          if(getRestaurantsRequest.getSearchFor().equals("")){return new GetRestaurantsResponse(allRestaurants);}
      
          // Determine service radius based on peak or normal hours
          if (isInPeakHours(currentTime)) {
              serviceRadius = PEAK_HOURS_SERVING_RADIUS_IN_KMS;
          } else {
              serviceRadius = NORMAL_HOURS_SERVING_RADIUS_IN_KMS;
          }
      
          // Retrieve restaurants by name (exact and partial matches)
          CompletableFuture<List<Restaurant>> future1=findByItemNameMt(latitude, longitude, searchString, currentTime, serviceRadius);
      
          // Retrieve restaurants by attributes (cuisines)
          CompletableFuture<List<Restaurant>> future2=findByAttributesMt(latitude, longitude, searchString, currentTime, serviceRadius);
      
          // Retrieve restaurants by item name
          CompletableFuture<List<Restaurant>> future3=findByItemNameMt(latitude, longitude, searchString, currentTime, serviceRadius);
          
          // Retrieve restaurants by item attributes
          CompletableFuture<List<Restaurant>> future4=findByItemAttributesMt(latitude, longitude, searchString, currentTime, serviceRadius);
      
          // Combine results from all sources
          // if(restaurantsByName==null){}
          CompletableFuture.allOf(future1,future2,future3,future4);
          try {
            allRestaurants.addAll(future1.get());
              allRestaurants.addAll(future2.get());
              allRestaurants.addAll(future3.get());
              allRestaurants.addAll(future4.get());
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
      
          // Apply ordering rules (not implemented here)
      
          // Remove duplicates from the list (if any)
          Set<String> uniqueRestaurantIds = new HashSet<>();
          List<Restaurant> uniqueRestaurants = new ArrayList<>();
          for (Restaurant restaurant : allRestaurants) {
              if (!uniqueRestaurantIds.contains(restaurant.getRestaurantId())) {
                  uniqueRestaurantIds.add(restaurant.getRestaurantId());
                  uniqueRestaurants.add(restaurant);
              }
          }
          long end=System.currentTimeMillis();
      System.out.println("the time taken for searchby queryMT=-=->"+(end-start));
          // Create and return the response object
          return new GetRestaurantsResponse(uniqueRestaurants);
  }
  @Async
  CompletableFuture<List<Restaurant>>findByNameMt(Double latitude, Double longitude,String searchString,LocalTime currentTime,Double servingRadiusInKms)
  {
    return CompletableFuture.completedFuture(restaurantRepositoryService.findRestaurantsByName(latitude, longitude, searchString, currentTime, servingRadiusInKms));
  }
  @Async
  CompletableFuture<List<Restaurant>>findByAttributesMt(Double latitude, Double longitude,String searchString,LocalTime currentTime,Double servingRadiusInKms)
  {
    return CompletableFuture.completedFuture(restaurantRepositoryService.findRestaurantsByAttributes(latitude, longitude, searchString, currentTime, servingRadiusInKms));
  }
  @Async
  CompletableFuture<List<Restaurant>>findByItemNameMt(Double latitude, Double longitude,String searchString,LocalTime currentTime,Double servingRadiusInKms)
  {
    return CompletableFuture.completedFuture(restaurantRepositoryService.findRestaurantsByItemName(latitude, longitude, searchString, currentTime, servingRadiusInKms));
  }@Async
  CompletableFuture<List<Restaurant>>findByItemAttributesMt(Double latitude, Double longitude,String searchString,LocalTime currentTime,Double servingRadiusInKms)
  {
    return CompletableFuture.completedFuture(restaurantRepositoryService.findRestaurantsByItemAttributes(latitude, longitude, searchString, currentTime, servingRadiusInKms));
  }
}

