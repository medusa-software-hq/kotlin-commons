package software.medusa.commons.unix.path

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import software.medusa.commons.unix.path.UfsAbsolutePath.Companion.relativizeAgainst
import software.medusa.commons.unix.path.UfsAbsolutePath.Companion.resolve

class UfsPath_tests {
  @Test
  fun test_UfsRelativePath_construct_empty() {
    assertEquals(
        expected = UfsRelativePath.Empty,
        actual =
            UfsRelativePath(
                names = emptyList(),
            ),
    )
  }

  @Test
  fun test_UfsRelativePath_construct_emptyName() {
    assertIs<IllegalArgumentException>(
        assertFails {
          UfsRelativePath(
              names =
                  listOf(
                      UfsName.Literal(""),
                  ),
          )
        },
    )
  }

  @Test
  fun test_UfsRelativePath_construct_nameContainingSeparator() {
    assertIs<IllegalArgumentException>(
        assertFails {
          UfsRelativePath(
              names =
                  listOf(
                      UfsName.Literal("files"),
                      UfsName.Literal("n/a.txt"),
                  ),
          )
        },
    )
  }

  @Test
  fun test_UfsRelativePath_construct_symbolicName_dot() {
    assertIs<IllegalArgumentException>(
        assertFails {
          UfsRelativePath(
              names =
                  listOf(
                      UfsName.Literal("characters"),
                      UfsName.Literal("."),
                  ),
          )
        },
    )
  }

  @Test
  fun test_UfsRelativePath_construct_symbolicName_dotDot() {
    assertIs<IllegalArgumentException>(
        assertFails {
          UfsRelativePath(
              names =
                  listOf(
                      UfsName.Literal("double-characters"),
                      UfsName.Literal(".."),
                  ),
          )
        },
    )
  }

  @Test
  fun test_UfsRelativePath_construct_literalName_tripleDot() {
    val unixPath =
        UfsRelativePath(
            names =
                listOf(
                    UfsName.Literal("triple-characters"),
                    UfsName.Literal("..."),
                ),
        )

    assertEquals(
        expected =
            listOf(
                UfsName.Literal("triple-characters"),
                UfsName.Literal("..."),
            ),
        actual = unixPath.names,
    )
  }

  @Test
  fun test_UfsRelativePath_construct_literalName_hiddenFile() {
    val unixPath =
        UfsRelativePath(
            names =
                listOf(
                    UfsName.Literal("code"),
                    UfsName.Literal(".gitignore"),
                ),
        )

    assertEquals(
        expected =
            listOf(
                UfsName.Literal("code"),
                UfsName.Literal(".gitignore"),
            ),
        actual = unixPath.names,
    )
  }

  @Test
  fun test_UfsRelativePath_parse_empty() {
    assertIs<IllegalArgumentException>(
        assertFails { UfsRelativePath.parse("") },
    )
  }

  @Test
  fun test_UfsRelativePath_toUnixRelativePathString_empty() {
    assertEquals(
        expected = "",
        actual = UfsRelativePath.Empty.toUnixRelativePathString(),
    )
  }

