package com.spuriouslabs.apps.autoplex.plex.utils;

/**
 * Created by omalleym on 7/25/17.
 */

public class MusicLibrary {
	private String name;
	private int id;

	public MusicLibrary(String name, int id)
	{
		this.name = name;
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public int getId()
	{
		return id;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setId(int id)
	{
		this.id = id;
	}
}
