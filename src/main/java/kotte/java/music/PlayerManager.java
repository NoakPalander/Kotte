package kotte.java.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.HashMap;
import java.util.Map;

public class PlayerManager {
    private static final PlayerManager s_Instance = new PlayerManager();
    private final AudioPlayerManager m_PlayerManager;
    private final Map<Long, GuildMusicManager> m_MusicManagers;

    private PlayerManager() {
        m_PlayerManager = new DefaultAudioPlayerManager();
        m_MusicManagers = new HashMap<>();

        AudioSourceManagers.registerRemoteSources(m_PlayerManager);
        AudioSourceManagers.registerLocalSource(m_PlayerManager);
    }

    public synchronized GuildMusicManager GetGuildMusicManager(final Guild guild) {
        long id = guild.getIdLong();
        GuildMusicManager manager = m_MusicManagers.get(id);

        if (manager == null) {
            manager = new GuildMusicManager(m_PlayerManager);
            m_MusicManagers.put(id, manager);
        }

        guild.getAudioManager().setSendingHandler(manager.getSendHandler());
        return manager;
    }

    public void LoadAndPlay(final TextChannel channel, final String trackUrl) {
        GuildMusicManager manager = GetGuildMusicManager(channel.getGuild());
        m_PlayerManager.loadItemOrdered(manager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("Adding to queue: " + track.getInfo().title).queue();
                Play(manager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();
                if (firstTrack == null)
                    firstTrack = playlist.getTracks().get(0);

                channel.sendMessage("Adding to queue: " + firstTrack.getInfo().title + " (first track of playlist "
                        + playlist.getName() + ")").queue();

                Play(manager, firstTrack);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Couldn't find track: " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Couldn't play: " + exception.getMessage()).queue();
                exception.printStackTrace();
            }
        });

    }

    public void ClearList(final Guild guild) {
        GetGuildMusicManager(guild).player.stopTrack();
        GetGuildMusicManager(guild).player.setPaused(false);
    }

    private void Play(final GuildMusicManager manager, final AudioTrack track) {
        System.out.println("Playing track " + track.getInfo().title);
        manager.scheduler.queue(track);
    }

    public static synchronized PlayerManager GetInstance() {
        return s_Instance;
    }
}
