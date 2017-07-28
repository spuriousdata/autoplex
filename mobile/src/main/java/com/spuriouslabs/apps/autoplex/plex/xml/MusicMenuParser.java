package com.spuriouslabs.apps.autoplex.plex.xml;

import android.media.browse.MediaBrowser;
import android.support.annotation.Nullable;
import android.util.Xml;

import com.spuriouslabs.apps.autoplex.plex.utils.MenuItem;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by omalleym on 7/15/2017.
 */

public class MusicMenuParser extends XMLParser<List<MenuItem>>
{
	@Nullable
	protected List<MenuItem> parse_feed(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		List<MenuItem> menu = new ArrayList<>();
		int event_type = parser.getEventType();

		parser.require(XmlPullParser.START_TAG, ns, "MediaContainer");
		while (event_type != XmlPullParser.END_DOCUMENT) {
			String name;
			switch (event_type) {
				case XmlPullParser.START_TAG:
					name = parser.getName();
					if (name.equals("Directory")) {
						menu.add(new MenuItem(
								parser.getAttributeValue(ns, "title"),
								parser.getAttributeValue(ns, "key"),
								MediaBrowser.MediaItem.FLAG_BROWSABLE));
					} else if (name.equals("Track")) {
						menu.add(new MenuItem(
								parser.getAttributeValue(ns, "title"),
								parser.getAttributeValue(ns, "key"),
								MediaBrowser.MediaItem.FLAG_PLAYABLE));
					}
					break;
			}
			event_type = parser.next();
		}
		return menu;
	}
}
