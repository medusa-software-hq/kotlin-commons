package software.medusa.commons.unix.filesystem.mutation

/**
 * A described change to a single filesystem entity.
 *
 * Mutations are plain data: they describe *what* should change without performing it. Applying a
 * mutation to a concrete mutable entity is done separately (see [applyMutation]), which keeps the
 * description reusable, inspectable, and testable independently of any filesystem backend.
 */
sealed class UfsEntityMutation
