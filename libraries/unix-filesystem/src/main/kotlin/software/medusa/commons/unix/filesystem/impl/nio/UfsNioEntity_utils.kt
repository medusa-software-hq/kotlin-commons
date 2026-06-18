package software.medusa.commons.unix.filesystem.impl.nio

import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import software.medusa.commons.unix.filesystem.UfsMutableEntity

data object UfsNioEntity_utils {
  fun load(
      entityPath: Path,
  ): UfsMutableEntity =
      when {
        entityPath.isRegularFile(LinkOption.NOFOLLOW_LINKS) ->
            UfsNioFile(
                filePath = entityPath,
            )

        entityPath.isDirectory(LinkOption.NOFOLLOW_LINKS) ->
            UfsNioDirectory(
                directoryPath = entityPath,
            )

        else ->
            throw IllegalStateException(
                "Unexpected or non-existing file type for path: $entityPath",
            )
      }
}
