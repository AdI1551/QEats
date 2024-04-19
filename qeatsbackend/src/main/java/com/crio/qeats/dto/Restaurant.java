
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import lombok.Getter;

import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Restaurant {
  @NotNull
  private String restaurantId;
  @NotNull
  private String name;
  @NotNull
  private String city;
  @NotNull
  private String imageUrl;
  @NotNull
  private double latitude;
  @NotNull
  private double longitude;
  @NotNull
  private String opensAt;
  @NotNull
  private String closesAt;
  @NotNull
  private List<String> attributes;


}

