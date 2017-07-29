package com.spuriouslabs.apps.autoplex.plex.xml;

import android.support.annotation.Nullable;

import com.spuriouslabs.apps.autoplex.plex.utils.BrowsableMenuItem;
import com.spuriouslabs.apps.autoplex.plex.utils.PlayableMenuItem;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by omalleym on 7/15/2017.
 */

public class MusicMenuParser extends XMLParser<List<BrowsableMenuItem>>
{
	@Nullable
	protected List<BrowsableMenuItem> parse_feed() throws XmlPullParserException, IOException
	{
		List<BrowsableMenuItem> menu = new ArrayList<>();
		int event_type = parser.getEventType();
		String year = null;

		parser.require(XmlPullParser.START_TAG, ns, "MediaContainer");
		while (event_type != XmlPullParser.END_DOCUMENT) {
			String name;
			switch (event_type) {
				case XmlPullParser.START_TAG:
					name = parser.getName();

					if (name.equals("MediaContainer")) {
						year = parser.getAttributeValue(ns, "parentYear");
					} else if (name.equals("Directory")) {
						menu.add(new BrowsableMenuItem(
								parser.getAttributeValue(ns, "title"),
								parser.getAttributeValue(ns, "key"),
								parser.getAttributeValue(ns, "thumb")
						));
					} else if (name.equals("Track")) {
						PlayableMenuItem pmi = new PlayableMenuItem(
								parser.getAttributeValue(ns, "title"),
								parser.getAttributeValue(ns, "key"),
								parser.getAttributeValue(ns, "thumb"));
						pmi.setDuration(Integer.parseInt(parser.getAttributeValue(ns, "duration")));
						pmi.setAlbum(parser.getAttributeValue(ns, "parentTitle"));
						pmi.setArtist(parser.getAttributeValue(ns, "grandparentTitle"));

						nextTag("Media");
						nextTag("Part");

						pmi.setMediaUri(parser.getAttributeValue(ns, "key"));

						if (year != null) {
							pmi.setDate(year);
							year = null;
						}
						menu.add(pmi);
					}
					break;
			}
			event_type = parser.next();
		}
		return menu;
	}
}
