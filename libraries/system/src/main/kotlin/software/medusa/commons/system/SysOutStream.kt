package software.medusa.commons.system

import kotlinx.coroutines.flow.Flow

/**
 * One of a process's output streams.
 *
 * A pipe can be read once, so [consumeLines] and [consumeText] are alternative views of the same
 * bytes rather than two features: take one, once. The second call — whichever it is — fails.
 */
interface SysOutStream {
  /**
   * Takes the stream as lines.
   *
   * Nothing is read ahead of whoever collects: the kernel's pipe buffer is the only buffer, and how
   * much more to allow is the collector's to decide with `buffer(…)` or `produceIn(…)`.
   *
   * @return The lines, in order, ending when the process closes the stream.
   */
  fun consumeLines(): Flow<String>

  /**
   * Takes the stream whole, decoding once — not lines rejoined, so the text is exactly what was
   * written.
   *
   * @return Everything the process wrote to the stream.
   */
  suspend fun consumeText(): String
}
