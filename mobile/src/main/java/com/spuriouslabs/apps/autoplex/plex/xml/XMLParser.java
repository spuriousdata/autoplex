package com.spuriouslabs.apps.autoplex.plex.xml;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by omalleym on 7/28/17.
 */

abstract public class XMLParser<T>
{
	protected String ns = null;

	public T parse(String xml) throws XmlPullParserException, IOException
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

	abstract protected T parse_feed(XmlPullParser parser) throws XmlPullParserException, IOException;
}
