// models/RestockRequest.java
package app.models;

public class RestockRequest {
    public Integer requestID;
    public Integer ingredientID;
    public Integer supplierID;
    public Integer quantityRequested;
    public String status;
    public Long requestedAt;

    public RestockRequest() {}

    public RestockRequest(Integer requestID) {
        this.requestID = requestID;
    }
}