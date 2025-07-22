package dev.kuku.vfl.core.models;

import dev.kuku.vfl.IVFL;

/// Wrapper class to transfer 3 objects at once
public record LoggerAndBlockLogData(IVFL logger, BlockData blockData, LogData logData) {

}