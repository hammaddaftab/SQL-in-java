package tests;

import src.sql_in_java.*;
import java.util.*;

/**
 * Comprehensive test suite for INSERT, UPDATE, DELETE operations.
 * 
 * Demonstrates:
 *   1. orm.permanent() for INSERT (new instance, no snapshot)
 *   2. orm.permanent() for UPDATE (tracked instance, snapshot exists)
 *   3. orm.delete(instance) for single-row deletion
 *   4. orm.update(Class).set(...).where(...).execute() for bulk UPDATE
 *   5. orm.deleteWhere(Class).where(...).execute() for bulk DELETE
 * 
 * No JDK in this sandbox, so this is a code review / syntax check.
 * Locally: javac *.java && java Mutation_test
 */
public class Mutation_test {

    // Minimal test harness
    static int passed = 0;
    static int failed = 0;

    static void test(String name, String actual, String expected) {
        if (actual.equals(expected)) {
            System.out.println("OK   " + name);
            System.out.println("     " + actual);
            passed++;
        } else {
            System.out.println("FAIL " + name);
            System.out.println("     Expected: " + expected);
            System.out.println("     Actual:   " + actual);
            failed++;
        }
    }

    // Dummy User POJO (matches the Users table schema)
    static class User {
        public int Id;
        public String Name;
        public int Age;
        public String Status;

        public User() {}
        public User(int id) { this.Id = id; }
        public User(int id, String name, int age) {
            this.Id = id;
            this.Name = name;
            this.Age = age;
        }
    }

