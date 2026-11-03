package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.net.URI;

public interface MapProvider {
    URI searchUri(String query);
    URI reverseUri(double latitude, double longitude);
    String userAgent();
    String name();
}
