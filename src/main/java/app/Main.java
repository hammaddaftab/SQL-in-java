package app;

import app.models.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.StringWriter;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import static app.ToData.*;
import static app.Helpers.*;
import sql_in_java.ORM;

public class Main {

    // equivalent of userSockets — Javalin manages WS connections differently,
    // keyed by session id which Javalin provides on the WS context
    static Map<String, io.javalin.websocket.WsContext> userSockets = new HashMap<>();

    static final String SECRET_KEY = "unguessable_random_number";

    // Mustache equivalent of crow::mustache::load + render
    static String renderTemplate(String templateName, Object data) {
        MustacheFactory mf = new DefaultMustacheFactory("templates");
        Mustache m = mf.compile(templateName);
        StringWriter writer = new StringWriter();
        m.execute(writer, data);
        return writer.toString();
    }

    public static void main(String[] args) {

        // equivalent of sqlite3* db = openDB()
        ORM orm = new ORM();
        orm.connect("localhost:5576", "root", "4321");
        Database.createTables(orm);

        // equivalent of crow::SimpleApp app
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");   // serve static files
        }).start(8080);

        // INDEX route — equivalent of CROW_ROUTE(app, "/")
        app.get("/", ctx -> {
            List<Product> products = Database.selectAllProducts(orm);
            String html = renderTemplate("clientOrder.html", productsToData(products));
            ctx.html(html);
        });

        // ADMIN route — equivalent of CROW_ROUTE(app, "/admin")
        app.get("/admin", ctx -> {
            if (!isAdmin(ctx, SECRET_KEY)) {
                customRedirect(ctx, "/admin/auth");
                return;                          // equivalent of returning early in C++
            }
            String html = renderTemplate("adminView.html", null);
            ctx.html(html);
        });
    }
}