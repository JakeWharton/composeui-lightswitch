import kotlin.concurrent.AtomicInt
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

/** Tracks closable resources and closes them all at once. */
internal class Closer : AutoCloseable {
	private val closeables = mutableListOf<AutoCloseable>()

	/** Add an [AutoCloseable] resource to be closed. */
	operator fun plusAssign(closeable: AutoCloseable) {
		closeables += closeable
	}

	/** Add a lambda resource to be invoked when closed. */
	operator fun plusAssign(action: () -> Unit) {
		closeables += ActionCloseable(action)
	}

	private class ActionCloseable(
		private val action: () -> Unit,
	) : AutoCloseable {
		private val closed = AtomicInt(0)
		override fun close() {
			if (closed.compareAndSet(0, 1)) {
				action()
			}
		}
	}

	/**
	 * Create a new [Closer] and add it as a resource to this instance.
	 * Closing the child will only close its resources.
	 * Closing this instance will close the child's resources as well.
	 */
	fun childCloser(): Closer {
		val child = Closer()
		closeables += child
		return child
	}

	/**
	 * Close the resources tracked by this instance.
	 * Resources are closed in reverse order from which they were added.
	 * Any exceptions which occur during closing will be thrown as the first exception
	 * with all subsequent ones attached as [suppressed exceptions][Throwable.addSuppressed].
	 */
	override fun close() {
		performClose(null)?.let {
			throw it
		}
	}

	/**
	 * Close the resources tracked by this instance.
	 * Resources are closed in reverse order from which they were added.
	 * Any exceptions which occur during closing will be ignored.
	 */
	fun closeQuietly() {
		performClose(null)
	}

	/**
	 * Close the resources tracked by this instance in response to an exception.
	 * Resources are closed in reverse order from which they were added.
	 * Any exceptions which occur during closing will be attached as
	 * [suppressed exceptions][Throwable.addSuppressed] to [t].
	 */
	fun closeAndRethrow(t: Throwable): Nothing {
		performClose(t)
		throw t
	}

	private fun performClose(initialThrowable: Throwable?): Throwable? {
		var t = initialThrowable
		for (closeable in closeables.asReversed()) {
			try {
				closeable.close()
			} catch (e: Exception) {
				if (t == null) {
					t = e
				} else {
					t.addSuppressed(e)
				}
			}
		}
		return t
	}
}

internal class CloserScope(val closer: Closer) {
	fun <R : AutoCloseable> R.useInScope(): R = also(closer::plusAssign)
}

/**
 * Create a child [Closer] which will be closed whether [block] returns successfully
 * or throws an exception.
 *
 * @see Closer.childCloser
 */
internal fun <R> CloserScope.childCloseFinallyScope(block: CloserScope.() -> R): R {
	contract {
		callsInPlace(block, EXACTLY_ONCE)
	}
	return closer.childCloser().use {
		CloserScope(it).block()
	}
}

/**
 * Create a [Closer] which will be closed whether [block] returns successfully
 * or throws an exception.
 */
internal fun <R> closeFinallyScope(block: CloserScope.() -> R): R {
	contract {
		callsInPlace(block, EXACTLY_ONCE)
	}
	return Closer().use {
		CloserScope(it).block()
	}
}

/**
 * Create a [Closer] which will be closed if [block] throws.
 * If no exception is thrown, you **must** encapsulate the [`closer`][CloserScope.closer]
 * within the returned value and close it at a later time.
 */
internal inline fun <R> closeOnThrowScope(block: CloserScope.() -> R): R {
	contract {
		callsInPlace(block, EXACTLY_ONCE)
	}
	val closer = Closer()
	return try {
		CloserScope(closer).block()
	} catch (t: Throwable) {
		closer.closeAndRethrow(t)
	}
}
