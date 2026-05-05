package tests;

import src.sql_in_java.*;

/**
 * Proper mutation tests that actually call the ORM builders and compare generated SQL.
 * 
 * Tests cover:
 *   - orm.update(Class).set(...).where(...).generateSQL()
 *   - orm.deleteWhere(Class).where(...).generateSQL()
 *   - Complex WHERE clauses with AND, OR, predicates
 * 
 * Each test calls the builder directly and compares its output to expected SQL.
 * No hand-concatenated expected strings; the builders ARE the source of truth.
 */
public class Mutation_test {

    static int passed = 0, failed = 0;

    static void assertSQL(String label, String actual, String expected) {
        if (actual.equals(expected)) {
            System.out.println("OK   " + label);
            passed++;
        } else {
            System.out.println("FAIL " + label);
            System.out.println("  Expected: " + expected);
            System.out.println("  Got:      " + actual);
            failed++;
        }
    }

    static class User {
        public Integer Id, Age;
        public String Name, Status;
        User() {}
        User(int id) { Id = id; }
        User(int id, String name) { Id = id; Name = name; }
    }

    static class Account {
        public Integer Id;
        public String Email, Type;
        Account() {}
    }

    public static void main(String[] args) throws Exception {
        ORM orm = new ORM();

        Table Users = Table.create("Users")
            .has("Id").asInt().is(Constraint.PRIMARYKEY)
            .has("Name").asString(100)
            .has("Age").asInt()
            .has("Status").asString(50);
        orm.register(User.class, Users);

        Table Accounts = Table.create("Accounts")
            .has("Id").asInt().is(Constraint.PRIMARYKEY)
            .has("Email").asString(150)
            .has("Type").asString(20);
        orm.register(Account.class, Accounts);

        System.out.println("=== BULK UPDATE TESTS ===\n");

        // Test 1: Single SET, no WHERE
        assertSQL(
            "1. UPDATE single column, no WHERE",
            orm.update(User.class).set("Status", "inactive").generateSQL(),
            "UPDATE Users SET Status = 'inactive';"
        );

        // Test 2: Multiple SET
        assertSQL(
            "2. UPDATE multiple columns",
            orm.update(User.class)
                .set("Status", "archived")
                .set("Age", 0)
                .generateSQL(),
            "UPDATE Users SET Status = 'archived', Age = 0;"
        );

        // Test 3: SET with WHERE
        assertSQL(
            "3. UPDATE with WHERE predicate",
            orm.update(User.class)
                .set("Status", "inactive")
                .where(Users.c("Age").below(18))
                .generateSQL(),
            "UPDATE Users SET Status = 'inactive' WHERE Age < 18;"
        );

        // Test 4: SET with WHERE AND
        assertSQL(
            "4. UPDATE with WHERE and AND",
            orm.update(User.class)
                .set("Status", "flagged")
                .where(Users.c("Age").above(65))
                .and(Users.c("Name").like("A%"))
                .generateSQL(),
            "UPDATE Users SET Status = 'flagged' WHERE Age > 65 AND Name LIKE 'A%';"
        );

        // Test 5: SET with WHERE OR
        assertSQL(
            "5. UPDATE with WHERE or OR",
            orm.update(User.class)
                .set("Status", "pending")
                .where(Users.c("Status").eq("new"))
                .or(Users.c("Status").eq("trial"))
                .generateSQL(),
            "UPDATE Users SET Status = 'pending' WHERE Status = 'new' OR Status = 'trial';"
        );

        // Test 6: SET with IN
        assertSQL(
            "6. UPDATE with IN predicate",
            orm.update(User.class)
                .set("Status", "deleted")
                .where(Users.c("Id").in(1, 2, 3))
                .generateSQL(),
            "UPDATE Users SET Status = 'deleted' WHERE Id IN (1, 2, 3);"
        );

        // Test 7: SET with BETWEEN
        assertSQL(
            "7. UPDATE with BETWEEN",
            orm.update(User.class)
                .set("Status", "mid-aged")
                .where(Users.c("Age").between(30, 60))
                .generateSQL(),
            "UPDATE Users SET Status = 'mid-aged' WHERE Age BETWEEN 30 AND 60;"
        );

        // Test 8: SET to NULL
        assertSQL(
            "8. UPDATE column to NULL",
            orm.update(User.class)
                .set("Status", null)
                .where(Users.c("Age").isNull())
                .generateSQL(),
            "UPDATE Users SET Status = NULL WHERE Age IS NULL;"
        );

        // Test 9: Multiple SET with complex WHERE
        assertSQL(
            "9. UPDATE multiple columns, complex WHERE",
            orm.update(User.class)
                .set("Status", "flagged")
                .set("Age", 999)
                .where(Users.c("Name").like("J%"))
                .and(Users.c("Status").ne("active"))
                .generateSQL(),
            "UPDATE Users SET Status = 'flagged', Age = 999 WHERE Name LIKE 'J%' AND Status <> 'active';"
        );

        // Test 10: Single quote escaping in SET
        assertSQL(
            "10. UPDATE with string containing single quote",
            orm.update(User.class)
                .set("Name", "O'Brien")
                .where(Users.c("Id").eq(1))
                .generateSQL(),
            "UPDATE Users SET Name = 'O''Brien' WHERE Id = 1;"
        );

        System.out.println("\n=== BULK DELETE TESTS ===\n");

        // Test 11: DELETE with WHERE
        assertSQL(
            "11. DELETE with WHERE",
            orm.deleteWhere(User.class)
                .where(Users.c("Status").eq("archived"))
                .generateSQL(),
            "DELETE FROM Users WHERE Status = 'archived';"
        );

        // Test 12: DELETE with WHERE AND
        assertSQL(
            "12. DELETE with WHERE and AND",
            orm.deleteWhere(User.class)
                .where(Users.c("Status").eq("deleted"))
                .and(Users.c("Age").above(100))
                .generateSQL(),
            "DELETE FROM Users WHERE Status = 'deleted' AND Age > 100;"
        );

        // Test 13: DELETE with WHERE OR
        assertSQL(
            "13. DELETE with WHERE or OR",
            orm.deleteWhere(User.class)
                .where(Users.c("Status").eq("spam"))
                .or(Users.c("Status").eq("banned"))
                .generateSQL(),
            "DELETE FROM Users WHERE Status = 'spam' OR Status = 'banned';"
        );

        // Test 14: DELETE with IN
        assertSQL(
            "14. DELETE with IN predicate",
            orm.deleteWhere(User.class)
                .where(Users.c("Id").in(10, 20, 30))
                .generateSQL(),
            "DELETE FROM Users WHERE Id IN (10, 20, 30);"
        );

        // Test 15: DELETE with BETWEEN
        assertSQL(
            "15. DELETE with BETWEEN",
            orm.deleteWhere(User.class)
                .where(Users.c("Age").between(18, 65))
                .generateSQL(),
            "DELETE FROM Users WHERE Age BETWEEN 18 AND 65;"
        );

        // Test 16: DELETE with isNotNull
        assertSQL(
            "16. DELETE with IS NOT NULL",
            orm.deleteWhere(User.class)
                .where(Users.c("Status").isNotNull())
                .generateSQL(),
            "DELETE FROM Users WHERE Status IS NOT NULL;"
        );

        // Test 17: DELETE complex WHERE with multiple conditions
        assertSQL(
            "17. DELETE complex WHERE (AND/OR nested)",
            orm.deleteWhere(User.class)
                .where(Users.c("Status").in("inactive", "archived"))
                .and(Users.c("Age").isNotNull())
                .generateSQL(),
            "DELETE FROM Users WHERE Id IN ('inactive', 'archived') AND Age IS NOT NULL;"
        );

        System.out.println("\n=== INSTANCE-BASED DELETE (via builder) ===\n");

        // Test 18: DELETE single instance by PK
        assertSQL(
            "18. DELETE instance (simulated by PK match)",
            orm.deleteWhere(User.class)
                .where(Users.c("Id").eq(42))
                .generateSQL(),
            "DELETE FROM Users WHERE Id = 42;"
        );

        System.out.println("\n=== UPDATE via builder (simulating permanent's diff path) ===\n");

        // Test 19: UPDATE only changed field (simulating diff snapshot -> current)
        assertSQL(
            "19. UPDATE single changed field",
            orm.update(User.class)
                .set("Age", 26)
                .where(Users.c("Id").eq(1))
                .generateSQL(),
            "UPDATE Users SET Age = 26 WHERE Id = 1;"
        );

        // Test 20: UPDATE multiple changed fields
        assertSQL(
            "20. UPDATE multiple changed fields",
            orm.update(User.class)
                .set("Name", "Charles")
                .set("Age", 29)
                .where(Users.c("Id").eq(3))
                .generateSQL(),
            "UPDATE Users SET Name = 'Charles', Age = 29 WHERE Id = 3;"
        );

        System.out.println("\n=== CROSS-TABLE TESTS ===\n");

        // Test 21: UPDATE different table
        assertSQL(
            "21. UPDATE on Account table",
            orm.update(Account.class)
                .set("Type", "premium")
                .where(Accounts.c("Email").like("%@company.com"))
                .generateSQL(),
            "UPDATE Accounts SET Type = 'premium' WHERE Email LIKE '%@company.com';"
        );

        // Test 22: DELETE different table
        assertSQL(
            "22. DELETE from Account table",
            orm.deleteWhere(Account.class)
                .where(Accounts.c("Type").ne("active"))
                .generateSQL(),
            "DELETE FROM Accounts WHERE Type <> 'active';"
        );

        System.out.println("\n=== SUMMARY ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));
    }
}
