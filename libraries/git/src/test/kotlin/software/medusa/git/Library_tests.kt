package software.medusa.git

import kotlin.test.Test
import kotlin.test.assertEquals

class Library_tests {
  @Test
  fun test_getAnswer() {
    assertEquals(
        expected = 42,
        actual = Library.getAnswer(),
    )
  }
}
