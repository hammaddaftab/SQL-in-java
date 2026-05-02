import java.sql.Timestamp;

/**
 * Tests for the SELECT DSL.
 *
 * Reads top-to-bottom: each section builds on the one above it.
 *   1.  bare SELECT
 *   2.  projection + DISTINCT
 *   3.  single-column predicates (eq, gt, lt, ...)
 *   4.  English aliases (above, below, atLeast, atMost)
 *   5.  AND / OR / NOT composition
 *   6.  all() / any() helpers
 *   7.  pattern, range, set, null operators
 *   8.  column-to-column comparisons
 *   9.  string escaping
 *  10.  ORDER BY (explicit + default ASC + multi-column)
 *  11.  LIMIT / OFFSET + top / skip aliases
 *  12.  GROUP BY + HAVING
 *  13.  match-by-example via select(instance)
 *  14.  example OR example
 *  15.  example AND explicit predicate
 *  16.  kitchen-sink query combining everything
 *
 * Run with:  javac *.java && java Select_test
 */
public class Select_test {

    // --- shared schema + ORM -------------------------------------------------
    // Built once and reused so each test reads as one focused assertion.

    static Table Users;
    static ORM orm;

    static void setUp() {
        Users = new Table("Users")
            .has("Id").asInt()
            .has("Name").asString(50)
            .has("Age").asInt()
            .has("MinAge").asInt()        // used to demonstrate column-vs-column
            .has("Balance").asDecimal(10, 2)
            .has("Status").asString(20)
            .has("DeptId").asString(3)
            .has("CreatedAt").asInt();    // INTEGER stand-in for a timestamp epoch

        Users.c("Id").is(Constraint.PRIMARYKEY);

        orm = new ORM();
        orm.register(User.class, Users);
    }

    // --- micro test harness --------------------------------------------------
    // Avoids pulling in JUnit so the project keeps zero dependencies.

    static int passed = 0;
    static int failed = 0;

