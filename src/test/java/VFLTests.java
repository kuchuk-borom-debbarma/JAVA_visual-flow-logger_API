import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

public class VFLTests {
    void wrapper(Consumer<Void> fn) {
        System.out.println("Executing " + fn.toString());
        fn.accept(null);
        System.out.println("Done");
    }

    @Fn
    void foo() {
        int a = 12 + 2;
        System.out.println("A = " + a);
        bar();
    }

    @Fn
    void bar() {
        int sum = 1 * 234;
        System.out.println(sum);
    }

    @Test
    void expected() {
        wrapper(_ -> {
            foo();
            //since bar is annotated as Fn it needs sub wrapper
            wrapper(_ -> bar());
        });
    }


}
