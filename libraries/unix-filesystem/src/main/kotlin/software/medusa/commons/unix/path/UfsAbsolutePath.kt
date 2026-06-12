package software.medusa.commons.unix.path

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import software.medusa.commons.unix.path.UfsRelativePath.Companion.toLiteral

/** Absolute Unix-style path. */
data class UfsAbsolutePath<out NameT : UfsName>(
    /** Path relative to the root directory. */
    val innerPath: UfsRelativePath<NameT>,
) : UfsPath<NameT>() {
  companion object {
    /** Path to the root directory ("/"). */
    val Root: UfsLiteralAbsolutePath =
        UfsAbsolutePath(
            innerPath = UfsRelativePath.Empty,
        )

    fun <NameT : UfsName> of(
        vararg names: NameT,
    ): UfsAbsolutePath<NameT> =
        UfsAbsolutePath(
            innerPath = UfsRelativePath(names = names.toList()),
        )

    fun <NameT : UfsName> of(
        names: List<NameT>,
    ): UfsAbsolutePath<NameT> =
        UfsAbsolutePath(
            innerPath = UfsRelativePath(names = names),
        )

    /**
     * Parses a conventional absolute Unix-style path string into an [UfsAbsolutePath] instance.
     *
     * @throws IllegalArgumentException if the path string is empty, doesn't start with the
     *   separator character or contains consecutive slashes.
     */
    fun parse(unixPathString: String): UfsAbsolutePath<*> {
      require(unixPathString.isNotEmpty()) { "Path string cannot be empty" }

      require(unixPathString.first() == Separator) {
        "Absolute path string must start with '$Separator' character"
      }

      return when (unixPathString.length) {
        1 -> Root

        else ->
            UfsAbsolutePath(
                innerPath = UfsRelativePath.parse(unixPathString.drop(1)),
            )
      }
    }

    /**
     * Resolves a nested relative path against this absolute path, returning a new absolute path.
     */
    fun <NameT : UfsName> UfsAbsolutePath<NameT>.resolve(
        nestedPath: UfsRelativePath<NameT>,
    ): UfsAbsolutePath<NameT> =
        UfsAbsolutePath(
            innerPath = UfsRelativePath.concat(innerPath, nestedPath),
        )

    /** Resolves a single name against this absolute path, returning a new absolute path. */
    fun <NameT : UfsName> UfsAbsolutePath<NameT>.resolve(
        name: NameT,
    ): UfsAbsolutePath<NameT> =
        resolve(
            nestedPath = UfsRelativePath.of(name),
        )

    fun UfsAbsolutePath<*>.toLiteral(): UfsLiteralAbsolutePath? {
      val literalInnerPath = innerPath.toLiteral() ?: return null

      return UfsLiteralAbsolutePath(innerPath = literalInnerPath)
    }

    fun UfsAbsolutePath<*>.toAbsoluteNioPath(): Path = Paths.get(toUnixAbsolutePathString())

    fun UfsAbsolutePath<*>.toIoFile(): File = toAbsoluteNioPath().toFile()

    fun UfsLiteralAbsolutePath.relativizeAgainst(
        basePath: UfsLiteralAbsolutePath,
    ): UfsLiteralRelativePath {
      require(innerPath.names.size >= basePath.innerPath.names.size) {
        "Path $this is not within base path $basePath"
      }

      require(innerPath.names.take(basePath.innerPath.names.size) == basePath.innerPath.names) {
        "Path $this is not within base path $basePath"
      }

      return UfsLiteralRelativePath(
          names = innerPath.names.drop(basePath.innerPath.names.size),
      )
    }
  }

  /** Builds a conventional Unix-style absolute path string. */
  fun toUnixAbsolutePathString(): String = "$Separator${innerPath.toUnixRelativePathString()}"
}

typealias UfsLiteralAbsolutePath = UfsAbsolutePath<UfsName.Literal>
