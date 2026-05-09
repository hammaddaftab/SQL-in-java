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

    public Integer getIngredientID() { return ingredientID; }
    public String getName() { return name; }
    public String getType() { return type; }
    public Integer getStock() { return stock; }
    public Integer getRestockThreshold() { return restockThreshold; }
    public Integer getSupplierID() { return supplierID; }
}