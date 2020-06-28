package kotte

import kotte.music.PlayCommand
import kotte.java.music.PlayerManager
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.dv8tion.jda.api.*
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.min
import kotlin.system.exitProcess

class Bot(private var jda: JDA?, private var botConfig: BotConfig, private var serverConfig: ServerConfig, private var parser: MessageParser) {
    constructor() : this(null, BotConfig(), ServerConfig(), MessageParser())

    var isOnline = false

    private fun reloadResources(oldListener: MessageParser, resource: String, botConfig: String, serverConfig: String) {
        val mapper = jacksonObjectMapper()

        this.botConfig = mapper.readValue(File(resource + botConfig))
        this.serverConfig = mapper.readValue(File(resource + serverConfig))

        jda!!.removeEventListener(oldListener)
        parser = MessageParser(this.serverConfig)

        parser.setOldHooks(oldListener)
        jda!!.addEventListener(parser)
    }

    private fun setCommandHooks() {
        /* -- Text-channel commands -- */

        // !hello, greets the user
        parser.addCommand("hello", object : ReceivedEvent {
            override fun invoke(e: GuildMessageReceivedEvent, args: ArrayList<String>?) {
                val commands: HashMap<String?, (GuildMessageReceivedEvent) -> Unit> = hashMapOf(
                        // tts argument
                        "tts" to { it -> it.channel.sendMessage("${it.author.asMention} hello there").tts(true).queue(); },

                        // no argument
                        null to { it -> it.channel.sendMessage("${it.author.asMention} hello there").queue(); }
                )

                parser.handleArgument(args, commands, e)
            }

            override fun getHelp(): String = "Prompts the bot to greet you. Optional argument [tts]."
        })

        parser.addCommand("die", object : ReceivedEvent{
            override fun invoke(e: GuildMessageReceivedEvent, args: ArrayList<String>?) {
                val commands: HashMap<String?, (GuildMessageReceivedEvent) -> Unit> = hashMapOf(
                        // help command
                        "help" to { it -> it.channel.sendMessage("Usage: ${getHelp()}").queue(); },

                        // no argument
                        null to { it ->
                            val id = it.author.asTag.substring(it.author.asTag.indexOf('#'))
                            if (serverConfig.admins!!.contains(id)) {
                                println("Instructed to commit suicide by: ${it.author.asTag}")
                                jda!!.presence.setStatus(OnlineStatus.fromKey("offline"))

                                Thread.sleep(500)
                                exitProcess(-1)
                            }
                        }
                )
                parser.handleArgument(args, commands, e)
            }

            override fun getHelp(): String = "Emergency kills the bot."
        })

        /* -- !Text-channel commands -- */
        /* -- Music commands -- */
        parser.addCommand("join", object : ReceivedEvent {
            override fun invoke(e: GuildMessageReceivedEvent, args: ArrayList<String>?) {
                val commands: HashMap<String?, (GuildMessageReceivedEvent) -> Unit> = hashMapOf(
                        // help argument
                        "help" to { it -> it.channel.sendMessage("Usage: ${getHelp()}").queue(); },

                        // no argument
                        null to lambda@{ it ->
                            val vc = it.member!!.voiceState!!.channel
                            val tc = it.channel
                            val manager = it.guild.audioManager

                            // Check for failure
                            if (it.guild.audioManager.isConnected) {
                                tc.sendMessage("${it.author.asMention} I am already connected in a channel!").queue()
                                return@lambda
                            }
                            if (!it.member!!.voiceState!!.inVoiceChannel()) {
                                tc.sendMessage("${it.author.asMention} You need to be in a voice channel to connect me!").queue()
                                return@lambda
                            }
                            if (!it.guild.selfMember.hasPermission(vc!!, Permission.VOICE_CONNECT)) {
                                tc.sendMessage("${it.author.asMention} I lack permission to join your channel!").queue()
                                return@lambda
                            }
                            manager.openAudioConnection(vc)
                        }
                )

                parser.handleArgument(args, commands, e)
            }

            override fun getHelp(): String = "Makes the bot join your voice channel."
        })

        parser.addCommand("leave", object : ReceivedEvent {
            override fun invoke(e: GuildMessageReceivedEvent, args: ArrayList<String>?) {
                val commands: HashMap<String?, (GuildMessageReceivedEvent) -> Unit> = hashMapOf(
                        // help argument
                        "help" to { it -> it.channel.sendMessage("Usage: ${getHelp()}").queue(); },

                        // no arguments
                        null to lambda@ { it ->
                            val manager = it.guild.audioManager
                            if (!manager.isConnected) {
                                it.channel.sendMessage("${it.author.asMention} I'm not in a channel!").queue()
                                return@lambda
                            }
                            if (!manager.connectedChannel!!.members.contains(it.member)) {
                                it.channel.sendMessage("${it.author.asMention} You can't kick me if you're not in the channel!").queue()
                                println("${it.author.asTag} tried to kick me!")
                                return@lambda
                            }

                            PlayerManager.GetInstance().ClearList(it.guild)
                            manager.closeAudioConnection()
                        }
                )

                parser.handleArgument(args, commands, e)
            }

            override fun getHelp(): String = "Makes the bot leave your voice channel."
        })

        parser.addCommand("play", PlayCommand(botConfig.yutoken))

        parser.addCommand("stop", object : ReceivedEvent {
            override fun invoke(e: GuildMessageReceivedEvent, args: ArrayList<String>?) {
                val commands: HashMap<String?, (GuildMessageReceivedEvent) -> Unit> = hashMapOf(
                        // help argument
                        "help" to { it -> it.channel.sendMessage("Usage: ${getHelp()}").queue(); },

                        // no argument
                        null to { it ->
                            if (it.guild.audioManager.isConnected)
                                PlayerManager.GetInstance().ClearList(it.guild)
                            else
                                it.channel.sendMessage("${it.author.asMention} I'm not in a channel!").queue()
                        }
                )

                parser.handleArgument(args, commands, e)
            }

            override fun getHelp(): String = "Stops the music bot if it's connected to a channel."
        })

        parser.addCommand("pause", object : ReceivedEvent {
            override fun invoke(e: GuildMessageReceivedEvent, args: ArrayList<String>?) {
                val commands: HashMap<String?, (GuildMessageReceivedEvent) -> Unit> = hashMapOf(
                        "help" to { it ->  it.channel.sendMessage("Usage: ${getHelp()}").queue(); },

                        // no argument
                        null to { it -> PlayerManager.GetInstance().GetGuildMusicManager(it.guild).player.isPaused = true }
                )

                parser.handleArgument(args, commands, e)
            }

            override fun getHelp(): String = "Pauses the music bot."
        })

        parser.addCommand("skip", object : ReceivedEvent {
            override fun invoke(e: GuildMessageReceivedEvent, args: ArrayList<String>?) {
                val commands: HashMap<String?, (GuildMessageReceivedEvent) -> Unit> = hashMapOf(
                        // help argument
                        "help" to { it ->  it.channel.sendMessage("Usage: ${getHelp()}").queue(); },

                        // no argument
                        null to { it ->
                            if (PlayerManager.GetInstance().GetGuildMusicManager(it.guild).player.playingTrack == null) {
                                println("The bot is not playing anything! Unable to skip song.")
                                it.channel.sendMessage("The bot is not playing anything! Unable to skip song.").queue()
                            }
                            else {
                                var message = "Skipping ${PlayerManager.GetInstance().GetGuildMusicManager(it.guild).player.playingTrack.info.title}"
                                println(message)
                                it.channel.sendMessage(message).queue()

                                PlayerManager.GetInstance().GetGuildMusicManager(it.guild).scheduler.nextTrack()
                                message = "Now playing: ${PlayerManager.GetInstance().GetGuildMusicManager(it.guild).player.playingTrack.info.title}"
                                println(message)
                                it.channel.sendMessage(message).queue()
                            }
                        }
                )

                parser.handleArgument(args, commands, e)
            }

            override fun getHelp(): String = "Skips the current song."
        })

        parser.addCommand("queue", object : ReceivedEvent {
            override fun invoke(e: GuildMessageReceivedEvent, args: ArrayList<String>?) {
                val commands: HashMap<String?, (GuildMessageReceivedEvent) -> Unit> = hashMapOf(
                        // help argument
                        "help" to { it ->  it.channel.sendMessage("Usage: ${getHelp()}").queue(); },

                        // no argument
                        null to { it ->
                            val playerManager = PlayerManager.GetInstance()
                            val musicManager = playerManager.GetGuildMusicManager(it.guild)
                            val queue = musicManager.scheduler.queue

                            if (queue.isEmpty()) {
                                it.channel.sendMessage("The queue is empty!").queue()
                            }
                            else {
                                val tracks = ArrayList(queue)
                                val builder = EmbedBuilder()
                                builder.setTitle("Song Queue (Total: ${queue.size})")

                                if (musicManager.player.playingTrack != null)
                                    builder.appendDescription("(Currently playing): ${musicManager.player.playingTrack.info.title} " +
                                            "- ${musicManager.player.playingTrack.info.author}")

                                for (i: Int in 0 until min(queue.size, 20)) {
                                    val track = tracks[i]
                                    builder.appendDescription("(${i+1}): ${track.info.title} - ${track.info.author}")
                                }

                                it.channel.sendMessage(builder.build()).queue()
                            }
                        }
                )

                parser.handleArgument(args, commands, e)
            }

            override fun getHelp(): String = "Gets the music-queue."
        })
    }

