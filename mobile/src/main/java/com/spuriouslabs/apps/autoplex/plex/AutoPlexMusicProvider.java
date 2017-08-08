package com.spuriouslabs.apps.autoplex.plex;

import android.media.MediaDescription;
import android.os.AsyncTask;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.util.ArrayMap;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.spuriouslabs.apps.autoplex.http.PlexTokenHttpRequest;
import com.spuriouslabs.apps.autoplex.plex.utils.BrowsableMenuItem;
import com.spuriouslabs.apps.autoplex.plex.utils.PlayableMenuItem;
import com.spuriouslabs.apps.autoplex.plex.xml.MusicMenuParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

	public List<MediaItem> getMenu(String parent) {

		Log.d(TAG, "getMenu(" + parent + ")");

		if (!hasMenuFor(parent))
			return Collections.emptyList();

		List<MediaItem> menu = new ArrayList<>();
		MediaDescription.Builder menu_builder = new MediaDescription.Builder();

		for (BrowsableMenuItem item : music_list_by_id.get(parent)) {
			/*
			 * So, this is insane. Somewhere in the android musicservice code there must be a
			 * specific type check for MediaItem because if you upcast the BrowsableMenuItem to
			 * MediaItem, it doesn't work. You must create a brand new vanilla MediaItem instance
			 * here and push that onto the list.
			 */
			menu.add(new MediaItem(item.getDescription(), item.getFlags()));
		}

		return menu;
	}

	public List<PlayableMenuItem> getAlbum(String album_uri)
	{
		List<PlayableMenuItem> album = new ArrayList<>();

		for (BrowsableMenuItem i : music_list_by_id.get(album_uri)) {
			Log.d(TAG, "Adding item " + i.getTitle() + " to album.");
			album.add((PlayableMenuItem)i);
		}
		return album;
	}

	public Map<String, String> getPlexTokenHeaders()
	{
		Map<String, String> headers = new ArrayMap<>();
		headers.put("X-Plex-Token", connector.getToken());
		return headers;
	}

	public synchronized boolean hasMenuFor(String parent)
	{
		return !(music_list_by_id.isEmpty() || !music_list_by_id.containsKey(parent));
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
	public void retrieveMediaAsync(final String parent, final Callback callback)
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
				retrieveMedia(parent, callback);
				return "";
			}
		}.execute();
	}

	private synchronized void retrieveMedia(final String parent, final Callback callback)
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

							if (item.getFlags() == FLAG_PLAYABLE)
								tracks_by_id.put(item.getMediaId(), (PlayableMenuItem)item);

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
