package dev.kuku.vfl.core.fluent_api.steps;

import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;
import java.util.function.Supplier;


@RequiredArgsConstructor
public class SupplierStep<R> {
    protected final VFL vfl;
    protected final Supplier<R> supplier;

    private Function<R, String> updateEndMsg(Function<R, String> msgSerializer, Object... args) {
        return (r) -> {
            // Get the message template from the user's serializer
            String messageTemplate = msgSerializer.apply(r);

            // Format the message with user args + return value
            // Args convention: user args fill {0}, {1}, {2}... and return value fills the last placeholder
            return Util.FormatMessage(messageTemplate, args, r);
        };
    }


    public R asLog(Function<R, String> messageSerializer, Object... args) {
        return vfl.logFn(supplier, updateEndMsg(messageSerializer, args));
    }

    public R asError(Function<R, String> errorMessageSerializer, Object... args) {
        return vfl.errorFn(supplier, updateEndMsg(errorMessageSerializer, args));
    }

    public R asWarning(Function<R, String> warningMessageSerializer, Object... args) {
        return vfl.warnFn(supplier, updateEndMsg(warningMessageSerializer, args));
    }

    // ========== STRING MESSAGE OVERLOADS ==========
    // These overloads provide a more convenient API when you have a static message template
    // and don't need dynamic message generation based on the return value.

    public R asLog(String message, Object... args) {
        Function<R, String> serializer = (r) -> {
            Object[] allArgs = Util.CombineArgsWithReturn(args, r);
            return Util.FormatMessage(message, allArgs);
        };

        return asLog(serializer);
    }

    public R asError(String message, Object... args) {
        Function<R, String> serializer = (r) -> {
            Object[] allArgs = Util.CombineArgsWithReturn(args, r);
            return Util.FormatMessage(message, allArgs);
        };

        return asError(serializer);
    }

    public R asWarning(String message, Object... args) {
        Function<R, String> serializer = (r) -> {
            Object[] allArgs = Util.CombineArgsWithReturn(args, r);
            return Util.FormatMessage(message, allArgs);
        };

        return asWarning(serializer);
    }
}