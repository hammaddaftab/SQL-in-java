package src.sql_in_java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import src.sql_in_java.Clauses.*;

/**
 * Fluent builder for SELECT statements.
 *
 * Two ways to start:
 *   orm.from(User.class)        — blank slate, use .where(predicate)
 *   orm.select(userInstance)    — "match by example": non-null fields become equality
 *                                 conditions; .or(other) ORs another example block
 *
 * Both modes can be combined: by-example conditions are AND-ed with the
 * explicit predicate, so you can write "this template AND age > 18".
 *
 * Three terminal operations:
 *   .generateSQL()   — returns the SQL string (no DB needed)
 *   .populate()      — by-example only: runs the query, expects exactly one row,
 *                      fills in the example instance's null fields, returns it
 *   .fetch()         — runs the query, returns one new instance per row
 */
public class SelectBuilder<T> {
    ORM orm;
    Class<T> clazz;
    Table table;

    // by-example: each instance contributes one OR'd block of equality conditions
    List<T> instances = new ArrayList<>();

    // explicit projection; null means SELECT *
    List<ColumnReference> projection = null;
    boolean distinct = false;

    Predicate wherePredicate = null;
    List<ColumnReference> groupBy = new ArrayList<>();
    Predicate havingPredicate = null;
    List<OrderByClause> orderBy = new ArrayList<>();
    Integer limit = null;
    Integer offset = null;

    SelectBuilder(ORM orm, Class<T> clazz) {
        this.orm = orm;
        this.clazz = clazz;
        this.table = orm.tables.get(clazz);
        if (this.table == null) throw new RuntimeException(
            clazz.getName() + " is not registered with the ORM"
        );
    }

    @SuppressWarnings("unchecked")
    SelectBuilder(ORM orm, T instance) {
        this(orm, (Class<T>) instance.getClass());
        this.instances.add(instance);
    }


    // ---------- projection ----------

    // .pick(Users.c("Name"), Users.c("Age"))  ->  SELECT Name, Age
    public SelectBuilder<T> pick(ColumnReference... cols) {
        this.projection = Arrays.asList(cols);
        return this;
    }

    public SelectBuilder<T> distinct() {
        this.distinct = true;
        return this;
    }


    // ---------- WHERE ----------

    // by-example: add another instance whose equality block is OR'd with the others
    public SelectBuilder<T> or(T instance) {
        if (!clazz.isInstance(instance)) throw new RuntimeException(
            "Instance of " + instance.getClass().getName() +
            " does not match query class " + clazz.getName()
        );
        instances.add(instance);
        return this;
    }

    public SelectBuilder<T> where(Predicate p) {
        this.wherePredicate = p;
        return this;
    }

    // chain predicates without nesting at the call site
    public SelectBuilder<T> and(Predicate p) {
        this.wherePredicate = (this.wherePredicate == null) ? p : this.wherePredicate.and(p);
        return this;
    }

    public SelectBuilder<T> or(Predicate p) {
        this.wherePredicate = (this.wherePredicate == null) ? p : this.wherePredicate.or(p);
        return this;
    }


    // ---------- GROUP BY / HAVING ----------

    public SelectBuilder<T> groupBy(ColumnReference... cols) {
        this.groupBy.addAll(Arrays.asList(cols));
        return this;
    }

    public SelectBuilder<T> having(Predicate p) {
        this.havingPredicate = p;
        return this;
    }


    // ---------- ORDER BY ----------

    // explicit direction:  .orderBy(Users.c("Age").desc(), Users.c("Name").asc())
    public SelectBuilder<T> orderBy(OrderByClause... clauses) {
        this.orderBy.addAll(Arrays.asList(clauses));
        return this;
    }

    // bare columns default to ASC:  .orderBy(Users.c("Age"))
    public SelectBuilder<T> orderBy(ColumnReference... cols) {
        for (ColumnReference c : cols) this.orderBy.add(c.asc());
        return this;
    }


    // ---------- LIMIT / OFFSET ----------

    public SelectBuilder<T> limit(int n)  { this.limit = n; return this; }
    public SelectBuilder<T> offset(int n) { this.offset = n; return this; }

    // English aliases — read more naturally in chains
    public SelectBuilder<T> top(int n)  { return limit(n); }
    public SelectBuilder<T> skip(int n) { return offset(n); }


    // =====================================================================
    // Execution
    //
    // generateSQL() never touches the network. populate() and fetch() require
    // orm.connect(...) to have been called.
    //
    // Queries are executed verbatim via Statement (no '?' placeholders);
    // string literals are already escaped via SQLFormat.literal in toSQL().
    // =====================================================================

    /**
     * For a by-example query like:
     *
     *   User u = new User(); u.Id = 32;
     *   orm.select(u).populate();
     *
     * Runs the generated SQL, expects exactly one row, and fills in the
     * remaining fields on `u` (in place). Returns the same instance for chaining.
     *
     * Throws if the example didn't include any conditions (would match every row),
     * if no rows match, or if more than one row matches.
     */
    public T populate() throws SQLException, IllegalAccessException {
        if (instances.size() != 1) {
            throw new RuntimeException(
                "populate() requires exactly one example instance — got " + instances.size() +
                ". Use fetch() to read multiple rows."
            );
        }

        T instance = instances.get(0);
        String sql = generateSQL();

        // Guard against accidental "fill from any row" — if the example had every
        // field null, the WHERE clause is empty and we'd populate from the first
        // arbitrary row. That's almost never what the caller meant.
        if (wherePredicate == null && orm.buildConditions(instance).isEmpty()) {
            throw new RuntimeException(
                "populate() called on an instance with no fields set — would match every row. " +
                "Set at least the primary key (or call fetch() instead)."
            );
        }

        Connection conn = orm.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.next()) {
                throw new RuntimeException("populate() found no row matching: " + sql);
            }

