import java.lang.annotation.*;
import java.lang.reflect.Field;

// collection
import java.util.*;

public class ORM {
    Map<Class<?>, Table> tables = new HashMap<>();
    Map<Class<?>, Field> primaryKeys = new HashMap<>();

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


    // takes in an object, generates a corresponding string
    private String formatValue(Object value) {
        if (value == null)             return "NULL";
        if (value instanceof String)   return "'" + value + "'";
        if (value instanceof Boolean)  return (Boolean) value ? "TRUE" : "FALSE";
        return value.toString();       // Integer, Double etc
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

    // looks at the non-null fields, and then generate AND clause correspondingly
    String buildConditions(Object instance) throws IllegalAccessException {
        StringBuilder sb = new StringBuilder();

        List<Field> setFields = new ArrayList<>();
        for (Field field : instance.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.get(instance) != null) {
                setFields.add(field);
            }
        }

        Iterator<Field> it = setFields.iterator();
        while (it.hasNext()) {
            Field field = it.next();
            sb.append(field.getName())
            .append(" = ")
            .append(formatValue(field.get(instance)));
            if (it.hasNext()) sb.append(" AND ");
        }

        return sb.toString();
    }

    public SelectBuilder select(Object instance) {
        return new SelectBuilder(this, instance);
    }
}