package dev.kuku.vfl.core.util;

import dev.kuku.vfl.core.IVFL;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class HelperUtil {
    public static String generateUID() {
        return UUID.randomUUID().toString();
    }
}