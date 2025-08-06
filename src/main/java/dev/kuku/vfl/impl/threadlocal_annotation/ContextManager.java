package dev.kuku.vfl.impl.threadlocal_annotation;

import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.impl.threadlocal_annotation.annotations.SpawnedThreadContext;

import java.util.Stack;

/**
 * Provides context that is persistent per thread
 */
public class ContextManager {
    public static final ThreadLocal<Stack<VFLBlockContext>> loggerCtxStack = new ThreadLocal<>();
    public static VFL logger = new VFL() {
        @Override
        protected VFLBlockContext getContext() {
            return loggerCtxStack.get().peek();
        }
    };
    public static ThreadLocal<SpawnedThreadContext> spawnedThreadContext = new ThreadLocal<>();
}
