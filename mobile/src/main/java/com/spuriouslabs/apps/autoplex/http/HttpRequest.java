package com.spuriouslabs.apps.autoplex.http;

import com.android.volley.AuthFailureError;
import com.android.volley.Response.Listener;
import com.android.volley.Response.ErrorListener;
import com.android.volley.toolbox.StringRequest;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by omalleym on 7/18/17.
 */

public class HttpRequest extends StringRequest {
	protected Map<String, String> headers;
	protected Map<String, String> params;

	public HttpRequest(int method, String url, Listener<String> listener, ErrorListener error_listener)
	{
		super(method, url, listener, error_listener);
	}

	public void setHeaders(Map<String, String> headers)
	{
		this.headers = new HashMap<>(headers);
	}

	public void setParams(Map<String, String> params)
	{
		this.params = new HashMap<>(params);
	}

	@Override
	public Map<String, String> getHeaders() throws AuthFailureError
	{
		return headers != null ? headers : super.getHeaders();
	}

	@Override
	public Map<String, String> getParams() throws AuthFailureError
	{
		return params != null ? params : super.getParams();
	}
}
