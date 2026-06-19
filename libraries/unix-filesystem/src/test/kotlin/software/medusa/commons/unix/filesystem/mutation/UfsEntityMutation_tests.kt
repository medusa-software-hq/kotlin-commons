package software.medusa.commons.unix.filesystem.mutation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.encodeToByteString
import software.medusa.commons.unix.filesystem.impl.memory.UfsMemoryDirectory
import software.medusa.commons.unix.filesystem.impl.memory.UfsMemoryFile
import software.medusa.commons.unix.path.UfsName

class UfsEntityMutation_tests {
  @Test
  fun test_applyMutation_file_update_replacesContent() = runTest {
    val rootDirectory = UfsMemoryDirectory()

    val file =
        rootDirectory.createFile(
            name = UfsName.Literal("note.txt"),
            initialContent = "old".encodeToByteString(),
        )

    file.applyMutation(
        UfsFileMutation.Update(newContent = "new".encodeToByteString()),
    )

    assertEquals(
        expected = "new".encodeToByteString(),
        actual = file.read(),
    )
  }

  @Test
  fun test_applyMutation_file_delete_removesFromParent() = runTest {
    val rootDirectory = UfsMemoryDirectory()

    val file =
        rootDirectory.createFile(
            name = UfsName.Literal("note.txt"),
            initialContent = "content".encodeToByteString(),
        )

    file.applyMutation(UfsFileMutation.Delete)

    assertNull(rootDirectory.extract(UfsName.Literal("note.txt")))
  }

  @Test
  fun test_applyMutation_directory_deleteNonRecursive_removesEmptyDirectory() = runTest {
    val rootDirectory = UfsMemoryDirectory()

    val directory = rootDirectory.createDirectory(UfsName.Literal("empty"))

    directory.applyMutation(
        UfsDirectoryMutation.Delete(mode = UfsDirectoryMutation.Delete.Mode.NonRecursive),
    )

    assertNull(rootDirectory.extract(UfsName.Literal("empty")))
  }

  @Test
  fun test_applyMutation_directory_deleteNonRecursive_onNonEmptyDirectory_fails() = runTest {
    val rootDirectory = UfsMemoryDirectory()

    val directory = rootDirectory.createDirectory(UfsName.Literal("data"))

    directory.createFile(
        name = UfsName.Literal("child.txt"),
        initialContent = "content".encodeToByteString(),
    )

    assertFailsWith<IllegalStateException> {
      directory.applyMutation(
          UfsDirectoryMutation.Delete(mode = UfsDirectoryMutation.Delete.Mode.NonRecursive),
      )
    }
  }

  @Test
  fun test_applyMutation_directory_deleteRecursive_removesNestedTree() = runTest {
    val rootDirectory = UfsMemoryDirectory()

    val directory = rootDirectory.createDirectory(UfsName.Literal("data"))

    directory
        .createDirectory(UfsName.Literal("nested"))
        .createFile(
            name = UfsName.Literal("child.txt"),
            initialContent = "content".encodeToByteString(),
        )

    directory.applyMutation(
        UfsDirectoryMutation.Delete(mode = UfsDirectoryMutation.Delete.Mode.Recursive),
    )

    assertNull(rootDirectory.extract(UfsName.Literal("data")))
  }

  @Test
  fun test_applyMutation_directory_dive_mutatesExistingChild() = runTest {
    val rootDirectory = UfsMemoryDirectory()

    val file =
        rootDirectory.createFile(
            name = UfsName.Literal("note.txt"),
            initialContent = "old".encodeToByteString(),
        )

    rootDirectory.applyMutation(
        UfsDirectoryMutation.Dive(
            operationByName =
                mapOf(
                    UfsName.Literal("note.txt") to
                        UfsDirectoryMutation.Dive.Operation.Mutate(
                            mutation =
                                UfsFileMutation.Update(newContent = "new".encodeToByteString()),
                        ),
                ),
        ),
    )

    assertEquals(
        expected = "new".encodeToByteString(),
        actual = file.read(),
    )
  }

