package software.medusa.commons.markdown.utils

interface PoppableQueue<E : Any> {
  fun isEmpty(): Boolean

  fun pop(): E?

  fun peek(): E?
}

fun <E : Any> poppableQueueOf(elements: Iterable<E>): PoppableQueue<E> {
  val deque = ArrayDeque<E>(elements.toList())

  return object : PoppableQueue<E> {
    override fun isEmpty(): Boolean = deque.isEmpty()

    override fun pop(): E? = deque.removeFirstOrNull()

    override fun peek(): E? = deque.firstOrNull()
  }
}

fun <E : Any> PoppableQueue<E>.popWhile(
    predicate: (E) -> Boolean,
): Sequence<E> = sequence {
  while (true) {
    val element = peek() ?: return@sequence
    if (!predicate(element)) return@sequence
    pop()
    yield(element)
  }
}

inline fun <E : Any, reified R : Any> PoppableQueue<E>.popOfWhileNotNull(
    crossinline transform: (E) -> R?,
): Sequence<R> = sequence {
  while (true) {
    val element = peek() ?: return@sequence
    val transformedElement = transform(element) ?: return@sequence
    pop()
    yield(transformedElement)
  }
}

inline fun <E : Any, reified R : Any> PoppableQueue<E>.popWhileIsInstance(): Sequence<R> =
    sequence {
      while (true) {
        val element = peek() as? R ?: return@sequence
        pop()
        yield(element)
      }
    }
