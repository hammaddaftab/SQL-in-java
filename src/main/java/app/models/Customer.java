// models/Customer.java
package app.models;

public class Customer {
    public Integer customerID;
    public String firstName;
    public String lastName;

    public Customer() {}

    public Customer(Integer customerID) {
        this.customerID = customerID;
    }
}