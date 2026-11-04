package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomPrintRequestBeanTest {
    @Test
    void jspFieldsAreExposedAsJavaBeanProperties() throws Exception {
        Set<String> properties = Arrays.stream(Introspector.getBeanInfo(CustomPrintRequest.class).getPropertyDescriptors())
                .map(java.beans.PropertyDescriptor::getName)
                .collect(Collectors.toSet());

        assertTrue(properties.containsAll(Set.of("id", "requestCode", "status", "originalFilename",
                "sourceUnit", "widthMm", "depthMm", "heightMm", "material", "printingMethod",
                "quantity", "shopName", "productionDays", "quoteValidUntil", "sellerResponseNote",
                "analysisMessage", "quotedAmount", "estimatedTotal", "designFileId")));
    }
}
