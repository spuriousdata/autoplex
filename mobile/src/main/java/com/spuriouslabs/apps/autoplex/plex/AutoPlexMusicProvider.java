package com.spuriouslabs.apps.autoplex.plex;

import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser.MediaItem;
import android.os.AsyncTask;
import android.telecom.Call;
import android.util.Log;
import android.view.MenuItem;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.spuriouslabs.apps.autoplex.AutoPlexMusicService;
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
import static android.media.browse.MediaBrowser.MediaItem.FLAG_PLAYABLE;

/**
 * Created by omalleym on 7/28/17.
 *
 * Lot's of this was copied from https://github.com/googlesamples/android-MediaBrowserService/blob/master/Application/src/main/java/com/example/android/mediabrowserservice/model/MusicProvider.java
 */

public class AutoPlexMusicProvider
{
	private static final String TAG = AutoPlexMusicProvider.class.getSimpleName();

	public static final String MEDIA_ID_ROOT = "__ROOT__";

	private final PlexConnector connector;
	private static AutoPlexMusicProvider instance;

	// Categorized caches for music track data:
	private final LinkedHashMap<String, List<BrowsableMenuItem>> music_list_by_id;
	private final LinkedHashMap<String, PlayableMenuItem> tracks_by_id;

	private volatile Counter request_counter = new Counter();

	/**
	 * Callback used by MusicService.
	 */
	public interface Callback {
		void onMusicCatalogReady(boolean success);
	}

	private AutoPlexMusicProvider(PlexConnector connector) {
		music_list_by_id = new LinkedHashMap<>();
		tracks_by_id = new LinkedHashMap<>();
		this.connector = connector;
	}

	public static AutoPlexMusicProvider get_instance(PlexConnector connector)
	{
		if (instance == null)
			instance = new AutoPlexMusicProvider(connector);
		return instance;
	}

	public List<BrowsableMenuItem> getMenu(String parent) {

		Log.d(TAG, "getMenu(" + parent + ")");

		if (!hasMenuFor(parent))
			return Collections.emptyList();

		return music_list_by_id.get(parent);
	}

	public boolean hasMenuFor(String parent)
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
	public PlayableMenuItem getMusic(String music_id) {
		return tracks_by_id.get(music_id);
	}

	/**
	 * Get the list of music tracks from a server and caches the track information
	 * for future reference, keying tracks by musicId and grouping by genre.
	 */
	public void retrieveMediaAsync(final String parent, final Callback callback) {
		Log.d(TAG, "retrieveMediaAsync called");
		if (hasMenuFor(parent)) {
			// alredy got this, so call the callback and return
			callback.onMusicCatalogReady(true);
			return;
		}

		// Asynchronously load the music catalog in a separate thread
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				retrieveMedia(parent, callback);
				return parent;
			}
		}.execute();
	}

	private synchronized void retrieveMedia(final String parent, final Callback callback) {

		Log.d(TAG, "retrieveMedia(" + parent + ")");

		if (!hasMenuFor(parent)) {

			String menu_url;
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

							if (item.getFlags() == FLAG_PLAYABLE)
								tracks_by_id.put(item.getMediaId(), (PlayableMenuItem)item);
						}
						music_list_by_id.put(parent, menu_list);
						callback.onMusicCatalogReady(true);
					} catch (XmlPullParserException | IOException e) {
						e.printStackTrace();
						callback.onMusicCatalogReady(false);
					}
				}
			}, new Response.ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					error.printStackTrace();
				}
			}));
		}
	}
}
