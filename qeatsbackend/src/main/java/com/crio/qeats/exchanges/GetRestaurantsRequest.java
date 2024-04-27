/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.exchanges;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetRestaurantsRequest {
    public GetRestaurantsRequest(double Latitude, double Longitude) {this.Latitude=Latitude;this.Longitude=Longitude;}
    @NotNull
    @Max(value = 90)
    @Min(value = -90)

    private Double Latitude; 


    @NotNull
    @Max(value = 90)
    @Min(value = -90)

    private Double Longitude;

    private String searchFor;

    
}

