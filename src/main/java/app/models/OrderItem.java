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

    public Integer getOrderItemID() { return orderItemID; }
    public Integer getOrderID() { return orderID; }
    public Integer getProductID() { return productID; }
    public Integer getQuantity() { return quantity; }
    public Integer getPreparedCount() { return preparedCount; }
    public Double getPriceAtOrder() { return priceAtOrder; }
}