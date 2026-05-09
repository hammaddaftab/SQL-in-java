// models/ProductIngredient.java
package app.models;

public class ProductIngredient {
    public Integer productIngredientID;
    public Integer productID;
    public Integer ingredientID;
    public Integer quantityRequired;

    public ProductIngredient() {}

    public ProductIngredient(Integer productIngredientID) {
        this.productIngredientID = productIngredientID;
    }
}