package com.spuriouslabs.apps.autoplex.plex.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
/**
 * Created by omalleym on 8/3/17.
 */

public class MetadataCacheDB extends SQLiteOpenHelper
{
	public static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "autoplex_cache.db";

	public MetadataCacheDB(Context ctx)
	{
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL(MetadataCacheDBContract.AlbumEntry.CREATE);
		db.execSQL(MetadataCacheDBContract.ArtistEntry.CREATE);
		db.execSQL(MetadataCacheDBContract.TrackEntry.CREATE);
	}

	public void onUpgrade(SQLiteDatabase db, int old_version, int new_version)
	{
		// This database is only a cache for online data, so its upgrade policy is
		// to simply to discard the data and start over
		db.execSQL(MetadataCacheDBContract.AlbumEntry.DROP);
		db.execSQL(MetadataCacheDBContract.ArtistEntry.DROP);
		db.execSQL(MetadataCacheDBContract.TrackEntry.DROP);
		onCreate(db);
	}

	public void onDowngrade(SQLiteDatabase db, int old_version, int new_version)
	{
		onUpgrade(db, old_version, new_version);
	}
}
