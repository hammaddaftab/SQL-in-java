package sql_in_java.tests;

import sql_in_java.*;

/**
 * Test for orm.createSchema() — generates DDL for all registered tables.
 * 
 * Demonstrates that createSchema() emits a complete schema with:
 *   - All columns with types and constraints
 *   - PRIMARY KEY declarations
 *   - FOREIGN KEY constraints
 *   - Proper formatting with newlines and indentation
 */
public class Schema_test {

    static class User {
        Integer Id;
        String Name;
        Integer DeptId;
    }

    static class Department {
        String Id;
        String Name;
    }

    public static void main(String[] args) {
        ORM orm = new ORM();

        // Define Users table with FK to Departments
        Table Users = new Table("Users")
            .has("Id").asInt().is(Constraint.PRIMARYKEY)
            .has("Name").asString(100)
            .has("DeptId").asString(3);

        // Define Departments table
        Table Departments = new Table("Departments")
            .has("Id").asString(3).is(Constraint.PRIMARYKEY)
            .has("Name").asString(50);

        // Add FK constraint: Users.DeptId references Departments.Id
        Users.has("DeptId").refers(Departments.c("Id"));

        // Register both tables
        orm.register(User.class, Users);
        orm.register(Department.class, Departments);

        System.out.println("=== Schema Generation Test ===\n");

        // Generate the complete schema
        String ddl = orm.createSchema();
        System.out.println("Generated DDL:\n");
        System.out.println(ddl);

        System.out.println("\n=== Verification ===\n");

        // Verify the output contains expected elements
        boolean hasUsers = ddl.contains("CREATE TABLE Users");
        boolean hasDepartments = ddl.contains("CREATE TABLE Departments");
        boolean hasUsersPK = ddl.contains("Id INT PRIMARY KEY");
        boolean hasDeptsPK = ddl.contains("Id VARCHAR(3) PRIMARY KEY");
        boolean hasFK = ddl.contains("FOREIGN KEY");
        boolean hasReferences = ddl.contains("REFERENCES Departments");

        System.out.println("✓ Contains 'CREATE TABLE Users': " + hasUsers);
        System.out.println("✓ Contains 'CREATE TABLE Departments': " + hasDepartments);
        System.out.println("✓ Users has PRIMARY KEY: " + hasUsersPK);
        System.out.println("✓ Departments has PRIMARY KEY: " + hasDeptsPK);
        System.out.println("✓ Contains FOREIGN KEY constraint: " + hasFK);
        System.out.println("✓ FK references Departments: " + hasReferences);

        if (hasUsers && hasDepartments && hasUsersPK && hasDeptsPK && hasFK && hasReferences) {
            System.out.println("\n✓ All checks passed!");
        } else {
            System.out.println("\n✗ Some checks failed!");
            System.exit(1);
        }
    }
}
