import java.util.ArrayList;
import java.util.*;

class SelectBuilder {
    // in case if we want to select multiple instances using orm.select(u1).and(u2)
    List<Object> instances = new ArrayList<>();
    ORM orm;

    SelectBuilder(ORM orm, Object first) {
        this.orm = orm;
        this.instances.add(first);
    }

    SelectBuilder and(Object instance) {
        instances.add(instance);
        return this;
    }

    List<Object> fetch() throws IllegalAccessException {
        String sql = generateSQL();
        // TODO: the things in comments
        System.out.println(sql); // talk to JDBC somehow
        return new ArrayList<>(); // mappig of result rows returned to java objects
    }

    String generateSQL() throws IllegalAccessException {
        // all instances must be the same class
        Class<?> clazz = instances.get(0).getClass();
        Table table = orm.tables.get(clazz.getName());

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(table.tableName).append(" WHERE ");

        // each instance becomes one OR block
        Iterator<Object> it = instances.iterator();
        while (it.hasNext()) {
            Object instance = it.next();
            sb.append("(");
            sb.append(orm.buildConditions(instance));
            sb.append(")");
            if (it.hasNext()) sb.append(" OR ");
        }

        sb.append(";");
        return sb.toString();
    }
}