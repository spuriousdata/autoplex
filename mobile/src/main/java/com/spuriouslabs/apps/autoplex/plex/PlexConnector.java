package com.spuriouslabs.apps.autoplex.plex;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.spuriouslabs.apps.autoplex.http.HttpRequest;
import com.android.volley.toolbox.Volley;
import com.spuriouslabs.apps.autoplex.R;
import com.spuriouslabs.apps.autoplex.plex.utils.MenuItem;
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
	private int music_library_key = -1;
	Map<String, String> top_level_menu;

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
			// Raise some kind of exception or error or something?
			;;

		rq = Volley.newRequestQueue(ctx);
	}

	/*
	 * Setup steps in order:
		discoverPlexConnections();
		discoverMusicLibraryKey();
		post_setup_callback.callback();
	*/

	public void discoverMusicLibraryKey(String plex_uri)
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
					music_library_key = Integer.parseInt(lsp.parse_library_sections(response));
					settings.edit().putInt("music_library_key", music_library_key).apply();
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

	public List<MenuItem> getTopMenu(String plex_uri)
	{
		Map<String, String> headers = new HashMap<String, String>();
		RequestFuture<String> future = RequestFuture.newFuture();
		String menu_url = plex_uri + "/library/sections/" + music_library_key;

		headers.put("X-Plex-Token", token);

		HttpRequest req = new HttpRequest(Request.Method.GET, menu_url, future, future);

		req.setHeaders(headers);
		rq.add(req);

		MusicMenuParser mmp = new MusicMenuParser();
		try {
			String data = future.get();
			return mmp.parse_menu(data);
		} catch (XmlPullParserException | IOException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
}
