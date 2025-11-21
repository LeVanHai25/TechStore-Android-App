package com.example.app_store.utils;

import com.example.app_store.models.Order;
import java.util.ArrayList;
import java.util.List;

public class OrderCache {
    private static List<Order> cachedOrders = new ArrayList<>();

    public static void setOrders(List<Order> orders) {
        cachedOrders = orders != null ? new ArrayList<>(orders) : new ArrayList<>();
    }

    public static Order getOrderById(int orderId) {
        for (Order order : cachedOrders) {
            int id = (order.getId() != 0) ? order.getId() : order.getOrder_id();
            if (id == orderId) {
                return order;
            }
        }
        return null;
    }

    public static void clear() {
        cachedOrders.clear();
    }
}





