// models/Supplier.java
package app.models;

public class Supplier {
    public Integer supplierID;
    public String name;
    public String contact;
    public String email;
    public String address;
    public Double rating;

    public Supplier() {}

    public Supplier(Integer supplierID) {
        this.supplierID = supplierID;
    }

    public Integer getSupplierID() { return supplierID; }
    public String getName() { return name; }
    public String getContact() { return contact; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public Double getRating() { return rating; }
}