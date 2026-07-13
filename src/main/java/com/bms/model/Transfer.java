package com.bms.model;

import java.sql.Timestamp;

public class Transfer {
    private int transferId;
    private int fromWarehouseId;
    private String fromWarehouseName;
    private int toWarehouseId;
    private String toWarehouseName;
    private int productId;
    private String productName;
    private int quantity;
    private Timestamp transferDate;
    private Integer approvedBy;
    private String approvedByName;

    public Transfer() {}

    public int getTransferId() { return transferId; }
    public void setTransferId(int transferId) { this.transferId = transferId; }

    public int getFromWarehouseId() { return fromWarehouseId; }
    public void setFromWarehouseId(int fromWarehouseId) { this.fromWarehouseId = fromWarehouseId; }

    public String getFromWarehouseName() { return fromWarehouseName; }
    public void setFromWarehouseName(String fromWarehouseName) { this.fromWarehouseName = fromWarehouseName; }

    public int getToWarehouseId() { return toWarehouseId; }
    public void setToWarehouseId(int toWarehouseId) { this.toWarehouseId = toWarehouseId; }

    public String getToWarehouseName() { return toWarehouseName; }
    public void setToWarehouseName(String toWarehouseName) { this.toWarehouseName = toWarehouseName; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public Timestamp getTransferDate() { return transferDate; }
    public void setTransferDate(Timestamp transferDate) { this.transferDate = transferDate; }

    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer approvedBy) { this.approvedBy = approvedBy; }

    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }

    public boolean isApproved() {
        return approvedBy != null;
    }
}
