package io.github.koss.mammut.base.util

import android.util.Log

fun interface Logger {

    fun log(priority: Logging.Priority, tag: String, message: () -> String, throwable: Throwable?)
}

private val AndroidLogger = Logger { priority, tag, message, throwable ->
    val realLog: (tag: String, message: String, Throwable?) -> Unit = when (priority) {
        Logging.Priority.Verbose -> Log::v
        Logging.Priority.Debug -> Log::d
        Logging.Priority.Info -> Log::i
        Logging.Priority.Warn -> Log::w
        Logging.Priority.Error -> Log::e
    }

    realLog(tag, message(), throwable)
}

object Logging {

    enum class Priority {
        Verbose,
        Debug,
        Info,
        Warn,
        Error,
    }

    var loggers = mutableListOf(
        AndroidLogger
    )

    inline fun <reified T> T.logWarning(throwable: Throwable? = null, noinline message: () -> String) =
            log(priority = Priority.Warn, tag = T::class.simpleName, throwable = throwable, message = message)

    @Suppress("unused") // T is not technically used, but is required to avoid being explicit with typing.
    inline fun <reified T> T.log(
            priority: Priority = Priority.Debug,
            tag: String? = null,
            throwable: Throwable? = null,
            noinline message: () -> String
    ) {
        val realTag = tag ?: T::class.simpleName!!

        loggers.forEach { logger: Logger -> logger.log(priority, realTag, message, throwable) }
    }
}