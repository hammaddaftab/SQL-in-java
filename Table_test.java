

public class Table_test {
    public static void main() {
        // TestCase 1
        Table Users = new Table("Users")
            .has("Username").asString(50)
            .has("DeptId").asString(3)
            .has("Password").asString(20)
            .has("Age").asInt()
            .has("Balance").asDecimal(10, 3);
            
        String UsersSQLString = Users.toSQL();
        System.out.println(UsersSQLString);
        // This produces an output:
        // Users(
        //     Username VARCHAR(50)
        //     Password VARCHAR(20)
        //     Age INTEGER
        //     Balance DECIMAL(010, 3)
        // )



        // TestCase 2
        Table School = new Table("Department")
            .has("DeptName").asString(30)
            .refers(Users.c("DeptId"));

        String SchoolSQLString = School.toSQL();
        System.out.println(SchoolSQLString);



        // TestCase 3
        Constraints myconstraints = Constraint.UNIQUE.and(Constraint.NOTNULL);
        School
            .c("DeptId").is(Constraint.UNIQUE)
            .c("DeptName").is(myconstraints);
        String SchoolSQLStringConstraints = School.toSQL();
        System.out.println(SchoolSQLStringConstraints);
    }
}
