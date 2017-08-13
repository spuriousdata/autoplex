package com.spuriouslabs.apps.autoplex.http;

import com.android.volley.Response;

import java.util.HashMap;

/**
 * Created by omalleym on 7/27/17.
 */

public class PlexTokenHttpHeadRequest extends HttpRequest
{
	public PlexTokenHttpHeadRequest(String uri, String token,
									Response.Listener<String> listener,
									Response.ErrorListener error_listener)
	{
		super(Method.HEAD, uri, listener, error_listener);
		headers = new HashMap<>();
		headers.put("X-Plex-Token", token);
	}
}
