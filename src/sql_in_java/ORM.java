package src.sql_in_java;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// collection
import java.util.*;

public class ORM {
    Map<Class<?>, Table> tables = new HashMap<>();
    Map<Class<?>, Field> primaryKeys = new HashMap<>();

    // JDBC connection. Lazily set via connect(...). Reused by populate() / fetch().
    // Kept package-private so SelectBuilder can read it without a public getter chain.
    Connection connection;

    public void register(Class<?> clazz, Table table) {

        if (table.primaryKey == null) throw new RuntimeException(
            "Table " + table.tableName + " has no primary key defined"
        );

        // finds the primary key field
        Field primaryKeyField = null;
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(table.primaryKey.column.name)) {
                primaryKeyField = field;
                break;
            }
        }

        if (primaryKeyField == null) throw new RuntimeException(
            "The class provided has no matching primary key field (Case-sensitive)."
        );

        // every attribute in Table has a matching attribute in the Class provided 
        Map<String, Field> nameToField = new HashMap<>();
        for (Field f: clazz.getDeclaredFields()) {
            nameToField.put(f.getName(), f);
        }
        for (Column column: table.columnsList.values()) {
            if (!nameToField.containsKey(column.name)) throw new RuntimeException(
                "Column " + column.name + " has no matching attribute in the provided Class."
            );
        }

        tables.put(clazz, table);
        primaryKeys.put(clazz, primaryKeyField);
    }


    // =====================================================================
    // JDBC connection management
    //
    // The MySQL driver (mysql-connector-j) must be on the classpath. Modern
    // JDBC (4.0+) auto-registers drivers via SPI, so no Class.forName call
    // is required.
    //
    //   ORM orm = new ORM();
    //   orm.register(User.class, Users);
    //   orm.connect("jdbc:mysql://localhost:3306/mydb", "root", "secret");
    //   ...
    //   orm.close();
    // =====================================================================

    public ORM connect(String url, String user, String password) {
        try {
            this.connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException(
                "Failed to connect to database at " + url + ": " + e.getMessage(), e
            );
        }
        return this;
    }

    // For users who already manage a Connection (e.g. via a DataSource / pool).
    public ORM connect(Connection conn) {
        this.connection = conn;
        return this;
    }

    public Connection getConnection() {
        if (connection == null) throw new RuntimeException(
            "ORM is not connected. Call orm.connect(url, user, password) first."
        );
        return connection;
    }

    public void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
            connection = null;
        }
    }


    // Object -> tracked instance, Map<String, Object>
    Map<Object, Map<String, Object>> snapshots = new IdentityHashMap<>();

    public void track(Object instance) throws IllegalAccessException {
        Class<?> clazz = instance.getClass();

        // make sure this class was registered
        if (!tables.containsKey(clazz)) throw new RuntimeException(
            clazz.getName() + " is not registered with the ORM"
        );

        Map<String, Object> snapshot = new HashMap<>();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            snapshot.put(field.getName(), field.get(instance));
        }

        snapshots.put(instance, snapshot);
    }


    // instance is the tracked instance
    // snapshot is a copy of the tracked instance, made when the tracking started
    public void updateSnapshot(Object instance) throws IllegalAccessException {
        Map<String, Object> snapshot = snapshots.get(instance);
        if (snapshot == null) return;

        for (Field field : instance.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            // put current tracked values into the snapshot
            snapshot.put(field.getName(), field.get(instance));
        }
    }

    // Build a "match by example" AND-clause from non-null fields on the instance.
    // Only iterates fields that correspond to registered columns; stray POJO fields
    // are ignored, so adding helper fields to the class won't leak into SQL.
    String buildConditions(Object instance) throws IllegalAccessException {
        Class<?> clazz = instance.getClass();
        Table table = tables.get(clazz);
        if (table == null) throw new RuntimeException(
            clazz.getName() + " is not registered with the ORM"
        );

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Column column : table.columnsList.values()) {
            Field field;
            try {
                field = clazz.getDeclaredField(column.name);
            } catch (NoSuchFieldException e) {
                continue; // shouldn't happen — register() validates this
            }
            field.setAccessible(true);
            Object value = field.get(instance);
            if (value == null) continue;

            if (!first) sb.append(" AND ");
            sb.append(column.name).append(" = ").append(SQLFormat.literal(value));
            first = false;
        }

        return sb.toString();
    }

    // Entry points for the SELECT DSL.
    //   orm.select(instance)  -> match-by-example, OR additional instances via .or(other)
    //   orm.from(User.class)  -> blank slate, build conditions via .where(predicate)
    //
    // Both are generic so .populate() / .fetch() can return the registered type
    // without forcing the caller to cast.
    public <T> SelectBuilder<T> select(T instance) {
        return new SelectBuilder<>(this, instance);
    }

    public <T> SelectBuilder<T> from(Class<T> clazz) {
        return new SelectBuilder<>(this, clazz);
    }

    // =====================================================================
    // Mutations: permanent (INSERT or UPDATE), delete, bulk operations
    // =====================================================================

    /**
     * Persist an instance to the database (INSERT or UPDATE).
     * 
     * Semantics:
     *   - If a snapshot exists for this instance → UPDATE (only changed columns)
     *   - If no snapshot + PK is null → INSERT with auto-generated PK
     *   - If no snapshot + PK is non-null → INSERT with explicit PK
     * 
     * After INSERT, the instance is tracked (snapshot created).
     * After UPDATE, the snapshot is refreshed.
     * 
     * Example:
     *   User u = new User(); u.Name = "Alice"; u.Age = 25;
     *   orm.permanent(u);  // INSERT
     *   
     *   u.Age = 26;
     *   orm.permanent(u);  // UPDATE (only age changed)
     */
    public <T> T permanent(T instance) throws Exception {
        Class<?> clazz = instance.getClass();
        Table table = tables.get(clazz);
        if (table == null) {
            throw new RuntimeException(
                clazz.getName() + " is not registered with the ORM"
            );
        }

        Map<String, Object> snapshot = snapshots.get(instance);

        if (snapshot != null) {
            // UPDATE case: build a predicate matching the PK, then set changed columns
            if (table.primaryKey == null) {
                throw new RuntimeException(
                    "Table " + table.tableName + " has no primary key; cannot update by ID"
                );
            }

            Field pkField;
            try {
                pkField = clazz.getDeclaredField(table.primaryKey.column.name);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(
                    "Primary key field " + table.primaryKey.column.name + " not found", e
                );
            }
            pkField.setAccessible(true);
            Object pkValue = pkField.get(instance);

            if (pkValue == null) {
                throw new RuntimeException("Cannot update: primary key is null");
            }

            // Delegate to UpdateBuilder, only setting changed columns.
            // Cast is safe: clazz came from instance.getClass(), and instance is T.
            @SuppressWarnings("unchecked")
            Class<T> classTSafe = (Class<T>) clazz;
            UpdateBuilder<T> ub = update(classTSafe);
            boolean hasChanges = false;

            for (Column col : table.columnsList.values()) {
                Field field;
                try {
                    field = clazz.getDeclaredField(col.name);
                } catch (NoSuchFieldException e) {
                    continue;
                }
                field.setAccessible(true);
                Object currentValue = field.get(instance);
                Object snapshotValue = snapshot.get(col.name);

                // Skip if unchanged
                if (currentValue == null && snapshotValue == null) continue;
                if (currentValue != null && currentValue.equals(snapshotValue)) continue;

                ub.set(col.name, currentValue);
                hasChanges = true;
            }

            if (hasChanges) {
                ub.where(table.primaryKey.eq(pkValue)).execute();
            }

            // Refresh snapshot
            updateSnapshot(instance);
        } else {
            // INSERT case
            insertInstance(instance, table);
            // Create snapshot for future tracking
            Map<String, Object> newSnapshot = new HashMap<>();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                newSnapshot.put(field.getName(), field.get(instance));
            }
            snapshots.put(instance, newSnapshot);
        }

        return instance;
    }

    /**
     * Delete an instance from the database (requires a snapshot / tracked instance).
     * 
     * Example:
     *   User u = orm.select(...).fetch().get(0);
     *   orm.delete(u);  // DELETE WHERE id = u.id
     */
    public <T> void delete(T instance) throws Exception {
        Class<?> clazz = instance.getClass();
        Table table = tables.get(clazz);
        if (table == null) {
            throw new RuntimeException(
                clazz.getName() + " is not registered with the ORM"
            );
        }

        Map<String, Object> snapshot = snapshots.get(instance);
        if (snapshot == null) {
            throw new RuntimeException(
                "Cannot delete an untracked instance. " +
                "Instance must have been fetched via orm.select/fetch or explicitly tracked."
            );
        }

        if (table.primaryKey == null) {
            throw new RuntimeException(
                "Table " + table.tableName + " has no primary key; cannot delete by ID"
            );
        }

        // Get the PK value from the snapshot (the original, unmodified value)
        Field pkField;
        try {
            pkField = clazz.getDeclaredField(table.primaryKey.column.name);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(
                "Primary key field " + table.primaryKey.column.name + " not found on " + clazz.getName(), e
            );
        }
        pkField.setAccessible(true);
        Object pkValue = pkField.get(instance);

        if (pkValue == null) {
            throw new RuntimeException(
                "Cannot delete: primary key is null"
            );
        }

        // Delegate to DeleteBuilder: DELETE FROM ... WHERE pk = value
        deleteWhere(clazz).where(table.primaryKey.eq(pkValue)).execute();

        // Remove from snapshots
        snapshots.remove(instance);
    }

    /**
     * Entry point for bulk UPDATE statements.
     * 
     * Example:
     *   orm.update(User.class)
     *      .set("Status", "inactive")
     *      .where(Users.c("Age").below(18))
     *      .execute();
     */
    public <T> UpdateBuilder<T> update(Class<T> clazz) {
        return new UpdateBuilder<T>(this, clazz);
    }

    /**
     * Entry point for bulk DELETE statements.
     * 
     * Example:
     *   orm.delete(User.class)
     *      .where(Users.c("Status").eq("archived"))
     *      .execute();
     */
    public <T> DeleteBuilder<T> deleteWhere(Class<T> clazz) {
        return new DeleteBuilder<T>(this, clazz);
    }

    // =====================================================================
    // Private helpers: instance INSERT and UPDATE
    // =====================================================================

    /**
     * Generate and execute an INSERT statement for a new instance.
     */
    private <T> void insertInstance(T instance, Table table) throws Exception {
        Class<?> clazz = instance.getClass();
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();

        boolean first = true;
        for (Column col : table.columnsList.values()) {
            Field field;
            try {
                field = clazz.getDeclaredField(col.name);
            } catch (NoSuchFieldException e) {
                continue;
            }
            field.setAccessible(true);
            Object value = field.get(instance);
            if (value == null) continue; // Skip nulls in INSERT

            if (!first) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append(col.name);
            values.append(SQLFormat.literal(value));
            first = false;
        }

        String sql = "INSERT INTO " + table.tableName + " (" + columns + ") VALUES (" + values + ");";

        if (connection == null) {
            throw new RuntimeException("ORM is not connected to a database");
        }

        try (java.sql.Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql.substring(0, sql.length() - 1)); // Remove semicolon
        }
    }



    /**
     * Return the Table for a registered class (package-private for UpdateBuilder/DeleteBuilder).
     */
    Table getTableFor(Class<?> clazz) {
        return tables.get(clazz);
    }
}
