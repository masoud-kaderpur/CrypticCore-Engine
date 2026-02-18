package at.tuwien.crypticcore.io;

/**
 * Callback interface for monitoring data processing.
 * <p>Implementing this interface allows external components to react to
 * progress updates without coupling the core engine to specific
 * output channels like consoles or loggers.</p>
 */
@FunctionalInterface
public interface ProgressObserver {
  /**
   * Triggered when a new progress threshold is reached.
   *
   * @param percentage completion rate from 0 to 100
   */
  void onProgressUpdate(int percentage);
}