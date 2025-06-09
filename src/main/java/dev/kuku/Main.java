package dev.kuku;

import dev.kuku.vfl.VflBlockOperator;
import dev.kuku.vfl.VflClientBuilder;

public class Main {
    public static void main(String[] args) {
        rootOperation();
    }

    public static void rootOperation() {
        var client = VflClientBuilder.start().build();
        client.startRootBlock("root", a -> {
            a.log("start root block");
            a.log("Next block will be nested block");
            double ans = a.log(vflBlockOperator -> sum(1, 2, vflBlockOperator), "Adding 1 and 2", aDouble -> "Sum result is " + aDouble, "sum");
            System.out.println("GGEZ");
            return null;
        });
    }

    public static double sum(double a, double b, VflBlockOperator block) {
        block.log("sum " + a + " + " + b + " is " + (a + b));
        return a + b;
    }
}