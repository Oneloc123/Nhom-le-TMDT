package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.util.Locale;

public final class MapProviderFactory {
    private MapProviderFactory() { }
    public static MapProvider configured() {
        String provider=System.getenv("PK_MAP_PROVIDER");
        if(provider==null||provider.isBlank())provider="nominatim";
        return switch(provider.trim().toLowerCase(Locale.ROOT)) {
            case "nominatim", "osm" -> new NominatimMapProvider();
            default -> throw new IllegalStateException("Nhà cung cấp bản đồ chưa có adapter máy chủ: "+provider+". Chọn PK_MAP_PROVIDER=nominatim hoặc bổ sung adapter tương ứng.");
        };
    }
}
