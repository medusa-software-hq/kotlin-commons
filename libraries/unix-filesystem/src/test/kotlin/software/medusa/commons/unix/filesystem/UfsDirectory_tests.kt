package software.medusa.commons.unix.filesystem

import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.encodeToByteString
import software.medusa.commons.unix.filesystem.impl.memory.UfsMemoryDirectory
import software.medusa.commons.unix.filesystem.impl.memory.UfsMemoryFile
import software.medusa.commons.unix.filesystem.impl.nio.UfsNioDirectory
import software.medusa.commons.unix.filesystem.impl.nio.UfsNioEntity_testUtils.withTempDir
import software.medusa.commons.unix.path.UfsName
import software.medusa.commons.unix.path.UfsRelativePath

class UfsDirectory_tests {
  @Test
  fun test_extract_relativePath_nestedExisting() = runTest {
    val rootDirectory = UfsMemoryDirectory()

    val nestedDirectory =
        rootDirectory.createDirectory(
            UfsName.Literal("dist"),
        )

    nestedDirectory.createFile(
        name = UfsName.Literal("bundle.js"),
        initialContent = "console.log('hello')".encodeToByteString(),
    )

    val extractedEntity =
        rootDirectory.extractDeepReadonly(
            UfsRelativePath.of(
                UfsName.Literal("dist"),
                UfsName.Literal("bundle.js"),
            ),
        )

    val extractedFile = assertIs<UfsMemoryFile>(extractedEntity)

    assertEquals(
        expected = "console.log('hello')".encodeToByteString(),
        actual = extractedFile.read(),
    )
  }

  @Test
  fun test_extract_relativePath_throughFile_returnsNull() = runTest {
    val rootDirectory = UfsMemoryDirectory()

    rootDirectory.createFile(
        name = UfsName.Literal("dist"),
        initialContent = "content".encodeToByteString(),
    )

    val extractedEntity =
        rootDirectory.extractDeepReadonly(
            UfsRelativePath.of(
                UfsName.Literal("dist"),
                UfsName.Literal("bundle.js"),
            ),
        )

    assertNull(extractedEntity)
  }

  @Test
  fun test_copyRecursivelyTo_copiesNestedFiles_and_preservesUnrelatedTargetEntries() = runTest {
    val sourceDirectory = UfsMemoryDirectory()
    val targetDirectory = UfsMemoryDirectory()

    sourceDirectory.createFile(
        name = UfsName.Literal("root.txt"),
        initialContent = "source root".encodeToByteString(),
    )

    sourceDirectory
        .createDirectory(
            UfsName.Literal("nested"),
        )
        .createFile(
            name = UfsName.Literal("copied.txt"),
            initialContent = "nested source".encodeToByteString(),
        )

    targetDirectory.createFile(
        name = UfsName.Literal("root.txt"),
        initialContent = "target root".encodeToByteString(),
    )

    targetDirectory.createFile(
        name = UfsName.Literal("keep.txt"),
        initialContent = "keep me".encodeToByteString(),
    )

    sourceDirectory.copyRecursivelyTo(targetDirectory)

    val copiedRootFile =
        assertIs<UfsMemoryFile>(
            targetDirectory.extract(
                UfsName.Literal("root.txt"),
            ),
        )

    val copiedNestedFile =
        assertIs<UfsMemoryFile>(
            targetDirectory.extractDeepReadonly(
                UfsRelativePath.of(
                    UfsName.Literal("nested"),
                    UfsName.Literal("copied.txt"),
                ),
            ),
        )

    val preservedExtraFile =
        assertIs<UfsMemoryFile>(
            targetDirectory.extract(
                UfsName.Literal("keep.txt"),
            ),
        )

    assertEquals(
        expected = "source root".encodeToByteString(),
        actual = copiedRootFile.read(),
    )

    assertEquals(
        expected = "nested source".encodeToByteString(),
        actual = copiedNestedFile.read(),
    )

    assertEquals(
        expected = "keep me".encodeToByteString(),
        actual = preservedExtraFile.read(),
    )
  }

