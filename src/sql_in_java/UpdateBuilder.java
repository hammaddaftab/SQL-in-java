package src.sql_in_java;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for bulk UPDATE statements.
 * 
 * Usage:
 *   orm.update(User.class)
 *      .set("Status", "inactive")
 *      .set("UpdatedAt", now)
 *      .where(Users.c("Age").below(18))
 *      .execute();
 */
public class UpdateBuilder<T> {
    private ORM orm;
    private Class<T> entityClass;
    private Table table;
    private List<SetClause> setClauses = new ArrayList<>();
    private Predicate wherePredicate = null;

    public UpdateBuilder(ORM orm, Class<T> entityClass) {
        this.orm = orm;
        this.entityClass = entityClass;
        this.table = orm.getTableFor(entityClass);
        if (this.table == null) {
            throw new RuntimeException(
                entityClass.getName() + " is not registered with the ORM"
            );
        }
    }

    /**
     * Set a column to a value (by string column name).
     * For bulk updates where you're not working with an instance.
     */
    public UpdateBuilder<T> set(String columnName, Object value) {
        setClauses.add(new SetClause(columnName, value));
        return this;
    }

    /**
     * Add a WHERE predicate.
     */
    public UpdateBuilder<T> where(Predicate predicate) {
        if (predicate == null) {
            throw new RuntimeException("WHERE predicate cannot be null");
        }
        this.wherePredicate = predicate;
        return this;
    }

    /**
     * Refine the WHERE with AND.
     */
    public UpdateBuilder<T> and(Predicate predicate) {
        if (wherePredicate == null) {
            this.wherePredicate = predicate;
        } else {
            this.wherePredicate = Predicate.all(wherePredicate, predicate);
        }
        return this;
    }

    /**
     * Refine the WHERE with OR.
     */
    public UpdateBuilder<T> or(Predicate predicate) {
        if (wherePredicate == null) {
            this.wherePredicate = predicate;
        } else {
            this.wherePredicate = Predicate.any(wherePredicate, predicate);
        }
        return this;
    }

    /**
     * Generate the UPDATE SQL statement (for inspection / debugging).
     */
    public String generateSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(table.tableName).append(" SET ");

        for (int i = 0; i < setClauses.size(); i++) {
            if (i > 0) sb.append(", ");
            SetClause sc = setClauses.get(i);
            sb.append(sc.columnName).append(" = ").append(SQLFormat.literal(sc.value));
        }

        if (wherePredicate != null) {
            sb.append(" WHERE ").append(wherePredicate.toSQL());
        }

        sb.append(";");
        return sb.toString();
    }

    /**
     * Execute the UPDATE and return the number of rows affected.
     */
    public int execute() throws Exception {
        String sql = generateSQL();
        
        // Remove trailing semicolon for JDBC
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }

        if (orm.getConnection() == null) {
            throw new RuntimeException("ORM is not connected to a database");
        }

        try (Statement stmt = orm.getConnection().createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    /**
     * Helper class to hold SET column = value pairs.
     */
    private static class SetClause {
        String columnName;
        Object value;

        SetClause(String columnName, Object value) {
            this.columnName = columnName;
            this.value = value;
        }
    }
}
