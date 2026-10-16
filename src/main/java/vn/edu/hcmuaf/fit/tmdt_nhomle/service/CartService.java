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
        int safeQuantity = normalizeQuantity(quantity);
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
        if (user != null) {
            if (quantity <= 0) {
                mutateDb(user.getId(), conn -> CartDao.deleteItem(conn,
                        CartDao.getOrCreateCartId(conn, user.getId()), shopProductId));
            } else {
                ShopOffer offer = ProductDao.findOffer(shopProductId)
                        .orElseThrow(() -> new CartException("Chào bán không tồn tại hoặc đã ngừng bán."));
                validateQuantity(offer, quantity);
                mutateDb(user.getId(), conn -> CartDao.updateQuantity(conn,
                        CartDao.getOrCreateCartId(conn, user.getId()), shopProductId, quantity));
            }
            return;
        }
        Map<Long, CartItem> cart = guestCart(session);
        CartItem item = cart.get(shopProductId);
        if (item == null) {
            return;
        }
        if (quantity <= 0) {
            cart.remove(shopProductId);
        } else {
            validateQuantity(item, quantity);
            item.setQuantity(quantity);
        }
    }

    public void remove(HttpSession session, User user, long shopProductId) {
        update(session, user, shopProductId, 0);
    }

    public void clear(HttpSession session, User user) {
        if (user != null) {
            mutateDb(user.getId(), conn -> CartDao.clearItems(conn,
                    CartDao.getOrCreateCartId(conn, user.getId())));
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
        for (CartItem item : guest.values()) {
            try {
                addToDb(user.getId(), ProductDao.findOffer(item.getShopProductId()).orElse(null),
                        item.getQuantity(), item.getDesignFileId());
            } catch (CartException | DataAccessException ignored) {
                // Offer không còn bán — bỏ qua dòng cũ, không chặn đăng nhập.
            }
        }
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
            long cartId = CartDao.getOrCreateCartId(conn, userId);
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

    private static int normalizeQuantity(int quantity) {
        return Math.max(1, quantity);
    }

    @FunctionalInterface
    private interface SqlWork {
        void run(Connection conn) throws SQLException;
    }
}
