package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ModelAnalysis;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreeDModelAnalyzerTest {
    private Path tempDir;

    private final ThreeDModelAnalyzer analyzer = new ThreeDModelAnalyzer();

    @BeforeEach
    void createWorkspaceTempDir() throws Exception {
        Path target = Path.of("target").toAbsolutePath();
        Files.createDirectories(target);
        tempDir = Files.createTempDirectory(target, "model-test-");
    }

    @AfterEach
    void removeWorkspaceTempDir() throws Exception {
        if (tempDir == null || !Files.exists(tempDir)) return;
        try (var paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
            });
        }
    }

    @Test
    void analyzesClosedObjAndDefaultsToMillimeters() throws Exception {
        Path file = write("cube.obj", cubeObj());

        ModelAnalysis result = analyzer.analyze(file, "obj", null);

        assertEquals(ModelAnalysis.Status.READY, result.status());
        assertEquals("mm", result.unit());
        assertEquals(10.0, result.widthMm(), 1.0e-9);
        assertEquals(10.0, result.depthMm(), 1.0e-9);
        assertEquals(10.0, result.heightMm(), 1.0e-9);
        assertEquals(1.0, result.volumeCm3(), 1.0e-9);
        assertEquals(6.0, result.surfaceAreaCm2(), 1.0e-9);
        assertEquals(12, result.triangleCount());
    }

    @Test
    void analyzesClosedObjWithConvexPlanarPolygonFaces() throws Exception {
        Path file = write("polygon-cube.obj", polygonCubeObj());

        ModelAnalysis result = analyzer.analyze(file, "obj", "mm");

        assertEquals(ModelAnalysis.Status.READY, result.status());
        assertEquals(12, result.triangleCount());
        assertEquals(1.0, result.volumeCm3(), 1.0e-9);
    }

    @Test
    void appliesCustomerSelectedInchUnitBeforeReportingGeometry() throws Exception {
        Path file = write("cube.obj", cubeObj());

        ModelAnalysis result = analyzer.analyze(file, "obj", "in");

        assertEquals("in", result.unit());
        assertEquals(254.0, result.widthMm(), 1.0e-9);
        assertEquals(16_387.064, result.volumeCm3(), 0.01);
    }

    @Test
    void routesOpenMeshesToWorkshopReview() throws Exception {
        Path file = write("open.obj", "v 0 0 0\nv 10 0 0\nv 0 10 0\nf 1 2 3\n");

        ModelAnalysis result = analyzer.analyze(file, "obj", "mm");

        assertEquals(ModelAnalysis.Status.QUOTE_REQUIRED, result.status());
        assertTrue(result.message().contains("Mesh không kín"));
    }

    @Test
    void routesConcaveObjPolygonFacesToWorkshopReview() throws Exception {
        Path file = write("concave.obj", "v 0 0 0\nv 10 0 0\nv 4 4 0\nv 10 10 0\nv 0 10 0\nf 1 2 3 4 5\n");

        ModelAnalysis result = analyzer.analyze(file, "obj", "mm");

        assertEquals(ModelAnalysis.Status.QUOTE_REQUIRED, result.status());
        assertTrue(result.message().contains("không lồi hoặc không phẳng"));
    }

    @Test
    void routesNonPlanarObjPolygonFacesToWorkshopReview() throws Exception {
        Path file = write("non-planar.obj", "v 0 0 0\nv 10 0 0\nv 10 10 0.1\nv 0 10 0\nf 1 2 3 4\n");

        ModelAnalysis result = analyzer.analyze(file, "obj", "mm");

        assertEquals(ModelAnalysis.Status.QUOTE_REQUIRED, result.status());
        assertTrue(result.message().contains("không lồi hoặc không phẳng"));
    }

    @Test
    void rejectsMalformedObjWithoutGeometry() throws Exception {
        Path file = write("broken.obj", "v NaN 0 0\nf 1 2 3\n");
        assertEquals(ModelAnalysis.Status.INVALID, analyzer.analyze(file, "obj", "mm").status());
    }

    @Test
    void analyzesValidAsciiStl() throws Exception {
        Path file = write("tetra.stl", tetraStl());
        ModelAnalysis result = analyzer.analyze(file, "stl", "mm");
        assertEquals(ModelAnalysis.Status.READY, result.status());
        assertEquals(4, result.triangleCount());
        assertEquals(10.0, result.widthMm(), 1.0e-9);
        assertTrue(result.volumeCm3() > 0);
    }

    @Test
    void parsesMultipleObjObjectsAsOnePrintJob() throws Exception {
        String tetra = tetraObj(0);
        String second = tetraObj(20);
        StringBuilder combined = new StringBuilder(tetra);
        for (String line : second.split("\\R")) {
            if (line.startsWith("v ")) combined.append(line).append('\n');
        }
        combined.append("f 5 7 6\nf 5 6 8\nf 5 8 7\nf 6 7 8\n");
        Path file = write("multiple.obj", combined.toString());
        ModelAnalysis result = analyzer.analyze(file, "obj", "mm");
        assertEquals(ModelAnalysis.Status.READY, result.status());
        assertEquals(8, result.triangleCount());
        assertEquals(0.333333333, result.volumeCm3(), 1.0e-8);
    }

    @Test
    void keepsVolumeAccurateForGeometryFarFromTheOrigin() throws Exception {
        Path file = write("translated.obj", tetraObj(1_000_000_000));

        ModelAnalysis result = analyzer.analyze(file, "obj", "mm");

        assertEquals(ModelAnalysis.Status.READY, result.status());
        assertEquals(1.0 / 6.0, result.volumeCm3(), 1.0e-8);
    }

    @Test
    void flagsUnusuallySmallGeometryForCustomerConfirmation() throws Exception {
        Path file = write("small.obj", cubeObj().replace("10.0", "0.01").replace("10 ", "0.01 "));

        ModelAnalysis result = analyzer.analyze(file, "obj", "mm");

        assertEquals(ModelAnalysis.Status.NEEDS_CONFIRMATION, result.status());
    }

    @Test
    void validatesStepSignatureButDoesNotInventGeometry() throws Exception {
        Path file = write("model.step", "ISO-10303-21;\nHEADER;\nENDSEC;\nDATA;\nENDSEC;\nEND-ISO-10303-21;\n");

        ModelAnalysis result = analyzer.analyze(file, "step", "mm");

        assertEquals(ModelAnalysis.Status.QUOTE_REQUIRED, result.status());
        assertEquals(0.0, result.volumeCm3());
    }

    @Test
    void rejectsStepWithMissingDataSection() throws Exception {
        Path file = write("broken.step", "ISO-10303-21;\nHEADER;\nENDSEC;\nEND-ISO-10303-21;\n");
        assertEquals(ModelAnalysis.Status.INVALID, analyzer.analyze(file, "step", "mm").status());
    }

    @Test
    void validates3mfXmlButRoutesItsGeometryToQuote() throws Exception {
        Path file = tempDir.resolve("model.3mf");
        String xml = "<?xml version=\"1.0\"?><model xmlns=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02\"><resources/><build/></model>";
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("3D/3dmodel.model"));
            zip.write(xml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        ModelAnalysis result = analyzer.analyze(file, "3mf", "mm");
        assertEquals(ModelAnalysis.Status.QUOTE_REQUIRED, result.status());
        assertEquals(0.0, result.volumeCm3());
    }

    private Path write(String name, String contents) throws Exception {
        Path path = tempDir.resolve(name);
        Files.writeString(path, contents, StandardCharsets.UTF_8);
        return path;
    }

    private String cubeObj() {
        return "v 0 0 0\nv 10 0 0\nv 10 10 0\nv 0 10 0\n"
                + "v 0 0 10\nv 10 0 10\nv 10 10 10\nv 0 10 10\n"
                + "f 1 3 2\nf 1 4 3\nf 5 6 7\nf 5 7 8\n"
                + "f 1 2 6\nf 1 6 5\nf 2 3 7\nf 2 7 6\n"
                + "f 3 4 8\nf 3 8 7\nf 4 1 5\nf 4 5 8\n";
    }

    private String polygonCubeObj() {
        return "v 0 0 0\nv 10 0 0\nv 10 10 0\nv 0 10 0\n"
                + "v 0 0 10\nv 10 0 10\nv 10 10 10\nv 0 10 10\n"
                + "f 1 4 3 2\nf 5 6 7 8\nf 1 2 6 5\n"
                + "f 2 3 7 6\nf 3 4 8 7\nf 4 1 5 8\n";
    }

    private String tetraStl() {
        return "solid tetra\n" + facet("0 0 0", "0 10 0", "10 0 0")
                + facet("0 0 0", "10 0 0", "0 0 10")
                + facet("0 0 0", "0 0 10", "0 10 0")
                + facet("10 0 0", "0 10 0", "0 0 10") + "endsolid tetra\n";
    }

    private String facet(String a, String b, String c) {
        return "facet normal 0 0 0\nouter loop\nvertex " + a + "\nvertex " + b + "\nvertex " + c
                + "\nendloop\nendfacet\n";
    }

    private String tetraObj(int offsetX) {
        return "v " + offsetX + " 0 0\nv " + (offsetX + 10) + " 0 0\nv " + offsetX + " 10 0\nv " + offsetX + " 0 10\n"
                + "f 1 3 2\nf 1 2 4\nf 1 4 3\nf 2 3 4\n";
    }
}
