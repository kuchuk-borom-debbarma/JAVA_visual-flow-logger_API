package dev.kuku.vfl;

import dev.kuku.vfl.impl.threadlocal.ThreadVFLAnnotationInitializer;
import dev.kuku.vfl.impl.threadlocal.VFLBlock;


// Separate class that gets loaded AFTER agent is installed
class UserService {
    @VFLBlock
    public void foo(String msg) {
        System.out.println("inside foo: " + msg);
    }
}

public class TestBlockLibrary {
    public static void main(String[] args) {
        // Initialize agent FIRST
        ThreadVFLAnnotationInitializer.initialise();

        // THEN create and use the target class
        UserService service = new UserService();
        service.foo("hello");
    }
}
