package dev.kuku.vfl.core.util;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

public class HelperUtil {
    public static String generateUID() {
        return UUID.randomUUID().toString();
    }

    public static String getCurrentMethodName() {
        return MethodHandles.lookup().lookupClass().getEnclosingMethod().getName();
    }
}
