package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ModelAnalysis;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;

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
        ModelAnalysis result = analyze("cube.obj", cubeObj(), "obj", null);
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
        ModelAnalysis result = analyze("polygon-cube.obj", polygonCubeObj(), "obj", "mm");
        assertEquals(ModelAnalysis.Status.READY, result.status());
        assertEquals(12, result.triangleCount());
        assertEquals(1.0, result.volumeCm3(), 1.0e-9);
    }

    @Test
    void appliesCustomerSelectedInchUnitBeforeReportingGeometry() throws Exception {
        ModelAnalysis result = analyze("cube.obj", cubeObj(), "obj", "in");
        assertEquals("in", result.unit());
        assertEquals(254.0, result.widthMm(), 1.0e-9);
        assertEquals(16_387.064, result.volumeCm3(), 0.01);
    }

    @Test
    void routesOpenMeshesToQuoteAndKeepsMeasurableDimensions() throws Exception {
        ModelAnalysis result = analyze("open.obj", tetraObj(0, 0).replace("f 2 3 4\n", ""), "obj", "mm");
        assertEquals(ModelAnalysis.Status.QUOTE_REQUIRED, result.status());
        assertTrue(result.message().contains("Mesh"));
        assertEquals(10.0, result.widthMm(), 1.0e-9);
        assertEquals(10.0, result.depthMm(), 1.0e-9);
        assertEquals(0.0, result.volumeCm3());
        assertEquals(1.5, result.surfaceAreaCm2(), 1.0e-9);
    }

    @Test
    void routesConcaveAndNonPlanarFacesToWorkshopReview() throws Exception {
        ModelAnalysis concave = analyze("concave.obj", "v 0 0 0\nv 10 0 0\nv 4 4 0\nv 10 10 0\nv 0 10 0\nf 1 2 3 4 5\n", "obj", "mm");
        ModelAnalysis nonPlanar = analyze("non-planar.obj", "v 0 0 0\nv 10 0 0\nv 10 10 0.1\nv 0 10 0\nf 1 2 3 4\n", "obj", "mm");
        assertEquals(ModelAnalysis.Status.QUOTE_REQUIRED, concave.status());
        assertEquals(ModelAnalysis.Status.QUOTE_REQUIRED, nonPlanar.status());
    }

    @Test
    void rejectsMalformedObjWithoutGeometry() throws Exception {
        assertEquals(ModelAnalysis.Status.INVALID,
                analyze("broken.obj", "v NaN 0 0\nf 1 2 3\n", "obj", "mm").status());
    }

    @Test
    void rejectsNonObjFormatsEvenWhenTheFileContainsObjText() throws Exception {
        Path file = write("model.stl", cubeObj());
        assertEquals(ModelAnalysis.Status.INVALID, analyzer.analyze(file, "stl", "mm").status());
        assertEquals(ModelAnalysis.Status.INVALID, analyzer.analyze(file, "obj", "mm").status());
    }

    @Test
    void parsesMultipleObjectsGroupsAndGlobalFaceIndexes() throws Exception {
        String content = "o first\ng shell_a\n" + tetraObj(0, 0)
                + "o second\ng shell_b\n" + tetraObj(20, 4);
        ModelAnalysis result = analyze("multiple.obj", content, "obj", "mm");
        assertEquals(ModelAnalysis.Status.READY, result.status());
        assertEquals(8, result.triangleCount());
        assertEquals(0.333333333, result.volumeCm3(), 1.0e-8);
        assertEquals(30.0, result.widthMm(), 1.0e-9);
    }

    @Test
    void supportsNegativeIndexesAndTextureNormalReferences() throws Exception {
        String tetra = "v 0 0 0\nv 10 0 0\nv 0 10 0\nv 0 0 10\n"
                + "vt 0 0\nvn 0 0 1\nf -4/1/1 -2/1/1 -3/1/1\n"
                + "f -4//1 -3//1 -1//1\nf -4/1 -1/1 -2/1\nf -3/1/1 -2/1/1 -1/1/1\n";
        ModelAnalysis result = analyze("negative.obj", tetra, "obj", "mm");
        assertEquals(ModelAnalysis.Status.READY, result.status(), result.message());
    }

    @Test
    void keepsVolumeAccurateForGeometryFarFromTheOrigin() throws Exception {
        ModelAnalysis result = analyze("translated.obj", tetraObj(1_000_000_000, 0), "obj", "mm");
        assertEquals(ModelAnalysis.Status.READY, result.status());
        assertEquals(1.0 / 6.0, result.volumeCm3(), 1.0e-8);
    }

    @Test
    void flagsUnusuallySmallGeometryForCustomerConfirmation() throws Exception {
        String small = cubeObj().replace("10.0", "0.01").replace("10 ", "0.01 ");
        assertEquals(ModelAnalysis.Status.NEEDS_CONFIRMATION, analyze("small.obj", small, "obj", "mm").status());
    }

    @Test
    void routesExtremelyLargeGeometryToManualQuote() throws Exception {
        String large = cubeObj().replace("10.0", "200000").replace("10 ", "200000 ");
        ModelAnalysis result = analyze("large.obj", large, "obj", "mm");
        assertEquals(ModelAnalysis.Status.QUOTE_REQUIRED, result.status());
        assertTrue(result.widthMm() > 100_000);
        assertEquals(0.0, result.volumeCm3());
    }

    @Test
    void rejectsMissingVerticesFacesBadIndexesAndMalformedUtf8() throws Exception {
        Path noVertices = write("no-vertices.obj", "f 1 2 3\n");
        Path noFaces = write("no-faces.obj", "v 0 0 0\nv 1 0 0\nv 0 1 0\n");
        Path badTextureIndex = write("bad-texture.obj", "v 0 0 0\nv 1 0 0\nv 0 1 0\nf 1/1 2/1 3/1\n");
        Path malformedUtf8 = tempDir.resolve("malformed-utf8.obj");
        Files.write(malformedUtf8, new byte[]{'v', ' ', (byte) 0xC3, 0x28});
        assertEquals(ModelAnalysis.Status.INVALID, analyzer.analyze(noVertices, "obj", "mm").status());
        assertEquals(ModelAnalysis.Status.INVALID, analyzer.analyze(noFaces, "obj", "mm").status());
        assertEquals(ModelAnalysis.Status.INVALID, analyzer.analyze(badTextureIndex, "obj", "mm").status());
        assertEquals(ModelAnalysis.Status.INVALID, analyzer.analyze(malformedUtf8, "obj", "mm").status());
    }

    @Test
    void rejectsOversizedObjBeforeParsingIt() throws Exception {
        Path file = tempDir.resolve("oversized.obj");
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            channel.position(100L * 1024 * 1024);
            channel.write(ByteBuffer.wrap(new byte[]{'v'}));
        }
        assertEquals(ModelAnalysis.Status.INVALID, analyzer.analyze(file, "obj", "mm").status());
    }

    private ModelAnalysis analyze(String name, String contents, String extension, String unit) throws Exception {
        return analyzer.analyze(write(name, contents), extension, unit);
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

    private String tetraObj(int offsetX, int indexOffset) {
        int a = 1 + indexOffset, b = 2 + indexOffset, c = 3 + indexOffset, d = 4 + indexOffset;
        return "v " + offsetX + " 0 0\nv " + (offsetX + 10) + " 0 0\nv " + offsetX + " 10 0\nv " + offsetX + " 0 10\n"
                + "f " + a + " " + c + " " + b + "\nf " + a + " " + b + " " + d + "\n"
                + "f " + a + " " + d + " " + c + "\nf " + b + " " + c + " " + d + "\n";
    }
}
