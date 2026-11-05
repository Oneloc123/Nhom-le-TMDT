package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

/** Geometry measurements and the result of validating an uploaded 3D model. */
public record ModelAnalysis(
        Status status,
        String format,
        String unit,
        double widthMm,
        double depthMm,
        double heightMm,
        double volumeCm3,
        double surfaceAreaCm2,
        long triangleCount,
        String message) {

    public enum Status {
        READY,
        NEEDS_CONFIRMATION,
        QUOTE_REQUIRED,
        INVALID
    }

    public boolean canEstimate() {
        return status == Status.READY;
    }
}
