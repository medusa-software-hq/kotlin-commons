package software.medusa.commons.unix.path

/**
 * A simplified model of a Unix-style path.
 *
 * A path is a sequence of _names_ (path components). Each name refers to a file that is a direct
 * child of the directory referred to by the previous name. A file may be either a regular file or a
 * directory.
 *
 * Paths are explicitly represented as either _absolute_ or _relative_:
 * - an absolute path is interpreted relative to the root directory
 * - a relative path is interpreted relative to some other path or filesystem context
 *
 * Names may be either _literal_ or _symbolic_. Symbolic names represent the conventional Unix
 * special components "." (current directory) and ".." (parent directory).
 *
 * This model represents Unix-style path syntax, not full filesystem path-resolution semantics. In
 * particular:
 * - POSIX special paths beginning with "//" are unrepresentable
 * - filesystem-specific behavior, such as symbolic-link resolution, is not modeled
 * - arbitrary binary file names are unrepresentable because names are stored as [String]
 *
 * See the design documentation for background, rationale, and edge-case discussion.
 *
 * @param NameT Type of the names comprising the path.
 */
sealed class UfsPath<out NameT : UfsName> {
  companion object {
    const val Separator = '/'
  }
}

internal fun <T : Any> List<T?>.allNonNullOrNull(): List<T>? =
    when {
      all { it != null } -> @Suppress("UNCHECKED_CAST") (this as List<T>)
      else -> null
    }
