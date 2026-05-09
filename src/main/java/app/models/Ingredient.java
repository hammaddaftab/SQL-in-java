// models/Ingredient.java
package app.models;

public class Ingredient {
    public Integer ingredientID;
    public String name;
    public String type;
    public Integer stock;
    public Integer restockThreshold;
    public Integer supplierID;

    public Ingredient() {}

    public Ingredient(Integer ingredientID) {
        this.ingredientID = ingredientID;
    }
}