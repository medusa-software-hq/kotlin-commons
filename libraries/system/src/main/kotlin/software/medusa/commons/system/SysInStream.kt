package software.medusa.commons.system

/** A process's input stream. */
interface SysInStream {
  /** Writes [text] followed by a newline, and flushes. */
  suspend fun writeLine(text: String)

  /**
   * Signals end-of-input without ending the process. A child that reads its input to the end — many
   * command-line tools do, even when the real work comes from arguments — waits until this is
   * called. Idempotent.
   */
  fun close()
}
