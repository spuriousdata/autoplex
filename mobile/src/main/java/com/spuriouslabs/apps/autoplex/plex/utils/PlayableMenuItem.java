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
	private String date;
	private MediaMetadata metadata;
	private MediaMetadata.Builder metadata_builder = new MediaMetadata.Builder();

	public PlayableMenuItem(final String title, final String key, final String icon_uri)
	{
		super(title, key, icon_uri, FLAG_PLAYABLE);
		metadata_builder.putString(METADATA_KEY_MEDIA_ID, key);
		metadata_builder.putString(METADATA_KEY_TITLE, title);
		metadata_builder.putString(METADATA_KEY_DISPLAY_ICON_URI, icon_uri);
		metadata = metadata_builder.build();
	}

	public String getKey()
	{
		return getMediaId();
	}

	public void setMetadata(MediaMetadata m)
	{
		metadata = m;
		metadata_builder = new MediaMetadata.Builder(m);
	}

	public MediaMetadata getMetadata()
	{
		return metadata;
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
		return metadata.getString(METADATA_KEY_ALBUM);
	}

	private void putString(String key, String value)
	{
		metadata_builder.putString(key, value);
		metadata = metadata_builder.build();
	}

	private void putLong(String key, long value)
	{
		metadata_builder.putLong(key, value);
		metadata = metadata_builder.build();
	}

	public void setAlbum(String album)
	{
		putString(METADATA_KEY_ALBUM, album);
	}

	public String getArtist()
	{
		return metadata.getString(METADATA_KEY_ARTIST);
	}

	public void setArtist(String artist)
	{
		putString(METADATA_KEY_ARTIST, artist);
	}

	public long getDuration()
	{
		return metadata.getLong(METADATA_KEY_DURATION);
	}

	public void setDuration(long duration_ms)
	{
		putLong(METADATA_KEY_DURATION, duration_ms);
	}

	public String getGenre()
	{
		return metadata.getString(METADATA_KEY_GENRE);
	}

	public void setGenre(String genre)
	{
		putString(METADATA_KEY_GENRE, genre);
	}

	public String getAlbumArtUri()
	{
		return metadata.getString(METADATA_KEY_ALBUM_ART_URI);
	}

	public void setAlbumArtUri(String album_art_uri)
	{
		putString(METADATA_KEY_ALBUM_ART_URI, album_art_uri);
	}

	public long getTrackNumber()
	{
		return metadata.getLong(METADATA_KEY_TRACK_NUMBER);
	}

	public void setTrackNumber(int track_number)
	{
		putLong(METADATA_KEY_TRACK_NUMBER, track_number);
	}

	public long getNumTracks()
	{
		return metadata.getLong(METADATA_KEY_NUM_TRACKS);
	}

	public void setNumTracks(int num_tracks)
	{
		putLong(METADATA_KEY_NUM_TRACKS, num_tracks);
	}
}