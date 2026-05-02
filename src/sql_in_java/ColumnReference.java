package src.sql_in_java;

import java.util.*;
import src.sql_in_java.Clauses.*;

public class ColumnReference {
    Table table;
    Column column;

    ColumnReference(Table table, Column column) {
        this.table = table;
        this.column = column;
    }

    public Table is(Constraint constraint) {
        this.column.constraints.add(constraint);

        if (constraint == Constraint.PRIMARYKEY) {
            if (this.table.primaryKey != null) {
                throw new RuntimeException(
                    "Table " + this.table.tableName + 
                    " already has a primary key on column " + 
                    this.table.primaryKey.column.name
                );
            }
            this.table.primaryKey = this;
        }

        return this.table;
    }

    public Table is(Constraints constraints) {
        this.column.constraints.addAll(constraints);

        for (Constraint constraint : constraints) {
            if (constraint == Constraint.PRIMARYKEY) {
                if (this.table.primaryKey != null) {
                    throw new RuntimeException(
                        "Table " + this.table.tableName + 
                        " already has a primary key on column " + 
                        this.table.primaryKey.column.name
                    );
                }
                this.table.primaryKey = this;
            }
        }

        return this.table;
    }


    // =====================================================================
    // Predicate builders — produce composable Predicate objects for WHERE,
    // HAVING, etc. Read like English at the call site:
    //
    //   Users.c("Age").above(18)
    //   Users.c("Name").like("J%").and(Users.c("Status").eq("active"))
    //   Users.c("DeptId").in(1, 2, 3)
    //   Users.c("Balance").between(100, 500)
    //   Users.c("DeletedAt").isNull()
    // =====================================================================

    // null-aware: eq(null) becomes IS NULL, ne(null) becomes IS NOT NULL
    public Predicate eq(Object value)  { return value == null ? isNull()    : new ComparisonPredicate(this, "=",  value); }
    public Predicate ne(Object value)  { return value == null ? isNotNull() : new ComparisonPredicate(this, "<>", value); }
    public Predicate gt(Object value)  { return new ComparisonPredicate(this, ">",  value); }
    public Predicate lt(Object value)  { return new ComparisonPredicate(this, "<",  value); }
    public Predicate gte(Object value) { return new ComparisonPredicate(this, ">=", value); }
    public Predicate lte(Object value) { return new ComparisonPredicate(this, "<=", value); }

    // column-to-column comparisons (for joins / cross-row predicates)
    public Predicate eq(ColumnReference o)  { return new ComparisonPredicate(this, "=",  o); }
    public Predicate ne(ColumnReference o)  { return new ComparisonPredicate(this, "<>", o); }
    public Predicate gt(ColumnReference o)  { return new ComparisonPredicate(this, ">",  o); }
    public Predicate lt(ColumnReference o)  { return new ComparisonPredicate(this, "<",  o); }
    public Predicate gte(ColumnReference o) { return new ComparisonPredicate(this, ">=", o); }
    public Predicate lte(ColumnReference o) { return new ComparisonPredicate(this, "<=", o); }

    // English-language aliases — pick whichever reads best at the call site
    public Predicate above(Object v)   { return gt(v); }
    public Predicate below(Object v)   { return lt(v); }
    public Predicate atLeast(Object v) { return gte(v); }
    public Predicate atMost(Object v)  { return lte(v); }

    public Predicate like(String pattern)    { return new LikePredicate(this, pattern, false); }
    public Predicate notLike(String pattern) { return new LikePredicate(this, pattern, true);  }

    public Predicate between(Object low, Object high)    { return new BetweenPredicate(this, low, high, false); }
    public Predicate notBetween(Object low, Object high) { return new BetweenPredicate(this, low, high, true);  }

    public Predicate in(Object... values)    { return new InPredicate(this, Arrays.asList(values), false); }
    public Predicate notIn(Object... values) { return new InPredicate(this, Arrays.asList(values), true);  }
    public Predicate in(List<?> values)      { return new InPredicate(this, values, false); }
    public Predicate notIn(List<?> values)   { return new InPredicate(this, values, true);  }

    public Predicate isNull()    { return new NullPredicate(this, false); }
    public Predicate isNotNull() { return new NullPredicate(this, true);  }

    // ORDER BY direction
    public OrderByClause asc()  { return new OrderByClause(this, true);  }
    public OrderByClause desc() { return new OrderByClause(this, false); }
}