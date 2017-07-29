package com.spuriouslabs.apps.autoplex.plex.utils;

import android.media.MediaDescription;
import android.media.MediaMetadata;

import static android.media.MediaMetadata.METADATA_KEY_ALBUM;
import static android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI;
import static android.media.MediaMetadata.METADATA_KEY_ARTIST;
import static android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI;
import static android.media.MediaMetadata.METADATA_KEY_DURATION;
import static android.media.MediaMetadata.METADATA_KEY_GENRE;
import static android.media.MediaMetadata.METADATA_KEY_MEDIA_ID;
import static android.media.MediaMetadata.METADATA_KEY_NUM_TRACKS;
import static android.media.MediaMetadata.METADATA_KEY_TITLE;
import static android.media.MediaMetadata.METADATA_KEY_TRACK_NUMBER;
import static android.media.browse.MediaBrowser.MediaItem.FLAG_PLAYABLE;

/**
 * Created by omalleym on 7/28/17.
 */

public class PlayableMenuItem extends BrowsableMenuItem
{
	private String media_uri;
	private String album;
	private String artist;
	private long duration_ms;
	private String genre;
	private String album_art_uri;
	private int track_number;
	private int num_tracks;
	private String date;

	public static PlayableMenuItem fromMediaMetadata(MediaMetadata m)
	{
		PlayableMenuItem p = new PlayableMenuItem(m.getString(METADATA_KEY_TITLE),
				m.getString(METADATA_KEY_MEDIA_ID), m.getString(METADATA_KEY_DISPLAY_ICON_URI));

		p.setAlbum(m.getString(METADATA_KEY_ALBUM));
		p.setArtist(m.getString(METADATA_KEY_ARTIST));
		p.setGenre(m.getString(METADATA_KEY_GENRE));
		p.setAlbumArtUri(m.getString(METADATA_KEY_ALBUM_ART_URI));
		p.setDuration(m.getLong(METADATA_KEY_DURATION));
		p.setTrackNumber((int)m.getLong(METADATA_KEY_TRACK_NUMBER));
		p.setNumTracks((int)m.getLong(METADATA_KEY_NUM_TRACKS));

		return p;
	}

	public static PlayableMenuItem fromMediaDescription(MediaDescription m)
	{
		PlayableMenuItem p = new PlayableMenuItem(m.getTitle().toString(), m.getMediaId(),
				m.getIconUri().toString());
		p.setMediaUri(m.getMediaUri().toString());
		return p;
	}

	public PlayableMenuItem(final String title, final String key, final String icon_uri, String media_uri, String album,
							String artist, long duration_ms, String genre, String album_art_uri,
							int track_number, int num_tracks, String date)
	{
		super(title, key, icon_uri);
		this.media_uri = media_uri;
		this.album = album;
		this.album_art_uri = album_art_uri;
		this.artist = artist;
		this.duration_ms = duration_ms;
		this.genre = genre;
		this.track_number = track_number;
		this.num_tracks = num_tracks;
		this.date = date;
	}

	public PlayableMenuItem(final String title, final String key, final String icon_uri)
	{
		super(title, key, icon_uri);
	}

	public String getDate()
	{
		return date;
	}

	public void setDate(String date)
	{
		this.date = date;
	}

	public String getMediaUri()
	{
		return media_uri;
	}

	public void setMediaUri(String media_uri)
	{
		this.media_uri = media_uri;
	}

	public String getAlbum()
	{
		return album;
	}

	public void setAlbum(String album)
	{
		this.album = album;
	}

	public String getArtist()
	{
		return artist;
	}

	public void setArtist(String artist)
	{
		this.artist = artist;
	}

	public long getDuration()
	{
		return duration_ms;
	}

	public void setDuration(long duration_ms)
	{
		this.duration_ms = duration_ms;
	}

	public String getGenre()
	{
		return genre;
	}

	public void setGenre(String genre)
	{
		this.genre = genre;
	}

	public String getAlbumArtUri()
	{
		return album_art_uri;
	}

	public void setAlbumArtUri(String album_art_uri)
	{
		this.album_art_uri = album_art_uri;
	}

	public int getTrackNumber()
	{
		return track_number;
	}

	public void setTrackNumber(int track_number)
	{
		this.track_number = track_number;
	}

	public int getNumTracks()
	{
		return num_tracks;
	}

	public void setNumTracks(int num_tracks)
	{
		this.num_tracks = num_tracks;
	}

	@Override
	public @Flags int getFlag()
	{
		return FLAG_PLAYABLE;
	}
}