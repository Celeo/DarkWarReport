package com.darktidegames.celeo.warreport;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.darktidegames.celeo.warreport.recording.Leaderboards;

public class Updater implements Runnable
{

	private final WarReportPlugin plugin;

	public Updater(WarReportPlugin plugin)
	{
		this.plugin = plugin;
	}

	@Override
	public void run()
	{
		Map<String, Integer> kills = new HashMap<String, Integer>();
		Map<String, Integer> deaths = new HashMap<String, Integer>();
		Map<String, Double> kdr = new HashMap<String, Double>();
		Map<String, Integer> damage = new HashMap<String, Integer>();
		try
		{
			ResultSet rs = plugin.runQuery("Select * from logs");
			while (rs.next())
			{
				kills.put(rs.getString("player"), rs.getInt("kills"));
				deaths.put(rs.getString("player"), rs.getInt("deaths"));
				damage.put(rs.getString("player"), rs.getInt("damage"));
			}
			for (String k : kills.keySet())
				for (String d : deaths.keySet())
					if (k.equals(d))
						kdr.put(k, Double.valueOf((double) kills.get(k).intValue()
								/ (double) deaths.get(d).intValue()));
			Leaderboards.update(kills, deaths, kdr, damage);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}