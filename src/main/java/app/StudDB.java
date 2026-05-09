package app;

import java.util.*;
import app.models.*;
import sql_in_java.*;

/**
 * Complete implementation of DatabaseQueries using the custom SQL-in-Java ORM.
 * All queries use the fluent DSL: orm.from(Class).where(...).fetch() for SELECT,
 * orm.permanent(instance) for INSERT/UPDATE, orm.delete(instance) for DELETE,
 * and orm.update(Class).set(...).where(...).execute() for bulk operations.
 */
public class StudDB implements DatabaseQueries {

    private ORM orm;
    private Table Products, Customers, Orders, OrderItems, OnlineOrders;
    private Table Ingredients, RestockRequests, Suppliers, CafeTables;

    public StudDB(ORM orm) {
        this.orm = orm;
        // Cache table references for convenience
        this.Products = orm.getTableFor(Product.class);
        this.Customers = orm.getTableFor(Customer.class);
        this.Orders = orm.getTableFor(CustomerOrder.class);
        this.OrderItems = orm.getTableFor(OrderItem.class);
        this.OnlineOrders = orm.getTableFor(OnlineOrder.class);
        this.Ingredients = orm.getTableFor(Ingredient.class);
        this.RestockRequests = orm.getTableFor(RestockRequest.class);
        this.Suppliers = orm.getTableFor(Supplier.class);
        this.CafeTables = orm.getTableFor(CafeTable.class);
    }

    // ========================================================================
    // PRODUCTS
    // ========================================================================

    @Override
    public List<Product> getAllProducts() {
        return orm.from(Product.class).fetch();
    }

