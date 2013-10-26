package com.darktidegames.celeo.warreport.recording;

import java.util.HashMap;
import java.util.Map;

public class Streak
{

	private final String name;
	private Map<String, Integer> kills = new HashMap<String, Integer>();

	public Streak(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public int getCount(String killed)
	{
		return kills.containsKey(killed) ? kills.get(killed).intValue() : 0;
	}

	public void logKill(String killed)
	{
		if (kills.containsKey(killed))
			kills.put(killed, Integer.valueOf(kills.get(killed).intValue() + 1));
		else
			kills.put(killed, Integer.valueOf(1));
	}

	public void gotKilled()
	{
		kills = new HashMap<String, Integer>();
	}

}