            mapRowInto(rs, instance);

            if (rs.next()) {
                throw new RuntimeException(
                    "populate() expected exactly one row, but the query returned more. SQL: " + sql
                );
            }

            return instance;
        }
    }

    /**
     * Run the query and return one fresh instance per row.
     *
     * Works for both entry points:
     *   orm.from(User.class).where(...).fetch()    — predicate-driven read
     *   orm.select(template).fetch()                — match-by-example read
     *
     * The class must have an accessible no-arg constructor.
     */
    public List<T> fetch() throws SQLException, IllegalAccessException {
        String sql = generateSQL();
        Connection conn = orm.getConnection();

        List<T> results = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                T row = newInstance();
                mapRowInto(rs, row);
                results.add(row);
            }
        }
        return results;
    }

    public String generateSQL() throws IllegalAccessException {
        StringBuilder sb = new StringBuilder();

        // SELECT
        sb.append("SELECT ");
        if (distinct) sb.append("DISTINCT ");
        if (projection == null || projection.isEmpty()) {
            sb.append("*");
        } else {
            for (int i = 0; i < projection.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(projection.get(i).column.name);
            }
        }

        // FROM
        sb.append(" FROM ").append(table.tableName);

        // WHERE
        String where = buildWhereSQL();
        if (!where.isEmpty()) sb.append(" WHERE ").append(where);

        // GROUP BY
        if (!groupBy.isEmpty()) {
            sb.append(" GROUP BY ");
            for (int i = 0; i < groupBy.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(groupBy.get(i).column.name);
            }
        }

        // HAVING
        if (havingPredicate != null) {
            sb.append(" HAVING ").append(havingPredicate.toSQL());
        }

        // ORDER BY
        if (!orderBy.isEmpty()) {
            sb.append(" ORDER BY ");
            for (int i = 0; i < orderBy.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(orderBy.get(i).toSQL());
            }
        }

        // LIMIT / OFFSET
        if (limit != null)  sb.append(" LIMIT ").append(limit);
        if (offset != null) sb.append(" OFFSET ").append(offset);

        return sb.append(";").toString();
    }

    // Combine by-example OR-blocks with the explicit predicate using AND.
    private String buildWhereSQL() throws IllegalAccessException {
        String byExample = null;
        if (!instances.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Object instance : instances) {
                String c = orm.buildConditions(instance);
                if (c.isEmpty()) continue;
                if (sb.length() > 0) sb.append(" OR ");
                sb.append("(").append(c).append(")");
            }
            if (sb.length() > 0) byExample = sb.toString();
        }

        String predicate = (wherePredicate != null) ? wherePredicate.toSQL() : null;

        if (byExample != null && predicate != null) return "(" + byExample + ") AND " + predicate;
        if (byExample != null) return byExample;
        if (predicate != null) return predicate;
        return "";
    }


    // ---------- row mapping (JDBC -> POJO) ----------

    private T newInstance() {
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                "Class " + clazz.getName() + " must declare a no-arg constructor for fetch()", e
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate " + clazz.getName(), e);
        }
    }

    // Walks the result set's metadata so projection-narrowed queries only set
    // the columns that actually came back. Columns without a matching field
    // (rare — register() validates the mapping) are silently skipped.
    private void mapRowInto(ResultSet rs, T instance) throws SQLException, IllegalAccessException {
        ResultSetMetaData meta = rs.getMetaData();
        int n = meta.getColumnCount();
        for (int i = 1; i <= n; i++) {
            String colName = meta.getColumnLabel(i);
            Object raw = rs.getObject(i);
            try {
                Field field = clazz.getDeclaredField(colName);
                field.setAccessible(true);
                field.set(instance, coerce(raw, field.getType()));
            } catch (NoSuchFieldException ignored) {
                // column not modelled on the POJO — skip
            }
        }
    }

    // MySQL returns BigDecimal for DECIMAL, Long for BIGINT, etc. The POJO field
    // is usually a plain Integer / Double. This narrows numerics to the field's
    // declared type so reflection's strict type check passes.
    private static Object coerce(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;
        if (value instanceof Number) {
            Number num = (Number) value;
            if (targetType == Integer.class || targetType == int.class)   return num.intValue();
            if (targetType == Long.class    || targetType == long.class)  return num.longValue();
            if (targetType == Double.class  || targetType == double.class) return num.doubleValue();
            if (targetType == Float.class   || targetType == float.class) return num.floatValue();
            if (targetType == Short.class   || targetType == short.class) return num.shortValue();
            if (targetType == Byte.class    || targetType == byte.class)  return num.byteValue();
        }
        return value; // let the reflection setter raise a clear error if still mismatched
    }
}
