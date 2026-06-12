package software.medusa.commons.unix.filesystem.impl.nio

import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.encodeToByteString
import software.medusa.commons.unix.filesystem.impl.nio.UfsNioEntity_testUtils.withTempDir
import software.medusa.commons.unix.path.UfsName

class UfsNioDirectory_tests {
  @Test
  fun test_listEntries_empty() = runTest {
    withTempDir { tempDirPath ->
      val compatDirectory = UfsNioDirectory(tempDirPath)

      assertEquals(
          expected = emptyMap(),
          actual = compatDirectory.readIndex().childEntityByName,
      )
    }
  }

  @Test
  fun test_listEntries_non_empty() = runTest {
    withTempDir { tempDirPath ->
      tempDirPath.resolve("a.txt").createFile()
      tempDirPath.resolve("a.txt").writeBytes("a".encodeToByteString().toByteArray())
      tempDirPath.resolve("nested").createDirectory()

      val compatDirectory = UfsNioDirectory(tempDirPath)
      val entries =
          compatDirectory.readIndex().childEntityByName.toSortedMap(compareBy { it.content })

      assertEquals(
          expected = listOf("a.txt", "nested"),
          actual = entries.keys.map { it.content },
      )

      assertIs<UfsNioFile>(entries.getValue(UfsName.Literal("a.txt")))
      assertIs<UfsNioDirectory>(entries.getValue(UfsName.Literal("nested")))
    }
  }

  @Test
  fun test_extract_nonExisting() = runTest {
    withTempDir { tempDirPath ->
      val compatDirectory = UfsNioDirectory(tempDirPath)

      assertEquals(
          expected = null,
          actual = compatDirectory.extract(UfsName.Literal("missing.txt")),
      )
    }
  }

  @Test
  fun test_extract_existing() = runTest {
    withTempDir { tempDirPath ->
      val fileName = UfsName.Literal("a.txt")
      val fileContent = "hello".encodeToByteString()
      val filePath = tempDirPath.resolve(fileName.content)

      filePath.createFile()
      filePath.writeBytes(fileContent.toByteArray())

      val compatDirectory = UfsNioDirectory(tempDirPath)
      val extractedFile = assertIs<UfsNioFile>(compatDirectory.extract(fileName))

      assertEquals(
          expected = fileContent,
          actual = extractedFile.read(),
      )
    }
  }

  @Test
  fun test_createFile() = runTest {
    withTempDir { tempDirPath ->
      val fileName = UfsName.Literal("created.txt")
      val fileContent = "created".encodeToByteString()
      val compatDirectory = UfsNioDirectory(tempDirPath)

      compatDirectory.createFile(
          name = fileName,
          initialContent = fileContent,
      )

      val filePath = tempDirPath.resolve(fileName.content)

      assertTrue(filePath.exists())
      assertEquals(
          expected = fileContent,
          actual = assertIs<UfsNioFile>(compatDirectory.extract(fileName)).read(),
      )
    }
  }

  @Test
  fun test_createDirectory() = runTest {
    withTempDir { tempDirPath ->
      val directoryName = UfsName.Literal("created")
      val compatDirectory = UfsNioDirectory(tempDirPath)

      compatDirectory.createDirectory(directoryName)

      val directoryPath = tempDirPath.resolve(directoryName.content)

      assertTrue(directoryPath.exists())
      assertTrue(directoryPath.isDirectory())
      assertIs<UfsNioDirectory>(compatDirectory.extract(directoryName))
    }
  }

  @Test
  fun test_delete_empty() = runTest {
    withTempDir { tempDirPath ->
      val compatDirectory =
          UfsNioDirectory(
              directoryPath = tempDirPath,
          )

      compatDirectory.delete()

      assertFalse(tempDirPath.exists())
    }
  }

  @Test
  fun test_delete_nonEmpty() = runTest {
    withTempDir { tempDirPath ->
      val singleFilePath = tempDirPath.resolve("a.txt")

      singleFilePath.createFile()

      val compatDirectory =
          UfsNioDirectory(
              directoryPath = tempDirPath,
          )

      assertFails { runTest { compatDirectory.delete() } }
    }
  }
}
