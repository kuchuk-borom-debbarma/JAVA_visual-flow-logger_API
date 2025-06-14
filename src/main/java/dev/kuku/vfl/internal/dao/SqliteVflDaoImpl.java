package dev.kuku.vfl.internal.dao;

import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.models.VflLogDataType;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class SqliteVflDaoImpl implements VflDao {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SqliteVflDaoImpl.class);
    Connection connection;

    public SqliteVflDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void upsertBlocks(List<VflBlockDataType> blocks) {
        Statement statement;
        try {
            statement = connection.createStatement();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            statement.execute("""
            insert into vfl_blocks (id) values (?)
            on conflict (id) DO NOTHING;
            """);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void upsertLogs(List<VflLogDataType> logs) {

    }
}
