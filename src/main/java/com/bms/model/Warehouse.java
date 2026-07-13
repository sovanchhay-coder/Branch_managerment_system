package com.bms.model;

public class Warehouse {
    private int warehouseId;
    private String name;
    private int typeId;
    private String typeName;
    private String location;
    private int provinceId;
    private String provinceName;
    private Integer parentWarehouseId;
    private String parentName;

    public Warehouse() {}

    public int getWarehouseId() { return warehouseId; }
    public void setWarehouseId(int warehouseId) { this.warehouseId = warehouseId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getTypeId() { return typeId; }
    public void setTypeId(int typeId) { this.typeId = typeId; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getProvinceId() { return provinceId; }
    public void setProvinceId(int provinceId) { this.provinceId = provinceId; }

    public String getProvinceName() { return provinceName; }
    public void setProvinceName(String provinceName) { this.provinceName = provinceName; }

    public Integer getParentWarehouseId() { return parentWarehouseId; }
    public void setParentWarehouseId(Integer parentWarehouseId) { this.parentWarehouseId = parentWarehouseId; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    @Override
    public String toString() {
        return name + " (" + typeName + ")";
    }
}