    fun boot(resource: String, botConfig: String, serverConfig: String) {
        val mapper = jacksonObjectMapper()

        this.botConfig = mapper.readValue(File(resource + botConfig))
        this.serverConfig = mapper.readValue(File(resource + serverConfig))

        println("BOOTING!")
        println("Instructed to boot from the cmd!")

        jda = JDABuilder(AccountType.BOT).setToken(this.botConfig.token).build()
        jda!!.presence.activity = Activity.playing("Protect followers from satan")
        jda!!.presence.setStatus(OnlineStatus.fromKey("online"))

        parser = MessageParser(this.serverConfig)

        setCommandHooks()

        jda!!.addEventListener(parser)
        isOnline = true
    }

    fun reload(resource: String, botConfig: String, serverConfig: String) {
        println("RELOADING!")
        println("Instructed to reload from the cmd!")
        reloadResources(parser, resource, botConfig, serverConfig)
    }

    fun reboot() {
        println("REBOOTING!")
        println("Instructed to reboot from the cmd!")
        jda!!.shutdown()

        Thread.sleep(500)
        jda = JDABuilder(AccountType.BOT).setToken(botConfig.token).build()
        jda!!.presence.activity = Activity.playing("Protect followers from satan")
        jda!!.presence.setStatus(OnlineStatus.ONLINE)
        parser = MessageParser(serverConfig)
        setCommandHooks()
        jda!!.addEventListener(parser)

    }

    fun shutdown() {
        println("SHUTTING DOWN!")
        println("Instructed to shutdown from the cmd!")
        jda!!.awaitReady().shutdown()
        isOnline = false
    }

    fun kill() {
        println("COMMITTING SUICIDE!")
        jda!!.presence.setStatus(OnlineStatus.OFFLINE)
        println("Instructed to Emergency terminate from the cmd!")
        exitProcess(-2)
    }

    fun getApi(): JDA? {
        return if (isOnline) jda else null
    }

    fun disconnect() {
        println("Disconnecting from voice channel!")
    }
}
