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

    static boolean requireAdmin(Context ctx) {
        if (!isAdmin(ctx, SECRET_KEY)) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return false;
        }
        return true;
    }

    static Integer getCustomerIdFromCookie(Context ctx) {
        String cid = ctx.cookie("customer_id");
        if (cid == null) return null;
        try { return Integer.parseInt(cid); } catch (NumberFormatException e) { return null; }
    }

    public static void main(String[] args) throws Exception {
        ORM orm = new ORM();
        orm.connect("jdbc:sqlite:cafe.db", null, null);
        Database.createTables(orm);
        dao = new StudDB(orm);
        seedData();

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
            config.router.apiBuilder(() -> {});
        }).start(5000);

        // =====================================================================
        // LEGACY PUBLIC ENDPOINTS
        // =====================================================================

        app.get("/products", ctx -> {
            ctx.json(Map.of("products", dao.getAllProducts()));
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
        // REACT API — PUBLIC
        // =====================================================================

        // GET /api/products
        app.get("/api/products", ctx -> {
            ctx.json(Map.of("products", dao.getAllProducts()));
        });

        // GET /api/tables
        app.get("/api/tables", ctx -> {
            ctx.json(Map.of("tables", dao.getAllTables()));
        });

        // POST /api/customers — register a new customer, returns customerID
        app.post("/api/customers", ctx -> {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String firstName = (String) body.get("firstName");
            String lastName  = (String) body.get("lastName");
            if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
                ctx.status(400).json(Map.of("error", "firstName and lastName required"));
                return;
            }
            Customer c = new Customer();
            c.firstName = firstName.trim();
            c.lastName  = lastName.trim();
            int id = dao.addCustomer(c);
            ctx.status(201).json(Map.of("customerID", id, "firstName", c.firstName, "lastName", c.lastName));
        });

        // GET /api/customers/:id
        app.get("/api/customers/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Customer c = dao.getCustomerById(id);
            if (c == null) {
                ctx.status(404).json(Map.of("error", "Customer not found"));
                return;
            }
            ctx.json(Map.of("customerID", c.customerID, "firstName", c.firstName, "lastName", c.lastName));
        });

        // POST /api/orders — place a new order (no cookie auth, customerID in body)
        app.post("/api/orders", ctx -> {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            Object cidObj = body.get("customerID");
            if (cidObj == null) {
                ctx.status(400).json(Map.of("error", "customerID required"));
                return;
            }
            int customerId;
            try { customerId = ((Number) cidObj).intValue(); }
            catch (Exception e) {
                ctx.status(400).json(Map.of("error", "Invalid customerID"));
                return;
            }

            Customer existing = dao.getCustomerById(customerId);
            if (existing == null) {
                ctx.status(404).json(Map.of("error", "Customer not found"));
                return;
            }

            String paymentMethod = (String) body.getOrDefault("paymentMethod", "cash");
            Object tableIdObj = body.get("tableID");

            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items == null || items.isEmpty()) {
                ctx.status(400).json(Map.of("error", "items required"));
                return;
            }

            CustomerOrder order = new CustomerOrder();
            order.customerID = customerId;
            if (tableIdObj != null) {
                try { order.tableID = ((Number) tableIdObj).intValue(); }
                catch (Exception ignored) {}
            }
            order.time = System.currentTimeMillis() / 1000;
            order.status = "pending";
            order.paymentMethod = paymentMethod;

            int orderId = dao.addOrder(order);
            double total = 0;

            for (Map<String, Object> item : items) {
                int pid = ((Number) item.get("productID")).intValue();
                int qty = ((Number) item.get("quantity")).intValue();
                if (qty <= 0) continue;
                Product p = dao.getProductById(pid);
                if (p == null) continue;
                OrderItem oi = new OrderItem();
                oi.orderID = orderId;
                oi.productID = pid;
                oi.quantity = qty;
                oi.preparedCount = 0;
                oi.priceAtOrder = p.price;
                dao.addOrderItem(oi);
                total += p.price * qty;
            }

            if ("online".equals(paymentMethod)) {
                dao.addOnlineOrder(orderId);
            }

            ctx.status(201).json(Map.of("orderID", orderId, "total", total));
        });

        // GET /api/orders?customerId=X
        app.get("/api/orders", ctx -> {
            String cidStr = ctx.queryParam("customerId");
            if (cidStr == null || cidStr.isEmpty()) {
                ctx.status(400).json(Map.of("error", "customerId query param required"));
                return;
            }
            int cid = Integer.parseInt(cidStr);
            List<CustomerOrder> orders = dao.getOrdersByCustomer(cid);
            List<Map<String, Object>> result = new ArrayList<>();
            for (CustomerOrder o : orders) {
                Map<String, Object> m = new HashMap<>();
                m.put("orderID",       o.orderID);
                m.put("customerID",    o.customerID);
                m.put("tableID",       o.tableID);
                m.put("time",          o.time);
                m.put("status",        o.status);
                m.put("paymentMethod", o.paymentMethod);
                m.put("isOnline",      dao.isOnlineOrder(o.orderID));
                result.add(m);
            }
            ctx.json(Map.of("orders", result));
        });

        // GET /api/orders/:id?customerId=X
        app.get("/api/orders/{id}", ctx -> {
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            String cidStr = ctx.queryParam("customerId");
            CustomerOrder order = dao.getOrderById(orderId);
            if (order == null) {
                ctx.status(404).json(Map.of("error", "Order not found"));
                return;
            }
            // Validate ownership if customerId supplied
            if (cidStr != null && !cidStr.isEmpty()) {
                int cid = Integer.parseInt(cidStr);
                if (!Objects.equals(order.customerID, cid)) {
                    ctx.status(404).json(Map.of("error", "Order not found"));
                    return;
                }
            }
            List<Map<String, Object>> items = dao.getOrderItemsWithDetails(orderId);
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("orderID",       order.orderID);
            orderMap.put("customerID",    order.customerID);
            orderMap.put("tableID",       order.tableID);
            orderMap.put("time",          order.time);
            orderMap.put("status",        order.status);
            orderMap.put("paymentMethod", order.paymentMethod);
            orderMap.put("isOnline",      dao.isOnlineOrder(orderId));
            data.put("order", orderMap);
            data.put("items", items);
            ctx.json(data);
        });

        // =====================================================================
        // REACT API — ADMIN
        // =====================================================================

        // POST /api/admin/login
        app.post("/api/admin/login", ctx -> {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String password = (String) body.get("password");
            if (ADMIN_PASSWORD.equals(password)) {
                ctx.cookie("session_id", SECRET_KEY);
                ctx.json(Map.of("role", "admin"));
            } else {
                ctx.status(401).json(Map.of("error", "Wrong password"));
            }
        });

        // POST /api/admin/logout
        app.post("/api/admin/logout", ctx -> {
            ctx.removeCookie("session_id");
            ctx.json(Map.of("status", "ok"));
        });

        // GET /api/admin/dashboard
        app.get("/api/admin/dashboard", ctx -> {
            if (!requireAdmin(ctx)) return;
            Map<String, Object> data = new HashMap<>();
            data.put("ordersToday",     dao.countOrdersToday());
            data.put("pendingOrders",   dao.countOrdersByStatus("pending"));
            data.put("lowStockItems",   dao.countLowStockItems());
            data.put("pendingRestocks", dao.countPendingRestocks());
            data.put("salesToday",      dao.getTotalSalesToday());
            data.put("topProducts",     dao.getTopSellingProducts(5));
            ctx.json(data);
        });

        // GET /api/admin/orders?status=X
        app.get("/api/admin/orders", ctx -> {
            if (!requireAdmin(ctx)) return;
            String statusFilter = ctx.queryParam("status");
            List<CustomerOrder> orders = (statusFilter != null && !statusFilter.isEmpty())
                ? dao.getOrdersByStatus(statusFilter)
                : dao.getAllOrders();

            List<Map<String, Object>> orderData = new ArrayList<>();
            for (CustomerOrder o : orders) {
                Customer c = dao.getCustomerById(o.customerID);
                String customerName = (c != null) ? c.firstName + " " + c.lastName : "Guest";
                Map<String, Object> m = new HashMap<>();
                m.put("orderID",       o.orderID);
                m.put("customerID",    o.customerID);
                m.put("customerName",  customerName);
                m.put("tableID",       o.tableID);
                m.put("time",          o.time);
                m.put("status",        o.status);
                m.put("paymentMethod", o.paymentMethod);
                m.put("isOnline",      dao.isOnlineOrder(o.orderID));
                orderData.add(m);
            }
            ctx.json(Map.of("orders", orderData));
        });

        // GET /api/admin/orders/:id
        app.get("/api/admin/orders/{id}", ctx -> {
            if (!requireAdmin(ctx)) return;
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            CustomerOrder order = dao.getOrderById(orderId);
            if (order == null) { ctx.status(404).json(Map.of("error", "Order not found")); return; }
            List<Map<String, Object>> items = dao.getOrderItemsWithDetails(orderId);
            Customer c = dao.getCustomerById(order.customerID);
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("orderID",       order.orderID);
            orderMap.put("customerID",    order.customerID);
            orderMap.put("customerName",  c != null ? c.firstName + " " + c.lastName : "Guest");
            orderMap.put("tableID",       order.tableID);
            orderMap.put("time",          order.time);
            orderMap.put("status",        order.status);
            orderMap.put("paymentMethod", order.paymentMethod);
            orderMap.put("isOnline",      dao.isOnlineOrder(orderId));
            data.put("order", orderMap);
            data.put("items", items);
            ctx.json(data);
        });

        // POST /api/admin/orders/:id/status
        app.post("/api/admin/orders/{id}/status", ctx -> {
            if (!requireAdmin(ctx)) return;
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String newStatus = (String) body.get("status");
            if (newStatus == null) { ctx.status(400).json(Map.of("error", "status required")); return; }
            dao.updateOrderStatus(orderId, newStatus);
            ctx.json(Map.of("status", "ok"));
        });

        // POST /api/admin/orders/:id/confirm
        app.post("/api/admin/orders/{id}/confirm", ctx -> {
            if (!requireAdmin(ctx)) return;
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            dao.confirmOnlineOrder(orderId);
            dao.updateOrderStatus(orderId, "confirmed");
            ctx.json(Map.of("status", "ok"));
        });

        // GET /api/admin/products
        app.get("/api/admin/products", ctx -> {
            if (!requireAdmin(ctx)) return;
            ctx.json(Map.of("products", dao.getAllProducts()));
        });

        // POST /api/admin/products
        app.post("/api/admin/products", ctx -> {
            if (!requireAdmin(ctx)) return;
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String name     = (String) body.get("name");
            Object priceObj = body.get("price");
            String category = (String) body.get("category");
            if (name == null || priceObj == null || category == null) {
                ctx.status(400).json(Map.of("error", "name, price, category required"));
                return;
            }
            Product p = new Product();
            p.name     = name;
            p.price    = ((Number) priceObj).doubleValue();
            p.category = category;
            int id = dao.addProduct(p);
            ctx.status(201).json(Map.of("productID", id));
        });

        // PUT /api/admin/products/:id
        app.put("/api/admin/products/{id}", ctx -> {
            if (!requireAdmin(ctx)) return;
            int id = Integer.parseInt(ctx.pathParam("id"));
            Product p = dao.getProductById(id);
            if (p == null) { ctx.status(404).json(Map.of("error", "Product not found")); return; }
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body.containsKey("name"))     p.name     = (String) body.get("name");
            if (body.containsKey("price"))    p.price    = ((Number) body.get("price")).doubleValue();
            if (body.containsKey("category")) p.category = (String) body.get("category");
            dao.updateProduct(p);
            ctx.json(Map.of("status", "ok"));
        });

        // DELETE /api/admin/products/:id
        app.delete("/api/admin/products/{id}", ctx -> {
            if (!requireAdmin(ctx)) return;
            int id = Integer.parseInt(ctx.pathParam("id"));
            dao.deleteProduct(id);
            ctx.json(Map.of("status", "ok"));
        });

        // GET /api/admin/inventory
        app.get("/api/admin/inventory", ctx -> {
            if (!requireAdmin(ctx)) return;
            ctx.json(Map.of(
                "ingredients", dao.getAllIngredients(),
                "suppliers",   dao.getAllSuppliers()
            ));
        });

        // GET /api/admin/suppliers
        app.get("/api/admin/suppliers", ctx -> {
            if (!requireAdmin(ctx)) return;
            ctx.json(Map.of("suppliers", dao.getAllSuppliers()));
        });

        // GET /api/admin/restock-requests
        app.get("/api/admin/restock-requests", ctx -> {
            if (!requireAdmin(ctx)) return;
            ctx.json(Map.of("requests", dao.getAllRestockRequestsWithDetails()));
        });

        // POST /api/admin/restock-requests
        app.post("/api/admin/restock-requests", ctx -> {
            if (!requireAdmin(ctx)) return;
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            Object ingId = body.get("ingredientID");
            Object supId = body.get("supplierID");
            Object qty   = body.get("quantity");
            if (ingId == null || supId == null || qty == null) {
                ctx.status(400).json(Map.of("error", "ingredientID, supplierID, quantity required"));
                return;
            }
            RestockRequest r = new RestockRequest();
            r.ingredientID     = ((Number) ingId).intValue();
            r.supplierID       = ((Number) supId).intValue();
            r.quantityRequested = ((Number) qty).intValue();
            r.status           = "pending";
            r.requestedAt      = System.currentTimeMillis() / 1000;
            int id = dao.addRestockRequest(r);
            ctx.status(201).json(Map.of("requestID", id));
        });

        // GET /api/admin/sales
        app.get("/api/admin/sales", ctx -> {
            if (!requireAdmin(ctx)) return;
            Map<String, Object> data = new HashMap<>();
            data.put("ordersToday",      dao.countOrdersToday());
            data.put("salesToday",       dao.getTotalSalesToday());
            data.put("pendingOrders",    dao.countOrdersByStatus("pending"));
            data.put("completedOrders",  dao.countOrdersByStatus("completed"));
            data.put("topProducts",      dao.getTopSellingProducts(10));
            ctx.json(data);
        });

        // =====================================================================
        // LEGACY CUSTOMER ENDPOINTS (cookie-based)
        // =====================================================================

        app.get("/customer/menu", ctx -> {
            ctx.json(Map.of("products", dao.getAllProducts(), "tables", dao.getAllTables()));
        });

        app.post("/customer/order", ctx -> {
            String cidStr = ctx.formParam("customerID");
            int customerId;
            if (cidStr != null && !cidStr.isEmpty()) {
                customerId = Integer.parseInt(cidStr);
            } else {
                Integer cookieCid = getCustomerIdFromCookie(ctx);
                if (cookieCid == null) { ctx.status(401).json(Map.of("error", "Customer not logged in")); return; }
                customerId = cookieCid;
            }
            String paymentMethod = ctx.formParam("paymentMethod");
            if (paymentMethod == null) paymentMethod = "cash";
            String tableIdStr = ctx.formParam("tableID");
            List<String> pidList = ctx.formParams("productID");
            List<String> qtyList = ctx.formParams("quantity");
            if (pidList == null || qtyList == null || pidList.isEmpty()) {
                ctx.status(400).json(Map.of("error", "No items in order")); return;
            }
            CustomerOrder order = new CustomerOrder();
            order.customerID = customerId;
            if (tableIdStr != null && !tableIdStr.isEmpty()) {
                try { order.tableID = Integer.parseInt(tableIdStr); } catch (NumberFormatException ignored) {}
            }
            order.time = System.currentTimeMillis() / 1000;
            order.status = "pending";
            order.paymentMethod = paymentMethod;
            int orderId = dao.addOrder(order);
            double total = 0;
            for (int i = 0; i < pidList.size(); i++) {
                int pid = Integer.parseInt(pidList.get(i));
                int qty = Integer.parseInt(qtyList.get(i));
                if (qty <= 0) continue;
                Product p = dao.getProductById(pid);
                if (p == null) continue;
                OrderItem item = new OrderItem();
                item.orderID = orderId; item.productID = pid;
                item.quantity = qty; item.preparedCount = 0; item.priceAtOrder = p.price;
                dao.addOrderItem(item);
                total += p.price * qty;
            }
            if ("online".equals(paymentMethod)) dao.addOnlineOrder(orderId);
            ctx.status(201).json(Map.of("orderID", orderId, "total", total));
        });

        app.get("/customer/orders", ctx -> {
            Integer cid = getCustomerIdFromCookie(ctx);
            if (cid == null) { ctx.status(401).json(Map.of("error", "Not logged in")); return; }
            ctx.json(Map.of("orders", dao.getOrdersByCustomer(cid)));
        });

        app.get("/customer/orders/{id}", ctx -> {
            Integer cid = getCustomerIdFromCookie(ctx);
            if (cid == null) { ctx.status(401).json(Map.of("error", "Not logged in")); return; }
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            CustomerOrder order = dao.getOrderById(orderId);
            if (order == null || !Objects.equals(order.customerID, cid)) {
                ctx.status(404).json(Map.of("error", "Order not found")); return;
            }
            ctx.json(Map.of("order", order, "items", dao.getOrderItemsWithDetails(orderId)));
        });

        // =====================================================================
        // SPA FALLBACK — serve index.html for any 404 that is a browser nav
        // request (no file extension, not an API route). Asset files (.js,
        // .css, etc.) and API paths are left as genuine 404s.
        // =====================================================================
        app.error(404, ctx -> {
            String path = ctx.path();
            if (!path.startsWith("/api") && !path.contains(".")) {
                try (var stream = Main.class.getResourceAsStream("/public/index.html")) {
                    if (stream != null) {
                        ctx.contentType("text/html").result(stream.readAllBytes());
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    static void seedData() {
        // Only seed if tables are empty
        if (!dao.getAllProducts().isEmpty()) return;

        // ---- Suppliers ----
        Supplier s1 = new Supplier(); s1.name = "BeanMasters Co.";   s1.contact = "Alice Nguyen";   s1.email = "alice@beanmasters.com";   s1.address = "12 Roast Lane, Seattle"; s1.rating = 4.8;
        Supplier s2 = new Supplier(); s2.name = "FreshDairy Ltd.";   s2.contact = "Bob Chen";       s2.email = "bob@freshdairy.com";       s2.address = "55 Milkway Ave, Portland"; s2.rating = 4.5;
        Supplier s3 = new Supplier(); s3.name = "SweetSyrup Inc.";   s3.contact = "Carol James";    s3.email = "carol@sweetsyrup.com";     s3.address = "88 Sugar St, Austin";  s3.rating = 4.2;
        Supplier s4 = new Supplier(); s4.name = "PastryPro Dist.";   s4.contact = "David Lee";      s4.email = "david@pastrypro.com";      s4.address = "7 Flour Blvd, Chicago"; s4.rating = 4.6;
        try {
            dao.getAllSuppliers(); // trigger lazy init
            dao.addSupplier(s1); dao.addSupplier(s2); dao.addSupplier(s3); dao.addSupplier(s4);
        } catch (Exception ignored) {}

        // ---- Ingredients ----
        seedIngredient("Espresso Beans",    "coffee",  150, 50,  s1.supplierID);
        seedIngredient("Whole Milk",        "dairy",   80,  30,  s2.supplierID);
        seedIngredient("Oat Milk",          "dairy",   40,  20,  s2.supplierID);
        seedIngredient("Vanilla Syrup",     "flavour", 60,  20,  s3.supplierID);
        seedIngredient("Caramel Syrup",     "flavour", 55,  20,  s3.supplierID);
        seedIngredient("Sugar",             "dry",     200, 50,  s3.supplierID);
        seedIngredient("Butter",            "dairy",   30,  15,  s2.supplierID);
        seedIngredient("All-Purpose Flour", "dry",     100, 40,  s4.supplierID);
        seedIngredient("Cocoa Powder",      "dry",     45,  20,  s4.supplierID);
        seedIngredient("Cream",             "dairy",   35,  15,  s2.supplierID);

        // ---- Products ----
        seedProduct("Espresso",             3.00, "coffee");
        seedProduct("Americano",            3.50, "coffee");
        seedProduct("Flat White",           4.00, "coffee");
        seedProduct("Cappuccino",           4.50, "coffee");
        seedProduct("Latte",                4.50, "coffee");
        seedProduct("Vanilla Latte",        5.00, "coffee");
        seedProduct("Caramel Macchiato",    5.50, "coffee");
        seedProduct("Cold Brew",            4.50, "coffee");
        seedProduct("Matcha Latte",         5.00, "other");
        seedProduct("Hot Chocolate",        4.00, "other");
        seedProduct("Green Tea",            3.50, "other");
        seedProduct("Chai Latte",           4.50, "other");
        seedProduct("Orange Juice",         4.00, "other");
        seedProduct("Croissant",            3.50, "food");
        seedProduct("Blueberry Muffin",     3.00, "food");
        seedProduct("Chocolate Muffin",     3.00, "food");
        seedProduct("Banana Bread",         3.50, "food");
        seedProduct("Avocado Toast",        8.50, "food");
        seedProduct("Eggs Benedict",       12.00, "food");
        seedProduct("Granola Bowl",         7.50, "food");
        seedProduct("BLT Sandwich",         9.00, "food");
        seedProduct("Grilled Cheese",       8.00, "food");
        seedProduct("Caesar Salad",         9.50, "food");
        seedProduct("Cheesecake Slice",     5.50, "food");
        seedProduct("Tiramisu",             6.00, "food");

        // ---- Cafe Tables ----
        seedTable(2, "Window");
        seedTable(2, "Window");
        seedTable(4, "Main Floor");
        seedTable(4, "Main Floor");
        seedTable(4, "Main Floor");
        seedTable(6, "Main Floor");
        seedTable(2, "Outdoor");
        seedTable(2, "Outdoor");
        seedTable(4, "Outdoor");
        seedTable(8, "Private Room");

        System.out.println("[Seed] Database seeded with products, tables, ingredients, and suppliers.");
    }

    static void seedProduct(String name, double price, String category) {
        try {
            Product p = new Product();
            p.name = name; p.price = price; p.category = category;
            dao.addProduct(p);
        } catch (Exception e) {
            System.err.println("[Seed] Failed to seed product: " + name + " — " + e.getMessage());
        }
    }

    static void seedIngredient(String name, String type, int stock, int threshold, int supplierId) {
        try {
            dao.addIngredient(name, type, stock, threshold, supplierId);
        } catch (Exception e) {
            System.err.println("[Seed] Failed to seed ingredient: " + name + " — " + e.getMessage());
        }
    }

    static void seedTable(int capacity, String location) {
        try {
            dao.addCafeTable(capacity, location);
        } catch (Exception e) {
            System.err.println("[Seed] Failed to seed table — " + e.getMessage());
        }
    }
}
