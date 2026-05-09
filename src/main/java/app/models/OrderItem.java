// models/OrderItem.java
package app.models;

public class OrderItem {
    public Integer orderItemID;
    public Integer orderID;
    public Integer productID;
    public Integer quantity;
    public Integer preparedCount;
    public Double priceAtOrder;

    public OrderItem() {}

    public OrderItem(Integer orderItemID) {
        this.orderItemID = orderItemID;
    }
}