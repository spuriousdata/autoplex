package com.spuriouslabs.apps.autoplex.plex;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.util.Log;
import android.view.Menu;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.spuriouslabs.apps.autoplex.http.HttpRequest;
import com.android.volley.toolbox.Volley;
import com.spuriouslabs.apps.autoplex.R;
import com.spuriouslabs.apps.autoplex.http.PlexTokenHttpRequest;
import com.spuriouslabs.apps.autoplex.plex.utils.MenuItem;
import com.spuriouslabs.apps.autoplex.plex.utils.MusicLibrary;
import com.spuriouslabs.apps.autoplex.plex.utils.PlexCallback;
import com.spuriouslabs.apps.autoplex.plex.utils.PlexConnectionSet;
import com.spuriouslabs.apps.autoplex.plex.xml.ConnectionResourceParser;
import com.spuriouslabs.apps.autoplex.plex.xml.LibrarySectionParser;
import com.spuriouslabs.apps.autoplex.plex.xml.MusicMenuParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static android.media.browse.MediaBrowser.MediaItem.FLAG_BROWSABLE;

/**
 * Created by omalleym on 7/15/2017.
 */

public class PlexConnector
{
	private SharedPreferences settings;
	private PlexConnectionSet connections;
	private String token;
	private RequestQueue rq;
	private MusicLibrary music_library;
	private PlexMenu plex_menu;

	private static PlexConnector instance = null;

	public static PlexConnector getInstance(Context ctx)
	{
		if (instance == null)
			instance = new PlexConnector(ctx);
		return instance;
	}

	public PlexMenu getPlexMenu(){ return plex_menu;}

	private PlexConnector(Context ctx)
	{
		settings = ctx.getSharedPreferences(ctx.getString(R.string.settings_name), MODE_PRIVATE);
		token = settings.getString(ctx.getString(R.string.token_name), null);

		String token_placeholder = ctx.getString(R.string.token_placeholder);

		if (token_placeholder.equals(token))
			Log.e("autoplex", "Token not set");

		String ml_name = settings.getString("music_library_name", null);
		int ml_key = settings.getInt("music_library_key", -1);
		if (ml_name != null && ml_key != -1)
			music_library = new MusicLibrary(ml_name, ml_key);

		rq = Volley.newRequestQueue(ctx);
	}

	public void discoverMusicLibraryKey(String plex_uri, final PlexCallback<MusicLibrary> callback)
	{
		String url = plex_uri + "/library/sections/";

		Log.d("autoplex", "Making http request to " + url);

		rq.add(new PlexTokenHttpRequest(Request.Method.GET, url, token, new Response.Listener<String>() {
			@Override
			public void onResponse(String response)
			{
				LibrarySectionParser lsp = new LibrarySectionParser();
				try {
					music_library = lsp.parse_library_sections(response);
					settings.edit()
							.putInt("music_library_key", music_library.getId())
							.putString("music_library_name", music_library.getName())
							.apply();
					callback.callback(music_library);
				} catch (XmlPullParserException | IOException e) {
					Log.e("autoplex", "Exception at line 95: " + e.toString());
				}
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error)
			{
				Log.e("autoplex", error.getMessage());
			}
		}));
	}


	public void discoverPlexConnections(final PlexCallback<PlexConnectionSet> callback)
	{
		String discovery_url = "https://plex.tv/api/resources?includeHttps=1&includeRelay=1&X-Plex-Token=" + token;

		rq.add(new HttpRequest(Request.Method.GET, discovery_url, new Response.Listener<String>() {
			@Override
			public void onResponse(String response)
			{
				ConnectionResourceParser crp = new ConnectionResourceParser();
				try {
					connections = crp.parse_connections(response);
					settings.edit()
							.putString("local_server_uri", connections.getLocalUri())
							.putString("relay_server_uri", connections.getRelayUri())
							.putString("remote_server_uri", connections.getRemoteUri())
							.apply();
					callback.callback(connections);
				} catch (XmlPullParserException | IOException e) {
					e.printStackTrace();
				}
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error)
			{
				error.printStackTrace();
			}
		}));
	}

	public List<MediaBrowser.MediaItem> getMenu(String parent)
	{
		//return plex_menu.getChildren(parent);
		return new ArrayList<>();
	}

	public void prefetchMenuItems()
	{
		if (plex_menu == null)
			plex_menu = new PlexMenu();
		plex_menu.buildMenu("root");
	}

	public String getPreferredUri()
	{
		String uri = null;

		switch (settings.getString("preferred_server", null)) {
			case "local":
				uri = settings.getString("local_server_uri", null);
				break;
			case "relay":
				uri = settings.getString("relay_server_uri", null);
				break;
			case "remote":
				uri = settings.getString("remote_server_uri", null);
				break;
		}
		return uri;
	}

	public class PlexMenu
	{
		private Map<String, List<MediaBrowser.MediaItem>> menu;

		public PlexMenu()
		{
			menu = new HashMap<String, List<MediaBrowser.MediaItem>>();
		}

		public Map<String, List<MediaBrowser.MediaItem>> getMenu()
		{
			return menu;
		}

		public void buildMenu(final String parent)
		{
			String menu_url;
			if (parent == "root")
				menu_url = getPreferredUri() + "/library/sections/" + music_library.getId();
			else if (parent.charAt(0) != '/')
				menu_url = getPreferredUri() + "/library/sections/" + music_library.getId() + "/" + parent;
			else menu_url = getPreferredUri() + parent;

			Log.w("autoplex", "Getting url: " + menu_url);

			rq.add(new PlexTokenHttpRequest(Request.Method.GET, menu_url, token, new Response.Listener<String>() {
				@Override
				public void onResponse(String response) {
					List<MediaBrowser.MediaItem> menu_list = new ArrayList<>();
					MediaDescription.Builder menu_builder = new MediaDescription.Builder();

					try {
						for(MenuItem item : new MusicMenuParser().parse_menu(response)) {
							menu_builder.setTitle(item.getTitle()).setMediaId(item.getKey());
							menu_list.add(new MediaBrowser.MediaItem(menu_builder.build(), item.getFlag()));
							if (item.getFlag() == FLAG_BROWSABLE)
								buildMenu(item.getKey());
						}
						menu.put(parent, menu_list);
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
		}
	}
}
