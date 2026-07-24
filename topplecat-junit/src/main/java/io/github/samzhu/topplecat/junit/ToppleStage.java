package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.ToppleCatException;

import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Method;

/**
 * Base class for canonical {@link ToppleTest} Java stages. A step method calls
 * {@link #recorded(Object...)} first, performs its work, and returns {@link #self()}.
 */
public abstract class ToppleStage<SELF extends ToppleStage<SELF>> {
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private ToppleNarrative.Session narrative;

    /** Records the calling step method and returns this stage for fluent Java code. */
    protected final SELF recorded(Object... arguments) {
        StackFrame caller = WALKER.walk(frames -> frames.skip(1).findFirst())
                .orElseThrow(() -> new ToppleCatException("Cannot locate the ToppleStage step method."));
        if (narrative != null) {
            narrative.record(this, caller, arguments == null ? new Object[0] : arguments.clone());
        }
        return self();
    }

    /** Adds an allowlisted evidence attachment to the currently active Stage step. */
    protected final SELF attach(ToppleAttachment attachment) {
        if (narrative == null) {
            throw new ToppleCatException("Topple attachment requires an active ToppleCase invocation.");
        }
        narrative.attach(attachment);
        return self();
    }

    /** Returns the concrete stage instance for fluent step methods. */
    @SuppressWarnings("unchecked")
    protected SELF self() {
        return (SELF) this;
    }

    final void bindNarrative(ToppleNarrative.Session session) {
        this.narrative = session;
    }

    static String sentence(StackFrame caller, Object[] arguments) {
        Method method = method(caller);
        As as = method.getAnnotation(As.class);
        return ToppleStageSentence.runtime(caller.getMethodName(), as == null ? null : as.value(), arguments,
                method.toString());
    }

    /** JVM identity shared with the compiler descriptor; no simple-name or argument-count matching is used. */
    static String stepId(StackFrame caller) {
        return caller.getDeclaringClass().getName() + "#" + caller.getMethodName()
                + caller.getMethodType().descriptorString();
    }

    private static Method method(StackFrame caller) {
        try {
            return caller.getDeclaringClass().getDeclaredMethod(caller.getMethodName(),
                    caller.getMethodType().parameterArray());
        } catch (NoSuchMethodException exception) {
            throw new ToppleCatException("Cannot resolve ToppleStage method " + caller.getClassName() + "."
                    + caller.getMethodName() + ".", exception);
        }
    }

}