  @Test
  fun test_copyRecursivelyTo_replacesExistingDirectoryWithFile() = runTest {
    val sourceDirectory = UfsMemoryDirectory()
    val targetDirectory = UfsMemoryDirectory()

    sourceDirectory.createFile(
        name = UfsName.Literal("entry"),
        initialContent = "replacement file".encodeToByteString(),
    )

    targetDirectory
        .createDirectory(
            UfsName.Literal("entry"),
        )
        .createFile(
            name = UfsName.Literal("nested.txt"),
            initialContent = "nested".encodeToByteString(),
        )

    sourceDirectory.copyRecursivelyTo(targetDirectory)

    val copiedEntity =
        assertIs<UfsMemoryFile>(
            targetDirectory.extract(
                UfsName.Literal("entry"),
            ),
        )

    assertEquals(
        expected = "replacement file".encodeToByteString(),
        actual = copiedEntity.read(),
    )
  }

  @Test
  fun test_copyRecursivelyTo_replacesExistingFileWithDirectory() = runTest {
    val sourceDirectory = UfsMemoryDirectory()
    val targetDirectory = UfsMemoryDirectory()

    sourceDirectory
        .createDirectory(
            UfsName.Literal("entry"),
        )
        .createFile(
            name = UfsName.Literal("nested.txt"),
            initialContent = "replacement directory".encodeToByteString(),
        )

    targetDirectory.createFile(
        name = UfsName.Literal("entry"),
        initialContent = "old file".encodeToByteString(),
    )

    sourceDirectory.copyRecursivelyTo(targetDirectory)

    val copiedDirectory =
        assertIs<UfsMemoryDirectory>(
            targetDirectory.extract(
                UfsName.Literal("entry"),
            ),
        )

    val copiedFile =
        assertIs<UfsMemoryFile>(
            copiedDirectory.extract(
                UfsName.Literal("nested.txt"),
            ),
        )

    assertEquals(
        expected = "replacement directory".encodeToByteString(),
        actual = copiedFile.read(),
    )
  }

  @Test
  fun test_materializeIn_materializesNestedFilesToFilesystem() = runTest {
    withTempDir { outputDirectory ->
      val sourceDirectory = UfsMemoryDirectory()

      sourceDirectory
          .createDirectory(
              UfsName.Literal("nested"),
          )
          .createFile(
              name = UfsName.Literal("hello.txt"),
              initialContent = "hello".encodeToByteString(),
          )

      val scriptFile =
          sourceDirectory.createFile(
              name = UfsName.Literal("tool.sh"),
              initialContent = "echo hi".encodeToByteString(),
          )

      scriptFile.makeExecutable()

      sourceDirectory.materializeIn(
          targetDirectory = UfsNioDirectory(directoryPath = outputDirectory),
      )

      val nestedDirectory = outputDirectory.resolve("nested")
      val helloFile = nestedDirectory.resolve("hello.txt")
      val toolFile = outputDirectory.resolve("tool.sh")

      assertEquals(true, nestedDirectory.isDirectory())
      assertEquals(true, helloFile.isRegularFile())
      assertEquals("hello", helloFile.readText())

      assertEquals(true, toolFile.exists())
      assertEquals("echo hi", toolFile.readText())
      assertEquals(true, toolFile.isExecutable())
    }
  }

  @Test
  fun test_materializeIn_materializesInMemoryTreeToFilesystem() = runTest {
    withTempDir { outputDirectory ->
      val sourceDirectory =
          UfsMemoryDirectory().apply {
            createDirectory(
                    UfsName.Literal("dir"),
                )
                .createFile(
                    name = UfsName.Literal("file.txt"),
                    initialContent = "content".encodeToByteString(),
                )

            createFile(
                    name = UfsName.Literal("script.sh"),
                    initialContent = "echo hi".encodeToByteString(),
                )
                .makeExecutable()
          }

      sourceDirectory.materializeIn(
          targetDirectory = UfsNioDirectory(directoryPath = outputDirectory),
      )

      assertEquals(true, outputDirectory.resolve("dir").isDirectory())
      assertEquals("content", outputDirectory.resolve("dir/file.txt").readText())
      assertEquals("echo hi", outputDirectory.resolve("script.sh").readText())
      assertEquals(true, outputDirectory.resolve("script.sh").isExecutable())
    }
  }
}
