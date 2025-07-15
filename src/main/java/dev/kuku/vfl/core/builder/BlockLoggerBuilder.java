package dev.kuku.vfl.core.builder;

import dev.kuku.vfl.core.buffer.SynchronousBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.logger.BlockLogger;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.serviceCall.NaiveVFLServerAPI;
import dev.kuku.vfl.core.serviceCall.VFLApi;

import java.util.Objects;
import java.util.UUID;

public class BlockLoggerBuilder {
    private BlockLoggerBuilder() {
    }

    //Options allowed when creating 1. Specify block data OR specify root block name
    public static FirstStep create() {
        return new BlockLoggerBuilderInternal();
    }

    public interface FirstStep {
        SecondStep rootBlock(String blockName);

        SecondStep fromBlockData(BlockData blockData);
    }

    public interface SecondStep {
        OptionalStep apiUrl(String url);
    }

    public interface OptionalStep {
        OptionalStep buffer(VFLBuffer buffer);

        OptionalStep bufferSize(int bufferSize);

        OptionalStep Api(VFLApi api);

        BlockLogger build();
    }

    private static class BlockLoggerBuilderInternal implements FirstStep, SecondStep, OptionalStep {
        private String blockName;
        private BlockData blockData;
        private VFLBuffer buffer;
        private int bufferSize;
        private VFLApi api;
        private String apiLink;

        @Override
        public SecondStep rootBlock(String blockName) {
            Objects.requireNonNull(blockName, "blockName is null");
            this.blockName = blockName;
            return this;
        }

        @Override
        public SecondStep fromBlockData(BlockData blockData) {
            Objects.requireNonNull(blockData, "blockData is null");
            Objects.requireNonNull(blockData.getBlockName(), "blockName is null");
            Objects.requireNonNull(blockData.getId(), "blockId is null");
            this.blockData = blockData;
            return this;
        }

        @Override
        public OptionalStep buffer(VFLBuffer buffer) {
            Objects.requireNonNull(buffer, "buffer is null");
            this.buffer = buffer;
            return this;
        }

        @Override
        public OptionalStep bufferSize(int bufferSize) {
            if (bufferSize <= 0) {
                throw new IllegalArgumentException("bufferSize must be > 0");
            }
            this.bufferSize = bufferSize;
            return this;
        }

        @Override
        public OptionalStep Api(VFLApi api) {
            this.api = api;
            return this;
        }

        @Override
        public BlockLogger build() {
            var bd = Objects.requireNonNullElseGet(blockData, () -> new BlockData(UUID.randomUUID().toString(), null, this.blockName));
            var a = Objects.requireNonNullElseGet(this.api, () -> new NaiveVFLServerAPI(this.apiLink));
            var b = Objects.requireNonNullElseGet(this.buffer, () -> new SynchronousBuffer(this.bufferSize, a));
            return new BlockLogger(bd, b);
        }

        @Override
        public OptionalStep apiUrl(String url) {
            this.apiLink = url;
            return this;
        }
    }
}
