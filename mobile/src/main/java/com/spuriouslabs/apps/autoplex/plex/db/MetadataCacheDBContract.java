package com.spuriouslabs.apps.autoplex.plex.db;

import android.provider.BaseColumns;

/**
 * Created by omalleym on 8/3/17.
 */

public class MetadataCacheDBContract
{
	private MetadataCacheDBContract() {}

	public static class ArtistEntry implements BaseColumns
	{
		public static final String TABLE = "artists";
		public static final String COLUMN_KEY = "key";
		public static final String COLUMN_NAME = "name";
		public static final String COLUMN_ALBUM_COUNT = "album_count";

		public static final String CREATE = "CREATE TABLE " + TABLE + " (\n" +
				COLUMN_KEY + " text PRIMARY KEY,\n" +
				COLUMN_NAME + " text,\n" +
				COLUMN_ALBUM_COUNT + " integer\n" +
				")";

		public static final String DROP = "DROP TABLE IF EXISTS" + TABLE;
	}

	public static class AlbumEntry implements BaseColumns
	{
		public static final String TABLE = "albums";
		public static final String COLUMN_ARTIST = "artist_id";
		public static final String COLUMN_KEY = "key";
		public static final String COLUMN_NAME = "name";

		public static final String CREATE = "CREATE TABLE " + TABLE + " (\n" +
				COLUMN_KEY + " text PRIMARY KEY,\n" +
				COLUMN_NAME + " text,\n" +
				COLUMN_ARTIST + " text\n" +
				")";

		public static final String DROP = "DROP TABLE IF EXISTS" + TABLE;
	}

	public static class TrackEntry implements BaseColumns
	{
		public static final String TABLE = "tracks";
		public static final String COLUMN_ARTIST = "artist_id";
		public static final String COLUMN_ALBUM = "album_id";
		public static final String COLUMN_KEY = "key";
		public static final String COLUMN_NAME = "name";
		public static final String COLUMN_DURATION = "duration";

		public static final String CREATE = "CREATE TABLE " + TABLE + " (\n" +
				COLUMN_KEY + " text PRIMARY KEY,\n" +
				COLUMN_NAME + " text,\n" +
				COLUMN_ARTIST + " text,\n" +
				COLUMN_ALBUM + " text,\n" +
				COLUMN_DURATION + " integer\n" +
				")";

		public static final String DROP = "DROP TABLE IF EXISTS" + TABLE;
	}
}
