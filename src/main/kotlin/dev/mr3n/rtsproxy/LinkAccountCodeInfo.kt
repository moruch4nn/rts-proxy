package dev.mr3n.rtsproxy

import java.util.*

data class LinkAccountCodeInfo(
    val name: String,
    val uniqueId: UUID,
    val code: String,
    val expiresAt: Date
)