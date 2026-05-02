package src.sql_in_java;

public enum Constraint {
    NOTNULL("NOT NULL"),
    UNIQUE("UNIQUE"),
    PRIMARYKEY("PRIMARY KEY");

    String SQLString; 

    Constraint(String SQLString) {
        this.SQLString = SQLString;
    }

    public Constraints and(Constraint constraint) {
        Constraints constraints = new Constraints();
        constraints.add(this);
        constraints.add(constraint);
        return constraints;
    }

    String toSQL() {
        return this.SQLString;
    }
}