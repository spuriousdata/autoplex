package com.spuriouslabs.apps.autoplex.plex.utils;

import com.android.volley.toolbox.StringRequest;

/**
 * Created by omalleym on 7/22/17.
 */

public class PlexConnectionSet {
	private String localUri;
	private String remoteUri;
	private String relayUri;

	public void setLocalUri(String localUri, boolean overwrite) {
		if (this.localUri != null && !overwrite)
			return;
		this.localUri = localUri;
	}
	
	public void setLocalUri(String localUri)
	{
		setLocalUri(localUri, false);
	}

	public void setRelayUri(String relayUri, boolean overwrite) {
		if (this.relayUri != null && !overwrite)
			return;
		this.relayUri = relayUri;
	}

	public void setRelayUri(String relayUri)
	{
		setRelayUri(relayUri, false);
	}

	public void setRemoteUri(String remoteUri, boolean overwrite) {
		if (this.remoteUri != null && !overwrite)
			return;
		this.remoteUri = remoteUri;
	}

	public void setRemoteUri(String remoteUri)
	{
		setRemoteUri(remoteUri, false);
	}

	public String getLocalUri() {
		return localUri;
	}

	public String getRelayUri() {
		return relayUri;
	}

	public String getRemoteUri() {
		return remoteUri;
	}
}
