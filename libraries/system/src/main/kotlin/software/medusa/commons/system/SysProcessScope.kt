package software.medusa.commons.system

/**
 * A running process, for as long as the block it was given to lasts.
 *
 * Each pipe holds only so much before a process that keeps writing to a full one stops making
 * progress on all of its streams. That is the operating system's behaviour, not this library's, and
 * it is left as it is — so between starting and [awaitExit] the block decides what gets read.
 * Exactly one shape is unsafe:
 *
 * > Never read one stream to its end while a sibling stream is untaken and the process may still be
 * > writing to it.
 *
 * Taking [standardOutput] whole and only then taking [standardError] is that shape, and it can
 * deadlock. Take them at the same time, or take neither and let [awaitExit] do it.
 *
 * A read that has been started runs until the stream ends or the block does. Giving up on one early
 * — wrapping it in a timeout, say — does not release it on every platform, because whether closing
 * a stream frees a thread already parked inside a read is up to the operating system. What reliably
 * releases a reader is leaving the block, which ends the process and closes its pipes.
 */
interface SysProcessScope {
  /** What the process reads as its input. */
  val standardInput: SysInStream

  /** What the process writes as its output. */
  val standardOutput: SysOutStream

  /** What the process writes as its errors. */
  val standardError: SysOutStream

  /**
   * Waits for the process to end.
   *
   * Any stream still untaken is taken here and read to its end alongside the wait, so a caller that
   * only wants the exit code cannot leave the process stuck against a full pipe. What is read that
   * way is discarded: a caller who wants the error output for a diagnostic has to take it.
   *
   * @return The code the process ended with.
   */
  suspend fun awaitExit(): Int
}
