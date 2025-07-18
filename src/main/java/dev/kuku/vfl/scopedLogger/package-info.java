/**
 * Provides scoped logging functionality with hierarchical context management.
 *
 * <p>Scoped logger establishes a root-level boundary for logger operations and automatically
 * handles the creation of sub-loggers for nested blocks. When creating sub-scopes, the logger
 * context is updated to maintain proper hierarchical logging structure.
 *
 * <p>Key capabilities:
 * <ul>
 *   <li>Synchronous scope management via {@link dev.kuku.vfl.scopedLogger.ScopedLogger#run(java.lang.String, java.lang.String, java.lang.Runnable)}</li>
 *   <li>Asynchronous scope management via {@link dev.kuku.vfl.scopedLogger.ScopedLogger#runAsync(java.lang.String, java.lang.String, java.lang.Runnable)}</li>
 * </ul>
 *
 * <p>For operations executing on different threads (virtual or platform), the async variant
 * copies the current logger context to the thread's scoped value, ensuring proper isolation
 * and context propagation across thread boundaries.
 */
package dev.kuku.vfl.scopedLogger;