  @Test
  fun test_UfsRelativePath_parse_topLevel() {
    val parsedPath: UfsRelativePath<UfsName> = UfsRelativePath.parse("dist")

    assertEquals(
        expected =
            UfsRelativePath.of(
                UfsName.Literal("dist"),
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_parse_leadingSeparator() {
    assertIs<IllegalArgumentException>(
        assertFails { UfsRelativePath.parse("/tmp") },
    )
  }

  @Test
  fun test_UfsRelativePath_toUnixRelativePathString_topLevel() {
    val unixPath =
        UfsRelativePath.of(
            UfsName.Literal("dist"),
        )

    assertEquals(
        expected = "dist",
        actual = unixPath.toUnixRelativePathString(),
    )
  }

  @Test
  fun test_UfsRelativePath_parse_trailingSeparator() {
    assertIs<IllegalArgumentException>(
        assertFails { UfsRelativePath.parse("tmp/") },
    )
  }

  @Test
  fun test_UfsRelativePath_parse_nested() {
    val parsedPath: UfsRelativePath<UfsName> = UfsRelativePath.parse("build/a.out")

    assertEquals(
        expected =
            UfsRelativePath.of(
                UfsName.Literal("build"),
                UfsName.Literal("a.out"),
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_toUnixRelativePathString_nested() {
    val unixPath =
        UfsRelativePath.of(
            UfsName.Literal("build"),
            UfsName.Literal("a.out"),
        )

    assertEquals(
        expected = "build/a.out",
        actual = unixPath.toUnixRelativePathString(),
    )
  }

  @Test
  fun test_UfsRelativePath_parse_deeplyNested() {
    val parsedPath: UfsRelativePath<UfsName> =
        UfsRelativePath.parse("archive/backups/2026/01/01/backup.tar.gz")

    assertEquals(
        expected =
            UfsRelativePath.of(
                UfsName.Literal("archive"),
                UfsName.Literal("backups"),
                UfsName.Literal("2026"),
                UfsName.Literal("01"),
                UfsName.Literal("01"),
                UfsName.Literal("backup.tar.gz"),
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_toUnixRelativePathString_deeplyNested() {
    val unixPath =
        UfsRelativePath.of(
            UfsName.Literal("archive"),
            UfsName.Literal("backups"),
            UfsName.Literal("2026"),
            UfsName.Literal("01"),
            UfsName.Literal("01"),
            UfsName.Literal("backup.tar.gz"),
        )

    assertEquals(
        expected = "archive/backups/2026/01/01/backup.tar.gz",
        actual = unixPath.toUnixRelativePathString(),
    )
  }

  @Test
  fun test_UfsRelativePath_parse_consecutive_separator() {
    assertIs<IllegalArgumentException>(
        assertFails { UfsRelativePath.parse("dist/bundles//bundle.js") },
    )
  }

  @Test
  fun test_UfsRelativePath_parse_dot() {
    val parsedPath: UfsRelativePath<UfsName> = UfsRelativePath.parse(".")

    assertEquals(
        expected =
            UfsRelativePath.of(
                UfsName.Symbolic.ThisDirectory,
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_toUnixRelativePathString_dot() {
    val unixPath =
        UfsRelativePath.of(
            UfsName.Symbolic.ThisDirectory,
        )

    assertEquals(
        expected = ".",
        actual = unixPath.toUnixRelativePathString(),
    )
  }

  @Test
  fun test_UfsRelativePath_parse_dot_nested() {
    val parsedPath: UfsRelativePath<UfsName> = UfsRelativePath.parse("./dist/bundle.js")

    assertEquals(
        expected =
            UfsRelativePath.of(
                UfsName.Symbolic.ThisDirectory,
                UfsName.Literal("dist"),
                UfsName.Literal("bundle.js"),
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_toUnixRelativePathString_dot_nested() {
    val unixPath =
        UfsRelativePath.of(
            UfsName.Symbolic.ThisDirectory,
            UfsName.Literal("dist"),
            UfsName.Literal("bundle.js"),
        )

    assertEquals(
        expected = "./dist/bundle.js",
        actual = unixPath.toUnixRelativePathString(),
    )
  }

  @Test
  fun test_UfsRelativePath_parse_dot_inner() {
    val parsedPath: UfsRelativePath<UfsName> = UfsRelativePath.parse("dist/./bundle.js")

    assertEquals(
        expected =
            UfsRelativePath.of(
                UfsName.Literal("dist"),
                UfsName.Symbolic.ThisDirectory,
                UfsName.Literal("bundle.js"),
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_toUnixRelativePathString_dot_inner() {
    val unixPath =
        UfsRelativePath.of(
            UfsName.Literal("dist"),
            UfsName.Symbolic.ThisDirectory,
            UfsName.Literal("bundle.js"),
        )

    assertEquals(
        expected = "dist/./bundle.js",
        actual = unixPath.toUnixRelativePathString(),
    )
  }

  @Test
  fun test_UfsRelativePath_parse_dotDot() {
    val parsedPath: UfsRelativePath<UfsName> = UfsRelativePath.parse("..")

    assertEquals(
        expected =
            UfsRelativePath.of(
                UfsName.Symbolic.ParentDirectory,
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_toUnixRelativePathString_dotDot() {
    val unixPath =
        UfsRelativePath.of(
            UfsName.Symbolic.ParentDirectory,
        )

    assertEquals(
        expected = "..",
        actual = unixPath.toUnixRelativePathString(),
    )
  }

  @Test
  fun test_UfsRelativePath_parse_dotDot_nested() {
    val parsedPath: UfsRelativePath<UfsName> = UfsRelativePath.parse("../dist/bundle.js")

    assertEquals(
        expected =
            UfsRelativePath.of(
                UfsName.Symbolic.ParentDirectory,
                UfsName.Literal("dist"),
                UfsName.Literal("bundle.js"),
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_toUnixRelativePathString_dotDot_nested() {
    val unixPath =
        UfsRelativePath.of(
            UfsName.Symbolic.ParentDirectory,
            UfsName.Literal("dist"),
            UfsName.Literal("bundle.js"),
        )

    assertEquals(
        expected = "../dist/bundle.js",
        actual = unixPath.toUnixRelativePathString(),
    )
  }

  @Test
  fun test_UfsRelativePath_parse_dotDot_repeated_nested() {
    val parsedPath: UfsRelativePath<UfsName> = UfsRelativePath.parse("../../../dist/bundle.js")

    assertEquals(
        expected =
            UfsRelativePath.of(
                UfsName.Symbolic.ParentDirectory,
                UfsName.Symbolic.ParentDirectory,
                UfsName.Symbolic.ParentDirectory,
                UfsName.Literal("dist"),
                UfsName.Literal("bundle.js"),
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_toUnixRelativePathString_dotDot_repeated_nested() {
    val unixPath =
        UfsRelativePath.of(
            UfsName.Symbolic.ParentDirectory,
            UfsName.Symbolic.ParentDirectory,
            UfsName.Symbolic.ParentDirectory,
            UfsName.Literal("dist"),
            UfsName.Literal("bundle.js"),
        )

    assertEquals(
        expected = "../../../dist/bundle.js",
        actual = unixPath.toUnixRelativePathString(),
    )
  }

  @Test
  fun test_UfsRelativePath_parse_dotDot_inner() {
    val parsedPath: UfsRelativePath<UfsName> = UfsRelativePath.parse("dist/../bundle.js")

    assertEquals(
        expected =
            UfsRelativePath.of(
                UfsName.Literal("dist"),
                UfsName.Symbolic.ParentDirectory,
                UfsName.Literal("bundle.js"),
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_toUnixRelativePathString_dotDot_inner() {
    val unixPath =
        UfsRelativePath.of(
            UfsName.Literal("dist"),
            UfsName.Symbolic.ParentDirectory,
            UfsName.Literal("bundle.js"),
        )

    assertEquals(
        expected = "dist/../bundle.js",
        actual = unixPath.toUnixRelativePathString(),
    )
  }

  @Test
  fun test_UfsRelativePath_concat_bothEmpty() {
    val concatenatedPath =
        UfsRelativePath.concat(
            UfsRelativePath.Empty,
            UfsRelativePath.Empty,
        )

    assertEquals(
        expected = UfsRelativePath.Empty,
        actual = concatenatedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_concat_firstEmpty() {
    val properPath =
        UfsRelativePath.of(
            UfsName.Literal("dist"),
        )

    val concatenatedPath =
        UfsRelativePath.concat(
            UfsRelativePath.Empty,
            properPath,
        )

    assertEquals(
        expected = properPath,
        actual = concatenatedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_concat_secondEmpty() {
    val properPath =
        UfsRelativePath.of(
            UfsName.Literal("dist"),
        )

    val concatenatedPath =
        UfsRelativePath.concat(
            properPath,
            UfsRelativePath.Empty,
        )

    assertEquals(
        expected = properPath,
        actual = concatenatedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_concat_bothProper() {
    val concatenatedPath =
        UfsRelativePath.concat(
            UfsRelativePath.of(
                UfsName.Literal("build"),
            ),
            UfsRelativePath.of(
                UfsName.Literal("a.out"),
            ),
        )

    assertEquals(
        expected =
            UfsRelativePath.of(
                UfsName.Literal("build"),
                UfsName.Literal("a.out"),
            ),
        actual = concatenatedPath,
    )
  }

  @Test
  fun test_UfsRelativePath_concat_multipleProper() {
    val concatenatedPath =
        UfsRelativePath.concat(
            UfsRelativePath.of(
                UfsName.Literal("build"),
            ),
            UfsRelativePath.of(
                UfsName.Literal("artifacts"),
                UfsName.Literal("intermediate"),
            ),
            UfsRelativePath.of(
                UfsName.Literal("a.o"),
            ),
        )

    assertEquals(
        expected =
            UfsRelativePath.of(
                UfsName.Literal("build"),
                UfsName.Literal("artifacts"),
                UfsName.Literal("intermediate"),
                UfsName.Literal("a.o"),
            ),
        actual = concatenatedPath,
    )
  }

  @Test
  fun test_UfsAbsolutePath_construct_emptyInner() {
    assertEquals(
        expected =
            UfsAbsolutePath(
                UfsRelativePath.Empty,
            ),
        actual = UfsAbsolutePath.Root,
    )
  }

  @Test
  fun test_UfsAbsolutePath_parse_empty() {
    assertIs<IllegalArgumentException>(
        assertFails { UfsAbsolutePath.parse("") },
    )
  }

  @Test
  fun test_UfsAbsolutePath_parse_root() {
    val parsedPath: UfsAbsolutePath<UfsName> = UfsAbsolutePath.parse("/")

    assertEquals(
        expected = UfsAbsolutePath.Root,
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsAbsolutePath_toUnixAbsolutePathString_root() {
    assertEquals(
        expected = "/",
        actual = UfsAbsolutePath.Root.toUnixAbsolutePathString(),
    )
  }

  @Test
  fun test_UfsAbsolutePath_parse_root_double() {
    assertIs<IllegalArgumentException>(
        assertFails { UfsAbsolutePath.parse("//") },
    )
  }

  @Test
  fun test_UfsAbsolutePath_parse_posix_special() {
    assertIs<IllegalArgumentException>(
        assertFails { UfsAbsolutePath.parse("//posix/special/path") },
    )
  }

  @Test
  fun test_UfsAbsolutePath_parse_topLevel() {
    val parsedPath: UfsAbsolutePath<UfsName> = UfsAbsolutePath.parse("/tmp")

    assertEquals(
        expected =
            UfsAbsolutePath.of(
                UfsName.Literal("tmp"),
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsAbsolutePath_parse_trailingSeparator() {
    assertIs<IllegalArgumentException>(
        assertFails { UfsAbsolutePath.parse("/tmp/") },
    )
  }

  @Test
  fun test_UfsAbsolutePath_toUnixAbsolutePathString_topLevel() {
    val unixPath =
        UfsAbsolutePath.of(
            UfsName.Literal("tmp"),
        )

    assertEquals(
        expected = "/tmp",
        actual = unixPath.toUnixAbsolutePathString(),
    )
  }

  @Test
  fun test_UfsAbsolutePath_parse_nested() {
    val parsedPath: UfsAbsolutePath<UfsName> = UfsAbsolutePath.parse("/usr/lib")

    assertEquals(
        expected =
            UfsAbsolutePath.of(
                UfsName.Literal("usr"),
                UfsName.Literal("lib"),
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsAbsolutePath_toUnixAbsolutePathString_nested() {
    val unixPath =
        UfsAbsolutePath.of(
            UfsName.Literal("usr"),
            UfsName.Literal("lib"),
        )

    assertEquals(
        expected = "/usr/lib",
        actual = unixPath.toUnixAbsolutePathString(),
    )
  }

  @Test
  fun test_UfsAbsolutePath_parse_symbolicNames() {
    val parsedPath: UfsAbsolutePath<UfsName> = UfsAbsolutePath.parse("/usr/./lib/..")

    assertEquals(
        expected =
            UfsAbsolutePath.of(
                UfsName.Literal("usr"),
                UfsName.Symbolic.ThisDirectory,
                UfsName.Literal("lib"),
                UfsName.Symbolic.ParentDirectory,
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsAbsolutePath_toUnixAbsolutePathString_symbolicNames() {
    val unixPath =
        UfsAbsolutePath.of(
            UfsName.Literal("usr"),
            UfsName.Symbolic.ThisDirectory,
            UfsName.Literal("lib"),
            UfsName.Symbolic.ParentDirectory,
        )

    assertEquals(
        expected = "/usr/./lib/..",
        actual = unixPath.toUnixAbsolutePathString(),
    )
  }

  @Test
  fun test_UfsAbsolutePath_parse_consecutive_separator() {
    assertIs<IllegalArgumentException>(
        assertFails { UfsAbsolutePath.parse("/usr/bin//vim") },
    )
  }

  @Test
  fun test_UfsAbsolutePath_parse_deeplyNested() {
    val parsedPath: UfsAbsolutePath<UfsName> = UfsAbsolutePath.parse("/usr/local/bin/vim")

    assertEquals(
        expected =
            UfsAbsolutePath.of(
                UfsName.Literal("usr"),
                UfsName.Literal("local"),
                UfsName.Literal("bin"),
                UfsName.Literal("vim"),
            ),
        actual = parsedPath,
    )
  }

  @Test
  fun test_UfsAbsolutePath_toUnixAbsolutePathString_deeplyNested() {
    val unixPath =
        UfsAbsolutePath.of(
            UfsName.Literal("usr"),
            UfsName.Literal("local"),
            UfsName.Literal("bin"),
            UfsName.Literal("vim"),
        )

    assertEquals(
        expected = "/usr/local/bin/vim",
        actual = unixPath.toUnixAbsolutePathString(),
    )
  }

  @Test
  fun test_UfsAbsolutePath_resolve_root_empty() {
    val resolvedPath = UfsAbsolutePath.Root.resolve(UfsRelativePath.Empty)

    assertEquals(
        expected = UfsAbsolutePath.Root,
        actual = resolvedPath,
    )
  }

  @Test
  fun test_UfsAbsolutePath_resolve_root_proper() {
    val resolvedPath =
        UfsAbsolutePath.Root.resolve(
            UfsRelativePath.of(
                UfsName.Literal("tmp"),
            ),
        )

    assertEquals(
        expected =
            UfsAbsolutePath.of(
                UfsName.Literal("tmp"),
            ),
        actual = resolvedPath,
    )
  }

  @Test
  fun test_UfsAbsolutePath_resolve_nested() {
    val resolvedPath =
        UfsAbsolutePath.of(
                UfsName.Literal("tmp"),
            )
            .resolve(
                UfsRelativePath.of(
                    UfsName.Literal("tmpdir-123"),
                    UfsName.Literal("file.txt"),
                ),
            )

    assertEquals(
        expected =
            UfsAbsolutePath.of(
                UfsName.Literal("tmp"),
                UfsName.Literal("tmpdir-123"),
                UfsName.Literal("file.txt"),
            ),
        actual = resolvedPath,
    )
  }

  @Test
  fun test_LiteralAbsolutePath_relativizeAgainst_nested() {
    val relativePath =
        UfsAbsolutePath.of(
                UfsName.Literal("tmp"),
                UfsName.Literal("project"),
                UfsName.Literal("src"),
                UfsName.Literal("App.kt"),
            )
            .relativizeAgainst(
                basePath =
                    UfsAbsolutePath.of(
                        UfsName.Literal("tmp"),
                        UfsName.Literal("project"),
                    ),
            )

    assertEquals(
        expected =
            UfsRelativePath.of(
                UfsName.Literal("src"),
                UfsName.Literal("App.kt"),
            ),
        actual = relativePath,
    )
  }

  @Test
  fun test_LiteralAbsolutePath_relativizeAgainst_samePath() {
    val relativePath =
        UfsAbsolutePath.of(
                UfsName.Literal("tmp"),
                UfsName.Literal("project"),
            )
            .relativizeAgainst(
                basePath =
                    UfsAbsolutePath.of(
                        UfsName.Literal("tmp"),
                        UfsName.Literal("project"),
                    ),
            )

    assertEquals(
        expected = UfsRelativePath.Empty,
        actual = relativePath,
    )
  }

  @Test
  fun test_LiteralAbsolutePath_relativizeAgainst_nonPrefix() {
    val exception = assertFails {
      UfsAbsolutePath.of(
              UfsName.Literal("tmp"),
              UfsName.Literal("other"),
              UfsName.Literal("App.kt"),
          )
          .relativizeAgainst(
              basePath =
                  UfsAbsolutePath.of(
                      UfsName.Literal("tmp"),
                      UfsName.Literal("project"),
                  ),
          )
    }

    assertIs<IllegalArgumentException>(exception)
  }
}
