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
        Table Products = new Table("Products")
            .has("Product_id").asInt().is(Constraint.PRIMARYKEY)
            .has("Name").asString(30)
            .has("Subgroup").asString(15)
            .has("Price").asDecimal(7, 2);

        Table Orders = new Table("Orders")
            .has("Order_id").asInt().is(Constraint.PRIMARYKEY)
            .has("Time").asInt()
            .has("Customer_name").asString(30);

        Table OrderItems = new Table("OrderItems")
            .has("Sr").asInt().is(Constraint.PRIMARYKEY)
            .has("Order_id").asInt()
            .has("Product_id").asInt()
            .has("Quantity").asInt()
            .has("is_ready").asInt();
        orm.register(Product.class, Products);
        orm.register(OrderItem.class, OrderItems);
        orm.register(Order.class, Orders);
    }

    public static List<Product> selectAllProducts(ORM orm) {
        return orm.from(Product.class).fetch();
    }
}
