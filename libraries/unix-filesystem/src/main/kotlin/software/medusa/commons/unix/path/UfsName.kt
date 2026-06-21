package software.medusa.commons.unix.path

/** A name within a Unix-style path. */
sealed interface UfsName {
  companion object {
    /**
     * Parses a name string into a [UfsName] instance. If the name string is "." or "..", it will be
     * parsed as a symbolic name, otherwise as a literal name.
     *
     * @throws IllegalArgumentException if the name string is empty or contains the separator
     *   character.
     */
    internal fun parse(nameString: String): UfsName =
        when (nameString) {
          Symbolic.ThisDirectory.content -> Symbolic.ThisDirectory
          Symbolic.ParentDirectory.content -> Symbolic.ParentDirectory
          else -> Literal(content = nameString)
        }
  }

  /**
   * A literal name, corresponding to a (possibly existing) file on the filesystem. Arbitrary
   * non-Unicode (binary) names are unrepresentable.
   */
  @JvmInline
  value class Literal(
      override val content: String,
  ) : UfsName {
    companion object {
      /** Concatenates a list of [Literal] names into a single name, without any separator. */
      fun concat(
          names: List<Literal>,
      ): Literal =
          Literal(
              content = names.joinToString(separator = "") { it.content },
          )
    }

    init {
      require(content.isNotEmpty()) { "UfsName cannot be empty" }

      require(!content.contains(UfsPath.Separator)) {
        "UfsName cannot contain '${UfsPath.Separator}' character"
      }

      require(
          content != Symbolic.ThisDirectory.content && content != Symbolic.ParentDirectory.content
      ) {
        "UfsName cannot be ${Symbolic.ThisDirectory.content} or ${Symbolic.ParentDirectory.content}"
      }
    }
  }

  /** A symbolic name, having a special meaning. */
  sealed interface Symbolic : UfsName {
    /** The symbolic name "." (dot) refers to the current directory. */
    data object ThisDirectory : Symbolic {
      override val content: String = "."
    }

    /** The symbolic name ".." (dot-dot) refers to the parent directory. */
    data object ParentDirectory : Symbolic {
      override val content: String = ".."
    }
  }

  val content: String
}
