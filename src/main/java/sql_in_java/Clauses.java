package sql_in_java;

public class Clauses {
    public static class OrderByClause {
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
}
