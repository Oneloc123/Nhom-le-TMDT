package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class NominatimMapProvider implements MapProvider {
    @Override public URI searchUri(String query) {
        return URI.create("https://nominatim.openstreetmap.org/search?q=" + enc(query)
                + "&format=jsonv2&addressdetails=1&limit=5&countrycodes=vn");
    }
    @Override public URI reverseUri(double latitude,double longitude) {
        return URI.create("https://nominatim.openstreetmap.org/reverse?lat=" + enc(Double.toString(latitude))
                + "&lon=" + enc(Double.toString(longitude)) + "&format=jsonv2&addressdetails=1&zoom=18");
    }
    @Override public String userAgent() {
        String configured=System.getenv("PK_GEO_USER_AGENT");
        return configured==null||configured.isBlank()
                ? "PrintKraftAccount/1.0 (address lookup; configure PK_GEO_USER_AGENT with a monitored contact)"
                : configured.trim();
    }
    @Override public String name(){return "nominatim";}
    private static String enc(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
}
