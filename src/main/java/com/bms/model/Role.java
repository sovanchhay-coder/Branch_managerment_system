package com.bms.model;

public class Role {
    private int roleId;
    private String roleName;
    private Integer parentRoleId;
    private int level;

    public Role() {}

    public Role(int roleId, String roleName, Integer parentRoleId, int level) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.parentRoleId = parentRoleId;
        this.level = level;
    }

    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public Integer getParentRoleId() { return parentRoleId; }
    public void setParentRoleId(Integer parentRoleId) { this.parentRoleId = parentRoleId; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    @Override
    public String toString() {
        return roleName;
    }
}
