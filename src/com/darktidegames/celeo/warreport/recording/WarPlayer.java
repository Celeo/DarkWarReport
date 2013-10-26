package com.darktidegames.celeo.warreport.recording;

import java.sql.ResultSet;

import com.darktidegames.celeo.warreport.WarReportPlugin;

public class WarPlayer
{

	private final WarReportPlugin plugin;
	public final String name;
	public int kills;
	public int deaths;
	public int damage;

	public WarPlayer(WarReportPlugin plugin, String name)
	{
		this.plugin = plugin;
		this.name = name;
	}

	public WarPlayer load()
	{
		try
		{
			ResultSet rs = plugin.runQuery(String.format("Select * from logs where player='%s'", name));
			while (rs.next())
			{
				kills = rs.getInt("kills");
				deaths = rs.getInt("deaths");
				damage = rs.getInt("damage");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return this;
	}

}