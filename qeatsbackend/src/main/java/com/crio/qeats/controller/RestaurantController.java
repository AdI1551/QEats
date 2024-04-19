/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.controller;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.services.RestaurantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;



@Controller
@RequestMapping(RestaurantController.RESTAURANT_API_ENDPOINT)
public class RestaurantController {
  private static final Logger log = LoggerFactory.getLogger(RestaurantController.class);


  public static final String RESTAURANT_API_ENDPOINT = "/qeats/v1";
  public static final String RESTAURANTS_API = "/restaurants";
  public static final String MENU_API = "/menu";
  public static final String CART_API = "/cart";
  public static final String CART_ITEM_API = "/cart/item";
  public static final String CART_CLEAR_API = "/cart/clear";
  public static final String POST_ORDER_API = "/order";
  public static final String GET_ORDERS_API = "/orders";

  @Autowired
  private RestaurantService restaurantService;



  @GetMapping(RESTAURANTS_API)
  @ResponseBody
  public ResponseEntity<GetRestaurantsResponse> getRestaurants(
    @ModelAttribute @Valid GetRestaurantsRequest getRestaurantsRequest) {

    log.info("getRestaurants called with {}", getRestaurantsRequest);
      GetRestaurantsResponse getRestaurantsResponse;
      //CHECKSTYLE:OFF
      
long startTimeInMillis = System.currentTimeMillis();
      getRestaurantsResponse  = restaurantService
          .findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now()); 
long endTimeInMillis = System.currentTimeMillis();

System.out.println("Your function inside controller took :" + (endTimeInMillis - startTimeInMillis));
          //CHECKSTYLE:ON
          List<Restaurant>allRestaurants=getRestaurantsResponse.getRestaurants();
          for(Restaurant res:allRestaurants)
          {
            String newName=res.getName().replaceAll("[Â©éí]", "e");
            res.setName(newName);
          }
          getRestaurantsResponse.setRestaurants(allRestaurants);
          return ResponseEntity.ok().body(getRestaurantsResponse);
 

}
private String replaceNonAsciiChars(String input) {
  StringBuilder result = new StringBuilder();
  for (char c : input.toCharArray()) {
      if (c < 128) {
          result.append(c);
      } else {
          result.append("");
      }
  }
  return result.toString();
}


}

