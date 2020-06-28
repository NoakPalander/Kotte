package kotte

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.util.ArrayList

interface ReceivedEvent {
    fun invoke(e: GuildMessageReceivedEvent, args: ArrayList<String>?)
    fun getHelp(): String
}