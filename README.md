## Basic Usage

### Generating SQL statements programmatically using JAVA:
```
Table Users = new Table("Users")
    .has("Username").asString(50)
    .has("Password").asString(20)
    .has("Age").asInt()
    .has("Balance").asDecimal(10, 3);On Thursday 2nd April 2026 (two-hour c
String UsersSQLString = Users.toSQL();

// TestCase 1
System.out.println(UsersSQLString);
// This produces an output:
// Users(
//     Username VARCHAR(50)
//     Password VARCHAR(20)
//     Age INTEGER
//     Balance DECIMAL(10, 3)
// )
```

