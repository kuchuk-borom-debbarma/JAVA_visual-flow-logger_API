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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.kuku.vfl.core.helpers.Util.GetMethodName;

@Slf4j
public class VFLAnnotationAdvice {
    public static final VFLAnnotationAdvice instance = new VFLAnnotationAdvice();

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\d+)\\}");
    private static final Pattern RETURN_PLACEHOLDER_PATTERN = Pattern.compile("\\{return\\}", Pattern.CASE_INSENSITIVE);

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

    /**
     * Resolves blockName from annotation or method name.
     */
    private String resolveBlockName(Method method, Object[] args) {
        SubBlock annotation = method.getAnnotation(SubBlock.class);
        String rawName = (annotation != null && isValid(annotation.blockName()))
                ? annotation.blockName().trim()
                : GetMethodName(method, args);
        return replaceArgPlaceholders(rawName, args);
    }

    /**
     * Resolves startMessage from annotation or returns null.
     */
    private String resolveStartMessage(Method method, Object[] args) {
        SubBlock annotation = method.getAnnotation(SubBlock.class);
        if (annotation != null && isValid(annotation.startMessage())) {
            return replaceArgPlaceholders(annotation.startMessage().trim(), args);
        }
        return null; // fallback: no message
    }

    /**
     * Resolves endMessage from annotation or default.
     */
    private String resolveEndMessage(Method method, Object[] args, Object returnedValue) {
        SubBlock annotation = method.getAnnotation(SubBlock.class);
        String template;
        if (annotation != null && isValid(annotation.endMessage())) {
            template = annotation.endMessage().trim();
        } else {
            template = "Returned : {return}";
        }
        // first replace {0} args placeholders
        String msg = replaceArgPlaceholders(template, args);
        // now replace {return}
        return RETURN_PLACEHOLDER_PATTERN.matcher(msg)
                .replaceAll(Matcher.quoteReplacement(String.valueOf(returnedValue)));
    }

    private boolean isValid(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Replace {0}, {1}, ... placeholders with arg.toString(), "null" for nulls.
     */
    private String replaceArgPlaceholders(String text, Object[] args) {
        if (text == null || args == null || args.length == 0) {
            return text;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int index;
            try {
                index = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                continue;
            }
            String replacement;
            if (index >= 0 && index < args.length) {
                Object arg = args[index];
                replacement = (arg == null) ? "null" : arg.toString();
            } else {
                replacement = matcher.group(0); // keep original if out of bounds
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

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

        // Use startMessage instead of hardcoded null
        SubBlockStartLog subBlockStartLog = VFLFlowHelper.CreateLogAndPush2Buffer(
                parentBlockContext.blockInfo.getId(),
                parentBlockContext.currentLogId,
                startMessage,
                subBlock.getId(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY,
                VFLInitializer.VFLAnnotationConfig.buffer
        );

        ThreadContextManager.GetCurrentBlockContext().currentLogId = subBlockStartLog.getId();
        ThreadContextManager.PushBlockToThreadLogStack(subBlock);
    }

    public void on_exit(Method method, Object[] args, Object returnedValue, Throwable threw) {
        String blockName = resolveBlockName(method, args);

        if (threw != null) {
            Log.Error("Exception in SubBlock '{}': {} - {}", blockName, threw.getClass().getName(), threw.getMessage());
        }

        String endMsg = resolveEndMessage(method, args, returnedValue);
        ThreadContextManager.CloseAndPopCurrentContext(endMsg);
    }
}
