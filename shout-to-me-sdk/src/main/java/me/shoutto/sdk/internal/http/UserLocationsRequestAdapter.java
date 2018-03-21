package me.shoutto.sdk.internal.http;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import me.shoutto.sdk.StmBaseEntity;

/**
 * Custom request adapter for User Locations.
 */

public class UserLocationsRequestAdapter implements StmJsonRequestAdapter<SortedSet<? extends StmBaseEntity>> {
    @Override
    public String adapt(SortedSet<? extends StmBaseEntity> userLocationSortedSet) {
        Map<String, SortedSet<? extends StmBaseEntity>> map = new HashMap<>();
        map.put("locations", userLocationSortedSet);

        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new GsonDateAdapter())
                .create();
        return gson.toJson(map, map.getClass());
    }
}
