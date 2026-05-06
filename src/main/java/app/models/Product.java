package app.models;

public class Product {
    public int id;
    public String name;
    public String subgroup;
    public double price;

    public Product(int id, String name, String subgroup, double price) {
        this.id = id;
        this.name = name;
        this.subgroup = subgroup;
        this.price = price;
    }
}