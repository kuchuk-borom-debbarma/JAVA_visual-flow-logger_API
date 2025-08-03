package bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.concurrent.CompletableFuture;

public class ByteBuddyInterceptorTest {

    public static class WrapperAdvice {
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean wrap(@Advice.Argument(0) Runnable runnable) {
            System.out.println("wrapper start");
            try {
                runnable.run();
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
                throw e;
            } finally {
                System.out.println("wrapper end");
            }
            return true; // Skip original method
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter boolean skipped,
                                @Advice.Return(readOnly = false) CompletableFuture<Void> result) {
            if (skipped) {
                result = CompletableFuture.completedFuture(null);
            }
        }
    }

    @org.junit.jupiter.api.Test
    public void test() {
        ByteBuddyAgent.install();

        new ByteBuddy()
                .redefine(CompletableFuture.class)
                .visit(Advice.to(WrapperAdvice.class).on(
                        ElementMatchers.named("runAsync")
                                .and(ElementMatchers.takesArguments(1))
                ))
                .make()
                .load(CompletableFuture.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

        CompletableFuture.runAsync(() -> System.out.println("hello world")).join();
    }
}