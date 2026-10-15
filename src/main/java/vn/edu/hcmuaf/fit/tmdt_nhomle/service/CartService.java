package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CartItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CartService {
    public static final String SESSION_KEY = "shoppingCart";

    @SuppressWarnings("unchecked")
    public Map<Integer, CartItem> getCart(HttpSession session) {
        Object value = session.getAttribute(SESSION_KEY);
        if (value instanceof Map<?, ?>) {
            return (Map<Integer, CartItem>) value;
        }
        Map<Integer, CartItem> cart = new LinkedHashMap<>();
        session.setAttribute(SESSION_KEY, cart);
        return cart;
    }

    public void add(HttpSession session, int productId, int quantity) {
        Product product = ProductStore.findById(productId).orElseThrow();
        Map<Integer, CartItem> cart = getCart(session);
        CartItem item = cart.get(productId);
        if (item == null) {
            cart.put(productId, new CartItem(product, Math.max(1, quantity)));
        } else {
            item.setQuantity(item.getQuantity() + Math.max(1, quantity));
        }
    }

    public void update(HttpSession session, int productId, int quantity) {
        Map<Integer, CartItem> cart = getCart(session);
        if (quantity <= 0) {
            cart.remove(productId);
            return;
        }
        CartItem item = cart.get(productId);
        if (item != null) {
            item.setQuantity(quantity);
        }
    }

    public void remove(HttpSession session, int productId) {
        getCart(session).remove(productId);
    }

    public void clear(HttpSession session) {
        getCart(session).clear();
    }

    public List<CartItem> items(HttpSession session) {
        return new ArrayList<>(getCart(session).values());
    }

    public int totalQuantity(HttpSession session) {
        return items(session).stream().mapToInt(CartItem::getQuantity).sum();
    }

    public BigDecimal subtotal(HttpSession session) {
        return items(session).stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
