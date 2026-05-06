package app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.models.Order;
import app.models.OrderItem;
import app.models.Product;
import app.models.ProductCount;

public class ToData {
        // ── toData overloads — direct translation of crow::json::wvalue toData() 
    // using plain Maps since Mustache accepts them natively ──────────────────

    static Map<String, Object> toData(Product p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.id);
        map.put("name", p.name);
        map.put("subgroup", p.subgroup);
        map.put("price", p.price);
        return map;
    }

    static Map<String, Object> toData(ProductCount pc) {
        Map<String, Object> map = new HashMap<>();
        map.put("product_id", pc.productId);
        map.put("name", pc.name);
        map.put("total_quantity", pc.totalQuantity);
        return map;
    }

    static Map<String, Object> toData(OrderItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("product_id", item.productId);
        map.put("name", item.name);
        map.put("quantity", item.quantity);
        map.put("is_ready", item.isReady);
        map.put("all_done", item.quantity == item.isReady);          // ← was missing
        map.put("pending", item.quantity - item.isReady);            // ← was missing
        return map;
    }

    static Map<String, Object> toData(Order o) {
        Map<String, Object> map = new HashMap<>();
        map.put("order_id", o.orderId);
        map.put("customer_name", o.customerName);
        map.put("time", Helpers.getFormattedTime(o.time));                   // ← was putting raw long before
        map.put("items", o.items.stream().map(ToData::toData).toList());
        return map;
    }

    static Map<String, Object> productsToData(List<Product> products) {
        Map<String, Object> map = new HashMap<>();
        map.put("products", products.stream().map(ToData::toData).toList());
        return map;
    }

    static Map<String, Object> productCountsToData(List<ProductCount> productCounts) {
        Map<String, Object> map = new HashMap<>();
        map.put("product_count", productCounts.stream()
                .map(ToData::toData)
                .toList());
        return map;
    }
}
