package app;

import java.util.List;

import app.models.Order;
import app.models.OrderItem;
import app.models.Product;
import sql_in_java.ORM;
import sql_in_java.Table;
import sql_in_java.Constraint;

public class Database {

    public static void createTables(ORM orm) {
        Table Supplier = new Table("Supplier")
            .has("SupplierID").asInt().is(Constraint.PRIMARYKEY.and(Constraint.AUTOINCREMENT))
            .has("Name").asString()
            .has("Contact").asString()
            .has("Email").asString()
            .has("Address").asString()
            .has("Rating").asDecimal();

        Table Ingredient = new Table("Ingredient")
            .has("IngredientID").asInt().is(Constraint.PRIMARYKEY.and(Constraint.AUTOINCREMENT))
            .has("Name").asString()
            .has("Type").asString()
            .has("Stock").asInt()
            .has("RestockThreshold").asInt()
            .has("SupplierID").asInt().is(Constraint.NOTNULL)
            .refers(Supplier.c("SupplierID"));

        Table Product = new Table("Product")
            .has("ProductID").asInt().is(Constraint.PRIMARYKEY.and(Constraint.AUTOINCREMENT))
            .has("Name").asString()
            .has("Price").asDecimal()
            .has("Category").asString();


        Table CafeTable = new Table("CafeTable")
            .has("TableID").asInt().is(Constraint.PRIMARYKEY.and(Constraint.AUTOINCREMENT))
            .has("Capacity").asInt()
            .has("Location").asString();

        Table Customer = new Table("Customer")
            .has("CustomerID").asInt().is(Constraint.PRIMARYKEY.and(Constraint.AUTOINCREMENT))
            .has("FirstName").asString()
            .has("LastName").asString();

        Table Order = new Table("Order")
            .has("OrderID").asInt().is(Constraint.PRIMARYKEY.and(Constraint.AUTOINCREMENT))
            .has("CustomerID").asInt().is(Constraint.NOTNULL)
            .has("TableID").asInt().is(Constraint.NOTNULL)
            .has("Time").asInt()
            .has("PaymentMethod").asString()
            .refers(Customer.c("CustomerID"))
            .refers(CafeTable.c("TableID"));

        Table OnlineOrder = new Table("OnlineOrder")
            .has("OrderID").asInt().is(Constraint.PRIMARYKEY)  // no AUTOINCREMENT — shares PK with Order
            .has("Is_Confirmed").asBoolean()
            .refers(Order.c("OrderID"));


        Table RestockRequest = new Table("RestockRequest")
            .has("RequestID").asInt().is(Constraint.PRIMARYKEY.and(Constraint.AUTOINCREMENT))
            .has("IngredientID").asInt().is(Constraint.NOTNULL)
            .has("SupplierID").asInt().is(Constraint.NOTNULL)
            .has("QuantityRequested").asInt()
            .has("Status").asString()
            .has("RequestedAt").asInt()
            .refers(Ingredient.c("IngredientID"))
            .refers(Supplier.c("SupplierID"));


        Table ProductIngredient = new Table("ProductIngredient")
            .has("ProductIngredientID").asInt()
                .is(Constraint.PRIMARYKEY.and(Constraint.AUTOINCREMENT))  // surrogate PK
            .has("ProductID").asInt().is(Constraint.NOTNULL)
            .has("IngredientID").asInt().is(Constraint.NOTNULL)
            .has("QuantityRequired").asInt()
            .refers(Product.c("ProductID"))
            .refers(Ingredient.c("IngredientID"));

        Table OrderItem = new Table("OrderItem")
            .has("OrderItemID").asInt()
                .is(Constraint.PRIMARYKEY.and(Constraint.AUTOINCREMENT))  // surrogate PK
            .has("OrderID").asInt().is(Constraint.NOTNULL)
            .has("ProductID").asInt().is(Constraint.NOTNULL)
            .has("Quantity").asInt()
            .has("PreparedCount").asInt()
            .has("PriceAtOrder").asDecimal()
            .refers(Order.c("OrderID"))
            .refers(Product.c("ProductID"));
    }

    public static List<Product> selectAllProducts(ORM orm) {
        return orm.from(Product.class).fetch();
    }
}
