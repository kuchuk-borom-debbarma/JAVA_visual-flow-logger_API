package dev.kuku.vfl.internal.dao;

import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.models.VflLogDataType;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public class SqliteVflDaoImpl implements VflDao {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SqliteVflDaoImpl.class);
    Connection connection;

    public SqliteVflDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void upsertBlocks(List<VflBlockDataType> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        //TODO handle parameter size limit by splitting the blocks into multiple mini lists and then calling upsert many times.
        /*
         * Example of the final SQL query for 3 blocks with validation:
         *
         * WITH batch_data AS (
         *   SELECT * FROM (VALUES
         *     ('block1', 'Main Block', NULL, 1718457300000, 1718457300000),
         *     ('block2', 'Child Block', 'block1', 1718457301000, 1718457301000),
         *     ('block3', 'Another Child', 'block1', 1718457302000, 1718457302000)
         *   ) AS blocks(id, block_name, parent_block_id, created, updated)
         * ),
         * validation AS (
         *   SELECT
         *     COUNT(*) as total_blocks,
         *     COUNT(CASE
         *       WHEN parent_block_id IS NULL THEN 1
         *       WHEN EXISTS (SELECT 1 FROM vfl_blocks WHERE id = batch_data.parent_block_id) THEN 1
         *       WHEN EXISTS (SELECT 1 FROM batch_data bd WHERE bd.id = batch_data.parent_block_id) THEN 1
         *       ELSE NULL
         *     END) as valid_blocks
         *   FROM batch_data
         * )
         * INSERT INTO vfl_blocks (id, block_name, parent_block_id, created, updated)
         * SELECT bd.id, bd.block_name, bd.parent_block_id, bd.created, bd.updated
         * FROM batch_data bd, validation v
         * WHERE v.total_blocks = v.valid_blocks
         * ON CONFLICT (id) DO NOTHING;
         */

        try {
            // Build the dynamic SQL with CTE validation
            StringBuilder sql = new StringBuilder();

            // CTE to hold batch data
            sql.append("WITH batch_data AS ( ");
            sql.append("SELECT * FROM (VALUES ");

            // Add placeholders for each block (5 parameters per block)
            for (int i = 0; i < blocks.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append("(?, ?, ?, ?, ?)");
            }

            sql.append(") AS blocks(id, block_name, parent_block_id, created, updated) ");
            sql.append("), ");

            // Validation CTE
            sql.append("validation AS ( ");
            sql.append("SELECT ");
            sql.append("COUNT(*) as total_blocks, ");
            sql.append("COUNT(CASE ");
            sql.append("WHEN parent_block_id IS NULL THEN 1 ");
            sql.append("WHEN EXISTS (SELECT 1 FROM vfl_blocks WHERE id = batch_data.parent_block_id) THEN 1 ");
            sql.append("WHEN EXISTS (SELECT 1 FROM batch_data bd WHERE bd.id = batch_data.parent_block_id) THEN 1 ");
            sql.append("ELSE NULL ");
            sql.append("END) as valid_blocks ");
            sql.append("FROM batch_data ");
            sql.append(") ");

            // Main INSERT with validation check
            sql.append("INSERT INTO vfl_blocks (id, block_name, parent_block_id, created, updated) ");
            sql.append("SELECT bd.id, bd.block_name, bd.parent_block_id, bd.created, bd.updated ");
            sql.append("FROM batch_data bd, validation v ");
            sql.append("WHERE v.total_blocks = v.valid_blocks ");  // Only proceed if all blocks are valid
            sql.append("ON CONFLICT (id) DO NOTHING");

            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                long time = Instant.now().toEpochMilli();

                // Set parameters for the batch_data CTE
                for (VflBlockDataType block : blocks) {
                    statement.setString(paramIndex++, block.getId());
                    statement.setString(paramIndex++, block.getBlockName());

                    // Handle Optional<String> for parent_block_id
                    if (block.getParentBlockId().isPresent()) {
                        statement.setString(paramIndex++, block.getParentBlockId().get());
                    } else {
                        statement.setNull(paramIndex++, java.sql.Types.VARCHAR);
                    }

                    statement.setLong(paramIndex++, time);
                    statement.setLong(paramIndex++, time);
                }

                int rowsAffected = statement.executeUpdate();

                // Check if validation failed (no rows inserted when it should have been)
                if (rowsAffected == 0 && !blocks.isEmpty()) {
                    // Perform a separate validation query to get specific error details
                    String validationQuery = buildValidationQuery(blocks.size());
                    try (PreparedStatement validationStmt = connection.prepareStatement(validationQuery)) {
                        int vParamIndex = 1;
                        for (VflBlockDataType block : blocks) {
                            validationStmt.setString(vParamIndex++, block.getId());
                            validationStmt.setString(vParamIndex++, block.getBlockName());
                            if (block.getParentBlockId().isPresent()) {
                                validationStmt.setString(vParamIndex++, block.getParentBlockId().get());
                            } else {
                                validationStmt.setNull(vParamIndex++, java.sql.Types.VARCHAR);
                            }
                            validationStmt.setLong(vParamIndex++, time);
                            validationStmt.setLong(vParamIndex++, time);
                        }

                        var resultSet = validationStmt.executeQuery();
                        if (resultSet.next()) {
                            int totalBlocks = resultSet.getInt("total_blocks");
                            int validBlocks = resultSet.getInt("valid_blocks");
                            if (totalBlocks != validBlocks) {
                                String errorMsg = String.format(
                                        "Validation failed: %d out of %d blocks have invalid parent_block_id. No blocks were inserted.",
                                        totalBlocks - validBlocks, totalBlocks
                                );
                                logger.error(errorMsg);
                                throw new IllegalArgumentException(errorMsg);
                            }
                        }
                    }
                }

                logger.debug("Upserted {} blocks", rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("Failed to upsert blocks", e);
            throw new RuntimeException("Failed to upsert blocks", e);
        }
    }

    private String buildValidationQuery(int blockCount) {
        StringBuilder sql = new StringBuilder();

        sql.append("WITH batch_data AS ( ");
        sql.append("SELECT * FROM (VALUES ");

        for (int i = 0; i < blockCount; i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?, ?, ?, ?)");
        }

        sql.append(") AS blocks(id, block_name, parent_block_id, created, updated) ");
        sql.append(") ");
        sql.append("SELECT ");
        sql.append("COUNT(*) as total_blocks, ");
        sql.append("COUNT(CASE ");
        sql.append("WHEN parent_block_id IS NULL THEN 1 ");
        sql.append("WHEN EXISTS (SELECT 1 FROM vfl_blocks WHERE id = batch_data.parent_block_id) THEN 1 ");
        sql.append("WHEN EXISTS (SELECT 1 FROM batch_data bd WHERE bd.id = batch_data.parent_block_id) THEN 1 ");
        sql.append("ELSE NULL ");
        sql.append("END) as valid_blocks ");
        sql.append("FROM batch_data");

        return sql.toString();
    }

    @Override
    public void upsertLogs(List<VflLogDataType> logs) {

    }
}
