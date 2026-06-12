package software.medusa.commons.unix.filesystem

import software.medusa.commons.unix.path.UfsName

/** An index of the direct children of a directory, keyed by their name. */
@JvmInline
value class UfsDirectoryIndex<out EntityT : UfsReadonlyEntity>(
    val childEntityByName: Map<UfsName.Literal, EntityT>,
)

typealias UfsReadonlyDirectoryIndex = UfsDirectoryIndex<UfsReadonlyEntity>

typealias UfsMutableDirectoryIndex = UfsDirectoryIndex<UfsMutableEntity>
