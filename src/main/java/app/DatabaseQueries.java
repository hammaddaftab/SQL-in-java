package app;

import java.util.List;
import java.util.Map;

import app.models.*;

public interface DatabaseQueries {

    // ---- Products ----
    List<Product> getAllProducts();
    Product getProductById(int id);
    int addProduct(Product p);
    void updateProduct(Product p);
    void deleteProduct(int id);

    // ---- Customers ----
    Customer getCustomerById(int id);

    // ---- Orders ----
    int addOrder(CustomerOrder o);
    CustomerOrder getOrderById(int id);
    List<CustomerOrder> getOrdersByCustomer(int customerId);
    List<CustomerOrder> getAllOrders();
    List<CustomerOrder> getOrdersByStatus(String status);
    void updateOrderStatus(int orderId, String status);

    // ---- Order Items ----
    int addOrderItem(OrderItem item);
    List<Map<String, Object>> getOrderItemsWithDetails(int orderId);

    // ---- Online Orders ----
    void addOnlineOrder(int orderId);
    void confirmOnlineOrder(int orderId);
    boolean isOnlineOrder(int orderId);

    // ---- Ingredients ----
    List<Ingredient> getAllIngredients();
    List<Ingredient> getLowStockIngredients();

    // ---- Restock Requests ----
    int addRestockRequest(RestockRequest r);
    List<Map<String, Object>> getAllRestockRequestsWithDetails();
    int countPendingRestocks();

    // ---- Suppliers ----
    List<Supplier> getAllSuppliers();
    Supplier getSupplierById(int id);

    // ---- Cafe Tables ----
    List<CafeTable> getAllTables();

    // ---- Stats ----
    int countOrdersToday();
    int countOrdersByStatus(String status);
    double getTotalSalesToday();
    int countLowStockItems();
    List<Map<String, Object>> getTopSellingProducts(int limit);
}
