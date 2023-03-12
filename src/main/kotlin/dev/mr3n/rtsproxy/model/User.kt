package dev.mr3n.rtsproxy.model

import java.util.*

class User {
    val name: String = ""
    val uuid: String = ""
    val verified: Boolean = false
    val lastLogin: Date = Date()
    val ip: String = ""
    val connections: Map<String, Map<String, ConnectionInfo>> = mapOf()
}