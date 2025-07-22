package dev.kuku.vfl.core.util;

import java.util.UUID;

public class HelperUtil {
    public static String generateUID() {
        return UUID.randomUUID().toString();
    }
}