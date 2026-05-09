// models/OnlineOrder.java
package app.models;

public class OnlineOrder {
    public Integer orderID;
    public Boolean isConfirmed;

    public OnlineOrder() {}

    public OnlineOrder(Integer orderID) {
        this.orderID = orderID;
    }
}