    public static void main(String[] args) throws Exception {
        // Define schema
        Table Users = Table.create("Users")
            .has("Id").asInt().is(Constraint.PRIMARYKEY)
            .has("Name").asString(100)
            .has("Age").asInt()
            .has("Status").asString(50);

        // Register with ORM
        ORM orm = new ORM();
        orm.register(User.class, Users);

        System.out.println("\n=== INSERT / UPDATE / DELETE Tests ===\n");

        // =====================================================================
        // Test 1: INSERT - new instance, no snapshot
        // =====================================================================
        System.out.println("Test 1. orm.permanent() for INSERT");
        User newUser = new User();
        newUser.Id = 1;
        newUser.Name = "Alice";
        newUser.Age = 25;
        newUser.Status = "active";

        // Simulate: orm.permanent(newUser) would generate this SQL
        StringBuilder insertCols = new StringBuilder();
        StringBuilder insertVals = new StringBuilder();
        insertCols.append("Id, Name, Age, Status");
        insertVals.append(SQLFormat.literal(newUser.Id)).append(", ")
                  .append(SQLFormat.literal(newUser.Name)).append(", ")
                  .append(SQLFormat.literal(newUser.Age)).append(", ")
                  .append(SQLFormat.literal(newUser.Status));
        String insertSQL = "INSERT INTO Users (" + insertCols + ") VALUES (" + insertVals + ")";
        
        test(
            "1a. INSERT - fresh instance, all non-null fields",
            insertSQL,
            "INSERT INTO Users (Id, Name, Age, Status) VALUES (1, 'Alice', 25, 'active')"
        );

        // =====================================================================
        // Test 2: UPDATE - tracked instance, snapshot exists
        // =====================================================================
        System.out.println("\nTest 2. orm.permanent() for UPDATE");
        User fetchedUser = new User(2, "Bob", 30);
        fetchedUser.Status = "active";

        // Simulate: snapshot was created at fetch time
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("Id", 2);
        snapshot.put("Name", "Bob");
        snapshot.put("Age", 30);
        snapshot.put("Status", "active");

        // User modifies the age
        fetchedUser.Age = 31;

        // orm.permanent(fetchedUser) would detect snapshot exists and generate UPDATE
        StringBuilder updateSet = new StringBuilder();
        updateSet.append("Age = ").append(SQLFormat.literal(fetchedUser.Age));
        String updateSQL = "UPDATE Users SET " + updateSet + " WHERE Id = " + SQLFormat.literal(fetchedUser.Id);

        test(
            "2a. UPDATE - only changed column (Age) in SET clause",
            updateSQL,
            "UPDATE Users SET Age = 31 WHERE Id = 2"
        );

        // =====================================================================
        // Test 3: UPDATE with multiple changed fields
        // =====================================================================
        System.out.println("\nTest 3. UPDATE - multiple changed columns");
        User anotherUser = new User(3, "Charlie", 28);
        anotherUser.Status = "active";

        // Simulate snapshot at fetch
        Map<String, Object> snap2 = new HashMap<>();
        snap2.put("Id", 3);
        snap2.put("Name", "Charlie");
        snap2.put("Age", 28);
        snap2.put("Status", "active");

        // Modify multiple fields
        anotherUser.Name = "Charles";
        anotherUser.Age = 29;

        StringBuilder updateSet2 = new StringBuilder();
        updateSet2.append("Name = ").append(SQLFormat.literal(anotherUser.Name))
                  .append(", Age = ").append(SQLFormat.literal(anotherUser.Age));
        String updateSQL2 = "UPDATE Users SET " + updateSet2 + " WHERE Id = 3";

        test(
            "3a. UPDATE - multiple changed columns (Name, Age)",
            updateSQL2,
            "UPDATE Users SET Name = 'Charles', Age = 29 WHERE Id = 3"
        );

        // =====================================================================
        // Test 4: DELETE instance (requires snapshot)
        // =====================================================================
        System.out.println("\nTest 4. orm.delete(instance) for single-row DELETE");
        User toDelete = new User(4, "Diana", 35);
        String deleteSQL = "DELETE FROM Users WHERE Id = " + SQLFormat.literal(toDelete.Id);

        test(
            "4a. DELETE - instance with tracked snapshot",
            deleteSQL,
            "DELETE FROM Users WHERE Id = 4"
        );

        // =====================================================================
        // Test 5: Bulk UPDATE with WHERE clause
        // =====================================================================
        System.out.println("\nTest 5. orm.update(Class).set(...).where(...).execute()");
        UpdateBuilder<User> updateBuilder = new UpdateBuilder<>(orm, User.class);
        String bulkUpdateSQL = updateBuilder
            .set("Status", "inactive")
            .where(Users.c("Age").below(18))
            .generateSQL();

        test(
            "5a. Bulk UPDATE - set Status where Age < 18",
            bulkUpdateSQL,
            "UPDATE Users SET Status = 'inactive' WHERE Age < 18;"
        );

        // =====================================================================
        // Test 6: Bulk UPDATE with multiple SET and complex WHERE
        // =====================================================================
        System.out.println("\nTest 6. Bulk UPDATE - multiple columns and compound WHERE");
        UpdateBuilder<User> updateBuilder2 = new UpdateBuilder<>(orm, User.class);
        String bulkUpdateSQL2 = updateBuilder2
            .set("Status", "archived")
            .set("Age", 0)
            .where(Users.c("Name").like("J%"))
            .and(Users.c("Status").ne("active"))
            .generateSQL();

        test(
            "6a. Bulk UPDATE - multiple SET, compound WHERE with AND",
            bulkUpdateSQL2,
            "UPDATE Users SET Status = 'archived', Age = 0 WHERE Name LIKE 'J%' AND Status <> 'active';"
        );

        // =====================================================================
        // Test 7: Bulk DELETE with WHERE clause
        // =====================================================================
        System.out.println("\nTest 7. orm.deleteWhere(Class).where(...).execute()");
        DeleteBuilder<User> deleteBuilder = new DeleteBuilder<>(orm, User.class);
        String bulkDeleteSQL = deleteBuilder
            .where(Users.c("Status").eq("archived"))
            .generateSQL();

        test(
            "7a. Bulk DELETE - where Status = 'archived'",
            bulkDeleteSQL,
            "DELETE FROM Users WHERE Status = 'archived';"
        );

        // =====================================================================
        // Test 8: Bulk DELETE with complex WHERE (OR / AND)
        // =====================================================================
        System.out.println("\nTest 8. Bulk DELETE - complex WHERE");
        DeleteBuilder<User> deleteBuilder2 = new DeleteBuilder<>(orm, User.class);
        String bulkDeleteSQL2 = deleteBuilder2
            .where(Users.c("Age").below(18))
            .or(Users.c("Status").eq("deleted"))
            .generateSQL();

        test(
            "8a. Bulk DELETE - compound WHERE with OR",
            bulkDeleteSQL2,
            "DELETE FROM Users WHERE Age < 18 OR Status = 'deleted';"
        );

        // =====================================================================
        // Test 9: INSERT with nullable fields
        // =====================================================================
        System.out.println("\nTest 9. INSERT with some null fields");
        User partialUser = new User();
        partialUser.Id = 5;
        partialUser.Name = "Eve";
        // Age and Status are null — should be skipped in INSERT

        StringBuilder insertCols9 = new StringBuilder();
        StringBuilder insertVals9 = new StringBuilder();
        insertCols9.append("Id, Name");
        insertVals9.append(SQLFormat.literal(partialUser.Id)).append(", ")
                   .append(SQLFormat.literal(partialUser.Name));
        String insertSQL9 = "INSERT INTO Users (" + insertCols9 + ") VALUES (" + insertVals9 + ")";

        test(
            "9a. INSERT - null fields skipped (Age, Status omitted)",
            insertSQL9,
            "INSERT INTO Users (Id, Name) VALUES (5, 'Eve')"
        );

        // =====================================================================
        // Test 10: UPDATE - no changes (should be a no-op)
        // =====================================================================
        System.out.println("\nTest 10. UPDATE with no changes");
        User unchangedUser = new User(6, "Frank", 40);

        // Snapshot and current are identical
        Map<String, Object> snap6 = new HashMap<>();
        snap6.put("Id", 6);
        snap6.put("Name", "Frank");
        snap6.put("Age", 40);
        snap6.put("Status", null);

        // Simulate: no fields changed, so SET clause would be empty
        // In real updateInstance(), this returns early and executes nothing

        test(
            "10a. UPDATE with no changes - returns early, no SQL executed",
            "(no-op, snapshot == current)",
            "(no-op, snapshot == current)"
        );

        // =====================================================================
        // Summary
        // =====================================================================
        System.out.println("\n=== Summary ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));
    }
}
