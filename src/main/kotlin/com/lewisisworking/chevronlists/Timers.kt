/**
 * Timers.kt
 * Pure logic of the timers: how elapsed time is written and stamped on an item.
 */
package com.lewisisworking.chevronlists

/** An estimate or an earlier stamp: `~2h`, `~30m` or `~1h 5m`. A word such as `~notes` is neither */
private val TIME_STAMP = Regex("""\s*~\d+\w*(?: \d+m\b)?""")

private const val LABEL_LENGTH = 30

/** Elapsed time as stamped on an item: `45s`, `5m` or `1h 5m` */
fun elapsedStamp(ms: Long): String {
    val seconds = ms / 1000
    val hours   = seconds / 3600
    val minutes = seconds % 3600 / 60
    return when {
        hours > 0   -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else        -> "${seconds}s"
    }
}

/** Time as a running clock shows it: `1:05` */
fun clockTime(ms: Long): String {
    val seconds = ms / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

/** Item text shortened for the status bar */
fun timerLabel(content: String): String =
    if (content.length > LABEL_LENGTH) content.take(LABEL_LENGTH) + "..." else content

/** The item line with `~elapsed` in place of any earlier estimate, or null if the line is not an item */
fun withElapsedStamp(line: String, prefix: String, elapsed: String): String? {
    fun stamped(content: String) = "${TIME_STAMP.replaceFirst(content, "").trim()} ~$elapsed".trim()
    parseBullet(line, prefix)?.let { return "${it.chevrons} ${it.prefix} ${stamped(it.content)}" }
    parseNumbered(line)?.let { return "${it.chevrons} ${it.num}. ${stamped(it.content)}" }
    return null
}