    @Override
    public Product getProductById(int id) {
        Product p = new Product(id);
        try {
            return orm.select(p).populate();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int addProduct(Product p) {
        try {
            orm.permanent(p);
            return p.productID;
        } catch (Exception e) {
            throw new RuntimeException("Failed to add product", e);
        }
    }

    @Override
    public void updateProduct(Product p) {
        try {
            orm.permanent(p);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update product", e);
        }
    }

    @Override
    public void deleteProduct(int id) {
        try {
            Product p = new Product(id);
            orm.delete(p);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete product", e);
        }
    }

    // ========================================================================
    // CUSTOMERS
    // ========================================================================

    @Override
    public Customer getCustomerById(int id) {
        Customer c = new Customer(id);
        try {
            return orm.select(c).populate();
        } catch (Exception e) {
            return null;
        }
    }

    // ========================================================================
    // ORDERS
    // ========================================================================

    @Override
    public int addOrder(CustomerOrder o) {
        try {
            orm.permanent(o);
            return o.orderID;
        } catch (Exception e) {
            throw new RuntimeException("Failed to add order", e);
        }
    }

    @Override
    public CustomerOrder getOrderById(int id) {
        CustomerOrder o = new CustomerOrder(id);
        try {
            return orm.select(o).populate();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<CustomerOrder> getOrdersByCustomer(int customerId) {
        try {
            return orm.from(CustomerOrder.class)
                .where(Orders.c("customerID").eq(customerId))
                .fetch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get orders by customer", e);
        }
    }

    @Override
    public List<CustomerOrder> getAllOrders() {
        return orm.from(CustomerOrder.class).fetch();
    }

    @Override
    public List<CustomerOrder> getOrdersByStatus(String status) {
        try {
            return orm.from(CustomerOrder.class)
                .where(Orders.c("status").eq(status))
                .fetch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get orders by status", e);
        }
    }

    @Override
    public void updateOrderStatus(int orderId, String status) {
        try {
            orm.update(CustomerOrder.class)
                .set("status", status)
                .where(Orders.c("orderID").eq(orderId))
                .execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update order status", e);
        }
    }

    // ========================================================================
    // ORDER ITEMS
    // ========================================================================

    @Override
    public int addOrderItem(OrderItem item) {
        try {
            orm.permanent(item);
            return item.orderItemID;
        } catch (Exception e) {
            throw new RuntimeException("Failed to add order item", e);
        }
    }

    @Override
    public List<Map<String, Object>> getOrderItemsWithDetails(int orderId) {
        try {
            List<OrderItem> items = orm.from(OrderItem.class)
                .where(OrderItems.c("orderID").eq(orderId))
                .fetch();

            List<Map<String, Object>> result = new ArrayList<>();
            for (OrderItem item : items) {
                Product p = getProductById(item.productID);
                Map<String, Object> m = new HashMap<>();
                m.put("orderItemID", item.orderItemID);
                m.put("orderID", item.orderID);
                m.put("productID", item.productID);
                m.put("productName", p != null ? p.name : "Unknown");
                m.put("quantity", item.quantity);
                m.put("preparedCount", item.preparedCount);
                m.put("priceAtOrder", item.priceAtOrder);
                result.add(m);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get order items with details", e);
        }
    }

    // ========================================================================
    // ONLINE ORDERS
    // ========================================================================

    @Override
    public void addOnlineOrder(int orderId) {
        try {
            OnlineOrder oo = new OnlineOrder();
            oo.orderID = orderId;
            oo.isConfirmed = false;
            orm.permanent(oo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add online order", e);
        }
    }

    @Override
    public void confirmOnlineOrder(int orderId) {
        try {
            orm.update(OnlineOrder.class)
                .set("isConfirmed", true)
                .where(OnlineOrders.c("orderID").eq(orderId))
                .execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to confirm online order", e);
        }
    }

    @Override
    public boolean isOnlineOrder(int orderId) {
        try {
            OnlineOrder oo = new OnlineOrder(orderId);
            return orm.select(oo).populate() != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ========================================================================
    // INGREDIENTS
    // ========================================================================

    @Override
    public List<Ingredient> getAllIngredients() {
        return orm.from(Ingredient.class).fetch();
    }

    @Override
    public List<Ingredient> getLowStockIngredients() {
        try {
            // Fetch all and filter in memory (no HAVING support yet)
            List<Ingredient> all = orm.from(Ingredient.class).fetch();
            List<Ingredient> low = new ArrayList<>();
            for (Ingredient ing : all) {
                if (ing.stock != null && ing.restockThreshold != null &&
                    ing.stock < ing.restockThreshold) {
                    low.add(ing);
                }
            }
            return low;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get low stock ingredients", e);
        }
    }

    // ========================================================================
    // RESTOCK REQUESTS
    // ========================================================================

    @Override
    public int addRestockRequest(RestockRequest r) {
        try {
            orm.permanent(r);
            return r.requestID;
        } catch (Exception e) {
            throw new RuntimeException("Failed to add restock request", e);
        }
    }

    @Override
    public List<Map<String, Object>> getAllRestockRequestsWithDetails() {
        try {
            List<RestockRequest> requests = orm.from(RestockRequest.class).fetch();
            List<Map<String, Object>> result = new ArrayList<>();
            for (RestockRequest r : requests) {
                Ingredient ing = new Ingredient(r.ingredientID);
                Ingredient ingFull;
                try {
                    ingFull = orm.select(ing).populate();
                } catch (Exception ex) {
                    ingFull = null;
                }
                
                Supplier sup = getSupplierById(r.supplierID);
                
                Map<String, Object> m = new HashMap<>();
                m.put("requestID", r.requestID);
                m.put("ingredientID", r.ingredientID);
                m.put("ingredientName", ingFull != null ? ingFull.name : "Unknown");
                m.put("supplierID", r.supplierID);
                m.put("supplierName", sup != null ? sup.name : "Unknown");
                m.put("quantityRequested", r.quantityRequested);
                m.put("status", r.status);
                m.put("requestedAt", r.requestedAt);
                result.add(m);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get restock requests with details", e);
        }
    }

    @Override
    public int countPendingRestocks() {
        try {
            List<RestockRequest> pending = orm.from(RestockRequest.class)
                .where(RestockRequests.c("status").eq("pending"))
                .fetch();
            return pending.size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count pending restocks", e);
        }
    }

    // ========================================================================
    // SUPPLIERS
    // ========================================================================

    @Override
    public List<Supplier> getAllSuppliers() {
        return orm.from(Supplier.class).fetch();
    }

    @Override
    public Supplier getSupplierById(int id) {
        Supplier s = new Supplier(id);
        try {
            return orm.select(s).populate();
        } catch (Exception e) {
            return null;
        }
    }

    // ========================================================================
    // CAFE TABLES
    // ========================================================================

    @Override
    public List<CafeTable> getAllTables() {
        return orm.from(CafeTable.class).fetch();
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    @Override
    public int countOrdersToday() {
        try {
            long today = (System.currentTimeMillis() / 1000) - 86400;
            List<CustomerOrder> orders = orm.from(CustomerOrder.class)
                .where(Orders.c("time").above(today))
                .fetch();
            return orders.size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count orders today", e);
        }
    }

    @Override
    public int countOrdersByStatus(String status) {
        try {
            List<CustomerOrder> orders = orm.from(CustomerOrder.class)
                .where(Orders.c("status").eq(status))
                .fetch();
            return orders.size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count orders by status", e);
        }
    }

    @Override
    public double getTotalSalesToday() {
        try {
            long today = (System.currentTimeMillis() / 1000) - 86400;
            List<OrderItem> items = orm.from(OrderItem.class).fetch();
            double total = 0;
            for (OrderItem item : items) {
                CustomerOrder order = getOrderById(item.orderID);
                if (order != null && order.time != null && order.time > today) {
                    if (item.priceAtOrder != null && item.quantity != null) {
                        total += item.priceAtOrder * item.quantity;
                    }
                }
            }
            return total;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get total sales today", e);
        }
    }

    @Override
    public int countLowStockItems() {
        return getLowStockIngredients().size();
    }

    @Override
    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        try {
            List<OrderItem> items = orm.from(OrderItem.class).fetch();
            Map<Integer, Integer> productSales = new HashMap<>();
            for (OrderItem item : items) {
                productSales.put(item.productID,
                    productSales.getOrDefault(item.productID, 0) + item.quantity);
            }

            // Sort by quantity descending and take top limit
            List<Map<String, Object>> result = new ArrayList<>();
            productSales.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .forEach(entry -> {
                    Product p = getProductById(entry.getKey());
                    Map<String, Object> m = new HashMap<>();
                    m.put("productID", entry.getKey());
                    m.put("productName", p != null ? p.name : "Unknown");
                    m.put("totalQuantitySold", entry.getValue());
                    m.put("price", p != null ? p.price : 0);
                    result.add(m);
                });
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get top selling products", e);
        }
    }
}
