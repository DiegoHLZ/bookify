package com.bookify.backend.discovery.dto;

import java.math.BigDecimal;

public interface NearbyLocationProjection {
    Long getBusinessId();
    String getBusinessSlug();
    String getBusinessName();
    String getCategoryCode();
    BigDecimal getRatingAverage();
    Integer getRatingCount();
    Long getLocationId();
    String getLocationName();
    String getAddress();
    String getCity();
    String getCountryCode();
    String getTimezone();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
    Double getDistanceMeters();
}
