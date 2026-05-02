package src.sql_in_java;

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
 */
public class SelectBuilder {
    ORM orm;
    Class<?> clazz;
    Table table;

    // by-example: each instance contributes one OR'd block of equality conditions
    List<Object> instances = new ArrayList<>();

    // explicit projection; null means SELECT *
    List<ColumnReference> projection = null;
    boolean distinct = false;

    Predicate wherePredicate = null;
    List<ColumnReference> groupBy = new ArrayList<>();
    Predicate havingPredicate = null;
    List<OrderByClause> orderBy = new ArrayList<>();
    Integer limit = null;
    Integer offset = null;

    SelectBuilder(ORM orm, Class<?> clazz) {
        this.orm = orm;
        this.clazz = clazz;
        this.table = orm.tables.get(clazz);
        if (this.table == null) throw new RuntimeException(
            clazz.getName() + " is not registered with the ORM"
        );
    }

    SelectBuilder(ORM orm, Object instance) {
        this(orm, instance.getClass());
        this.instances.add(instance);
    }


    // ---------- projection ----------

    // .pick(Users.c("Name"), Users.c("Age"))  ->  SELECT Name, Age
    public SelectBuilder pick(ColumnReference... cols) {
        this.projection = Arrays.asList(cols);
        return this;
    }

    public SelectBuilder distinct() {
        this.distinct = true;
        return this;
    }


    // ---------- WHERE ----------

    // by-example: add another instance whose equality block is OR'd with the others
    public SelectBuilder or(Object instance) {
        if (!clazz.isInstance(instance)) throw new RuntimeException(
            "Instance of " + instance.getClass().getName() +
            " does not match query class " + clazz.getName()
        );
        instances.add(instance);
        return this;
    }

    public SelectBuilder where(Predicate p) {
        this.wherePredicate = p;
        return this;
    }

    // chain predicates without nesting at the call site
    public SelectBuilder and(Predicate p) {
        this.wherePredicate = (this.wherePredicate == null) ? p : this.wherePredicate.and(p);
        return this;
    }

    public SelectBuilder or(Predicate p) {
        this.wherePredicate = (this.wherePredicate == null) ? p : this.wherePredicate.or(p);
        return this;
    }


    // ---------- GROUP BY / HAVING ----------

    public SelectBuilder groupBy(ColumnReference... cols) {
        this.groupBy.addAll(Arrays.asList(cols));
        return this;
    }

    public SelectBuilder having(Predicate p) {
        this.havingPredicate = p;
        return this;
    }


    // ---------- ORDER BY ----------

    // explicit direction:  .orderBy(Users.c("Age").desc(), Users.c("Name").asc())
    public SelectBuilder orderBy(OrderByClause... clauses) {
        this.orderBy.addAll(Arrays.asList(clauses));
        return this;
    }

    // bare columns default to ASC:  .orderBy(Users.c("Age"))
    public SelectBuilder orderBy(ColumnReference... cols) {
        for (ColumnReference c : cols) this.orderBy.add(c.asc());
        return this;
    }


    // ---------- LIMIT / OFFSET ----------

    public SelectBuilder limit(int n)  { this.limit = n; return this; }
    public SelectBuilder offset(int n) { this.offset = n; return this; }

    // English aliases — read more naturally in chains
    public SelectBuilder top(int n)  { return limit(n); }
    public SelectBuilder skip(int n) { return offset(n); }


    // ---------- execute ----------

    public List<Object> fetch() throws IllegalAccessException {
        String sql = generateSQL();
        System.out.println(sql); // TODO: hand off to JDBC + map rows back to objects
        return new ArrayList<>();
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
}
