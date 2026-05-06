// models/OrderItem.java
package app.models;

public class OrderItem {
    public int productId;
    public String name;
    public int quantity;
    public int isReady;

    public OrderItem(int productId, String name, int quantity, int isReady) {
        this.productId = productId;
        this.name = name;
        this.quantity = quantity;
        this.isReady = isReady;
    }
}