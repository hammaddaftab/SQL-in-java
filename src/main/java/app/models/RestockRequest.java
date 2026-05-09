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

    public Integer getRequestID() { return requestID; }
    public Integer getIngredientID() { return ingredientID; }
    public Integer getSupplierID() { return supplierID; }
    public Integer getQuantityRequested() { return quantityRequested; }
    public String getStatus() { return status; }
    public Long getRequestedAt() { return requestedAt; }
}