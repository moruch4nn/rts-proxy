package dev.mr3n.rtsproxy

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.DocumentChange
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.google.inject.Inject
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.mr3n.rtsproxy.model.User
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import java.nio.file.Path
import java.util.*
import kotlin.collections.set

@Plugin(id = "rtsproxy", name = "RTS Proxy")
class RTSProxy @Inject constructor(private val server: ProxyServer, @DataDirectory val dataDirectory: Path) {

    init { INSTANCE = this }

    val users = mutableMapOf<UUID, User>()

    init {
        val dataDir = dataDirectory.toFile()
        dataDir.mkdirs()
        dataDir.resolve("key.json")
    }

    private val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
        .setProjectId("redtownserver")
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .build()

    internal val db: Firestore = firestoreOptions.service

    private val registration = db.collection("users")
        .addSnapshotListener { snapshots, error ->
            if(error != null) {
                // ERROR🔥
                return@addSnapshotListener
            }
            snapshots?.documentChanges?.forEach { documentChange ->
                when(documentChange.type) {
                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                        val user = documentChange.document.toObject(User::class.java)
                        val uniqueId = UUID.fromString(user.uuid)
                        this.users[uniqueId] = user
                    }
                    DocumentChange.Type.REMOVED -> {
                        val uniqueId = UUID.fromString(documentChange.document.id)
                        this.users.remove(uniqueId)
                    }
                }
            }
        }

    val codes = mutableMapOf<UUID, String>()
    val usernames = mutableMapOf<UUID, String>()

    @Subscribe
    fun on(event: ProxyShutdownEvent) { registration.remove() }

    @Subscribe
    fun on(event: ProxyInitializeEvent) {
        val commandManager = server.commandManager
        val meta = commandManager.metaBuilder("velocitydefaultserver")
            .aliases("set")
            .plugin(this)
            .build()
        commandManager.register(meta,DefaultServerCommand.createDefaultServerCommand(server))
        JDABuilder.createDefault(System.getenv("RTS_DISCORD_BOT_TOKEN"))
            .enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS)
            .addEventListeners(DiscordBot())
            .build()
    }

    @Subscribe
    fun on(event: PostLoginEvent) {
        val request = mapOf(
            "uuid" to event.player.uniqueId.toString(),
            "name" to event.player.username,
            "ip" to event.player.remoteAddress.hostName,
            "verified" to true
        )
        if(this.users.containsKey(event.player.uniqueId)) {
            db.collection("users").document("${event.player.uniqueId}").update(request)
        } else {
            db.collection("users").document("${event.player.uniqueId}").set(request)
        }
    }

    @Subscribe
    fun on(event: LoginEvent) {
        this.usernames[event.player.uniqueId] = event.player.username
        // すでにDiscordと連携している場合はreturn
        if((this.users[event.player.uniqueId]?.connections?.get("discord")?.size?:0) >= 1) { return }
        // 認証コードを生成f
        val authCode = this.codes.getOrPut(event.player.uniqueId) {
            // 認証コード
            var authCode: String
            do { authCode = List(6) {"0123456789".random()}.joinToString("") } while (this.codes.containsValue(authCode))
            authCode
      }
        val reason = Component.text("RedTownServer", Style.style(TextColor.color(237, 28, 36)))
            .append(Component.text(" - ", Style.style(TextColor.color(79, 79, 79))))
            .append(Component.text("Discordアカウントと連携する方法\n", Style.style(TextColor.color(255, 255, 255))))
            .append(Component.text("1.", Style.style(TextColor.color(232, 60, 60))))
            .append(Component.text("公式Discord鯖の", Style.style(TextColor.color(170, 170, 170))))
            .append(Component.text("#アカウント連携", Style.style(TextColor.color(153, 170, 181))))
            .append(
                Component.text("(bit.ly/a-rts)", Style.style(TextColor.color(170, 170, 170), TextDecoration.UNDERLINED)).clickEvent(
                ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://bit.ly/a-rts")))
            .append(Component.text("にアクセス\n", Style.style(TextColor.color(170, 170, 170))))
            .append(Component.text("2.", Style.style(TextColor.color(232, 60, 60))))
            .append(Component.text("チャンネル内の", Style.style(TextColor.color(170, 170, 170))))
            .append(Component.text("[マインクラフトと連携する]", Style.style(TextColor.color(71, 82, 196), TextDecoration.UNDERLINED)))
            .append(Component.text("ボタンをクリック\n", Style.style(TextColor.color(170, 170, 170))))
            .append(Component.text("3.", Style.style(TextColor.color(232, 60, 60))))
            .append(Component.text("認証コード(", Style.style(TextColor.color(170, 170, 170))))
            .append(Component.text(authCode, Style.style(TextColor.color(232, 60, 60))))
            .append(Component.text(")を送信して認証を完了\n", Style.style(TextColor.color(170, 170, 170))))
        event.result = ResultedEvent.ComponentResult.denied(reason)
    }

    companion object {
        internal lateinit var INSTANCE: RTSProxy
    }
}