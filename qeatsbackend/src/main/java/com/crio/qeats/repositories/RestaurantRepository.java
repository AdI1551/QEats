/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositories;

import com.crio.qeats.models.RestaurantEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
@Repository
public interface RestaurantRepository extends MongoRepository<RestaurantEntity, String> {
   Optional<List<RestaurantEntity>> findByIdIn(Set<String> ids);
   
    Optional<List<RestaurantEntity>> findByNameContainingIgnoreCase(String searchString);
    Optional<List<RestaurantEntity>> findByAttributesContainingIgnoreCase(String searchString);

    Optional<List<RestaurantEntity>>
    findByAttributesInIgnoreCase(List<String> attributes);
    

    @Query("{'name': {$regex: '^?0$', $options: 'i'}}")
    Optional<List<RestaurantEntity>> findRestaurantsByNameExact(String name);
    @Query("{'name': {$regex: '.*?0.*', $options: 'i'}}")
    Optional<List<RestaurantEntity>> findRestaurantsByName(String name);
   

}

