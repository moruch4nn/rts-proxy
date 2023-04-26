package dev.mr3n.rtsproxy

import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

object RateLimiter {
    var count = 0
    val timer = Timer()
    init {
        timer.scheduleAtFixedRate(1000L,1000L) { count = 0 }
    }
    fun check(): Boolean {
        count ++
        return count < 10
    }
}