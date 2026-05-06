// models/Order.java
package app.models;

import java.util.List;

public class Order {
    public int orderId;
    public String customerName;
    public long time;              // time_t was long long in C++
    public List<OrderItem> items;

    public Order(int orderId, String customerName, long time, List<OrderItem> items) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.time = time;
        this.items = items;
    }
}