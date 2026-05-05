package src.sql_in_java;

import java.sql.Statement;

/**
 * Fluent builder for bulk DELETE statements.
 * 
 * Usage:
 *   orm.delete(User.class)
 *      .where(Users.c("Status").eq("archived"))
 *      .execute();
 */
public class DeleteBuilder<T> {
    private ORM orm;
    private Class<T> entityClass;
    private Table table;
    private Predicate wherePredicate = null;

    public DeleteBuilder(ORM orm, Class<T> entityClass) {
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
     * Add a WHERE predicate.
     */
    public DeleteBuilder<T> where(Predicate predicate) {
        if (predicate == null) {
            throw new RuntimeException("WHERE predicate cannot be null");
        }
        this.wherePredicate = predicate;
        return this;
    }

    /**
     * Refine the WHERE with AND.
     */
    public DeleteBuilder<T> and(Predicate predicate) {
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
    public DeleteBuilder<T> or(Predicate predicate) {
        if (wherePredicate == null) {
            this.wherePredicate = predicate;
        } else {
            this.wherePredicate = Predicate.any(wherePredicate, predicate);
        }
        return this;
    }

    /**
     * Generate the DELETE SQL statement (for inspection / debugging).
     */
    public String generateSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(table.tableName);

        if (wherePredicate != null) {
            sb.append(" WHERE ").append(wherePredicate.toSQL());
        }

        sb.append(";");
        return sb.toString();
    }

    /**
     * Execute the DELETE and return the number of rows affected.
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
}
