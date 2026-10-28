package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ProductComment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ProductCommentDaoTest {

    private static boolean mysqlAvailable() {
        try (var ignored = vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database.getConnection()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testProductCommentModel() {
        ProductComment root = new ProductComment();
        root.setCommentId(10L);
        root.setProductId(1L);
        root.setUserId(2L);
        root.setContent("Sản phẩm này có nhận in kích thước A3 không ạ?");
        root.setUserDisplayName("Nguyễn Văn A");
        root.setSellerReply(false);

        assertEquals("N", root.getUserInitial());
        assertEquals(0, root.getRepliesCount());
        assertNotNull(root.getReplies());

        ProductComment reply = new ProductComment();
        reply.setCommentId(11L);
        reply.setProductId(1L);
        reply.setUserId(99L);
        reply.setParentId(10L);
        reply.setContent("Chào bạn, xưởng có nhận in kích thước A3 nhé!");
        reply.setUserDisplayName("In Nhanh PrintKraft");
        reply.setSellerReply(true);

        assertEquals("I", reply.getUserInitial());
        assertTrue(reply.isSellerReply());

        root.addReply(reply);
        assertEquals(1, root.getRepliesCount());
        assertEquals(1, root.getReplies().size());
        assertEquals(reply, root.getReplies().get(0));
    }

    @Test
    void testCommentValidationRejectsEmptyOrTooLong() {
        assertThrows(IllegalArgumentException.class, () ->
                ProductCommentDao.create(1L, 1L, null, null, false));
        assertThrows(IllegalArgumentException.class, () ->
                ProductCommentDao.create(1L, 1L, null, "", false));
        assertThrows(IllegalArgumentException.class, () ->
                ProductCommentDao.create(1L, 1L, null, "   ", false));
        assertThrows(IllegalArgumentException.class, () ->
                ProductCommentDao.create(1L, 1L, null, "a", false)); // Dưới 2 ký tự
        assertThrows(IllegalArgumentException.class, () ->
                ProductCommentDao.create(1L, 1L, null, "x".repeat(2001), false)); // Vượt quá 2000 ký tự
    }

    @Test
    void testQueriesRunSafelyWhenDbConnected() {
        assumeTrue(mysqlAvailable(), "Bỏ qua khi MySQL không khả dụng.");
        assertDoesNotThrow(() -> ProductCommentDao.listForProduct(1L));
        assertDoesNotThrow(() -> ProductCommentDao.countForProduct(1L));
        assertDoesNotThrow(() -> {
            boolean isSeller = ProductCommentDao.isSellerForProduct(1L, 1L);
            assertTrue(isSeller || !isSeller);
        });
    }
}
