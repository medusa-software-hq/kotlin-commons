package software.medusa.commons.unix.path

import java.nio.file.Path
import java.nio.file.Paths

/** Relative Unix-style path. */
data class UfsRelativePath<out NameT : UfsName>(
    val names: List<NameT>,
) : UfsPath<NameT>() {
  companion object {
    /**
     * An empty relative path. Semantically, an empty relative path is equivalent to a series of
     * [UfsName.Symbolic.ThisDirectory] names (".", "./.", "././.", etc.).
     */
    val Empty: UfsLiteralRelativePath =
        UfsRelativePath(
            names = emptyList(),
        )

    fun <NameT : UfsName> of(
        vararg names: NameT,
    ): UfsRelativePath<NameT> = UfsRelativePath(names = names.toList())

    fun <NameT : UfsName> of(
        names: List<NameT>,
    ): UfsRelativePath<NameT> = UfsRelativePath(names = names)

    /**
     * Parses a conventional relative Unix-style path string into a [UfsRelativePath] instance.
     *
     * @throws IllegalArgumentException if the path string is empty, starts with the separator
     *   character or contains consecutive slashes.
     */
    fun parse(
        unixPathString: String,
    ): UfsRelativePath<*> {
      val firstChar =
          unixPathString.firstOrNull()
              ?: throw IllegalArgumentException("Path string cannot be empty")

      require(firstChar != Separator) {
        "Relative path string cannot start with '$Separator' character"
      }

      // Attempt to parse the path string. If it contains consecutive separators, some of the
      // resulting names strings will be empty.
      return UfsRelativePath(
          names = unixPathString.split(Separator).map { UfsName.parse(it) },
      )
    }

    fun <NameT : UfsName> concat(
        vararg paths: UfsRelativePath<NameT>,
    ): UfsRelativePath<NameT> = concat(paths.toList())

    fun <NameT : UfsName> concat(
        paths: List<UfsRelativePath<NameT>>,
    ): UfsRelativePath<NameT> =
        UfsRelativePath(
            names = paths.flatMap { it.names },
        )

    fun UfsRelativePath<*>.toLiteral(): UfsLiteralRelativePath? {
      val literalNames = names.map { it as? UfsName.Literal }.allNonNullOrNull() ?: return null

      return UfsLiteralRelativePath(names = literalNames)
    }

    fun UfsRelativePath<*>.toRelativeNioPath(): Path = Paths.get(toUnixRelativePathString())
  }

  val fileName: NameT?
    get() = names.lastOrNull()

  /** Builds a conventional Unix-style relative path string. */
  fun toUnixRelativePathString(): String =
      names.joinToString(separator = "$Separator") { it.content }
}

typealias UfsLiteralRelativePath = UfsRelativePath<UfsName.Literal>
