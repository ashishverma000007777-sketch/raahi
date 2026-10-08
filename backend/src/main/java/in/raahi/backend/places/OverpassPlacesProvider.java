package in.raahi.backend.places;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class OverpassPlacesProvider implements PlacesProvider {

    private static final List<String> OVERPASS_ENDPOINTS = List.of(
            "https://overpass.private.coffee/api/interpreter",
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter"
    );
    private static final long CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes

    private static final Map<String, String[]> TAG_BY_TYPE = Map.of(
            "dhaba", new String[]{"amenity", "restaurant"},
            "parking", new String[]{"amenity", "parking"},
            "toilet", new String[]{"amenity", "toilets"},
            "atm", new String[]{"amenity", "atm"},
            "fuel", new String[]{"amenity", "fuel"},
            "hotel", new String[]{"tourism", "hotel"}
    );

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

    private final Map<String, CacheEntry> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private final RestTemplate restTemplate;

    public OverpassPlacesProvider() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(12000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public List<Place> nearby(String placeType, double lat, double lng, double radiusKm) throws PlacesUnavailableException {
        String cacheKey = String.format(Locale.ROOT, "%s:%.2f:%.2f:%.1f",
                placeType == null ? "dhaba" : placeType.toLowerCase(), lat, lng, radiusKm);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.places;
        }

        String[] tag = TAG_BY_TYPE.getOrDefault(placeType, TAG_BY_TYPE.get("dhaba"));
        int radiusMeters = (int) (radiusKm * 1000);

        String overpassQuery = String.format(Locale.ROOT,
                "[out:json][timeout:15];(node[\"%s\"=\"%s\"](around:%d,%f,%f)[name];way[\"%s\"=\"%s\"](around:%d,%f,%f)[name];);out center 20;",
                tag[0], tag[1], radiusMeters, lat, lng,
                tag[0], tag[1], radiusMeters, lat, lng
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.USER_AGENT, "RaahiApp/2.0 (contact@raahi.in)");
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        String body = "data=" + URLEncoder.encode(overpassQuery, StandardCharsets.UTF_8);

        Exception lastException = null;
        for (String endpoint : OVERPASS_ENDPOINTS) {
            try {
                JsonNode response = restTemplate.postForObject(endpoint, new HttpEntity<>(body, headers), JsonNode.class);
                if (response == null || !response.has("elements")) continue;

                List<Place> places = new ArrayList<>();
                for (JsonNode element : response.path("elements")) {
                    double elLat = element.has("lat") ? element.path("lat").asDouble() : element.path("center").path("lat").asDouble();
                    double elLng = element.has("lon") ? element.path("lon").asDouble() : element.path("center").path("lon").asDouble();
                    String name = element.path("tags").path("name").asText(null);
                    if (name == null || (elLat == 0.0 && elLng == 0.0)) continue;
                    places.add(new Place(element.path("id").asText(), name, elLat, elLng, haversineKm(lat, lng, elLat, elLng)));
                }
                places.sort(Comparator.comparingDouble(p -> p.distanceKm() == null ? Double.MAX_VALUE : p.distanceKm()));

                if (cache.size() > 500) {
                    cache.entrySet().removeIf(e -> e.getValue().isExpired());
                }
                List<Place> unmodifiable = Collections.unmodifiableList(places);
                cache.put(cacheKey, new CacheEntry(unmodifiable));

                return unmodifiable;
            } catch (Exception e) {
                lastException = e;
            }
        }

        if (cached != null) {
            return cached.places;
        }
        throw new PlacesUnavailableException("Could not reach the places provider right now", lastException);
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