    static void assertSQL(String label, String actual, String expected) {
        if (actual.equals(expected)) {
            passed++;
            System.out.println("OK   " + label);
            System.out.println("     " + actual);
        } else {
            failed++;
            System.out.println("FAIL " + label);
            System.out.println("     expected: " + expected);
            System.out.println("     actual:   " + actual);
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        setUp();

        // 1. The simplest possible query: every row, every column.
        //    `from(Class)` is the predicate-driven entry point.
        assertSQL(
            "1. bare SELECT",
            orm.from(User.class).generateSQL(),
            "SELECT * FROM Users;"
        );

        // 2. Projection narrows the columns. `pick(...)` reads like "pick these".
        assertSQL(
            "2a. projection",
            orm.from(User.class).pick(Users.c("Name"), Users.c("Age")).generateSQL(),
            "SELECT Name, Age FROM Users;"
        );

        // 2b. DISTINCT is just a chained flag — order of pick/distinct doesn't matter.
        assertSQL(
            "2b. distinct projection",
            orm.from(User.class).pick(Users.c("Status")).distinct().generateSQL(),
            "SELECT DISTINCT Status FROM Users;"
        );

        // 3. Single comparisons: eq, ne, gt, lt, gte, lte are the canonical names.
        //    Each ColumnReference method returns a Predicate that .where() consumes.
        assertSQL(
            "3a. equality",
            orm.from(User.class).where(Users.c("Status").eq("active")).generateSQL(),
            "SELECT * FROM Users WHERE Status = 'active';"
        );
        assertSQL(
            "3b. greater-than",
            orm.from(User.class).where(Users.c("Age").gt(18)).generateSQL(),
            "SELECT * FROM Users WHERE Age > 18;"
        );
        assertSQL(
            "3c. not-equal",
            orm.from(User.class).where(Users.c("Status").ne("banned")).generateSQL(),
            "SELECT * FROM Users WHERE Status <> 'banned';"
        );

        // 4. English aliases. `above` / `below` / `atLeast` / `atMost` map to gt/lt/gte/lte
        //    and exist purely so the call site reads like a sentence.
        assertSQL(
            "4. english alias (above)",
            orm.from(User.class).where(Users.c("Balance").atLeast(100.0)).generateSQL(),
            "SELECT * FROM Users WHERE Balance >= 100.0;"
        );

        // 5a. AND: compose two predicates with .and(). Output flattens nested ANDs
        //     to a single parenthesised group instead of `((a AND b) AND c)`.
        assertSQL(
            "5a. AND of two predicates",
            orm.from(User.class).where(
                Users.c("Age").gt(18).and(Users.c("Status").eq("active"))
            ).generateSQL(),
            "SELECT * FROM Users WHERE (Age > 18 AND Status = 'active');"
        );

        // 5b. The builder also exposes .and() / .or() at the top level so you don't
        //     have to nest predicate constructors when chaining many conditions.
        assertSQL(
            "5b. chained .and() on builder",
            orm.from(User.class)
                .where(Users.c("Age").gt(18))
                .and(Users.c("Status").eq("active"))
                .and(Users.c("Balance").gt(0))
                .generateSQL(),
            "SELECT * FROM Users WHERE (Age > 18 AND Status = 'active' AND Balance > 0);"
        );

        // 5c. Mixed AND + OR. Precedence is preserved by parentheses in the output:
        //     reading the SQL back you can see exactly which parts are grouped.
        assertSQL(
            "5c. AND then OR (precedence preserved)",
            orm.from(User.class).where(
                Users.c("Age").gt(18)
                    .and(Users.c("Status").eq("active"))
                    .or(Users.c("Status").eq("admin"))
            ).generateSQL(),
            "SELECT * FROM Users WHERE ((Age > 18 AND Status = 'active') OR Status = 'admin');"
        );

        // 5d. NOT wraps a predicate. Both `.negate()` and `Predicate.not(p)` work.
        assertSQL(
            "5d. NOT predicate",
            orm.from(User.class).where(
                Predicate.not(Users.c("Status").eq("banned"))
            ).generateSQL(),
            "SELECT * FROM Users WHERE NOT (Status = 'banned');"
        );

        // 6. all() / any() are static convenience constructors when you want to
        //    list many predicates without trailing `.and(...).and(...)` chains.
        assertSQL(
            "6a. Predicate.all(...)",
            orm.from(User.class).where(
                Predicate.all(
                    Users.c("Age").gt(18),
                    Users.c("Age").lt(65),
                    Users.c("Status").eq("active")
                )
            ).generateSQL(),
            "SELECT * FROM Users WHERE (Age > 18 AND Age < 65 AND Status = 'active');"
        );
        assertSQL(
            "6b. Predicate.any(...)",
            orm.from(User.class).where(
                Predicate.any(
                    Users.c("Status").eq("admin"),
                    Users.c("Status").eq("owner"),
                    Users.c("Status").eq("staff")
                )
            ).generateSQL(),
            "SELECT * FROM Users WHERE (Status = 'admin' OR Status = 'owner' OR Status = 'staff');"
        );

        // 7a. LIKE / NOT LIKE for pattern matching.
        assertSQL(
            "7a. LIKE",
            orm.from(User.class).where(Users.c("Name").like("J%")).generateSQL(),
            "SELECT * FROM Users WHERE Name LIKE 'J%';"
        );

        // 7b. BETWEEN as a single call instead of `gte().and(lte())`.
        assertSQL(
            "7b. BETWEEN",
            orm.from(User.class).where(Users.c("Age").between(18, 30)).generateSQL(),
            "SELECT * FROM Users WHERE Age BETWEEN 18 AND 30;"
        );

        // 7c. IN with varargs — most natural for short lists.
        assertSQL(
            "7c. IN (varargs)",
            orm.from(User.class).where(Users.c("DeptId").in("D1", "D2", "D3")).generateSQL(),
            "SELECT * FROM Users WHERE DeptId IN ('D1', 'D2', 'D3');"
        );

        // 7d. NOT IN works the same way.
        assertSQL(
            "7d. NOT IN",
            orm.from(User.class).where(Users.c("DeptId").notIn("X1", "X2")).generateSQL(),
            "SELECT * FROM Users WHERE DeptId NOT IN ('X1', 'X2');"
        );

        // 7e. IS NULL has a dedicated method, but eq(null) is auto-rewritten to it
        //     so you don't have to remember which one to call.
        assertSQL(
            "7e. IS NULL via .isNull()",
            orm.from(User.class).where(Users.c("DeptId").isNull()).generateSQL(),
            "SELECT * FROM Users WHERE DeptId IS NULL;"
        );
        assertSQL(
            "7f. IS NULL via eq(null) auto-rewrite",
            orm.from(User.class).where(Users.c("DeptId").eq(null)).generateSQL(),
            "SELECT * FROM Users WHERE DeptId IS NULL;"
        );
        assertSQL(
            "7g. IS NOT NULL via ne(null) auto-rewrite",
            orm.from(User.class).where(Users.c("DeptId").ne(null)).generateSQL(),
            "SELECT * FROM Users WHERE DeptId IS NOT NULL;"
        );

        // 8. Column-vs-column comparison — the operator overloads also accept a
        //    ColumnReference, so you can express "Age > MinAge" without literals.
        assertSQL(
            "8. column-to-column comparison",
            orm.from(User.class).where(
                Users.c("Age").gt(Users.c("MinAge"))
            ).generateSQL(),
            "SELECT * FROM Users WHERE Age > MinAge;"
        );

        // 9. String literals are escaped — single quotes are doubled. This is the
        //    one piece of "real" SQL safety the formatter does for you.
        assertSQL(
            "9. string literal escaping",
            orm.from(User.class).where(Users.c("Name").eq("O'Brien")).generateSQL(),
            "SELECT * FROM Users WHERE Name = 'O''Brien';"
        );

        // 10a. ORDER BY with explicit direction via .desc() / .asc().
        assertSQL(
            "10a. ORDER BY desc",
            orm.from(User.class).orderBy(Users.c("Age").desc()).generateSQL(),
            "SELECT * FROM Users ORDER BY Age DESC;"
        );
        // 10b. Bare ColumnReference defaults to ASC — saves typing for the common case.
        assertSQL(
            "10b. ORDER BY default ASC",
            orm.from(User.class).orderBy(Users.c("Name")).generateSQL(),
            "SELECT * FROM Users ORDER BY Name ASC;"
        );
        // 10c. Multiple sort keys in priority order.
        assertSQL(
            "10c. ORDER BY multi-column",
            orm.from(User.class)
                .orderBy(Users.c("Age").desc(), Users.c("Name").asc())
                .generateSQL(),
            "SELECT * FROM Users ORDER BY Age DESC, Name ASC;"
        );

        // 11a. LIMIT and OFFSET — pagination basics.
        assertSQL(
            "11a. LIMIT + OFFSET",
            orm.from(User.class).limit(10).offset(20).generateSQL(),
            "SELECT * FROM Users LIMIT 10 OFFSET 20;"
        );
        // 11b. top() / skip() are English aliases for the same thing.
        assertSQL(
            "11b. top + skip aliases",
            orm.from(User.class).top(5).skip(10).generateSQL(),
            "SELECT * FROM Users LIMIT 5 OFFSET 10;"
        );

        // 12. GROUP BY + HAVING. Aggregate functions aren't modelled yet, so we
        //     demonstrate the clause structure with a plain column predicate.
        assertSQL(
            "12. GROUP BY + HAVING",
            orm.from(User.class)
                .pick(Users.c("DeptId"))
                .groupBy(Users.c("DeptId"))
                .having(Users.c("Balance").gt(0))
                .generateSQL(),
            "SELECT DeptId FROM Users GROUP BY DeptId HAVING Balance > 0;"
        );

        // 13. Match-by-example. Set the fields you care about on a POJO and they
        //     become equality conditions. Null fields are ignored.
        User template = new User();
        template.Name = "Alice";
        template.Age = 25;
        // Each example block is parenthesised in the output so that adding more
        // instances or an explicit predicate later never silently regroups the SQL.
        assertSQL(
            "13. select(instance) match-by-example",
            orm.select(template).generateSQL(),
            "SELECT * FROM Users WHERE (Name = 'Alice' AND Age = 25);"
        );

        // 14. Two example instances OR'd together — useful for "find any of these".
        User a = new User(); a.Name = "Alice";
        User b = new User(); b.Name = "Bob";
        assertSQL(
            "14. example OR example",
            orm.select(a).or(b).generateSQL(),
            "SELECT * FROM Users WHERE (Name = 'Alice') OR (Name = 'Bob');"
        );

        // 15. Example + explicit predicate. The two halves are AND-ed: the example
        //     block stays grouped so adding a predicate never breaks its meaning.
        User active = new User(); active.Status = "active";
        assertSQL(
            "15. example AND predicate",
            orm.select(active).where(Users.c("Age").gt(18)).generateSQL(),
            "SELECT * FROM Users WHERE ((Status = 'active')) AND Age > 18;"
        );

        // 16. The kitchen-sink — every clause at once, in canonical SQL order.
        assertSQL(
            "16. full kitchen-sink query",
            orm.from(User.class)
                .pick(Users.c("DeptId"), Users.c("Status"))
                .distinct()
                .where(Users.c("Age").between(18, 65))
                .and(Users.c("Status").in("active", "trial"))
                .and(Users.c("DeptId").isNotNull())
                .groupBy(Users.c("DeptId"), Users.c("Status"))
                .having(Users.c("Balance").gt(0))
                .orderBy(Users.c("DeptId").asc(), Users.c("Status").desc())
                .limit(50).offset(100)
                .generateSQL(),
            "SELECT DISTINCT DeptId, Status FROM Users "
            + "WHERE (Age BETWEEN 18 AND 65 AND Status IN ('active', 'trial') AND DeptId IS NOT NULL) "
            + "GROUP BY DeptId, Status "
            + "HAVING Balance > 0 "
            + "ORDER BY DeptId ASC, Status DESC "
            + "LIMIT 50 OFFSET 100;"
        );

        // --- summary ---------------------------------------------------------
        System.out.println("------------------------------------");
        System.out.println("Passed: " + passed + "    Failed: " + failed);
        if (failed > 0) System.exit(1);
    }
}


/**
 * POJO mirroring the Users table. Field names must match column names exactly
 * (case-sensitive) — that's what register() validates.
 *
 * Boxed types (Integer / Double) so a "not set" field reads as null and gets
 * skipped by buildConditions. CreatedAt is Integer here to match the schema's
 * INTEGER stand-in; in real code it would be a Timestamp.
 */
class User {
    Integer Id;
    String Name;
    Integer Age;
    Integer MinAge;
    Double Balance;
    String Status;
    String DeptId;
    Integer CreatedAt;
}
