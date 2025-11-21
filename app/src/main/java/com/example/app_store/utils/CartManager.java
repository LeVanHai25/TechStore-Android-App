package com.example.app_store.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.app_store.models.CartItem;
import com.example.app_store.models.Product;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static final String PREF_NAME = "cart_prefs";
    private static final String KEY_CART = "cart_items";
    private SharedPreferences prefs;
    private Gson gson;

    public CartManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void addToCart(Product product) {
        List<CartItem> cart = getCart();
        boolean found = false;

        for (CartItem item : cart) {
            if (item.getProduct() != null && item.getProduct().getId() == product.getId()) {
                item.setQuantity(item.getQuantity() + 1);
                found = true;
                break;
            }
        }

        if (!found) {
            // Trong CartManager.java, dòng 39
// Giả sử 'product.getId()' trả về ID của sản phẩm
            cart.add(new CartItem(product.getId(), 1, product));
        }

        saveCart(cart);
    }

    public List<CartItem> getCart() {
        String json = prefs.getString(KEY_CART, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<CartItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void saveCart(List<CartItem> cart) {
        String json = gson.toJson(cart);
        prefs.edit().putString(KEY_CART, json).apply();
    }

    public void clearCart() {
        prefs.edit().remove(KEY_CART).apply();
    }
}
