package com.spuriouslabs.apps.autoplex.http;

import com.android.volley.Response;

import java.util.HashMap;

/**
 * Created by omalleym on 7/27/17.
 */

public class PlexLoginHttpRequest extends HttpRequest
{
	public PlexLoginHttpRequest(String username, String password, String uuid,
								Response.Listener<String> listener,
								Response.ErrorListener error_listener)
	{
		super(Method.POST, "https://plex.tv/users/sign_in.json", listener, error_listener);

		headers = new HashMap<>();
		headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
		headers.put("X-Plex-Product", "Plex SSO");
		headers.put("Origin", "https://www.plex.tv");
		headers.put("X-Plex-Client-Identifier", uuid);
		headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		headers.put("Referer", "https://www.plex.tv/sign-in/");
		headers.put("Accept-Language", "en-US,en;q=0.8");

		params = new HashMap<>();
		params.put("user[login]", username);
		params.put("user[password]", password);
	}
}
