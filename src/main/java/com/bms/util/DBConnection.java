package com.bms.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {
    private static final HikariDataSource dataSource;

    static {
        ConfigLoader config = new ConfigLoader();
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.getDbUrl());
        hikari.setUsername(config.getDbUser());
        hikari.setPassword(config.getDbPassword());
        hikari.setMaximumPoolSize(10);
        hikari.setMinimumIdle(2);
        hikari.setIdleTimeout(30000);
        hikari.setConnectionTimeout(10000);
        hikari.setPoolName("BMS-HikariPool");
        dataSource = new HikariDataSource(hikari);
    }

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
