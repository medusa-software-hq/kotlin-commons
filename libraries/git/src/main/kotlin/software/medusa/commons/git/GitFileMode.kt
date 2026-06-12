package software.medusa.commons.git

import org.eclipse.jgit.lib.FileMode

sealed class GitFileMode {
  data object Regular : GitFileMode() {
    override val jFileMode: FileMode
      get() = FileMode.REGULAR_FILE
  }

  data object Executable : GitFileMode() {
    override val jFileMode: FileMode
      get() = FileMode.EXECUTABLE_FILE
  }

  internal abstract val jFileMode: FileMode
}
