package dev.kuku.vfl.impl.threadlocal.annotations;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadLocalAdviceData {
    public static final ThreadLocal<SpawnedThreadContext> parentThreadLoggerData = new ThreadLocal<>();
    public static Logger log = LoggerFactory.getLogger(ThreadLocalLogAnnotationProcessor.class);
    public static VFLBuffer buffer;
}
