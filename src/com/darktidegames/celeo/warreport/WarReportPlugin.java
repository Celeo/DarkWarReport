package com.darktidegames.celeo.warreport;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.java.JavaPlugin;

import com.darktidegames.celeo.warreport.recording.Fight;
import com.darktidegames.celeo.warreport.recording.Leaderboards;
import com.darktidegames.celeo.warreport.recording.Streak;
import com.darktidegames.celeo.warreport.recording.WarPlayer;

/**
 * @author Celeo
 */
public class WarReportPlugin extends JavaPlugin
{

	private File databaseFile = null;
	private Connection connection = null;
	private List<Fight> fights = null;
	long timeout = 10000L;
	private List<String> cowards = new ArrayList<String>();
	public Map<String, String> customMessages = new HashMap<String, String>();
	private List<String> invalidWords = new ArrayList<String>();
	private int maxCustomLength = 20;
	public Map<String, Integer> killStreak = new HashMap<String, Integer>();
	public List<Streak> streaks = new ArrayList<Streak>();

	public Map<DamageCause, List<String>> deathMessages = new HashMap<DamageCause, List<String>>();

	/**
	 * Ignoring kills and deaths for these names
	 */
	public List<String> ignorePlayers = new ArrayList<String>();

	/**
	 * Ignore pvp in these worlds
	 */
	public List<String> ignoreWorlds = new ArrayList<String>();

