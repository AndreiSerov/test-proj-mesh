package com.example.testprojmesh.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/** RFC 9457 problem types and bodies, derived from the HTTP status alone. */
public final class ProblemResponses {

    private ProblemResponses() {
    }

    /** {@code urn:problem:not-found} for 404 and so on; {@code urn:problem:http-<code>} for unknown codes. */
    public static URI type(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        String slug = known == null
            ? "http-" + status.value()
            : known.name().toLowerCase(Locale.ROOT).replace('_', '-');
        return URI.create("urn:problem:" + slug);
    }

    /** Writes a problem body outside Spring MVC (security filters). */
    public static void write(HttpServletRequest request, HttpServletResponse response, JsonMapper json,
                             HttpStatus status, String detail) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type(status).toString());
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getWriter(), body);
    }
}
