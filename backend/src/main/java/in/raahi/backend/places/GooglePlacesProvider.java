package in.raahi.backend.places;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GooglePlacesProvider implements PlacesProvider {

    private static final String PLACES_SEARCH_URL = "https://places.googleapis.com/v1/places:searchNearby";
    private static final long CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes

    private static final Map<String, List<String>> TYPES_BY_CATEGORY = Map.of(
            "dhaba", List.of("restaurant", "meal_takeaway"),
            "parking", List.of("parking"),
            "toilet", List.of("rest_stop"),
            "atm", List.of("atm"),
            "fuel", List.of("gas_station"),
            "hotel", List.of("lodging")
    );

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final long createdAt;
        final List<Place> places;

        CacheEntry(List<Place> places) {
            this.createdAt = System.currentTimeMillis();
            this.places = places;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CACHE_TTL_MS;
        }
    }

    public GooglePlacesProvider(@Value("${raahi.google.api-key:}") String apiKey) {
        this.apiKey = (apiKey == null || apiKey.isBlank()) ? null : apiKey.trim();
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public List<Place> nearby(String placeType, double lat, double lng, double radiusKm) throws PlacesUnavailableException {
        String cat = placeType == null ? "dhaba" : placeType.toLowerCase(Locale.ROOT);
        String cacheKey = String.format(Locale.ROOT, "%s:%.2f:%.2f:%.1f", cat, lat, lng, radiusKm);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.places;
        }

        if (apiKey == null) {
            if (cached != null) return cached.places;
            throw new PlacesUnavailableException("Google Places API key is not configured on the server", null);
        }

        List<String> includedTypes = TYPES_BY_CATEGORY.getOrDefault(cat, List.of("restaurant"));
        double radiusMeters = Math.min(50000.0, radiusKm * 1000.0);

        Map<String, Object> center = Map.of("latitude", lat, "longitude", lng);
        Map<String, Object> circle = Map.of("center", center, "radius", radiusMeters);
        Map<String, Object> locationRestriction = Map.of("circle", circle);

        Map<String, Object> payload = Map.of(
                "includedTypes", includedTypes,
                "maxResultCount", 20,
                "locationRestriction", locationRestriction
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask", "places.id,places.displayName,places.location");

        try {
            JsonNode response = restTemplate.postForObject(PLACES_SEARCH_URL, new HttpEntity<>(payload, headers), JsonNode.class);
            if (response == null || !response.has("places")) {
                List<Place> emptyList = Collections.emptyList();
                cache.put(cacheKey, new CacheEntry(emptyList));
                return emptyList;
            }

            List<Place> places = new ArrayList<>();
            for (JsonNode p : response.path("places")) {
                String id = p.path("id").asText("");
                String name = p.path("displayName").path("text").asText("Service Location");
                JsonNode loc = p.path("location");
                double pLat = loc.path("latitude").asDouble(Double.NaN);
                double pLng = loc.path("longitude").asDouble(Double.NaN);
                if (Double.isNaN(pLat) || Double.isNaN(pLng)) continue;

                double dist = haversineKm(lat, lng, pLat, pLng);
                places.add(new Place(id, name, pLat, pLng, dist));
            }

            places.sort(Comparator.comparingDouble(p -> p.distanceKm() == null ? Double.MAX_VALUE : p.distanceKm()));

            if (cache.size() > 500) {
                cache.entrySet().removeIf(e -> e.getValue().isExpired());
            }

            List<Place> unmodifiable = Collections.unmodifiableList(places);
            cache.put(cacheKey, new CacheEntry(unmodifiable));
            return unmodifiable;
        } catch (Exception e) {
            if (cached != null) return cached.places;
            throw new PlacesUnavailableException("Google Places request failed: " + e.getMessage(), e);
        }
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
