package src.sql_in_java;
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
    public SelectBuilder select(Object instance) {
        return new SelectBuilder(this, instance);
    }

    public SelectBuilder from(Class<?> clazz) {
        return new SelectBuilder(this, clazz);
    }
}
