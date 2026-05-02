import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A composable SQL boolean expression.
 *
 * Predicates are produced by ColumnReference operator methods
 * (eq, gt, like, between, in, isNull, ...) and can be combined with
 * .and(), .or(), .not(), or the static helpers all(), any(), not().
 */
public interface Predicate {
    String toSQL();

    default Predicate and(Predicate other) {
        return new AndPredicate(this, other);
    }

    default Predicate or(Predicate other) {
        return new OrPredicate(this, other);
    }

    default Predicate negate() {
        return new NotPredicate(this);
    }

    // English-style static helpers
    //   where(all(p1, p2, p3))   -> "(p1 AND p2 AND p3)"
    //   where(any(p1, p2, p3))   -> "(p1 OR p2 OR p3)"
    //   where(not(p1))           -> "NOT (p1)"
    static Predicate all(Predicate... predicates) {
        return new AndPredicate(predicates);
    }

    static Predicate any(Predicate... predicates) {
        return new OrPredicate(predicates);
    }

    static Predicate not(Predicate p) {
        return new NotPredicate(p);
    }
}


// -------- Boolean combinators --------

class AndPredicate implements Predicate {
    List<Predicate> parts = new ArrayList<>();

    AndPredicate(Predicate... ps) {
        for (Predicate p : ps) addFlat(p);
    }

    private void addFlat(Predicate p) {
        // flatten nested ANDs to keep SQL output clean
        if (p instanceof AndPredicate) parts.addAll(((AndPredicate) p).parts);
        else parts.add(p);
    }

    @Override
    public Predicate and(Predicate other) {
        addFlat(other);
        return this;
    }

    public String toSQL() {
        if (parts.size() == 1) return parts.get(0).toSQL();
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(" AND ");
            sb.append(parts.get(i).toSQL());
        }
        return sb.append(")").toString();
    }
}

class OrPredicate implements Predicate {
    List<Predicate> parts = new ArrayList<>();

    OrPredicate(Predicate... ps) {
        for (Predicate p : ps) addFlat(p);
    }

    private void addFlat(Predicate p) {
        if (p instanceof OrPredicate) parts.addAll(((OrPredicate) p).parts);
        else parts.add(p);
    }

    @Override
    public Predicate or(Predicate other) {
        addFlat(other);
        return this;
    }

    public String toSQL() {
        if (parts.size() == 1) return parts.get(0).toSQL();
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(" OR ");
            sb.append(parts.get(i).toSQL());
        }
        return sb.append(")").toString();
    }
}

class NotPredicate implements Predicate {
    Predicate inner;
    NotPredicate(Predicate p) { this.inner = p; }
    public String toSQL() { return "NOT (" + inner.toSQL() + ")"; }
}


// -------- Comparison / leaf predicates --------

class ComparisonPredicate implements Predicate {
    ColumnReference column;
    String operator;
    Object value;
    boolean valueIsColumn;

    ComparisonPredicate(ColumnReference column, String operator, Object value) {
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.valueIsColumn = false;
    }

    ComparisonPredicate(ColumnReference column, String operator, ColumnReference other) {
        this.column = column;
        this.operator = operator;
        this.value = other;
        this.valueIsColumn = true;
    }

    public String toSQL() {
        String rhs = valueIsColumn
            ? ((ColumnReference) value).column.name
            : SQLFormat.literal(value);
        return column.column.name + " " + operator + " " + rhs;
    }
}

class BetweenPredicate implements Predicate {
    ColumnReference column;
    Object low, high;
    boolean negated;

    BetweenPredicate(ColumnReference c, Object low, Object high, boolean negated) {
        this.column = c;
        this.low = low;
        this.high = high;
        this.negated = negated;
    }

    public String toSQL() {
        return column.column.name
            + (negated ? " NOT BETWEEN " : " BETWEEN ")
            + SQLFormat.literal(low) + " AND " + SQLFormat.literal(high);
    }
}

class InPredicate implements Predicate {
    ColumnReference column;
    List<?> values;
    boolean negated;

    InPredicate(ColumnReference c, List<?> values, boolean negated) {
        this.column = c;
        this.values = values;
        this.negated = negated;
    }

    public String toSQL() {
        StringBuilder sb = new StringBuilder(column.column.name);
        sb.append(negated ? " NOT IN (" : " IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(SQLFormat.literal(values.get(i)));
        }
        return sb.append(")").toString();
    }
}

class LikePredicate implements Predicate {
    ColumnReference column;
    String pattern;
    boolean negated;

    LikePredicate(ColumnReference c, String pattern, boolean negated) {
        this.column = c;
        this.pattern = pattern;
        this.negated = negated;
    }

    public String toSQL() {
        return column.column.name
            + (negated ? " NOT LIKE " : " LIKE ")
            + SQLFormat.literal(pattern);
    }
}

class NullPredicate implements Predicate {
    ColumnReference column;
    boolean negated;

    NullPredicate(ColumnReference c, boolean negated) {
        this.column = c;
        this.negated = negated;
    }

    public String toSQL() {
        return column.column.name + (negated ? " IS NOT NULL" : " IS NULL");
    }
}


// -------- ORDER BY clause --------

class OrderByClause {
    ColumnReference column;
    boolean ascending;

    OrderByClause(ColumnReference column, boolean ascending) {
        this.column = column;
        this.ascending = ascending;
    }

    String toSQL() {
        return column.column.name + (ascending ? " ASC" : " DESC");
    }
}


// -------- Shared SQL value formatting --------

class SQLFormat {
    static String literal(Object value) {
        if (value == null)            return "NULL";
        if (value instanceof String)  return "'" + ((String) value).replace("'", "''") + "'";
        if (value instanceof Boolean) return ((Boolean) value) ? "TRUE" : "FALSE";
        return value.toString(); // numbers, dates (via toString), etc.
    }
}
