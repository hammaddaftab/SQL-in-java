// models/Product.java
package app.models;

public class Product {
    public Integer productID;
    public String name;
    public Double price;
    public String category;

    public Product() {}

    public Product(Integer productID) {
        this.productID = productID;
    }
}