package com.bookify.backend.discovery.dto;

import java.math.BigDecimal;

public interface NearbyLocationProjection {
    Long getBusinessId();
    String getBusinessName();
    String getCategoryCode();
    BigDecimal getRatingAverage();
    Integer getRatingCount();
    Long getLocationId();
    String getLocationName();
    String getAddress();
    String getCity();
    String getCountryCode();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
    Double getDistanceMeters();
}
