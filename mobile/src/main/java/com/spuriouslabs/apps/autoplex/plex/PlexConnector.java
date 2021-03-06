package com.spuriouslabs.apps.autoplex.plex;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.spuriouslabs.apps.autoplex.http.HttpRequest;
import com.android.volley.toolbox.Volley;
import com.spuriouslabs.apps.autoplex.R;
import com.spuriouslabs.apps.autoplex.http.PlexLoginHttpRequest;
import com.spuriouslabs.apps.autoplex.http.PlexTokenHttpHeadRequest;
import com.spuriouslabs.apps.autoplex.http.PlexTokenHttpRequest;
import com.spuriouslabs.apps.autoplex.plex.utils.MusicLibrary;
import com.spuriouslabs.apps.autoplex.plex.utils.PlexCallback;
import com.spuriouslabs.apps.autoplex.plex.utils.PlexConnectionSet;
import com.spuriouslabs.apps.autoplex.plex.xml.ConnectionResourceParser;
import com.spuriouslabs.apps.autoplex.plex.xml.LibrarySectionParser;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.UUID;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by omalleym on 7/15/2017.
 */

public class PlexConnector
{
	private final String TAG = PlexConnector.class.getSimpleName();
	private SharedPreferences settings;
	private PlexConnectionSet connections;
	private String token;
	private RequestQueue rq;
	private MusicLibrary music_library;
	private String plex_client_id;

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

		plex_client_id = settings.getString("plex_client_id", null);

		if (plex_client_id == null) {
			plex_client_id = UUID.randomUUID().toString();
			settings.edit().putString("plex_client_id", plex_client_id).apply();
		}

		String token_placeholder = ctx.getString(R.string.token_placeholder);

		if (token_placeholder.equals(token))
			Log.e(TAG, "Token not set");

		String ml_name = settings.getString("music_library_name", null);
		int ml_key = settings.getInt("music_library_key", -1);
		if (ml_name != null && ml_key != -1)
			music_library = new MusicLibrary(ml_name, ml_key);

		rq = Volley.newRequestQueue(ctx);
	}

	public void login(String username, String password, final PlexCallback<String> callback)
	{
		addRequest(new PlexLoginHttpRequest(username, password, plex_client_id, new Response.Listener<String>() {
			@Override
			public void onResponse(String response)
			{
				try {
					JSONObject json = new JSONObject(response);
					token = json.getJSONObject("user").getString("authToken");
					settings.edit().putString("plex_token", token).apply();
					callback.callback(response);
				} catch (Exception e) {
					Log.e(TAG, "Error" + e.toString());
				}
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error)
			{
				Log.e(TAG, error.getMessage());
			}
		}));
	}

	public void discoverMusicLibraryKey(String plex_uri, final PlexCallback<MusicLibrary> callback)
	{
		String url = plex_uri + "/library/sections/";

		Log.d(TAG, "Making http request to " + url);

		addRequest(new PlexTokenHttpRequest(url, token, new Response.Listener<String>() {
			@Override
			public void onResponse(String response)
			{
				try {
					music_library = new LibrarySectionParser().parse(response);
					settings.edit()
							.putInt("music_library_key", music_library.getId())
							.putString("music_library_name", music_library.getName())
							.apply();
					callback.callback(music_library);
				} catch (XmlPullParserException | IOException e) {
					Log.e(TAG, "Exception at line 95: " + e.toString());
				}
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error)
			{
				Log.e(TAG, error.getMessage());
			}
		}));
	}


	public void discoverPlexConnections(final PlexCallback<PlexConnectionSet> callback)
	{
		String discovery_url = "https://plex.tv/api/resources?includeHttps=1&includeRelay=1&X-Plex-Token=" + token;

		addRequest(new HttpRequest(Request.Method.GET, discovery_url, new Response.Listener<String>() {
			@Override
			public void onResponse(String response)
			{
				try {
					connections = new ConnectionResourceParser().parse(response);
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

	public String getPreferredUri()
	{
		String uri = null;

		String ps = settings.getString("preferred_server", null);
		if (ps != null) {
			switch (ps) {
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
		}
		return uri;
	}

	public void testConnection(final PlexCallback<Boolean> callback)
	{
		String url = getMusicLibraryUrl();
		if (url == null) {
			callback.callback(false);
			return;
		}

		addRequest(new PlexTokenHttpHeadRequest(getMusicLibraryUrl(), token, new Response.Listener<String>() {
			@Override
			public void onResponse(String response)
			{
				callback.callback(true);
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error)
			{
				callback.callback(false);
			}
		}));
	}

	public void addRequest(HttpRequest r)
	{
		rq.add(r);
	}

	public String getMusicLibraryUrl()
	{
		return getMusicLibraryUrl("");
	}

	public String getMusicLibraryUrl(String append)
	{
		if (music_library != null)
			return getPreferredUri() + "/library/sections/" + music_library.getId() + append;
		return null;
	}

	public String getToken()
	{
		return token;
	}
}
