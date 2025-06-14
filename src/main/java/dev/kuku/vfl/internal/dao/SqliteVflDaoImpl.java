package dev.kuku.vfl.internal.dao;

import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.models.VflLogDataType;
import org.slf4j.Logger;

import java.sql.Connection;
import java.util.List;

public class SqliteVflDaoImpl implements VflDao {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SqliteVflDaoImpl.class);
    Connection connection;

    public SqliteVflDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void upsertBlocks(List<VflBlockDataType> blocks) {

    }

    @Override
    public void upsertLogs(List<VflLogDataType> logs) {

    }
}
