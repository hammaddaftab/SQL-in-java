package app;

import app.models.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import static app.Helpers.*;
import sql_in_java.ORM;

import java.util.*;

public class Main {

    static final String SECRET_KEY = "unguessable_random_number";
    static final String ADMIN_PASSWORD = "admin123";

    static DatabaseQueries dao;

    // ---- Auth helpers ----

    static boolean requireAdmin(Context ctx) {
        if (!isAdmin(ctx, SECRET_KEY)) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return false;
        }
        return true;
    }

    static Integer getCustomerId(Context ctx) {
        String cid = ctx.cookie("customer_id");
        if (cid == null) return null;
        try {
            return Integer.parseInt(cid);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static boolean requireCustomer(Context ctx) {
        if (getCustomerId(ctx) == null) {
            ctx.status(401).json(Map.of("error", "Customer not logged in"));
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        ORM orm = new ORM();
        orm.connect("jdbc:sqlite:cafe.db", null, null);
        Database.createTables(orm);
        dao = new StudDB(orm);

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
        }).start(5000);

        // =====================================================================
        // PUBLIC ENDPOINTS
        // =====================================================================

        app.get("/products", ctx -> {
            List<Product> products = dao.getAllProducts();
            ctx.json(Map.of("products", products));
        });

        app.post("/login", ctx -> {
            String type = ctx.formParam("type");
            if ("admin".equals(type)) {
                String passwordIn = ctx.formParam("password");
                if (ADMIN_PASSWORD.equals(passwordIn)) {
                    ctx.cookie("session_id", SECRET_KEY);
                    ctx.json(Map.of("role", "admin"));
                } else {
                    ctx.status(401).json(Map.of("error", "Wrong password"));
                }
                return;
            }
            if ("customer".equals(type)) {
                String cidStr = ctx.formParam("customerID");
                if (cidStr == null || cidStr.isEmpty()) {
                    ctx.status(400).json(Map.of("error", "customerID required"));
                    return;
                }
                try {
                    int cid = Integer.parseInt(cidStr);
                    Customer c = dao.getCustomerById(cid);
                    if (c != null) {
                        ctx.cookie("customer_id", String.valueOf(cid));
                        ctx.json(Map.of("role", "customer", "customerID", cid));
                    } else {
                        ctx.status(404).json(Map.of("error", "Customer not found"));
                    }
                } catch (NumberFormatException e) {
                    ctx.status(400).json(Map.of("error", "Invalid customerID"));
                }
                return;
            }
            ctx.status(400).json(Map.of("error", "Invalid login type"));
        });

        app.post("/logout", ctx -> {
            ctx.removeCookie("session_id");
            ctx.removeCookie("customer_id");
            ctx.json(Map.of("status", "ok"));
        });

        // =====================================================================
        // CUSTOMER ENDPOINTS
        // =====================================================================

        app.get("/customer/menu", ctx -> {
            if (!requireCustomer(ctx)) return;
            List<Product> products = dao.getAllProducts();
            List<CafeTable> tables = dao.getAllTables();
            Map<String, Object> data = new HashMap<>();
            data.put("products", products);
            data.put("tables", tables);
            ctx.json(data);
        });

        app.post("/customer/order", ctx -> {
            if (!requireCustomer(ctx)) return;
            int customerId = getCustomerId(ctx);

            String paymentMethod = ctx.formParam("paymentMethod");
            if (paymentMethod == null) paymentMethod = "cash";
            String tableIdStr = ctx.formParam("tableID");

            List<String> pidList = ctx.formParams("productID");
            List<String> qtyList = ctx.formParams("quantity");
            if (pidList == null || qtyList == null) {
                ctx.status(400).json(Map.of("error", "Invalid form data"));
                return;
            }
            String[] productIDs = pidList.toArray(new String[0]);
            String[] quantities = qtyList.toArray(new String[0]);

            if (productIDs.length == 0) {
                ctx.status(400).json(Map.of("error", "No items in order"));
                return;
            }

            CustomerOrder order = new CustomerOrder();
            order.customerID = customerId;
            if (tableIdStr != null && !tableIdStr.isEmpty()) {
                try {
                    order.tableID = Integer.parseInt(tableIdStr);
                } catch (NumberFormatException e) {
                    order.tableID = null;
                }
            }
            order.time = System.currentTimeMillis() / 1000;
            order.paymentMethod = paymentMethod;

            int orderId = dao.addOrder(order);
            double total = 0;

            for (int i = 0; i < productIDs.length; i++) {
                int pid = Integer.parseInt(productIDs[i]);
                int qty = Integer.parseInt(quantities[i]);
                if (qty <= 0) continue;
                Product p = dao.getProductById(pid);
                if (p == null) continue;
                OrderItem item = new OrderItem();
                item.orderID = orderId;
                item.productID = pid;
                item.quantity = qty;
                item.preparedCount = 0;
                item.priceAtOrder = p.price;
                dao.addOrderItem(item);
                total += p.price * qty;
            }

            if ("online".equals(paymentMethod)) {
                dao.addOnlineOrder(orderId);
            }

            ctx.status(201).json(Map.of("orderID", orderId, "total", total));
        });

        app.get("/customer/orders", ctx -> {
            if (!requireCustomer(ctx)) return;
            int customerId = getCustomerId(ctx);
            List<CustomerOrder> orders = dao.getOrdersByCustomer(customerId);
            ctx.json(Map.of("orders", orders));
        });

        app.get("/customer/orders/{id}", ctx -> {
            if (!requireCustomer(ctx)) return;
            int customerId = getCustomerId(ctx);
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            CustomerOrder order = dao.getOrderById(orderId);
            if (order == null || !Objects.equals(order.customerID, customerId)) {
                ctx.status(404).json(Map.of("error", "Order not found"));
                return;
            }
            List<Map<String, Object>> items = dao.getOrderItemsWithDetails(orderId);
            Map<String, Object> data = new HashMap<>();
            data.put("order", order);
            data.put("items", items);
            ctx.json(data);
        });

        // =====================================================================
        // ADMIN ENDPOINTS
        // =====================================================================

        app.get("/admin", ctx -> {
            if (!requireAdmin(ctx)) return;
            int ordersToday = dao.countOrdersToday();
            int pendingOrders = dao.countOrdersByStatus("pending");
            int lowStockItems = dao.countLowStockItems();
            int pendingRestocks = dao.countPendingRestocks();
            double salesToday = dao.getTotalSalesToday();
            List<Map<String, Object>> topProducts = dao.getTopSellingProducts(5);

            Map<String, Object> data = new HashMap<>();
            data.put("ordersToday", ordersToday);
            data.put("pendingOrders", pendingOrders);
            data.put("lowStockItems", lowStockItems);
            data.put("pendingRestocks", pendingRestocks);
            data.put("salesToday", salesToday);
            data.put("topProducts", topProducts);
            ctx.json(data);
        });

        app.get("/admin/products", ctx -> {
            if (!requireAdmin(ctx)) return;
            List<Product> products = dao.getAllProducts();
            ctx.json(Map.of("products", products));
        });

        app.post("/admin/products", ctx -> {
            if (!requireAdmin(ctx)) return;
            String name = ctx.formParam("name");
            String priceStr = ctx.formParam("price");
            String category = ctx.formParam("category");
            if (name == null || priceStr == null || category == null) {
                ctx.status(400).json(Map.of("error", "name, price, category required"));
                return;
            }
            Product p = new Product();
            p.name = name;
            p.price = Double.parseDouble(priceStr);
            p.category = category;
            int id = dao.addProduct(p);
            ctx.status(201).json(Map.of("productID", id));
        });

        app.post("/admin/products/{id}/update", ctx -> {
            if (!requireAdmin(ctx)) return;
            int id = Integer.parseInt(ctx.pathParam("id"));
            Product p = dao.getProductById(id);
            if (p == null) {
                ctx.status(404).json(Map.of("error", "Product not found"));
                return;
            }
            String name = ctx.formParam("name");
            String priceStr = ctx.formParam("price");
            String category = ctx.formParam("category");
            if (name != null) p.name = name;
            if (priceStr != null) p.price = Double.parseDouble(priceStr);
            if (category != null) p.category = category;
            dao.updateProduct(p);
            ctx.json(Map.of("status", "ok"));
        });

        app.post("/admin/products/{id}/delete", ctx -> {
            if (!requireAdmin(ctx)) return;
            int id = Integer.parseInt(ctx.pathParam("id"));
            dao.deleteProduct(id);
            ctx.json(Map.of("status", "ok"));
        });

        app.get("/admin/orders", ctx -> {
            if (!requireAdmin(ctx)) return;
            String statusFilter = ctx.queryParam("status");
            List<CustomerOrder> orders;
            if (statusFilter != null && !statusFilter.isEmpty()) {
                orders = dao.getOrdersByStatus(statusFilter);
            } else {
                orders = dao.getAllOrders();
            }

            List<Map<String, Object>> orderData = new ArrayList<>();
            for (CustomerOrder o : orders) {
                Customer c = dao.getCustomerById(o.customerID);
                String customerName = (c != null) ? c.firstName + " " + c.lastName : "Guest";
                Map<String, Object> m = new HashMap<>();
                m.put("orderID", o.orderID);
                m.put("customerID", o.customerID);
                m.put("customerName", customerName);
                m.put("tableID", o.tableID);
                m.put("time", o.time);
                m.put("paymentMethod", o.paymentMethod);
                m.put("isOnline", dao.isOnlineOrder(o.orderID));
                orderData.add(m);
            }

            ctx.json(Map.of("orders", orderData));
        });

        app.post("/admin/orders/{id}/status", ctx -> {
            if (!requireAdmin(ctx)) return;
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            String newStatus = ctx.formParam("status");
            if (newStatus == null) {
                ctx.status(400).json(Map.of("error", "status required"));
                return;
            }
            dao.updateOrderStatus(orderId, newStatus);
            ctx.json(Map.of("status", "ok"));
        });

        app.post("/admin/orders/{id}/confirm", ctx -> {
            if (!requireAdmin(ctx)) return;
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            dao.confirmOnlineOrder(orderId);
            dao.updateOrderStatus(orderId, "confirmed");
            ctx.json(Map.of("status", "ok"));
        });

        app.get("/admin/inventory", ctx -> {
            if (!requireAdmin(ctx)) return;
            List<Ingredient> ingredients = dao.getAllIngredients();
            List<Supplier> suppliers = dao.getAllSuppliers();
            Map<String, Object> data = new HashMap<>();
            data.put("ingredients", ingredients);
            data.put("suppliers", suppliers);
            ctx.json(data);
        });

        app.post("/admin/inventory/restock", ctx -> {
            if (!requireAdmin(ctx)) return;
            String ingIdStr = ctx.formParam("ingredientID");
            String supIdStr = ctx.formParam("supplierID");
            String qtyStr = ctx.formParam("quantity");
            if (ingIdStr == null || supIdStr == null || qtyStr == null) {
                ctx.status(400).json(Map.of("error", "ingredientID, supplierID, quantity required"));
                return;
            }
            RestockRequest r = new RestockRequest();
            r.ingredientID = Integer.parseInt(ingIdStr);
            r.supplierID = Integer.parseInt(supIdStr);
            r.quantityRequested = Integer.parseInt(qtyStr);
            r.status = "pending";
            r.requestedAt = System.currentTimeMillis() / 1000;
            int id = dao.addRestockRequest(r);
            ctx.status(201).json(Map.of("requestID", id));
        });

        app.get("/admin/restock-requests", ctx -> {
            if (!requireAdmin(ctx)) return;
            List<Map<String, Object>> requests = dao.getAllRestockRequestsWithDetails();
            ctx.json(Map.of("requests", requests));
        });

        app.get("/admin/suppliers", ctx -> {
            if (!requireAdmin(ctx)) return;
            List<Supplier> suppliers = dao.getAllSuppliers();
            ctx.json(Map.of("suppliers", suppliers));
        });

        app.get("/admin/sales", ctx -> {
            if (!requireAdmin(ctx)) return;
            int ordersToday = dao.countOrdersToday();
            double salesToday = dao.getTotalSalesToday();
            int pendingOrders = dao.countOrdersByStatus("pending");
            int completedOrders = dao.countOrdersByStatus("completed");
            List<Map<String, Object>> topProducts = dao.getTopSellingProducts(10);

            Map<String, Object> data = new HashMap<>();
            data.put("ordersToday", ordersToday);
            data.put("salesToday", salesToday);
            data.put("pendingOrders", pendingOrders);
            data.put("completedOrders", completedOrders);
            data.put("topProducts", topProducts);
            ctx.json(data);
        });
    }

}
