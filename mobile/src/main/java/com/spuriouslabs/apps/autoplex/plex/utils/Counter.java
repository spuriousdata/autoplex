package com.spuriouslabs.apps.autoplex.plex.utils;

/**
 * Created by omalleym on 7/28/17.
 */

public class Counter
{
	private volatile int count = 0;

	public synchronized int getCount()
	{
		return count;
	}

	public synchronized void up()
	{
		count += 1;
	}

	public synchronized void down()
	{
		count -= 1;

		if (count < 0) // something broke?
			count = 0;
	}
}
