package com.bms.db;

import com.bms.model.Province;
import com.bms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProvinceDAO {

    public List<Province> findAll() throws SQLException {
        String sql = "SELECT province_id, province_name FROM provinces ORDER BY province_name";
        List<Province> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Optional<Province> findById(int id) throws SQLException {
        String sql = "SELECT province_id, province_name FROM provinces WHERE province_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Province insert(Province p) throws SQLException {
        String sql = "INSERT INTO provinces (province_name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getProvinceName());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setProvinceId(rs.getInt(1));
                }
            }
        }
        return p;
    }

    public void update(Province p) throws SQLException {
        String sql = "UPDATE provinces SET province_name = ? WHERE province_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getProvinceName());
            ps.setInt(2, p.getProvinceId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM provinces WHERE province_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Province map(ResultSet rs) throws SQLException {
        return new Province(rs.getInt("province_id"), rs.getString("province_name"));
    }
}
