package service.userservice.userservice.service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Resolves a doctorId -> display name by calling auth-service via the API gateway.
 * DoctorProfile.name is @Transient and not persisted, so for any appointment that
 * was booked before doctorName was snapshotted onto the Appointment row, we
 * fall back to fetching the real name from auth-service (where User.name IS stored).
 *
 * Results are cached in-memory per JVM so repeated lookups are O(1).
 */
@Service
public class DoctorNameResolver {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gateway.base-url:http://localhost:8000}")
    private String gatewayBaseUrl;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Returns the doctor's name for the given doctorId, or null if it can't be resolved.
     * Never throws — network/parse failures just return null so the caller can fall back.
     */
    public String resolve(String doctorId) {
        if (doctorId == null || doctorId.isBlank()) return null;
        String cached = cache.get(doctorId);
        if (cached != null) return cached.isEmpty() ? null : cached;

        try {
            String url = gatewayBaseUrl + "/api/v1/auth/user/" + doctorId;
            ResponseEntity<Map> res = restTemplate.getForEntity(url, Map.class);
            Map<?, ?> body = res.getBody();
            if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                Object data = body.get("data");
                if (data instanceof Map<?, ?> userMap) {
                    Object name = userMap.get("name");
                    if (name != null) {
                        String s = String.valueOf(name).trim();
                        cache.put(doctorId, s);
                        return s;
                    }
                }
            }
        } catch (Exception ignored) {
            // Network failure, auth-service down, or unexpected shape — keep going.
        }

        // Negative cache so we don't keep retrying for the same bad id within a session
        cache.put(doctorId, "");
        return null;
    }
}