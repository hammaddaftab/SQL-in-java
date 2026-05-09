// models/CafeTable.java
package app.models;

public class CafeTable {
    public Integer tableID;
    public String location;
    public Integer capacity;

    public CafeTable() {}

    public CafeTable(Integer tableID) {
        this.tableID = tableID;
    }

    public Integer getTableID() { return tableID; }
    public String getLocation() { return location; }
    public Integer getCapacity() { return capacity; }
}