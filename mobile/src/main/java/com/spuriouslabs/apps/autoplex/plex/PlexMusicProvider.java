package com.spuriouslabs.apps.autoplex.plex;

import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowser.MediaItem;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.spuriouslabs.apps.autoplex.http.PlexTokenHttpRequest;
import com.spuriouslabs.apps.autoplex.plex.utils.MenuItem;
import com.spuriouslabs.apps.autoplex.plex.xml.MusicMenuParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static android.media.browse.MediaBrowser.MediaItem.FLAG_BROWSABLE;

/**
 * Created by omalleym on 7/28/17.
 *
 * Lot's of this was copied from https://github.com/googlesamples/android-MediaBrowserService/blob/master/Application/src/main/java/com/example/android/mediabrowserservice/model/MusicProvider.java
 */

public class PlexMusicProvider
{
	private static final String TAG = PlexMusicProvider.class.getSimpleName();

	public static final String MEDIA_ID_ROOT = "__ROOT__";
	public static final String MEDIA_ID_EMPTY_ROOT = "__EMPTY__";

	private final PlexConnector connector;

	// Categorized caches for music track data:
	private final LinkedHashMap<String, List<MediaItem>> music_list_by_id;

	private enum State {
		NON_INITIALIZED, INITIALIZING, INITIALIZED
	}

	private volatile State current_state = State.NON_INITIALIZED;

	/**
	 * Callback used by MusicService.
	 */
	public interface Callback {
		void onMusicCatalogReady(boolean success);
	}

	public PlexMusicProvider(PlexConnector connector) {
		music_list_by_id = new LinkedHashMap<>();
		this.connector = connector;
	}

	public Iterable<MediaItem> getAllMusic() {
		if (current_state != State.INITIALIZED || music_list_by_id.isEmpty()) {
			return Collections.emptyList();
		}
		return music_list_by_id.values();
	}

	/**
	 * Return the MediaMetadata for the given musicID.
	 *
	 * @param music_id The unique music ID.
	 */
	public MediaItem getMusic(String music_id) {
		return music_list_by_id.containsKey(music_id) ? music_list_by_id.get(music_id) : null;
	}

	/**
	 * Update the metadata associated with a music_id. If the music_id doesn't exist, the
	 * update is dropped. (That is, it does not create a new mediaId.)
	 * @param music_id The ID
	 * @param metadata New Metadata to associate with it
	 */
	public synchronized void updateMusic(String music_id, MediaItem metadata) {
		MediaItem track = music_list_by_id.get(music_id);
		if (track != null) {
			music_list_by_id.put(music_id, metadata);
		}
	}

	public boolean isInitialized() {
		return current_state == State.INITIALIZED;
	}

	/**
	 * Get the list of music tracks from a server and caches the track information
	 * for future reference, keying tracks by musicId and grouping by genre.
	 */
	public void retrieveMediaAsync(final Callback callback) {
		Log.d(TAG, "retrieveMediaAsync called");
		if (current_state == State.INITIALIZED) {
			// Already initialized, so call back immediately.
			callback.onMusicCatalogReady(true);
			return;
		}

		// Asynchronously load the music catalog in a separate thread
		new AsyncTask<Void, Void, State>() {
			@Override
			protected State doInBackground(Void... params) {
				retrieveMedia(null);
				return current_state;
			}

			@Override
			protected void onPostExecute(State current) {
				if (callback != null) {
					callback.onMusicCatalogReady(current == State.INITIALIZED);
				}
			}
		}.execute();
	}

	private synchronized void retrieveMedia(final String parent) {
		if (current_state == State.NON_INITIALIZED) {
			current_state = State.INITIALIZING;

			String menu_url;
			if (parent == null)
				menu_url = connector.getMusicLibraryUrl();
			else if (parent.charAt(0) != '/')
				menu_url = connector.getMusicLibraryUrl("/" + parent);
			else menu_url = connector.getPreferredUri() + parent;

			Log.d("autoplex", "Getting url: " + menu_url);

			connector.addRequest(new PlexTokenHttpRequest(Request.Method.GET, menu_url, connector.getToken(), new Response.Listener<String>() {
				@Override
				public void onResponse(String response) {
					List<MediaItem> menu_list = new ArrayList<>();
					MediaDescription.Builder menu_builder = new MediaDescription.Builder();

					try {
						for(MenuItem item : new MusicMenuParser().parse(response)) {
							menu_builder.setTitle(item.getTitle()).setMediaId(item.getKey());
							menu_list.add(new MediaBrowser.MediaItem(menu_builder.build(), item.getFlag()));
							if (item.getFlag() == FLAG_BROWSABLE)
								retrieveMedia(item.getKey());
						}
						music_list_by_id.put(parent, menu_list);
					} catch (XmlPullParserException | IOException e) {
						e.printStackTrace();
					}
				}
			}, new Response.ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					error.printStackTrace();
				}
			}));
			current_state = State.INITIALIZED;
		}
	}
}
