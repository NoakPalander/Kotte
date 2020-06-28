package kotte

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class MessageParser(private val serverConfig: ServerConfig) : ListenerAdapter() {
    constructor() : this(ServerConfig())

    private val messages: LinkedHashMap<String, String> = LinkedHashMap()
    private var commands: HashMap<String, ReceivedEvent> = HashMap()

    override fun onGuildMemberJoin(e: GuildMemberJoinEvent) {
        val channel = if (e.guild.defaultChannel != null) e.guild.defaultChannel else e.guild.textChannels[0]
        channel!!.sendMessage("${serverConfig.welcomeMessage} ${e.member.user.asMention}").queue()
    }

    override fun onGuildMemberLeave(e: GuildMemberLeaveEvent) {
        val channel = if (e.guild.defaultChannel != null) e.guild.defaultChannel else e.guild.textChannels[0]
        channel!!.sendMessage(serverConfig.goodbyeMessage).queue()
    }

    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) {
        try {
            Thread.sleep(500)
            if (!e.author.isBot) {
                for (bannedWord in serverConfig.banned!!) {
                    if (checkMessage(e, bannedWord)) {
                        messages.clear()
                        Thread.sleep(500)
                        break
                    }
                }

                checkCommand(e)
            }
        }
        catch (exception: Exception) {
            println(exception.message)
        }
    }

    private fun checkCommand(e: GuildMessageReceivedEvent) {
        // If message is command (starts with !)
        if (e.message.contentRaw.startsWith("!")) {
            // Strip the '!'
            var command = e.message.contentRaw.substring(1)

            // Gets all the arguments of the command
            val args = ArrayList(command.split(" "))
            args.removeAt(0)

            if (command.contains(" "))
                command = command.substring(0, command.indexOf(' '))

            // Calls the handle for the command
            if (args.isEmpty())
                commands[command]!!.invoke(e, null)
            else
                commands[command]!!.invoke(e, args)
        }
    }

    private fun checkMessage(e: GuildMessageReceivedEvent, bannedWord: String): Boolean {
        // If the banned word was a single word
        if (e.message.contentRaw.toLowerCase().contains(bannedWord)) {
            val response = e.message.contentRaw.replace("(?i)$bannedWord".toRegex(), "||${serverConfig.censor}||")

            e.channel.sendMessage("${e.author.asMention} said: $response").queue()
            e.channel.sendMessage("That word is banned on this server!").queue()
            e.message.delete().queue()

            return true
        }

        // Appends the message's content and id
        messages[e.messageId] = e.message.contentRaw

        // Convert keys to strings
        var advanced = messages.values.joinToString("")

        // Checks if the word is a banned one
        if (advanced.toLowerCase().contains(bannedWord)) {
            // Gets the start index of the banned word
            val start = advanced.toLowerCase().indexOf(bannedWord)
            // Gets the end index of the banned word
            val end = start + bannedWord.length - 1

            // Replaces all banned words with censored, respond message
            advanced = advanced.replace("(?i)$bannedWord".toRegex(), "||${serverConfig.censor}||")

            // Sends the response
            e.channel.sendMessage("${e.author.asMention} said: $advanced").queue()
            e.channel.sendMessage("That word is banned on this server!").queue()

            val ids: Array<String> = messages.keys.toTypedArray()

            // Iterates through messages' indices and deletes them
            for (i in start..end)
                e.channel.retrieveMessageById(ids[i]).queue { msg: Message -> msg.delete().queue(); }

            return true
        }

        return false
    }

    fun setOldHooks(parser: MessageParser) {
        commands = parser.commands
    }

    fun handleArgument(args: ArrayList<String>?, hooks: HashMap<String?, (GuildMessageReceivedEvent) -> Unit>, e: GuildMessageReceivedEvent) {
        // We have arguments
        if (args != null) {
            for (arg: String in args) {
                if (hooks.containsKey(arg))
                    hooks[arg]!!.invoke(e)
            }
        }
        else
            hooks[null]!!.invoke(e)
    }

    fun addCommand(key: String, event: ReceivedEvent) {
        commands[key] = event
    }
}