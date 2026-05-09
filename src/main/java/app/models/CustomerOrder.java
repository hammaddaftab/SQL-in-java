// models/CustomerOrder.java
package app.models;

public class CustomerOrder {
    public Integer orderID;
    public Integer customerID;
    public Integer tableID;
    public Long time;
    public String paymentMethod;

    public CustomerOrder() {}

    public CustomerOrder(Integer orderID) {
        this.orderID = orderID;
    }
}