
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
// @Log4j2
public class RestaurantServiceImpl implements RestaurantService {
  private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);
  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;



  public Boolean isInBetween(LocalTime curr,LocalTime starting,LocalTime ending)
  {
    return ((curr.isAfter(starting)||curr.equals(starting))&&(curr.isBefore(ending)||curr.equals(ending)));
  }
  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
      Double serviceRadius;
     
LocalTime startTime1 = LocalTime.of(8, 0);
LocalTime endTime1 = LocalTime.of(10, 0);

LocalTime startTime2 = LocalTime.of(13, 0);
LocalTime endTime2 = LocalTime.of(14, 0);

LocalTime startTime3 = LocalTime.of(19, 0);
LocalTime endTime3 = LocalTime.of(21, 0);


if(isInBetween(currentTime, startTime1, endTime1)||isInBetween(currentTime, startTime2, endTime2)||isInBetween(currentTime, startTime3, endTime3))
    {
    serviceRadius=peakHoursServingRadiusInKms;
    } 
else {
    serviceRadius=normalHoursServingRadiusInKms;
}
long startTimeInMillis = System.currentTimeMillis();
List<Restaurant>restaurants= restaurantRepositoryService.findAllRestaurantsCloseBy(getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), currentTime, serviceRadius);

long endTimeInMillis = System.currentTimeMillis();

System.out.println("Your function inside service took :" + (endTimeInMillis - startTimeInMillis));
 GetRestaurantsResponse getRestaurantsResponse=new GetRestaurantsResponse(restaurants);
      return getRestaurantsResponse;
  }


}

