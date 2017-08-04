package com.spuriouslabs.apps.autoplex.plex;

import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowser.MediaItem;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.spuriouslabs.apps.autoplex.http.PlexTokenHttpRequest;
import com.spuriouslabs.apps.autoplex.plex.utils.BrowsableMenuItem;
import com.spuriouslabs.apps.autoplex.plex.utils.Counter;
import com.spuriouslabs.apps.autoplex.plex.utils.PlayableMenuItem;
import com.spuriouslabs.apps.autoplex.plex.xml.MusicMenuParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static android.media.MediaMetadata.METADATA_KEY_ALBUM;
import static android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI;
import static android.media.MediaMetadata.METADATA_KEY_ARTIST;
import static android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI;
import static android.media.MediaMetadata.METADATA_KEY_DURATION;
import static android.media.MediaMetadata.METADATA_KEY_GENRE;
import static android.media.MediaMetadata.METADATA_KEY_MEDIA_ID;
import static android.media.MediaMetadata.METADATA_KEY_MEDIA_URI;
import static android.media.MediaMetadata.METADATA_KEY_NUM_TRACKS;
import static android.media.MediaMetadata.METADATA_KEY_TITLE;
import static android.media.MediaMetadata.METADATA_KEY_TRACK_NUMBER;

import static android.media.browse.MediaBrowser.MediaItem.FLAG_BROWSABLE;
import static android.media.browse.MediaBrowser.MediaItem.FLAG_PLAYABLE;

/**
 * Created by omalleym on 7/28/17.
 *
 * Lot's of this was copied from https://github.com/googlesamples/android-MediaBrowserService/blob/master/Application/src/main/java/com/example/android/mediabrowserservice/model/MusicProvider.java
 */

public class PlexMusicProvider
{
	private static final String TAG = PlexMusicProvider.class.getSimpleName();
	public static final String MEDIA_ID_ROOT = "__ROOT__";
	private static PlexMusicProvider instance = null;
	private final PlexConnector connector;

	// Categorized caches for music track data:
	private final LinkedHashMap<String, List<BrowsableMenuItem>> music_list_by_id;
	private final LinkedHashMap<String, PlayableMenuItem> tracks_by_id;

	/**
	 * Callback used by MusicService.
	 */
	public interface Callback {
		void onMusicCatalogReady(boolean success);
	}

	private PlexMusicProvider(PlexConnector connector) {
		music_list_by_id = new LinkedHashMap<>();
		tracks_by_id = new LinkedHashMap<>();
		this.connector = connector;
	}

	public static PlexMusicProvider get_instance(PlexConnector connector)
	{
		if (instance == null)
			instance = new PlexMusicProvider(connector);
		return instance;
	}

	public List<MediaItem> getMenu(String parent) {

		Log.d(TAG, "getMenu(" + parent + ")");

		if (!hasMenuFor(parent))
			return Collections.emptyList();

		List<MediaItem> menu = new ArrayList<>();
		MediaDescription.Builder menu_builder = new MediaDescription.Builder();

		for (BrowsableMenuItem item : music_list_by_id.get(parent)) {
			menu_builder.setTitle(item.getTitle()).setMediaId(item.getKey());
			menu.add(new MediaBrowser.MediaItem(menu_builder.build(), item.getFlag()));
		}

		return menu;
	}

	public synchronized boolean hasMenuFor(String parent)
	{
		if (music_list_by_id.isEmpty() || !music_list_by_id.containsKey(parent))
			return false;
		return true;
	}

	public String getUrlForMedia(PlayableMenuItem i)
	{
		return connector.getPreferredUri() + i.getMediaUri();
	}

