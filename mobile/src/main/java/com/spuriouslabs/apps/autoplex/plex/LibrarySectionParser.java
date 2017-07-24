package com.spuriouslabs.apps.autoplex.plex;

import android.support.annotation.Nullable;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by omalleym on 7/18/17.
 */

public class LibrarySectionParser {
	private final String ns = null;

	public String parse_library_sections(String xml) throws XmlPullParserException, IOException
	{
		InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);
			parser.nextTag();
			return parse_feed(parser);
		} finally {
			in.close();
		}
	}

	@Nullable
	private String parse_feed(XmlPullParser parser) throws XmlPullParserException, IOException
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
							return parser.getAttributeValue(ns, "key");
					}
					break;
			}
			event_type = parser.next();
		}
		return null;
	}
}
