package sql_in_java;

import java.sql.Timestamp;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Table {
    String nextLine = "\n"; // for readability
    String indent = "    "; // for readability
    String tableName;
    String activeColumnForChaining;
    Map<String, Column> columnsList = new LinkedHashMap<>();
    Map<String, ColumnReference> foreignKeys = new LinkedHashMap<>(); 
    ColumnReference primaryKey = null;

    public Table(String tableName) {
        this.tableName = tableName;
    }

    public Table has(String columnName) {
        activeColumnForChaining = columnName;
        return this;
    }

    public Table asString(int size) {
        columnsList.put(activeColumnForChaining, new Column(
            activeColumnForChaining,
            ParameterizedColumnType.VARCHAR,
            new SizeParameter(size)
        ));
        return this;
    }

    public Table asDecimal(int precision, int scale) {
        columnsList.put(activeColumnForChaining, new Column(
            activeColumnForChaining,
            ParameterizedColumnType.DECIMAL,
            new PrecisionScaleParameter(precision, scale)
        ));
        return this;
    }

    public Table asInt() {
        columnsList.put(activeColumnForChaining, new Column(
            activeColumnForChaining, 
            NonParameterizedColumnType.INTEGER
        ));
        return this;
    }

    public Table refers(ColumnReference target) {
        foreignKeys.put(target.column.name, target);
        if (target.column.columnType instanceof NonParameterizedColumnType) {
            NonParameterizedColumnType columnType = (NonParameterizedColumnType) target.column.columnType;
            this.columnsList.put(
                target.column.name,
                new Column(
                    target.column.name, 
                    columnType
                )
            );
        } else {
            ParameterizedColumnType columnType = (ParameterizedColumnType) target.column.columnType;
            this.columnsList.put(
                target.column.name,
                new Column(
                    target.column.name, 
                    columnType,
                    target.column.parameters

                )
            );
        }
        return this;
    }

    public ColumnReference c(String columnName) {
        Column column = columnsList.get(columnName);
        if (column == null) throw new RuntimeException(
            "Column " + columnName + " not found in " + tableName + ""
        );
        return new ColumnReference(this, column);
    }

    // delegate is to ColumnReference Object for the active column for inline constaint declaration
    public Table is(Constraint constraint) {
        return this.c(activeColumnForChaining).is(constraint);
    }

    public String toSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName).append(" (").append(nextLine);

        // columns — all but track if FK lines follow for trailing comma
        List<Column> cols = new ArrayList<>(columnsList.values());
        boolean hasFKs = !foreignKeys.isEmpty();

        for (int i = 0; i < cols.size(); i++) {
            boolean isLastColumn = (i == cols.size() - 1);
            sb.append(indent)
            .append(cols.get(i).toSQL())
            .append(isLastColumn && !hasFKs ? "" : ",")
            .append(nextLine);
        }

        // foreign key constraints
        List<Map.Entry<String, ColumnReference>> fkList = new ArrayList<>(foreignKeys.entrySet());
        for (int i = 0; i < fkList.size(); i++) {
            boolean isLastFK = (i == fkList.size() - 1);
            String localCol  = fkList.get(i).getKey();
            ColumnReference target = fkList.get(i).getValue();

            sb.append(indent)
            .append("FOREIGN KEY (").append(localCol).append(")")
            .append(" REFERENCES ").append(target.table.tableName)
            .append("(").append(target.column.name).append(")")
            .append(isLastFK ? "" : ",")
            .append(nextLine);
        }

        sb.append(");");
        return sb.toString();
    }
}



class Column {
    String name;
    ColumnType columnType;
    ColumnTypeParameters parameters;
    Constraints constraints = new Constraints();

    Column(String name, NonParameterizedColumnType columnType) {
        this.name = name;
        this.columnType = columnType;
    }

    Column(String name, ParameterizedColumnType columnType, ColumnTypeParameters parameters) {
        this.name = name;
        this.columnType = columnType;
        this.parameters = parameters;
    }

    public String toSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.name).append(" ");

        if (this.columnType instanceof ParameterizedColumnType) {
            sb.append(this.columnType.getSQLType()).append(parameters.toSQL());
        } else {
            sb.append(this.columnType.getSQLType());
        }

        for (Constraint constraint : this.constraints) {
            sb.append(" ").append(constraint.toSQL());
        }

        return sb.toString();
    }
}



interface ColumnType {
    String getSQLType();
    Class<?> getJavaType();
}

// Ignoring BINARY, BLOB and different sized INTEGERs
// DONE: improve it so the instances don't contain unnecessary varaibles
// By creating two enum types, Parametereiz and NonParametereized, and using
// composition, the ones which need params are passed a Parameter object instead
// of all tyeps in a single ColumnType and storing all possible parameters
// Changes: 1) enum ColumnType -> interface ColumnType
// enum ColumnType -> 1) ParamtereizedColumnType & NonParameterizedColumnType
enum ParameterizedColumnType implements ColumnType {
    VARCHAR(String.class, "VARCHAR"),
    DECIMAL(Double.class, "DECIMAL");

    
    Class<?> type;
    String SQLType;

    ParameterizedColumnType(Class<?> type, String SQLType) {
        this.type = type;
        this.SQLType = SQLType;
    }

    public String getSQLType() {
        return this.SQLType;
    }

    public Class<?> getJavaType() {
        return this.type;
    }
}

enum NonParameterizedColumnType implements ColumnType {
    CHAR(String.class, "CHAR"),
    
    INTEGER(Integer.class, "INTEGER"),

    // TODO: learn what java.sql.* provides and their difference from java.util.*
    DATE(Date.class, "DATE"),
    TIME(Time.class, "TIME"),
    // TODO: implement this somehow
    // DATETIME(Date),

    TIMESTAMP(Timestamp.class, "TIMESTAMP"),

    BOOLEAN(Boolean.class, "BOOLEAN");


    Class<?> type;
    String SQLType;

    NonParameterizedColumnType(Class<?> type, String SQLType) {
        this.type = type;
        this.SQLType = SQLType;
    }

    public String getSQLType() {
        return this.SQLType;
    }

    public Class<?> getJavaType() {
        return this.type;
    }
}



// ColumnTypeParameters composed inside ColumnType to represent
// the parameters of the ColumnType
interface ColumnTypeParameters {
    String toSQL();
}

class SizeParameter implements ColumnTypeParameters {
    int size;
    SizeParameter(int size) { this.size = size; }
    public String toSQL() { return "(" + size + ")"; }
}

class PrecisionScaleParameter implements ColumnTypeParameters {
    int precision, scale;
    PrecisionScaleParameter(int precision, int scale) {
        this.precision = precision;
        this.scale = scale;
    }
    public String toSQL() { return "(" + precision + ", " + scale + ")"; }
}
