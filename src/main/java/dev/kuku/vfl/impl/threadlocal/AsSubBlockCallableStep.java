package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A fluent builder for executing suppliers within diagram-logged sub-blocks with customizable start and end messages.
 *
 * <p>This class provides a fluent API for configuring and executing operations within VFL diagram logging
 * contexts. It supports different execution flow patterns that are visualized in the execution diagram:
 * sub-blocks within the main flow, detached branches, and forked flows that merge back.
 *
 * <h3>Execution Flow Types:</h3>
 * <ul>
 *   <li><strong>Sub-block:</strong> Nested within the main flow sequence</li>
 *   <li><strong>Detached:</strong> Branches off and never rejoins the main flow</li>
 *   <li><strong>Fork:</strong> Branches off but merges back into the main flow</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * String result = new AsSubBlockCallableStep<>(() -> performOperation(), "DataProcessing")
 *     .withStartMessage("Processing data for user {0}", userId)
 *     .withEndMessage("Completed processing with result: {0}")
 *     .execute(); // or executeDetached() or executeFork()
 * }</pre>
 *
 * <h3>Message Formatting:</h3>
 * <p>Both start and end messages support placeholder formatting using {@link Util#FormatMessage}.
 * End messages have access to the supplier's return value as the final parameter.
 *
 * @param <R> the return type of the supplier function
 * @author VFL Team
 * @since 1.0
 */
@RequiredArgsConstructor
public class AsSubBlockCallableStep<R> {
    private final Supplier<R> fn;
    private final String blockName;
    private String startMessage;
    private Function<R, String> endMessage;

    /**
     * Updates the message serializer to include formatted arguments and the return value.
     *
     * <p>This method wraps the user-provided message serializer with argument formatting logic.
     * It combines user-provided arguments with the supplier's return value according to the
     * args convention described in the class documentation.
     *
     * <h4>Argument Processing Logic:</h4>
     * <ol>
     *   <li>Execute the message serializer with the return value to get the base message template</li>
     *   <li>Pass the user-provided args as the first N parameters to {@link Util#FormatMessage}</li>
     *   <li>Pass the return value (R) as the final parameter to handle any remaining placeholders</li>
     * </ol>
     *
     * @param msgSerializer function that converts the return value to a message template
     * @param args          user-provided arguments for message formatting
     * @return enhanced message serializer that includes argument formatting
     */
    private Function<R, String> updateEndMsg(Function<R, String> msgSerializer, Object... args) {
        return (r) -> {
            // Get the message template from the user's serializer
            String messageTemplate = msgSerializer.apply(r);

            // Format the message with user args + return value
            // Args convention: user args fill {0}, {1}, {2}... and return value fills the last placeholder
            return Util.FormatMessage(messageTemplate, args, r);
        };
    }

    /**
     * Sets the message to be logged when the sub-block starts.
     *
     * <p>The start message supports placeholder formatting using {@link Util#FormatMessage}.
     * Placeholders are filled with the provided arguments in order.
     *
     * @param startMessage the message template to log at block start
     * @return this instance for method chaining
     */
    public AsSubBlockCallableStep<R> withStartMessage(String startMessage) {
        this.startMessage = startMessage;
        return this;
    }

    /**
     * Sets a dynamic end message using a function that receives the supplier's return value.
     *
     * <p>This method allows for complex end message generation based on the actual result
     * of the supplier execution. The message serializer function receives the return value
     * and should produce a message template that can be further formatted with the provided arguments.
     *
     * <h4>Message Formatting Order:</h4>
     * <ol>
     *   <li>The {@code endMessage} function is called with the return value</li>
     *   <li>The resulting template is formatted with {@code args} followed by the return value</li>
     * </ol>
     *
     * @param endMessage function that converts the return value to a message template
     * @param args       additional arguments for message formatting
     * @return this instance for method chaining
     */
    public AsSubBlockCallableStep<R> withEndMessage(Function<R, String> endMessage, Object... args) {
        this.endMessage = updateEndMsg(endMessage, args);
        return this;
    }

    /**
     * Sets a static end message template with optional formatting arguments.
     *
     * <p>The end message template supports placeholder formatting where user-provided arguments
     * fill the first N placeholders, and the supplier's return value fills the final placeholder.
     *
     * <h4>Example:</h4>
     * <pre>{@code
     * .withEndMessage("Processed {0} items with result: {1}", itemCount)
     * // If supplier returns "SUCCESS", logs: "Processed 42 items with result: SUCCESS"
     * }</pre>
     *
     * @param endMessage the message template to log at block end
     * @param args       arguments for message formatting (return value is automatically appended)
     * @return this instance for method chaining
     */
    public AsSubBlockCallableStep<R> withEndMessage(String endMessage, Object... args) {
        this.endMessage = (r) -> Util.FormatMessage(endMessage, args, r);
        return this;
    }

    /**
     * Executes the supplier as a sub-block within the main execution flow.
     *
     * <p>This operation appears as a nested block within the primary execution diagram,
     * maintaining the sequential flow structure. The sub-block is logically part of
     * the main execution path and represents a detailed breakdown of a larger operation.
     *
     * <p><strong>Diagram Representation:</strong> Shows as an indented/nested block
     * within the main flow sequence.
     *
     * <p><strong>Use this when:</strong> Breaking down complex operations into
     * smaller, trackable sub-operations that are part of the main workflow.
     *
     * @return the result of the supplier execution
     * @throws RuntimeException if the supplier throws an exception
     */
    public R execute() {
        return ThreadVFL.getCurrentLogger().supply(blockName, startMessage, fn, endMessage);
    }

    /**
     * Executes the supplier in a detached flow that never rejoins the main execution path.
     *
     * <p>This operation creates a separate branch in the execution diagram that diverges
     * from the main flow and continues independently. The detached flow represents
     * operations that run in isolation and don't contribute back to the primary
     * execution sequence.
     *
     * <p><strong>Diagram Representation:</strong> Shows as a branching path that
     * splits from the main flow without any merge point.
     *
     * <p><strong>Use this when:</strong> Performing auxiliary operations like
     * cleanup tasks, logging, metrics collection, or any operation whose outcome
     * doesn't affect the main execution flow.
     *
     * @return the result of the supplier execution
     * @throws RuntimeException if the supplier throws an exception
     */
    public R executeDetached() {
        return ThreadVFL.getCurrentLogger().supply(blockName, startMessage, fn, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN, endMessage);
    }

    /**
     * Executes the supplier in a forked flow that will merge back into the main execution path.
     *
     * <p>This operation creates a fork in the execution diagram where the flow splits
     * into parallel paths, with this branch eventually rejoining the main sequence.
     * The fork represents operations that can be conceptually separated but whose
     * results contribute to the overall execution outcome.
     *
     * <p><strong>Diagram Representation:</strong> Shows as a fork/split in the flow
     * with a corresponding join/merge point where it reconnects to the main path.
     *
     * <p><strong>Use this when:</strong> Performing operations that can be logically
     * separated (like parallel processing, validation checks, or sub-workflows)
     * but whose results are needed for the main execution to continue.
     *
     * @return the result of the supplier execution
     * @throws RuntimeException if the supplier throws an exception
     */
    public R executeFork() {
        return ThreadVFL.getCurrentLogger().supply(blockName, startMessage, fn, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN, endMessage);
    }

}