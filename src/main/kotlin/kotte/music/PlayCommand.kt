package kotte.music

import kotte.java.music.PlayerManager
import kotte.java.music.Youtube
import kotte.ReceivedEvent
import com.fasterxml.jackson.module.kotlin.*
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.net.URL
import java.io.IOException
import java.util.ArrayList

class PlayCommand(private val token: String) : ReceivedEvent {
    override fun invoke(e: GuildMessageReceivedEvent, args: ArrayList<String>?) {
        if (e.guild.audioManager.connectedChannel!!.members.contains(e.member) && e.guild.audioManager.isConnected) {
            // If arguments were passed
            if (args != null) {
                val manager = PlayerManager.GetInstance()

                // Check if the url was valid
                try {
                    URL(args[0]).openStream().close()

                    // Plays the song with the url passed
                    manager.LoadAndPlay(e.channel, args[0])
                }
                // Else, fetch from youtube
                catch (exception: Exception) {
                    val search: String = args.joinToString("+")
                    val urlString = "https://www.googleapis.com/youtube/v3/search?part=snippet&order=relevance&q=$search&key=$token"

                    try {
                        // Grabs the most relevant youtube-url
                        val mapper = jacksonObjectMapper()
                        val youtube = mapper.readValue<Youtube>(URL(urlString))
                        val out = "https://www.youtube.com/watch?v=${youtube.items[0].id.videoId}"

                        // Plays the song
                        manager.LoadAndPlay(e.channel, out)
                    }
                    catch (exception: IOException) {
                        println(exception.message)
                    }
                }

                manager.GetGuildMusicManager(e.guild).player.volume = 100
            }
            else {
                val manager: PlayerManager = PlayerManager.GetInstance()

                // If the player was paused, continue to play
                if (manager.GetGuildMusicManager(e.guild).player.playingTrack != null) {
                    val message = "Continuing to play: ${manager.GetGuildMusicManager(e.guild).player.playingTrack.info.title}"

                    e.channel.sendMessage(message).queue()
                    println(message)

                    // Resumes the song
                    PlayerManager.GetInstance().GetGuildMusicManager(e.guild).player.isPaused = false
                }
                else {
                    // If the song wasn't paused and no arguments were passed
                    e.channel.sendMessage("${e.author.asMention} You need to specify what song to play!").queue()
                }
            }
        }
        else {
            // If the bot isn't in any channel, or the user trying to play isn't in any channel
            e.channel.sendMessage("${e.author.asMention} I'm not in a channel, or you aren't").queue()
        }
    }

    override fun getHelp(): String = "Plays a song from a url or name. Resumes the song if the bot was previously paused."
}