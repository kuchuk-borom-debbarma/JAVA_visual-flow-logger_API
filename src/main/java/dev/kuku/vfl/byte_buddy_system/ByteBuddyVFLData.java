package dev.kuku.vfl.byte_buddy_system;

import dev.kuku.vfl.variants.thread_local.ThreadVFL;

import java.util.Stack;

public class ByteBuddyVFLData {
    public static ThreadLocal<Stack<ThreadVFL>> BYTE_BUDDY_ASYNC_LOGGERS = new ThreadLocal<>();
}
