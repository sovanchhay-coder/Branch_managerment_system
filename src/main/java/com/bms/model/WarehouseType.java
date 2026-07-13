package com.bms.model;

public class WarehouseType {
    private int typeId;
    private String typeName;

    public WarehouseType() {}

    public WarehouseType(int typeId, String typeName) {
        this.typeId = typeId;
        this.typeName = typeName;
    }

    public int getTypeId() { return typeId; }
    public void setTypeId(int typeId) { this.typeId = typeId; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    @Override
    public String toString() {
        return typeName;
    }
}