  @Test
  fun test_applyMutation_directory_dive_createsChildFromFileTemplate() = runTest {
    val rootDirectory = UfsMemoryDirectory()

    val templateFile =
        UfsMemoryFile(initialPath = "script body".encodeToByteString()).apply { makeExecutable() }

    rootDirectory.applyMutation(
        UfsDirectoryMutation.Dive(
            operationByName =
                mapOf(
                    UfsName.Literal("tool.sh") to
                        UfsDirectoryMutation.Dive.Operation.Create(templateEntity = templateFile),
                ),
        ),
    )

    val createdFile = assertIs<UfsMemoryFile>(rootDirectory.extract(UfsName.Literal("tool.sh")))

    assertEquals(
        expected = "script body".encodeToByteString(),
        actual = createdFile.read(),
    )

    assertEquals(expected = true, actual = createdFile.isExecutable())
  }

  @Test
  fun test_applyMutation_directory_dive_createsChildFromDirectoryTemplate() = runTest {
    val rootDirectory = UfsMemoryDirectory()

    val templateDirectory =
        UfsMemoryDirectory().apply {
          createFile(
              name = UfsName.Literal("inner.txt"),
              initialContent = "inner".encodeToByteString(),
          )
        }

    rootDirectory.applyMutation(
        UfsDirectoryMutation.Dive(
            operationByName =
                mapOf(
                    UfsName.Literal("nested") to
                        UfsDirectoryMutation.Dive.Operation.Create(
                            templateEntity = templateDirectory
                        ),
                ),
        ),
    )

    val createdDirectory =
        assertIs<UfsMemoryDirectory>(rootDirectory.extract(UfsName.Literal("nested")))

    val createdInnerFile =
        assertIs<UfsMemoryFile>(createdDirectory.extract(UfsName.Literal("inner.txt")))

    assertEquals(
        expected = "inner".encodeToByteString(),
        actual = createdInnerFile.read(),
    )
  }

  @Test
  fun test_applyMutation_directory_dive_leavesUnnamedChildrenUntouched() = runTest {
    val rootDirectory = UfsMemoryDirectory()

    rootDirectory.createFile(
        name = UfsName.Literal("keep.txt"),
        initialContent = "keep".encodeToByteString(),
    )

    val mutatedFile =
        rootDirectory.createFile(
            name = UfsName.Literal("change.txt"),
            initialContent = "old".encodeToByteString(),
        )

    rootDirectory.applyMutation(
        UfsDirectoryMutation.Dive(
            operationByName =
                mapOf(
                    UfsName.Literal("change.txt") to
                        UfsDirectoryMutation.Dive.Operation.Mutate(
                            mutation =
                                UfsFileMutation.Update(newContent = "new".encodeToByteString()),
                        ),
                ),
        ),
    )

    val keptFile = assertIs<UfsMemoryFile>(rootDirectory.extract(UfsName.Literal("keep.txt")))

    assertEquals(expected = "keep".encodeToByteString(), actual = keptFile.read())
    assertEquals(expected = "new".encodeToByteString(), actual = mutatedFile.read())
  }

  @Test
  fun test_applyMutation_directory_dive_mutateMissingChild_fails() = runTest {
    val rootDirectory = UfsMemoryDirectory()

    assertFailsWith<IllegalStateException> {
      rootDirectory.applyMutation(
          UfsDirectoryMutation.Dive(
              operationByName =
                  mapOf(
                      UfsName.Literal("absent.txt") to
                          UfsDirectoryMutation.Dive.Operation.Mutate(
                              mutation = UfsFileMutation.Delete,
                          ),
                  ),
          ),
      )
    }
  }

  @Test
  fun test_applyMutation_entity_kindMismatch_fails() = runTest {
    val rootDirectory = UfsMemoryDirectory()

    val directory = rootDirectory.createDirectory(UfsName.Literal("dir"))

    // A file mutation against a directory dispatches through the entity-level overload, which
    // rejects the kind mismatch at runtime.
    assertFailsWith<IllegalStateException> {
      directory.applyMutation(
          UfsFileMutation.Update(newContent = "x".encodeToByteString()),
      )
    }
  }
}
