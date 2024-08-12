package com.example.tinyreminder.utils;

import android.location.Location;
import java.util.ArrayList;
import java.util.List;

public class LocationCache {
    private static final int MAX_CACHED_LOCATIONS = 50;
    private List<Location> cachedLocations = new ArrayList<>();

    public void addLocation(Location location) {
        cachedLocations.add(location);
        if (cachedLocations.size() > MAX_CACHED_LOCATIONS) {
            cachedLocations.remove(0);
        }
    }

    public List<Location> getCachedLocations() {
        return new ArrayList<>(cachedLocations);
    }

    public void clearCache() {
        cachedLocations.clear();
    }
}