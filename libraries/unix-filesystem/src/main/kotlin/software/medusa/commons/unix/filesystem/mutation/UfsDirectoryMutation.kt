package software.medusa.commons.unix.filesystem.mutation

import software.medusa.commons.unix.filesystem.UfsReadonlyEntity
import software.medusa.commons.unix.path.UfsName

/** A described change to a directory. */
sealed class UfsDirectoryMutation : UfsEntityMutation() {
  /** Removes the directory. */
  data class Delete(
      val mode: Mode,
  ) : UfsDirectoryMutation() {
    enum class Mode {
      /** Removes the directory, assuming it is already empty. Fails otherwise. */
      NonRecursive,

      /** Removes the directory together with all of its nested children. */
      Recursive,
    }
  }

  /**
   * Dives into the directory and applies a per-child operation to a selection of children, keyed by
   * name. Children not named in [operationByName] are left untouched.
   */
  data class Dive(
      val operationByName: Map<UfsName.Literal, Operation>,
  ) : UfsDirectoryMutation() {
    sealed class Operation {
      /**
       * Creates a new child materialized from [templateEntity], assuming no child with that name
       * already exists.
       */
      data class Create(
          val templateEntity: UfsReadonlyEntity,
      ) : Operation()

      /** Applies [mutation] to the existing child with the matching name. */
      data class Mutate(
          val mutation: UfsEntityMutation,
      ) : Operation()
    }
  }
}
