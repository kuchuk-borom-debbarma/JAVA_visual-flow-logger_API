package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.kuku.vfl.core.helpers.VFLHelper.GetMethodName;

/**
 * ByteBuddy advice class injected into methods annotated with {@link SubBlock}.
 *
 * <p>This is applied at load‑time by {@link VFLInitializer} using ByteBuddy
 * and is responsible for:
 * <ul>
 *   <li>Creating a new VFL sub‑block on method entry</li>
 *   <li>Logging an optional start message</li>
 *   <li>Replacing any placeholders in block names or messages with
 *       runtime values (method args / return value)</li>
 *   <li>Logging exceptions if the method throws</li>
 *   <li>Popping the block stack and logging an optional end message on exit</li>
 * </ul>
 *
 * <h3>Placeholder resolution rules</h3>
 * The following placeholders used in {@link SubBlock#blockName()},
 * {@link SubBlock#startMessage()}, and {@link SubBlock#endMessage()} are replaced
 * at runtime:
 * <ul>
 *   <li><b>{0}, {1}, ...</b> — replaced with the string value of the Nth method argument
 *       (0‑indexed). Out‑of‑range indices are left unchanged.
 *       Null arguments are replaced with {@code "null"}.</li>
 *   <li><b>{r}</b> or <b>{return}</b> — replaced with the return value's string form in
 *       the <em>end message</em> only. Null replaced with {@code "null"}.
 *       Case‑insensitive.</li>
 * </ul>
 *
 * <p><b>Note:</b> This advice assumes it is only executed for methods that were
 * matched in the ByteBuddy transformation phase via
 * {@code ElementMatchers.isAnnotatedWith(SubBlock.class)}.
 */
@Slf4j
public class VFLAnnotationAdvice {

    public static final VFLAnnotationAdvice instance = new VFLAnnotationAdvice();

    // Regex for {0}, {1}, etc.
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\d+)}");
    // Regex for {r} or {return} (case-insensitive)
    private static final Pattern RETURN_PLACEHOLDER_PATTERN =
            Pattern.compile("\\{r(?:eturn)?}", Pattern.CASE_INSENSITIVE);

    /**
     * ByteBuddy entry point — delegates to {@link #on_enter(Method, Object[])}.
     */
    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin Method method,
                               @Advice.AllArguments Object[] args) {
        VFLAnnotationAdvice.instance.on_enter(method, args);
    }

    /**
     * ByteBuddy exit point — delegates to {@link #on_exit(Method, Object[], Object, Throwable)}.
     * Runs for both normal return and exceptional exit.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Origin Method method,
                              @Advice.AllArguments Object[] args,
                              @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnedValue,
                              @Advice.Thrown Throwable threw) {
        VFLAnnotationAdvice.instance.on_exit(method, args, returnedValue, threw);
    }

    /* -------------------- Runtime helper methods -------------------- */

    private boolean isValid(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Replace {0}, {1}, ... argument placeholders with actual argument string values.
     * Null arguments become "null". Invalid indexes are left unchanged.
     */
    private String replaceArgPlaceholders(String text, Object[] args) {
        if (text == null || args == null || args.length == 0) {
            return text;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int index;
            try {
                index = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                continue; // Should not happen for matched groups
            }
            String replacement;
            if (index >= 0 && index < args.length) {
                Object arg = args[index];
                replacement = (arg == null) ? "null" : arg.toString();
            } else {
                replacement = matcher.group(0); // leave as-is
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Replace {r} / {return} placeholders with return value string.
     * Only applied to endMessage. Null becomes "null".
     */
    private String replaceReturnPlaceholder(String text, Object returnedValue) {
        if (text == null) {
            return null;
        }
        String replacement = (returnedValue == null) ? "null" : returnedValue.toString();
        return RETURN_PLACEHOLDER_PATTERN.matcher(text)
                .replaceAll(Matcher.quoteReplacement(replacement));
    }

    /* -------------------- Actual enter/exit logic -------------------- */

    /**
     * Called at the start of a {@code @SubBlock} method.
     * Resolves block name, start message (with placeholders replaced),
     * creates and pushes a sub‑block, and issues a start log.
     * Skips if there is no active parent VFL block in context.
     */
    public void on_enter(Method method, Object[] args) {
        String blockName = VFLHelper.ResolveBlockName(method, args);
        String startMessage = VFLHelper.ResolveStartMessage(method, args);

        log.debug("Entered SubBlock: {}", blockName);

        BlockContext parentBlockContext = ThreadContextManager.GetCurrentBlockContext();
        if (parentBlockContext == null) {
            log.warn("Could not create block for @SubBlock-{}: no parent block", blockName);
            return;
        }

        log.debug("Creating sub-block '{}' from parent '{}-{}'.",
                blockName,
                parentBlockContext.blockInfo.getBlockName(),
                VFLHelper.TrimId(parentBlockContext.blockInfo.getId()));

        Block subBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(
                blockName,
                parentBlockContext.blockInfo.getId(),
                VFLInitializer.VFLAnnotationConfig.buffer
        );

        SubBlockStartLog subBlockStartLog = VFLFlowHelper.CreateLogAndPush2Buffer(
                parentBlockContext.blockInfo.getId(),
                parentBlockContext.currentLogId,
                startMessage,
                subBlock.getId(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY,
                VFLInitializer.VFLAnnotationConfig.buffer
        );

        Objects.requireNonNull(ThreadContextManager.GetCurrentBlockContext()).currentLogId = subBlockStartLog.getId();
        ThreadContextManager.PushBlockToThreadLogStack(subBlock);
        Log.INSTANCE.ensureBlockStarted();
    }

    /**
     * Called on method exit (normal or exceptional).
     * <ul>
     *   <li>If an exception was thrown, logs it immediately at error level</li>
     *   <li>Resolves the end message (with arguments & return value placeholders)</li>
     *   <li>Pops the current block off the thread local stack</li>
     * </ul>
     */
    public void on_exit(Method method, Object[] args, Object returnedValue, Throwable threw) {
        String blockName = VFLHelper.ResolveBlockName(method, args);

        if (threw != null) {
            Log.Error("Exception in SubBlock '{}': {} - {}",
                    blockName,
                    threw.getClass().getName(),
                    threw.getMessage());
        }

        String endMsg = VFLHelper.ResolveEndMessage(method, args, returnedValue);
        ThreadContextManager.PopCurrentStack(endMsg);
    }
}
