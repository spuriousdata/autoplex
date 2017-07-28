package com.spuriouslabs.apps.autoplex.plex.xml;

import android.support.annotation.Nullable;
import android.util.Xml;

import com.spuriouslabs.apps.autoplex.plex.utils.MusicLibrary;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by omalleym on 7/18/17.
 */

public class LibrarySectionParser extends XMLParser<MusicLibrary>
{
	@Nullable
	protected MusicLibrary parse_feed(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		int event_type = parser.getEventType();

		parser.require(XmlPullParser.START_TAG, ns, "MediaContainer");
		while (event_type != XmlPullParser.END_DOCUMENT) {
			String name;
			switch (event_type) {
				case XmlPullParser.START_TAG:
					name = parser.getName();
					if (name.equals("Directory")) {
						if (parser.getAttributeValue(ns, "type").equals("artist"))
							return new MusicLibrary(parser.getAttributeValue(ns, "title"),
									Integer.parseInt(parser.getAttributeValue(ns, "key")));
					}
					break;
			}
			event_type = parser.next();
		}
		return null;
	}
}
