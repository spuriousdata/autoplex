package com.spuriouslabs.apps.autoplex.plex;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.spuriouslabs.apps.autoplex.http.HttpRequest;
import com.android.volley.toolbox.Volley;
import com.spuriouslabs.apps.autoplex.R;
import com.spuriouslabs.apps.autoplex.plex.utils.MenuItem;
import com.spuriouslabs.apps.autoplex.plex.utils.MusicLibrary;
import com.spuriouslabs.apps.autoplex.plex.utils.PlexCallback;
import com.spuriouslabs.apps.autoplex.plex.utils.PlexConnectionSet;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static android.content.Context.MODE_PRIVATE;

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
	List<MenuItem> top_level_menu;

	private static PlexConnector instance = null;

	public static PlexConnector getInstance(Context ctx)
	{
		if (instance == null)
			instance = new PlexConnector(ctx);
		return instance;
	}

	private PlexConnector(Context ctx)
	{
		settings = ctx.getSharedPreferences(ctx.getString(R.string.settings_name), MODE_PRIVATE);
		token = settings.getString(ctx.getString(R.string.token_name), null);

		String token_placeholder = ctx.getString(R.string.token_placeholder);

		if (token_placeholder.equals(token))
			Log.e("autoplex", "Token not set");

		rq = Volley.newRequestQueue(ctx);
	}

	public void discoverMusicLibraryKey(String plex_uri, final PlexCallback<MusicLibrary> callback)
	{
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("X-Plex-Token", token);
		String url = plex_uri + "/library/sections/";

		Log.d("autoplex", "Making http request to " + url);

		HttpRequest req = new HttpRequest(Request.Method.GET, url, new Response.Listener<String>() {
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
		});
		req.setHeaders(headers);
		rq.add(req);
	}


	public void discoverPlexConnections(final PlexCallback<PlexConnectionSet> callback)
	{
		String discovery_url = "https://plex.tv/api/resources?includeHttps=1&includeRelay=1&X-Plex-Token=" + token;

		HttpRequest req = new HttpRequest(Request.Method.GET, discovery_url, new Response.Listener<String>() {
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
		});
		rq.add(req);
	}

	public List<MenuItem> getTopMenu()
	{
		return top_level_menu;
	}

	public void prefetchMenuItems()
	{
		Map<String, String> headers = new HashMap<String, String>();
		String menu_url = getPreferredUri() + "/library/sections/" + music_library.getId();

		headers.put("X-Plex-Token", token);

		HttpRequest req = new HttpRequest(Request.Method.GET, menu_url, new Response.Listener<String>() {
			@Override
			public void onResponse(String response)
			{
				MusicMenuParser mmp = new MusicMenuParser();
				try {
					top_level_menu = mmp.parse_menu(response);
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
		});

		req.setHeaders(headers);
		rq.add(req);
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
}
