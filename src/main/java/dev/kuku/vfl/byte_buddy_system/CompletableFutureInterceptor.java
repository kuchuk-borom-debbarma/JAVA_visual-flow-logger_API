package dev.kuku.vfl.byte_buddy_system;

import dev.kuku.vfl.variants.thread_local.ThreadVFL;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class CompletableFutureInterceptor {
    @RuntimeType
    public static CompletableFuture<?> interceptSupplyAsync(
            @AllArguments Object[] args,
            @SuperCall Callable<CompletableFuture<?>> originalMethod) {

        var l = ByteBuddyVFLData.BYTE_BUDDY_ASYNC_LOGGERS.get().peek();
        if (l != null && args.length > 0 && args[0] instanceof Supplier<?> originalSupplier) {
            Supplier<?> wrappedSupplier = () -> {
                ThreadVFL.loggerStack.get().push(l);
                try {
                    return originalSupplier.get();
                } finally {
                    ByteBuddyVFLData.BYTE_BUDDY_ASYNC_LOGGERS.remove();
                }
            };
            args[0] = wrappedSupplier;
        }
        //If we reach here it means l is null and we will simply call originalMethod
        try {
            return originalMethod.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
