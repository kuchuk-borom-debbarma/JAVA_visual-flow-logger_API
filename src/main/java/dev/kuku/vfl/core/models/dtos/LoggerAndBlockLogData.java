package dev.kuku.vfl.core.models.dtos;

import dev.kuku.vfl.IVFL;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;

/// Wrapper class to transfer 3 objects at once
public record LoggerAndBlockLogData(IVFL logger, Block block, Log log) {

}