	@Override
	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(new Listeners(this), this);
		getDataFolder().mkdirs();
		if (!new File(getDataFolder(), "config.yml").exists())
			saveDefaultConfig();
		load();
		fights = new ArrayList<Fight>();
		setupDatabase();
		getCommand("report").setExecutor(this);
		getCommand("reports").setExecutor(this);
		getCommand("customkill").setExecutor(this);
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Updater(this), 12000L, 12000L);
		getLogger().info("Enabled");
	}

	@Override
	public void onDisable()
	{
		save();
		getServer().getScheduler().cancelTasks(this);
		getLogger().info("Disabled");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (!(sender instanceof Player))
			return false;
		Player player = (Player) sender;
		if (label.equalsIgnoreCase("customkill"))
		{
			if (args == null || args.length == 0)
				return doHelp(player);
			if (!hasPerms(player, "warreport.customkill"))
				return true;
			if (args[0].equalsIgnoreCase("remove"))
			{
				customMessages.remove(player.getName());
				player.sendMessage("§7Custom message removed");
			}
			else if (args[0].equalsIgnoreCase("set"))
			{
				List<String> errors = new ArrayList<String>();
				if (args.length >= 2)
				{
					String message = "";
					for (int i = 1; i < args.length; i++)
					{
						if (message.equals(""))
							message = args[i];
						else
							message += " " + args[i];
					}
					for (String s : invalidWords)
					{
						if (message.toLowerCase().contains(s.toLowerCase()))
							errors.add("§cYour message contains invalid word §6'"
									+ s + "'");
					}
					if (message.length() > maxCustomLength)
					{
						errors.add("§cYour message is too long. Needed: §6"
								+ maxCustomLength + "§c, currently: §6"
								+ message.length());
					}
					int r_names = 0;
					int r_spaces = 0;
					try
					{
						for (int i = 0; i < message.length(); i++)
						{
							if (message.charAt(i) == '%'
									&& message.charAt(i + 1) == 's')
								r_names++;
							if (message.charAt(i) == ' ')
								r_spaces++;
						}
					}
					catch (Exception e)
					{}
					if (r_names != 2)
						errors.add("§cYour message does not contain two instances of '§6%s§c'");
					if (r_spaces <= 2)
						errors.add("§cYour message does not contain at least §63 spaces");
					if (message.contains("&k"))
						errors.add("§cYour message contain contain obfuscated letters");
					if (message.contains("&l"))
						errors.add("§cYour message contain contain bold letters");
					if (message.contains("&m"))
						errors.add("§cYour message contain contain strikethrough letters");
					if (message.contains("&n"))
						errors.add("§cYour message contain contain underlined letters");
					if (message.contains("&o"))
						errors.add("§cYour message contain contain italic letters");
					if (errors.isEmpty())
					{
						player.sendMessage("§7Custom kill message set to: "
								+ message.replace("&", "§"));
						customMessages.put(player.getName(), message);
						return true;
					}
					player.sendMessage("§cCustom kill message setting failed:");
					for (String s : errors)
					{
						player.sendMessage(s);
					}
				}
				else
					doHelp(player);
				return true;
			}
			else
				doHelp(player);
			return true;
		}
		if (label.equalsIgnoreCase("report")
				|| label.equalsIgnoreCase("reports"))
		{
			if (args == null || args.length == 0)
				return doHelp(player);
			if (Leaderboards.getLastUpdated().equals("never"))
			{
				player.sendMessage("§cLeaderboards not yet updated, scheduling an update ...");
				updateNow();
				return true;
			}
			if (args[0].equalsIgnoreCase("me"))
			{
				player.sendMessage("§7=== §bPersonal Readout §7===");
				player.sendMessage("§7Kills: §6"
						+ Leaderboards.getKills(player.getName())
						+ "\n§7Deaths: §6"
						+ Leaderboards.getDeaths(player.getName())
						+ "\n§7Damage: §6"
						+ Leaderboards.getDamage(player.getName()));
				player.sendMessage("§7Your kill/death ratio: §6"
						+ new DecimalFormat("#.####").format(Leaderboards.getKdr(player.getName())));
				return true;
			}
			else if (args[0].equalsIgnoreCase("kills"))
				Leaderboards.printTopKills(player);
			else if (args[0].equalsIgnoreCase("damage"))
				Leaderboards.printTopDamage(player);
			else if (args[0].equalsIgnoreCase("kdr"))
				Leaderboards.printTopKdr(player);
			else if (args[0].equalsIgnoreCase("global"))
				Leaderboards.printGlobal(player);
			else if (args[0].equalsIgnoreCase("top"))
				player.sendMessage("§e/report kills - top kills\n§e/report damage - top damage\n§e/report kdr - top kdr ratios");
			else if (args[0].equalsIgnoreCase("-reload"))
			{
				if (!hasPerms(player, "warreport.admin"))
					return true;
				load();
				player.sendMessage("§aReloaded from config");
			}
			else if (args[0].equalsIgnoreCase("-save"))
			{
				if (!hasPerms(player, "warreport.admin"))
					return true;
				save();
				player.sendMessage("§aSaved to config");
			}
			else if (args[0].equalsIgnoreCase("-updatenow"))
			{
				if (!hasPerms(player, "warreport.admin"))
					return true;
				player.sendMessage("§aScheduling ...");
				getServer().getScheduler().scheduleSyncDelayedTask(this, new Updater(this));
				player.sendMessage("§aDone");
			}
			else if (args[0].equalsIgnoreCase("-purge"))
			{
				if (!hasPerms(player, "warreport.admin"))
					return true;
				if (args.length != 2)
				{
					player.sendMessage("§c/report -purge [name]");
					return true;
				}
				String who = args[1];
				String sql = String.format("Delete from logs where killed like '%s%s'", who, "%");
				try
				{
					runQuery(sql);
				}
				catch (SQLException e)
				{
					player.sendMessage("§cAn error occurred!");
					e.printStackTrace();
				}
			}
			else
				player.sendMessage("§cFeatures not yet implemented");
			return true;
		}
		return doHelp(player);
	}

	private void updateNow()
	{
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Updater(this));
	}

	public void togglePlayerIgnoreFor(String playerName)
	{
		if (ignorePlayers.contains(playerName))
		{
			ignorePlayers.remove(playerName);
			getLogger().info("No longer ignoring fights with " + playerName);
		}
		else
		{
			ignorePlayers.add(playerName);
			getLogger().info("Now ignoring fights with " + playerName);
		}
	}

	private static boolean hasPerms(Player player, String node)
	{
		if (!player.hasPermission(node))
		{
			player.sendMessage("§cYou cannot use that feature");
			return false;
		}
		return true;
	}

	public static boolean isInt(String string)
	{
		try
		{
			Integer.valueOf(string);
			return true;
		}
		catch (Exception e)
		{}
		return false;
	}

	private boolean doHelp(Player player)
	{
		player.sendMessage("§c/report [me|global|kills|damage]");
		player.sendMessage("§c/customkill [set|remove] (message)");
		return true;
	}

	private void load()
	{
		reloadConfig();
		timeout = getConfig().getLong("timeout", timeout);
		invalidWords = getConfig().getStringList("invalidWords");
		if (invalidWords == null)
			invalidWords = new ArrayList<String>();
		ignoreWorlds = getConfig().getStringList("ignore.worlds");
		if (ignoreWorlds == null)
			ignoreWorlds = new ArrayList<String>();
		maxCustomLength = getConfig().getInt("maxCustomLength", maxCustomLength);
		for (String key : getConfig().getStringList("customMessages"))
			customMessages.put(key.split(";")[0], key.split(";")[1]);
		for (DamageCause cause : DamageCause.values())
			if (getConfig().isSet("deathMessages." + cause.name().toLowerCase()))
				deathMessages.put(cause, getConfig().getStringList("deathMessages."
						+ cause.name().toLowerCase()));
	}

	private void save()
	{
		List<String> ret = new ArrayList<String>();
		for (String key : customMessages.keySet())
			ret.add(key + ";" + customMessages.get(key));
		getConfig().set("customMessages", ret);
		saveConfig();
	}

	/**
	 * @param hurt
	 *            Player
	 * @param damager
	 *            Player
	 * @param damage
	 *            int
	 */
	public void addDamage(Player hurt, Player damager, int damage)
	{
		for (Fight fight : fights)
			if (fight.target.getName().equals(hurt.getName()))
				if (fight.lastAttack.longValue() + timeout > System.currentTimeMillis())
				{
					fight.addDamage(hurt, damager, damage);
					return;
				}
		addFight(new Fight(this, hurt, damager, damage));
	}

	public void closeFight(Fight fight)
	{
		fights.remove(fight);
	}

	private void setupDatabase()
	{
		getDataFolder().mkdirs();
		databaseFile = new File(getDataFolder(), "/WarReport.db");
		try
		{
			if (!databaseFile.exists())
				databaseFile.createNewFile();
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:"
					+ databaseFile);
			Statement stat = connection.createStatement();
			stat.executeUpdate("Create table if not exists `logs` ('player' VARCHAR(30), 'kills' INT, 'deaths' INT, 'damage' BIGINT);");
			stat.close();
			getLogger().info("Connected to SQLite database and initiated.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void storeFight(Fight fight, Player killed)
	{
		if (ignoreWorlds.contains(killed.getLocation().getWorld().getName()))
			return;
		try
		{
			Statement stat = connection.createStatement();
			String sql = null;
			WarPlayer wp = null;
			for (Player player : fight.getAllInvolved())
			{
				wp = new WarPlayer(this, player.getName()).load();
				wp.damage += fight.getDamageBy(player);
				if (killed.getName().equals(player.getName()))
					wp.deaths++;
				else
					wp.kills++;
				sql = String.format("Update logs set kills=%d,deaths=%d,damage=%d where player='%s'", wp.kills, wp.deaths, wp.damage, player.getName());
				if (stat.executeUpdate(sql) == 0)
				{
					sql = String.format("Insert into logs values ('%s', '%s', '%s', '%s')", player.getName(), wp.kills, wp.deaths, wp.damage);
					stat.executeUpdate(sql);
				}
			}
			stat.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param sql
	 *            String
	 * @return <b>ResultSet</b>
	 * @throws SQLException
	 */
	public ResultSet runQuery(String sql) throws SQLException
	{
		if (sql.startsWith("Select"))
			return connection.createStatement().executeQuery(sql);
		Statement stat = connection.createStatement();
		stat.executeUpdate(sql);
		stat.close();
		return null;
	}

	/*
	 * GET and SET
	 */

	public long getTimeout()
	{
		return timeout;
	}

	public List<Fight> getAllFights()
	{
		return fights;
	}

	public void addFight(Fight fight)
	{
		fights.add(fight);
	}

	public boolean removeFight(Fight fight)
	{
		return fights.remove(fight);
	}

	public List<String> getCowards()
	{
		return cowards;
	}

	public int getStreak(String killer, String killed)
	{
		checkStreaks(killer);
		for (Streak s : streaks)
			if (s.getName().equals(killer))
				return s.getCount(killed);
		return 0;
	}

	public void checkStreaks(String owner)
	{
		for (Streak s : streaks)
			if (s.getName().equals(owner))
				return;
		streaks.add(new Streak(owner));
	}

	public void resetStreak(String killed)
	{
		for (Streak s : streaks)
			if (s.getName().equals(killed))
				s.gotKilled();
	}

	public int getMaxStreakAgainst(String killed)
	{
		int count = 0;
		for (Streak s : streaks)
			count += s.getCount(killed);
		return count;
	}

	/**
	 * 
	 * @param fight
	 *            Fight
	 * @return True if the fight's end should be ignored
	 */
	public boolean shouldIgnore(Fight fight)
	{
		for (Player in : fight.getAllInvolved())
			if (ignorePlayers.contains(in.getName()))
			{
				getLogger().info("Ignoring the fight with " + in.getName());
				return true;
			}
		return false;
	}

	/**
	 * 
	 * @param playerName
	 *            String
	 * @return True if the plugin is ignoring kills with that player
	 */
	public boolean isIgnoringPlayer(String playerName)
	{
		return ignorePlayers.contains(playerName);
	}

	/**
	 * 
	 * @param cause
	 *            DamageCause
	 * @return String - formatted for an EntityDeathEvent.setDeathMessage() call
	 */
	public String getDeathMessage(Player died, DamageCause cause)
	{
		if (cause.equals(DamageCause.ENTITY_ATTACK))
			return null;
		if (!deathMessages.containsKey(cause))
			return "§c" + died.getName() + " §fdied";
		if (deathMessages.get(cause) == null
				|| deathMessages.get(cause).isEmpty())
			return "§c" + died.getName() + " §fdied";
		return deathMessages.get(cause).get(new Random().nextInt(deathMessages.get(cause).size()));
	}

}