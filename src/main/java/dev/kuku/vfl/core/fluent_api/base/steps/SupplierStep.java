package dev.kuku.vfl.core.fluent_api.base.steps;

import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fluent API step for executing suppliers with configurable logging output.
 *
 * <p>This class provides a fluent interface for executing {@link Supplier} functions
 * while automatically logging their results at different severity levels (LOG, WARN, ERROR).
 * The logging messages support parameterized formatting with a special convention for
 * handling the supplier's return value.
 *
 * <h3>Message Formatting and Args Convention:</h3>
 * <p>The {@code args} parameter in the logging methods follows a specific convention:
 * <ul>
 *   <li><strong>User-provided args:</strong> The first N arguments are mapped to placeholders
 *       in the message template in order ({0}, {1}, {2}, etc.)</li>
 *   <li><strong>Return value mapping:</strong> If there is one additional placeholder beyond
 *       the provided args, it is automatically mapped to the supplier's return value (R)</li>
 * </ul>
 *
 * <h4>Example Usage:</h4>
 * <pre>{@code
 * // Scenario 1: Only user-provided args
 * supplierStep.asLog(result -> "Processing {0} with {1}", "data", "config");
 * // Result: "Processing data with config"
 *
 * // Scenario 2: User args + return value
 * supplierStep.asLog(result -> "Processed {0} and got result: {1}", "data");
 * // If supplier returns "SUCCESS", result: "Processed data and got result: SUCCESS"
 *
 * // Scenario 3: Only return value
 * supplierStep.asLog(result -> "Operation completed with result: {0}");
 * // If supplier returns 42, result: "Operation completed with result: 42"
 * }</pre>
 *
 * @param <R> the return type of the supplier function
 * @author VFL Framework
 * @see VFL
 * @see Supplier
 * @since 1.0
 */
@RequiredArgsConstructor
public class SupplierStep<R> {

    /**
     * The VFL logger instance used for executing and logging the supplier.
     */
    protected final VFL vfl;

    /**
     * The supplier function to be executed and logged.
     */
    protected final Supplier<R> supplier;

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
     * @param args user-provided arguments for message formatting
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
     * Executes the supplier and logs the result at MESSAGE level.
     *
     * <p>This method runs the supplier function and logs its result using the provided
     * message serializer and arguments. The logging occurs at the standard MESSAGE level.
     *
     * <h4>Args Convention:</h4>
     * <p>Arguments are processed in the following order:
     * <ol>
     *   <li>User-provided {@code args} fill placeholders {0}, {1}, {2}, etc. in sequence</li>
     *   <li>If there's an additional placeholder, it's filled with the supplier's return value</li>
     * </ol>
     *
     * @param messageSerializer function to convert the return value into a message template
     * @param args optional arguments for message formatting (see class documentation for convention)
     * @return the result returned by the supplier
     *
     * @example
     * <pre>{@code
     * String result = supplierStep.asLog(
     *     r -> "Processed file {0} with {1} records",
     *     "data.csv"  // {0} = "data.csv", {1} = return value
     * );
     * }</pre>
     */
    public R asLog(Function<R, String> messageSerializer, Object... args) {
        return vfl.logFn(supplier, updateEndMsg(messageSerializer, args));
    }

    /**
     * Executes the supplier and logs the result at ERROR level.
     *
     * <p>This method runs the supplier function and logs its result using the provided
     * error message serializer and arguments. The logging occurs at the ERROR level,
     * typically used for logging error conditions or failure scenarios.
     *
     * <h4>Args Convention:</h4>
     * <p>Arguments are processed in the following order:
     * <ol>
     *   <li>User-provided {@code args} fill placeholders {0}, {1}, {2}, etc. in sequence</li>
     *   <li>If there's an additional placeholder, it's filled with the supplier's return value</li>
     * </ol>
     *
     * @param errorMessageSerializer function to convert the return value into an error message template
     * @param args optional arguments for message formatting (see class documentation for convention)
     * @return the result returned by the supplier
     *
     * @example
     * <pre>{@code
     * ValidationResult result = supplierStep.asError(
     *     r -> "Validation failed for {0}: {1}",
     *     "user input"  // {0} = "user input", {1} = return value (ValidationResult)
     * );
     * }</pre>
     */
    public R asError(Function<R, String> errorMessageSerializer, Object... args) {
        return vfl.errorFn(supplier, updateEndMsg(errorMessageSerializer, args));
    }

    /**
     * Executes the supplier and logs the result at WARNING level.
     *
     * <p>This method runs the supplier function and logs its result using the provided
     * warning message serializer and arguments. The logging occurs at the WARN level,
     * typically used for logging concerning conditions that don't prevent operation.
     *
     * <h4>Args Convention:</h4>
     * <p>Arguments are processed in the following order:
     * <ol>
     *   <li>User-provided {@code args} fill placeholders {0}, {1}, {2}, etc. in sequence</li>
     *   <li>If there's an additional placeholder, it's filled with the supplier's return value</li>
     * </ol>
     *
     * @param warningMessageSerializer function to convert the return value into a warning message template
     * @param args optional arguments for message formatting (see class documentation for convention)
     * @return the result returned by the supplier
     *
     * @example
     * <pre>{@code
     * CacheResult result = supplierStep.asWarning(
     *     r -> "Cache miss for key {0}, fallback returned: {1}",
     *     "user:123"  // {0} = "user:123", {1} = return value (CacheResult)
     * );
     * }</pre>
     */
    public R asWarning(Function<R, String> warningMessageSerializer, Object... args) {
        return vfl.warnFn(supplier, updateEndMsg(warningMessageSerializer, args));
    }

