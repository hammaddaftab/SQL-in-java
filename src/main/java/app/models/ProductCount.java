// models/ProductCount.java
package app.models;

public class ProductCount {
    public int productId;
    public String name;
    public int totalQuantity;

    public ProductCount(int productId, String name, int totalQuantity) {
        this.productId = productId;
        this.name = name;
        this.totalQuantity = totalQuantity;
    }
}