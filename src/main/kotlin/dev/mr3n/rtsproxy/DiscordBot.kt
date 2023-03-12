package dev.mr3n.rtsproxy

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal


class DiscordBot : ListenerAdapter() {
    override fun onModalInteraction(event: ModalInteractionEvent) {
        when(event.modalId) {
            "connect-mc-account-modal" -> {
                val code = event.getValue("connect-mc-account-code-input")?.asString
                if(code == null) {
                    event.reply("認証コードが正しくありません。もう一度コードをお確かめの上、再度入力してください。").setEphemeral(true).queue()
                } else {
                    val uniqueId = RTSProxy.INSTANCE.codes.filterValues { it == code }.keys.firstOrNull()
                    if(uniqueId==null) {
                        val message = """認証コードが正しくありません。もう一度コードをお確かめの上、再度入力してください。""".trimIndent()
                        event.reply(message).setEphemeral(true).queue()
                    } else {
                        val username = RTSProxy.INSTANCE.usernames[uniqueId]?:"[ERROR:取得に失敗しました。]"
                        val connectionInfo = mapOf(
                            "id" to event.user.id,
                            "name" to "${event.user.name}#${event.user.discriminator}",
                            "verified" to true,
                            "metadata" to mapOf<String, Any>(
                                "username" to event.user.name,
                                "discriminator" to event.user.discriminator,
                                "avatarHash" to event.user.avatarId!!
                            )
                        )
                        if(RTSProxy.INSTANCE.users.containsKey(uniqueId)) {
                            RTSProxy.INSTANCE.db
                                .collection("users")
                                .document("$uniqueId")
                                .update("connections.discord.${connectionInfo["id"]}", connectionInfo).get()
                        } else {
                            val userInfo = mapOf(
                                "uuid" to uniqueId.toString(),
                                "name" to username,
                                "ip" to "",
                                "verified" to true,
                                "connections" to mapOf<String, Any>(
                                    "discord" to mapOf<String, Any>(
                                        event.user.id to connectionInfo
                                    )
                                )
                            )
                            RTSProxy.INSTANCE.db
                                .collection("users")
                                .document("$uniqueId")
                                .set(userInfo)
                        }
                        val message = """${username}との連携が完了しました。"""
                        RTSProxy.INSTANCE.codes.remove(uniqueId)
                        event.reply(message).setEphemeral(true).queue()
                    }
                }
            }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        when(event.button.id) {
            "connect-mc-account-button" -> {
                val inputCode = TextInput.create("connect-mc-account-code-input", "コードを入力", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("000000")
                    .build()
                val modal = Modal.create("connect-mc-account-modal", "マンクラフトと連携する")
                    .addActionRow(inputCode)
                    .build()
                event.replyModal(modal).queue()
            }
        }
    }
}