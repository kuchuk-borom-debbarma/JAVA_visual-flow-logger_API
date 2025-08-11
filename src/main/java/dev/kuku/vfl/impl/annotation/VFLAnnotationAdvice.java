package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
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

import static dev.kuku.vfl.core.helpers.Util.GetMethodName;

@Slf4j
public class VFLAnnotationAdvice {

    public static final VFLAnnotationAdvice instance = new VFLAnnotationAdvice();

    // Regex for {0}, {1}, etc.
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\d+)}");
    // Regex for {r} or {return} (case-insensitive)
    private static final Pattern RETURN_PLACEHOLDER_PATTERN = Pattern.compile("\\{r(?:eturn)?}", Pattern.CASE_INSENSITIVE);

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin Method method, @Advice.AllArguments Object[] args) {
        VFLAnnotationAdvice.instance.on_enter(method, args);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Origin Method method,
                              @Advice.AllArguments Object[] args,
                              @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnedValue,
                              @Advice.Thrown Throwable threw) {
        VFLAnnotationAdvice.instance.on_exit(method, args, returnedValue, threw);
    }

    /* -------------------- RESOLUTION HELPERS -------------------- */

    /**
     * Get block name from annotation or use the method name.
     * Replaces {0}, {1}... placeholders with argument values.
     */
    private String resolveBlockName(Method method, Object[] args) {
        SubBlock annotation = method.getAnnotation(SubBlock.class);
        String rawName = (annotation != null && isValid(annotation.blockName()))
                ? annotation.blockName().trim()
                : GetMethodName(method, args);

        return replaceArgPlaceholders(rawName, args);
    }

    /**
     * Get start message from annotation if specified.
     * Supports only argument placeholders.
     */
    private String resolveStartMessage(Method method, Object[] args) {
        SubBlock annotation = method.getAnnotation(SubBlock.class);
        if (annotation != null && isValid(annotation.startMessage())) {
            String msg = annotation.startMessage().trim();
            return replaceArgPlaceholders(msg, args);
        }
        return null; // no start message
    }

    /**
     * Get end message from annotation if specified.
     * Supports argument placeholders and {r}/{return}.
     */
    private String resolveEndMessage(Method method, Object[] args, Object returnedValue) {
        SubBlock annotation = method.getAnnotation(SubBlock.class);
        if (annotation != null && isValid(annotation.endMessage())) {
            String msg = annotation.endMessage().trim();
            msg = replaceArgPlaceholders(msg, args);
            msg = replaceReturnPlaceholder(msg, returnedValue);
            return msg;
        }
        return null; // no end message
    }

    private boolean isValid(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /* -------------------- PLACEHOLDER REPLACERS -------------------- */

    /**
     * Replace {0}, {1}, etc. with args[i].toString() or "null".
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
                continue; // should not happen
            }

            String replacement;
            if (index >= 0 && index < args.length) {
                Object arg = args[index];
                replacement = (arg == null) ? "null" : arg.toString();
            } else {
                replacement = matcher.group(0); // leave as-is if out of range
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Replace {r} or {return} with returnedValue.toString() or "null".
     */
    private String replaceReturnPlaceholder(String text, Object returnedValue) {
        if (text == null) {
            return null;
        }
        String replacement = (returnedValue == null) ? "null" : returnedValue.toString();
        return RETURN_PLACEHOLDER_PATTERN.matcher(text)
                .replaceAll(Matcher.quoteReplacement(replacement));
    }

    /* -------------------- METHOD HOOKS -------------------- */

    public void on_enter(Method method, Object[] args) {
        String blockName = resolveBlockName(method, args);
        String startMessage = resolveStartMessage(method, args);

        log.debug("Entered SubBlock: {}", blockName);

        BlockContext parentBlockContext = ThreadContextManager.GetCurrentBlockContext();
        if (parentBlockContext == null) {
            log.warn("Could not create block for @SubBlock-{}: no parent block", blockName);
            return;
        }

        log.debug("Creating sub-block '{}' from parent '{}-{}'.",
                blockName,
                parentBlockContext.blockInfo.getBlockName(),
                Util.TrimId(parentBlockContext.blockInfo.getId()));

        Block subBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(
                blockName,
                parentBlockContext.blockInfo.getId(),
                VFLInitializer.VFLAnnotationConfig.buffer
        );

        // Pass start message to log
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
    }

    public void on_exit(Method method, Object[] args, Object returnedValue, Throwable threw) {
        String blockName = resolveBlockName(method, args);

        if (threw != null) {
            Log.Error("Exception in SubBlock '{}': {} - {}",
                    blockName,
                    threw.getClass().getName(),
                    threw.getMessage());
        }

        String endMsg = resolveEndMessage(method, args, returnedValue);
        ThreadContextManager.PopCurrentStack(endMsg);
    }
}
