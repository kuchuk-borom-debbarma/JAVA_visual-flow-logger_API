package dev.kuku.vfl.api.vflStore;

import dev.kuku.vfl.internal.VflLogStore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqliteVflLogStoreImpl implements VflLogStore {
    Connection connection;

    public SqliteVflLogStoreImpl() {
        try (Connection connection = DriverManager.getConnection("jdbc::sqlite::vfl");) {
            this.connection = connection;
        } catch (SQLException e) {
        }
    }
}