    // ========== STRING MESSAGE OVERLOADS ==========
    // These overloads provide a more convenient API when you have a static message template
    // and don't need dynamic message generation based on the return value.

    /**
     * Executes the supplier and logs the result at MESSAGE level using a static message template.
     *
     * <p>This is a convenience overload that allows you to provide a static message template
     * instead of a function-based message serializer. The message template is formatted using
     * the same args convention as the function-based version.
     *
     * <h4>Args Convention:</h4>
     * <p>Arguments are processed in the following order:
     * <ol>
     *   <li>User-provided {@code args} fill placeholders {0}, {1}, {2}, etc. in sequence</li>
     *   <li>If there's an additional placeholder, it's filled with the supplier's return value</li>
     * </ol>
     *
     * <p><strong>Note:</strong> This method creates a message serializer internally that ignores
     * the return value parameter (since the message template is static) and delegates to the
     * function-based {@link #asLog(Function, Object...)} method.
     *
     * @param message static message template with placeholders for formatting
     * @param args optional arguments for message formatting (see class documentation for convention)
     * @return the result returned by the supplier
     *
     * @example
     * <pre>{@code
     * // Simple message with only user args
     * String result = supplierStep.asLog("Processing file {0} with mode {1}", "data.csv", "read");
     *
     * // Message with user args + return value
     * String result = supplierStep.asLog("Processed {0} and got result: {1}", "data.csv");
     * // If supplier returns "SUCCESS", logs: "Processed data.csv and got result: SUCCESS"
     * }</pre>
     */
    public R asLog(String message, Object... args) {
        // Create a serializer that ignores the return value parameter and uses the static message
        Function<R, String> serializer = (r) -> Util.FormatMessage(message, args, r);

        // Delegate to the function-based version (pass empty args since formatting is handled in serializer)
        return asLog(serializer);
    }

    /**
     * Executes the supplier and logs the result at ERROR level using a static message template.
     *
     * <p>This is a convenience overload that allows you to provide a static error message template
     * instead of a function-based message serializer. The message template is formatted using
     * the same args convention as the function-based version.
     *
     * <h4>Args Convention:</h4>
     * <p>Arguments are processed in the following order:
     * <ol>
     *   <li>User-provided {@code args} fill placeholders {0}, {1}, {2}, etc. in sequence</li>
     *   <li>If there's an additional placeholder, it's filled with the supplier's return value</li>
     * </ol>
     *
     * <p><strong>Note:</strong> This method creates a message serializer internally that ignores
     * the return value parameter (since the message template is static) and delegates to the
     * function-based {@link #asError(Function, Object...)} method.
     *
     * @param message static error message template with placeholders for formatting
     * @param args optional arguments for message formatting (see class documentation for convention)
     * @return the result returned by the supplier
     *
     * @example
     * <pre>{@code
     * // Error message with user args + return value
     * ValidationResult result = supplierStep.asError(
     *     "Validation failed for input {0}: {1}",
     *     "user@example.com"
     * );
     * // If supplier returns ValidationResult with errors, logs:
     * // "Validation failed for input user@example.com: [validation details]"
     * }</pre>
     */
    public R asError(String message, Object... args) {
        // Create a serializer that ignores the return value parameter and uses the static message
        Function<R, String> serializer = (r) -> Util.FormatMessage(message, args, r);

        // Delegate to the function-based version (pass empty args since formatting is handled in serializer)
        return asError(serializer);
    }

    /**
     * Executes the supplier and logs the result at WARNING level using a static message template.
     *
     * <p>This is a convenience overload that allows you to provide a static warning message template
     * instead of a function-based message serializer. The message template is formatted using
     * the same args convention as the function-based version.
     *
     * <h4>Args Convention:</h4>
     * <p>Arguments are processed in the following order:
     * <ol>
     *   <li>User-provided {@code args} fill placeholders {0}, {1}, {2}, etc. in sequence</li>
     *   <li>If there's an additional placeholder, it's filled with the supplier's return value</li>
     * </ol>
     *
     * <p><strong>Note:</strong> This method creates a message serializer internally that ignores
     * the return value parameter (since the message template is static) and delegates to the
     * function-based {@link #asWarning(Function, Object...)} method.
     *
     * @param message static warning message template with placeholders for formatting
     * @param args optional arguments for message formatting (see class documentation for convention)
     * @return the result returned by the supplier
     *
     * @example
     * <pre>{@code
     * // Warning message with user args + return value
     * CacheResult result = supplierStep.asWarning(
     *     "Cache miss for key {0}, using fallback: {1}",
     *     "user:123"
     * );
     * // If supplier returns CacheResult with fallback data, logs:
     * // "Cache miss for key user:123, using fallback: [fallback data]"
     * }</pre>
     */
    public R asWarning(String message, Object... args) {
        // Create a serializer that ignores the return value parameter and uses the static message
        Function<R, String> serializer = (r) -> Util.FormatMessage(message, args, r);

        // Delegate to the function-based version (pass empty args since formatting is handled in serializer)
        return asWarning(serializer);
    }
}