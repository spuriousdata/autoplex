package com.spuriouslabs.apps.autoplex.plex.xml;

import android.support.annotation.Nullable;
import android.util.Xml;

import com.spuriouslabs.apps.autoplex.plex.utils.PlexConnectionSet;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by omalleym on 7/15/2017.
 */

public class ConnectionResourceParser extends XMLParser<PlexConnectionSet>
{
	@Nullable
	protected PlexConnectionSet parse_feed() throws XmlPullParserException, IOException
	{
		int event_type = parser.getEventType();
		boolean in_server_device = false;

		PlexConnectionSet connection_set = new PlexConnectionSet();

		parser.require(XmlPullParser.START_TAG, ns, "MediaContainer");
		while (event_type != XmlPullParser.END_DOCUMENT) {
			String name;
			switch (event_type) {
				case XmlPullParser.START_TAG:
					name = parser.getName();
					if (name.equals("Device") && parser.getAttributeValue(ns, "provides").equals("server")) {
						in_server_device = true;
					} else if (name.equals("Connection") && in_server_device) {
						String relay = parser.getAttributeValue(ns, "relay");
						String uri = parser.getAttributeValue(ns, "uri");
						int local = Integer.parseInt(parser.getAttributeValue(ns, "local"));
						boolean plexdirect = uri.contains("plex.direct");

						if (relay != null && Integer.parseInt(relay) == 1) {
							connection_set.setRelayUri(uri, plexdirect);
						} else if (local == 1) {
							connection_set.setLocalUri(uri, plexdirect);
						} else if (local == 0) {
							connection_set.setRemoteUri(uri, plexdirect);
						}
					}
					break;
				case XmlPullParser.END_TAG:
					if (parser.getName().equals("Device") && in_server_device)
						in_server_device = false;
					break;
			}
			event_type = parser.next();
		}
		return connection_set;
	}
}
