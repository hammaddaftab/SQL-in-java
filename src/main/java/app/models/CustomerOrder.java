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

    public Integer getOrderID() { return orderID; }
    public Integer getCustomerID() { return customerID; }
    public Integer getTableID() { return tableID; }
    public Long getTime() { return time; }
    public String getPaymentMethod() { return paymentMethod; }
}