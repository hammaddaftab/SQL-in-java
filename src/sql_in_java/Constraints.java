package src.sql_in_java;

import java.util.*;

public class Constraints extends ArrayList<Constraint> {
    public Constraints and(Constraint constraint) {
        this.add(constraint);
        return this;
    }
};
