package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CartDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CartItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopOffer;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CartService {

    public static final String SESSION_KEY = "shoppingCart";
    private static final int MAX_QUANTITY = 100_000;

    // ---------------------------------------------------------------- public

    public int totalQuantity(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        User user = AuthFilter.currentUser(request);
        return totalQuantity(session, user);
    }

    public int totalQuantity(HttpSession session, User user) {
        return items(session, user).stream().mapToInt(CartItem::getQuantity).sum();
    }

    public BigDecimal subtotal(HttpSession session, User user) {
        return items(session, user).stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<CartItem> items(HttpSession session, User user) {
        if (user != null) {
            return loadDbItems(user.getId());
        }
        return new ArrayList<>(guestCart(session).values());
    }

    public void add(HttpSession session, User user, long shopProductId, long productId, int quantity) {
        int safeQuantity = quantity;
        if (user != null) {
            addToDb(user.getId(), resolveOffer(shopProductId, productId), safeQuantity, null);
        } else {
            ShopOffer offer = resolveOffer(shopProductId, productId);
            validateQuantity(offer, safeQuantity);
            Map<Long, CartItem> cart = guestCart(session);
            CartItem existing = cart.get(offer.getShopProductId());
            if (existing == null) {
                cart.put(offer.getShopProductId(), guestItem(offer, safeQuantity));
            } else {
                validateQuantity(offer, existing.getQuantity() + safeQuantity);
                existing.setQuantity(existing.getQuantity() + safeQuantity);
            }
        }
    }

    public void update(HttpSession session, User user, long shopProductId, int quantity) {
        if (quantity <= 0) {
            throw new CartException("Số lượng phải lớn hơn 0. Hãy dùng nút Xóa để bỏ sản phẩm.");
        }
        if (user != null) {
            ShopOffer offer = ProductDao.findOffer(shopProductId)
                    .orElseThrow(() -> new CartException("Chào bán không tồn tại hoặc đã ngừng bán."));
            validateQuantity(offer, quantity);
            mutateDb(user.getId(), conn -> {
                long cartId = CartDao.getOrCreateCartIdForUpdate(conn, user.getId());
                CartDao.updateQuantity(conn, cartId, shopProductId, quantity);
                CartDao.refreshCartTotals(conn, cartId);
            });
            return;
        }
        Map<Long, CartItem> cart = guestCart(session);
        CartItem item = cart.get(shopProductId);
        if (item == null) {
            return;
        }
        validateQuantity(item, quantity);
        item.setQuantity(quantity);
    }

    public void remove(HttpSession session, User user, long shopProductId) {
        if (user != null) {
            mutateDb(user.getId(), conn -> {
                long cartId = CartDao.getOrCreateCartIdForUpdate(conn, user.getId());
                CartDao.deleteItem(conn, cartId, shopProductId);
                CartDao.refreshCartTotals(conn, cartId);
            });
        } else {
            guestCart(session).remove(shopProductId);
        }
    }

    public void clear(HttpSession session, User user) {
        if (user != null) {
            mutateDb(user.getId(), conn -> {
                long cartId = CartDao.getOrCreateCartIdForUpdate(conn, user.getId());
                CartDao.clearItems(conn, cartId);
                CartDao.refreshCartTotals(conn, cartId);
            });
        } else {
            guestCart(session).clear();
        }
    }

    public void mergeGuestCart(HttpSession guestSession, User user) {
        if (guestSession == null || user == null) {
            return;
        }
        Map<Long, CartItem> guest = guestCart(guestSession);
        if (guest.isEmpty()) {
            return;
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long cartId = CartDao.getOrCreateCartIdForUpdate(conn, user.getId());
                for (CartItem item : guest.values()) {
                    ShopOffer offer = CartDao.findOfferForUpdate(conn, item.getShopProductId())
                            .orElse(null);
                    // Chào bán đã ngừng/hết hàng không thể đưa vào giỏ tài khoản.
                    if (offer == null || !offer.isPurchasable()) {
                        continue;
                    }
                    int current = CartDao.findItemQuantity(conn, cartId,
                            offer.getShopProductId()).orElse(0);
                    int mergedQuantity = current + item.getQuantity();
                    validateQuantity(offer, mergedQuantity);
                    CartDao.upsertItem(conn, cartId, offer.getShopProductId(),
                            item.getQuantity(), item.getDesignFileId());
                }
                CartDao.refreshCartTotals(conn, cartId);
                conn.commit();
            } catch (SQLException | CartException error) {
                conn.rollback();
                throw error;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException error) {
            throw DataAccessException.of(error);
        }
        // Chỉ xóa giỏ khách sau khi toàn bộ giao dịch merge đã commit.
        guest.clear();
    }

    // ---------------------------------------------------------------- internal

    private ShopOffer resolveOffer(long shopProductId, long productId) {
        ShopOffer offer = null;
        if (shopProductId > 0) {
            offer = ProductDao.findOffer(shopProductId).orElse(null);
        } else if (productId > 0) {
            offer = ProductDao.findById(productId)
                    .map(Product::primaryOffer)
                    .orElse(null);
        }
        if (offer == null) {
            throw new CartException("Sản phẩm hoặc chào bán không tồn tại hoặc đã ngừng bán.");
        }
        if (!offer.isPurchasable()) {
            throw new CartException("Chào bán này hiện tạm hết hàng.");
        }
        return offer;
    }

    private void addToDb(long userId, ShopOffer offer, int quantity, Long designFileId) {
        if (offer == null) {
            throw new CartException("Chào bán không tồn tại hoặc đã ngừng bán.");
        }
        if (!offer.isPurchasable()) {
            throw new CartException("Chào bán này hiện tạm hết hàng.");
        }
        validateQuantity(offer, quantity);
        mutateDb(userId, conn -> {
            long cartId = CartDao.getOrCreateCartIdForUpdate(conn, userId);
            int current = CartDao.findItemQuantity(conn, cartId,
                    offer.getShopProductId()).orElse(0);
            validateQuantity(offer, current + quantity);
            CartDao.upsertItem(conn, cartId, offer.getShopProductId(), quantity, designFileId);
            CartDao.refreshCartTotals(conn, cartId);
        });
    }

    private void mutateDb(long userId, SqlWork work) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                work.run(conn);
                conn.commit();
            } catch (SQLException | CartException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private List<CartItem> loadDbItems(long userId) {
        try (Connection conn = Database.getConnection()) {
            long cartId = CartDao.getOrCreateCartId(conn, userId);
            return CartDao.listItems(conn, cartId);
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Long, CartItem> guestCart(HttpSession session) {
        Object value = session.getAttribute(SESSION_KEY);
        if (value instanceof Map<?, ?>) {
            return (Map<Long, CartItem>) value;
        }
        Map<Long, CartItem> cart = new LinkedHashMap<>();
        session.setAttribute(SESSION_KEY, cart);
        return cart;
    }

    private CartItem guestItem(ShopOffer offer, int quantity) {
        CartItem item = new CartItem();
        item.setShopProductId(offer.getShopProductId());
        item.setShopName(offer.getShopName());
        item.setMaterial(offer.getMaterial());
        item.setMinimumQuantity(offer.getMinimumQuantity());
        item.setUnitPrice(offer.getUnitPrice());
        item.setQuantity(quantity);
        Product product = ProductDao.findById(offer.getProductId()).orElse(null);
        if (product == null) {
            product = new Product();
            product.setId(offer.getProductId());
            product.setName(offer.getSku() == null ? "Sản phẩm" : offer.getSku());
        }
        item.setProduct(product);
        return item;
    }

    private static void validateQuantity(ShopOffer offer, int quantity) {
        if (quantity < 1) {
            throw new CartException("Số lượng phải lớn hơn 0.");
        }
        if (quantity > MAX_QUANTITY) {
            throw new CartException("Số lượng tối đa mỗi dòng là " + MAX_QUANTITY + ".");
        }
        int minimum = offer.getMinimumQuantity();
        if (minimum > 0 && quantity < minimum) {
            throw new CartException("Chào bán này có số lượng tối thiểu là " + minimum + ".");
        }
    }

    private static void validateQuantity(CartItem item, int quantity) {
        if (quantity < 1) {
            throw new CartException("Số lượng phải lớn hơn 0.");
        }
        if (quantity > MAX_QUANTITY) {
            throw new CartException("Số lượng tối đa mỗi dòng là " + MAX_QUANTITY + ".");
        }
        if (item.getMinimumQuantity() > 0 && quantity < item.getMinimumQuantity()) {
            throw new CartException("Chào bán này có số lượng tối thiểu là "
                    + item.getMinimumQuantity() + ".");
        }
    }

    @FunctionalInterface
    private interface SqlWork {
        void run(Connection conn) throws SQLException;
    }
}