	/**
	 * Return the MediaMetadata for the given musicID.
	 *
	 * @param music_id The unique music ID.
	 */
	public MediaMetadata getMusic(String music_id)
	{
		PlayableMenuItem track;
		if (tracks_by_id.containsKey(music_id))
			track = tracks_by_id.get(music_id);
		else return null;

		return new MediaMetadata.Builder()
				.putString(METADATA_KEY_MEDIA_ID, track.getKey())
				.putString(METADATA_KEY_MEDIA_URI, track.getMediaUri())
				.putString(METADATA_KEY_ALBUM, track.getAlbum())
				.putString(METADATA_KEY_ARTIST, track.getArtist())
				.putString(METADATA_KEY_GENRE, track.getGenre())
				.putString(METADATA_KEY_ALBUM_ART_URI, track.getAlbumArtUri())
				.putString(METADATA_KEY_TITLE, track.getTitle())
				.putString(METADATA_KEY_DISPLAY_ICON_URI, track.getIconUri())
				.putLong(METADATA_KEY_DURATION, track.getDuration())
				.putLong(METADATA_KEY_TRACK_NUMBER, track.getTrackNumber())
				.putLong(METADATA_KEY_NUM_TRACKS, track.getNumTracks())
				.build();
	}

	/**
	 * I have no idea under what circumstances this would be called. What would be doing the updating?
	 * @param music_id
	 * @param metadata
	 */
	public synchronized void updateMusic(String music_id, MediaMetadata metadata)
	{
		MediaMetadata track = getMusic(music_id);
		if (track != null) {
			tracks_by_id.put(music_id, PlayableMenuItem.fromMediaMetadata(metadata));
		}
	}

	/**
	 * Get the list of music tracks from a server and caches the track information
	 * for future reference, keying tracks by musicId and grouping by genre.
	 */
	public void retrieveMediaAsync(final String parent, final Callback callback, final boolean recurse)
	{
		Log.d(TAG, "retrieveMediaAsync called");
		if (hasMenuFor(parent)) {
			// already got this, so call the callback and return
			callback.onMusicCatalogReady(true);
			return;
		}

		// Asynchronously load the music catalog in a separate thread
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				retrieveMedia(parent, recurse, callback);
				return "";
			}
		}.execute();
	}

	public void retrieveMediaAsync(final String parent, final Callback callback)
	{
		retrieveMediaAsync(parent, callback, false);
	}

	private synchronized void retrieveMedia(final String parent, final boolean recurse, final Callback callback)
	{

		Log.d(TAG, "retrieveMedia(" + parent + ")");

		if (!hasMenuFor(parent)) {

			final String menu_url;
			if (parent.equals(MEDIA_ID_ROOT))
				menu_url = connector.getMusicLibraryUrl();
			else if (parent.charAt(0) != '/')
				menu_url = connector.getMusicLibraryUrl("/" + parent);
			else menu_url = connector.getPreferredUri() + parent;

			Log.d(TAG, "Getting url: " + menu_url);

			connector.addRequest(new PlexTokenHttpRequest(Request.Method.GET, menu_url,
					connector.getToken(), new Response.Listener<String>() {
				@Override
				public void onResponse(String response) {
					List<BrowsableMenuItem> menu_list = new ArrayList<>();

					try {
						for(BrowsableMenuItem item : new MusicMenuParser().parse(response)) {
							menu_list.add(item);

							if (item.getFlag() == FLAG_PLAYABLE)
								tracks_by_id.put(item.getKey(), (PlayableMenuItem)item);
							else if (item.getFlag() == FLAG_BROWSABLE && recurse)
								retrieveMedia(item.getKey(), recurse, callback);

						}
						music_list_by_id.put(parent, menu_list);
					} catch (XmlPullParserException | IOException e) {
						e.printStackTrace();
					}

					Log.d(TAG, "Finished fetching url: " + menu_url);
					callback.onMusicCatalogReady(true);
				}
			}, new Response.ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					Log.e(TAG, "Error fetching url: " + menu_url);
					error.printStackTrace();
					callback.onMusicCatalogReady(false);
				}
			}));
		}
	}
}
