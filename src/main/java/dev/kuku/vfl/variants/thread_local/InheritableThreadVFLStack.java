package dev.kuku.vfl.variants.thread_local;

import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.Util;
import lombok.extern.slf4j.Slf4j;

import java.util.Stack;

@Slf4j
public class InheritableThreadVFLStack extends InheritableThreadLocal<Stack<ThreadVFL>> {
    /*
    When a new thread is spawned, Only copy the latest ThreadVFL instance from the inheriting thread.
    The last element is the parent from which this thread is being spawned
     */
    @Override
    protected Stack<ThreadVFL> childValue(Stack<ThreadVFL> parentValue) {
        //Create a copy of latest logger from parent with inheritedContext set to true
        ThreadVFL latestVFL = new ThreadVFL(new VFLBlockContext(parentValue.peek().ctx.blockInfo, true, parentValue.peek().ctx.buffer));
        Stack<ThreadVFL> threadStack = new Stack<>();
        threadStack.add(latestVFL);
        log.debug("Creating new log stack with inherited parent VFL : {} in thread {}", Util.trimId(latestVFL.ctx.blockInfo.getId()), Util.getThreadInfo());
        return threadStack;
    }
}
