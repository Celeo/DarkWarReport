package com.darktidegames.celeo.warreport.recording;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import com.darktidegames.celeo.warreport.WarReportPlugin;

/**
 * Holds the information required for keeping track of kills in realtime.
 * Extends PastFight, as the String objects representing Player objects in an
 * instance of PastFight are herein actually their predecessors.
 * 
 * @author Celeo
 */
@SuppressWarnings("boxing")
public class Fight extends PastFight
{

	public final WarReportPlugin plugin;
	public final Player target;
	public List<DamageRecord> damageRecords;
	public Long lastAttack;

	/**
	 * 
	 * @param plugin
	 *            WarReportPlugin
	 * @param target
	 *            Player
	 * @param damager
	 *            Player
	 * @param damage
	 *            int
	 */
	public Fight(WarReportPlugin plugin, Player target, Player damager, int damage)
	{
		this.plugin = plugin;
		this.target = target;
		this.start = System.currentTimeMillis();
		this.lastAttack = System.currentTimeMillis();
		this.damageRecords = new ArrayList<DamageRecord>();
		addDamage(target, damager, damage);
	}

	public void addDamage(Player hurt, Player damager, int damage)
	{
		long now = System.currentTimeMillis();
		if (lastAttack + plugin.getTimeout() < now)
		{
			plugin.closeFight(this);
			plugin.addDamage(hurt, damager, damage);
		}
		lastAttack = now;
		damageRecords.add(new DamageRecord(damager, hurt, damage));
	}

	/**
	 * End the fight. Calculate all totals, inform all players involved.
	 * 
	 * @param killed
	 *            Player
	 * @param drops
	 *            List of ItemStack
	 */
	public void processKill(Player killer, Player killed)
	{
		if (plugin.shouldIgnore(this))
		{
			plugin.removeFight(this);
			return;
		}
		plugin.getLogger().info("Processing data for fight where "
				+ killed.getName() + " died");
		// vars
		Map<Player, Integer> damageTakenFrom = new HashMap<Player, Integer>();
		int totalDamageTaken = 0;
		String attackers = parseKillers();
		finish = System.currentTimeMillis();
		// damage, total damage, and attackers
		for (DamageRecord dr : damageRecords)
		{
			if (dr.getTo().equals(killed))
			{
				if (damageTakenFrom.containsKey(dr.getFrom()))
					damageTakenFrom.put(dr.getFrom(), damageTakenFrom.get(dr.getFrom())
							+ dr.getAmount());
				else
					damageTakenFrom.put(dr.getFrom(), dr.getAmount());
				totalDamageTaken += dr.getAmount();
			}
		}
		// display
		for (Player player : getAllInvolved())
		{
			if (player == null || !player.isOnline())
				continue;
			if (player.equals(killed))
			{
				// message to show to the killed guy
				player.sendMessage("§7You died to §c" + getKillerNames()
						+ "§7!");
				player.sendMessage("§7You took a total of §6"
						+ totalDamageTaken
						+ " §7damage in the attack, split §6" + attackers);
				continue;
			}
			// message to show to everyone else doing damage in the fight
			player.sendMessage("§7===== §cKill Report §7=====");
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			player.sendMessage("§7Start: §6"
					+ sdf.format(start)
					+ " §7finish: §6"
					+ sdf.format(finish)
					+ " §7Length: §6"
					+ new DecimalFormat("#.##").format(calculateLength(start, finish))
					+ " §7minutes");
			player.sendMessage("§c" + killed.getName()
					+ " §7has died in your attack!");
			player.sendMessage("§c" + killed.getName()
					+ " §7took a total of §6" + totalDamageTaken
					+ " §7damage split §6" + attackers);
		}
		plugin.storeFight(this, killed);
		plugin.removeFight(this);
	}

	/**
	 * 
	 * @return String formatted as
	 *         killer1:damage1;killer2:damage2;killer3;damage3...
	 */
	public String parseKillers()
	{
		Map<String, Integer> retMap = new HashMap<String, Integer>();
		String from;
		int amount;
		for (DamageRecord dr : damageRecords)
		{
			from = "";
			amount = 0;
			from = dr.getFrom().getName();
			amount = dr.getAmount();
			if (retMap.containsKey(from) && retMap.get(from) != null)
				retMap.put(from, retMap.get(from) + amount);
			else
				retMap.put(from, amount);
		}
		String retString = "";
		for (String name : retMap.keySet())
		{
			if (retString.equals(""))
				retString = name + ":" + retMap.get(name);
			else
				retString += ";" + name + ":" + retMap.get(name);
		}
		return retString;
	}

	public String getKillerNames()
	{
		String ret = "";
		for (String str : Arrays.asList(parseKillers().split(";")))
		{
			if (ret.equals(""))
				ret = str.split(":")[0];
			else
				ret += ", " + str.split(":")[0];
		}
		return ret;
	}

	public List<Player> getAllInvolved()
	{
		List<Player> ret = new ArrayList<Player>();
		ret.add(target);
		for (DamageRecord dr : damageRecords)
			if (!ret.contains(dr.getFrom()))
				ret.add(dr.getFrom());
		return ret;
	}

	/**
	 * @param player
	 *            Player
	 * @return True if the player is part of the fight
	 */
	public boolean isInvolved(Player player)
	{
		for (DamageRecord dr : damageRecords)
			if (dr.getFrom().equals(player))
				return true;
		return player.equals(target);
	}

	public boolean isTarget(Player player)
	{
		return target.equals(player);
	}

	public int getDamageBy(Player player)
	{
		int ret = 0;
		for (DamageRecord dr : damageRecords)
			if (dr.getFrom().getName().equals(player.getName()))
				ret += dr.getAmount();
		return ret;
	}

}