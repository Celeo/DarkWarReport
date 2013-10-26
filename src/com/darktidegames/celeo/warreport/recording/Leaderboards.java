package com.darktidegames.celeo.warreport.recording;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

public class Leaderboards
{

	private static Leaderboards instance = null;

	private static String lastUpdated = "never";
	private static Map<String, Double> kdr = new HashMap<String, Double>();
	private static Map<String, Integer> kills = new HashMap<String, Integer>();
	private static Map<String, Integer> deaths = new HashMap<String, Integer>();
	private static Map<String, Integer> damage = new HashMap<String, Integer>();
	private static List<String> topKills = new ArrayList<String>();
	private static List<String> topDamage = new ArrayList<String>();
	private static List<String> topKdr = new ArrayList<String>();

	private static int totalKills = 0;
	private static int totalDeaths = 0;
	private static long totalDamage = 0;

	private Leaderboards()
	{
	}

	public static Leaderboards getInstance()
	{
		if (instance == null)
			instance = new Leaderboards();
		return instance;
	}

	public static int getKills(String name)
	{
		return kills.containsKey(name) ? kills.get(name).intValue() : 0;
	}

	public static int getDeaths(String name)
	{
		return deaths.containsKey(name) ? deaths.get(name).intValue() : 0;
	}

	public static int getDamage(String name)
	{
		return damage.containsKey(name) ? damage.get(name).intValue() : 0;
	}

	public static double getKdr(String name)
	{
		if (kdr.containsKey(name))
			return kdr.get(name).doubleValue();
		kdr.put(name, Double.valueOf(0));
		return 0.0;
	}

	public static String getLastUpdated()
	{
		return lastUpdated;
	}

	public static void setLastUpdated(String set)
	{
		lastUpdated = set;
	}

	private static void markLastUpdatedNow()
	{
		lastUpdated = new SimpleDateFormat("MM/dd HH:mm").format(new Date(System.currentTimeMillis()));
	}

	/**
	 * Update the Leaderboards' statistics
	 * 
	 * @param setKills
	 *            Map String, Integer
	 * @param setDeaths
	 *            Map String, Integer
	 * @param setRatios
	 *            Map String, Double
	 * @param setDamage
	 *            Map String, Integer
	 * @param fights
	 *            int
	 */
	public static void update(Map<String, Integer> setKills, Map<String, Integer> setDeaths, Map<String, Double> setRatios, Map<String, Integer> setDamage)
	{
		kills = setKills;
		deaths = setDeaths;
		kdr = setRatios;
		damage = setDamage;
		totalKills = 0;
		totalDeaths = 0;
		totalDamage = 0;
		for (int i : setKills.values())
			totalKills += i;
		for (int i : setDeaths.values())
			totalDeaths += i;
		for (int i : setDamage.values())
			totalDamage += i;
		updateTopKills();
		updateTopDamage();
		updateTopKdr();
		markLastUpdatedNow();
	}

	private static void updateTopKills()
	{
		topKills.clear();
		int currentTop = Integer.MAX_VALUE;
		for (int i = 0; i < 10; i++)
		{
			int search = getHighestAfter(new ArrayList<Integer>(kills.values()), currentTop);
			topKills.add(searchMap(kills, search) + " - " + search);
			currentTop = search;
		}
	}

	private static void updateTopDamage()
	{
		topDamage.clear();
		int currentTop = Integer.MAX_VALUE;
		for (int i = 0; i < 10; i++)
		{
			int search = getHighestAfter(new ArrayList<Integer>(damage.values()), currentTop);
			topDamage.add(searchMap(damage, search) + " - " + search);
			currentTop = search;
		}
	}

	private static void updateTopKdr()
	{
		DecimalFormat df = new DecimalFormat("#.###");
		topKdr.clear();
		double currentTop = Double.MAX_VALUE;
		for (int i = 0; i < 10; i++)
		{
			double search = getHighestAfterDouble(new ArrayList<Double>(kdr.values()), currentTop);
			topKdr.add(searchMapDouble(kdr, search) + " - " + df.format(search));
			currentTop = search;
		}
	}

	private static String searchMap(Map<String, Integer> map, int lookup)
	{
		for (String key : map.keySet())
			if (map.get(key).intValue() == lookup)
				return key;
		return null;
	}

	private static int getHighestAfter(List<Integer> list, int after)
	{
		int current = 0;
		for (Integer i : list)
			if (i.intValue() > current && i.intValue() < after)
				current = i.intValue();
		return current;
	}

	private static String searchMapDouble(Map<String, Double> map, double lookup)
	{
		for (String key : map.keySet())
			if (map.get(key).doubleValue() == lookup)
				return key;
		return null;
	}

	private static double getHighestAfterDouble(List<Double> list, double after)
	{
		double current = 0;
		for (Double i : list)
			if (i.doubleValue() > current && i.doubleValue() < after)
				current = i.doubleValue();
		return current;
	}

	/**
	 * TODO: topKills is empty
	 * 
	 * @param player
	 *            Player
	 */
	public static void printTopKills(Player player)
	{
		player.sendMessage("§7=== §4Kill Leaderboards §7===");
		int count = 1;
		for (String name : topKills)
		{
			player.sendMessage("§c" + count + ". §6" + name);
			count++;
		}
	}

	public static void printTopDamage(Player player)
	{
		player.sendMessage("§7=== §cDamage Leaderboards §7===");
		int count = 1;
		for (String name : topDamage)
		{
			player.sendMessage("§c" + count + ". §6" + name);
			count++;
		}
	}

	public static void printTopKdr(Player player)
	{
		player.sendMessage("§7=== §dKdr Leaderboards §7===");
		int count = 1;
		for (String name : topKdr)
		{
			player.sendMessage("§c" + count + ". §6" + name);
			count++;
		}
	}

	public static void printGlobal(Player player)
	{
		player.sendMessage("§7=== §2Global information §7===");
		player.sendMessage("§7Total number of kills: §6" + totalKills);
		player.sendMessage("§7Total number of deaths: §6" + totalDeaths);
		player.sendMessage("§7Total amount of damage dealt: §6" + totalDamage);
	}

}