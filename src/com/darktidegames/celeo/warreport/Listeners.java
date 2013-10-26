package com.darktidegames.celeo.warreport;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.darktidegames.celeo.warreport.recording.Fight;
import com.darktidegames.celeo.warreport.recording.Streak;

/**
 * Holds all of the listeners for this plugin
 * 
 * @author Celeo
 */
public class Listeners implements Listener
{

	private final WarReportPlugin plugin;

	/**
	 * Constructor
	 * 
	 * @param plugin
	 *            KillApiPlugin
	 */
	public Listeners(WarReportPlugin plugin)
	{
		this.plugin = plugin;
	}

	/**
	 * Something took damage - let's see if it's a player!
	 * 
	 * @param event
	 *            EntityDamageEvent
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerDamagePlayer(EntityDamageEvent event)
	{
		if (event.isCancelled())
			return;
		if (event.getDamage() == 0)
			return;
		if (!(event.getEntity() instanceof Player))
			return;
		if (!(event instanceof EntityDamageByEntityEvent))
			return;
		EntityDamageByEntityEvent eve = (EntityDamageByEntityEvent) event;
		if (eve.getDamager() instanceof Player)
		{
			Player damager = (Player) eve.getDamager();
			Player hurt = (Player) event.getEntity();
			int damage = event.getDamage();
			plugin.addDamage(hurt, damager, damage);
		}
		else if (eve.getDamager() instanceof Arrow
				&& ((Arrow) eve.getDamager()).getShooter() instanceof Player)
		{
			Player damager = (Player) ((Arrow) eve.getDamager()).getShooter();
			Player hurt = (Player) event.getEntity();
			int damage = event.getDamage();
			plugin.addDamage(hurt, damager, damage);
		}
	}

	/**
	 * Something died - let's see if it's a player!
	 * 
	 * @param event
	 *            EntityDeathEvent
	 */
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event)
	{
		if (!(event.getEntity() instanceof Player))
			return;
		Player killed = (Player) event.getEntity();
		if (killed.getLastDamageCause() instanceof EntityDamageByEntityEvent)
		{
			EntityDamageByEntityEvent eve = (EntityDamageByEntityEvent) killed.getLastDamageCause();
			Entity killer = eve.getEntity();
			if (killer instanceof Player)
			{
				System.out.println("Player killed " + killed.getName());
				for (Fight fight : plugin.getAllFights())
					if (fight.isTarget(killed))
					{
						fight.processKill((Player) killer, killed);
						break;
					}
				for (Streak s : plugin.streaks)
					if (s.getName().equals(((Player) killer).getName()))
						s.logKill(killed.getName());
			}
			else if (killer instanceof Arrow)
			{
				System.out.println("Arrow killed " + killed.getName());
				for (Fight fight : plugin.getAllFights())
					if (fight.isTarget(killed))
					{
						fight.processKill((Player) ((Arrow) killer).getShooter(), killed);
						break;
					}
				for (Streak s : plugin.streaks)
					if (s.getName().equals(((Player) ((Arrow) killer).getShooter()).getName()))
						s.logKill(killed.getName());
			}
			else
			{
				System.out.println("Other killed " + killed.getName());
			}
			if (new Random().nextInt(101) > 20)
				return;
			ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1);
			skull.setDurability((short) 3);
			SkullMeta meta = (SkullMeta) skull.getItemMeta();
			meta.setOwner(killed.getName());
			skull.setItemMeta(meta);
			event.getDrops().add(skull);
		}
		else
		{
			List<Fight> close = new ArrayList<Fight>();
			for (Fight fight : plugin.getAllFights())
				if (fight.isTarget(killed))
					close.add(fight);
			for (Fight fight : close)
				plugin.closeFight(fight);
			return;
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event)
	{
		event.setDroppedExp(0);
		String killed = event.getEntity().getName();
		if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
		{
			EntityDamageByEntityEvent eve = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
			if (eve.getDamager() instanceof Player)
			{
				event.setDeathMessage(null);
				String killer = ((Player) eve.getDamager()).getName();
				if (plugin.ignorePlayers.contains(killer)
						|| plugin.ignorePlayers.contains(killed))
					return;
				for (Player online : plugin.getServer().getOnlinePlayers())
				{
					if (!plugin.customMessages.containsKey(killer))
						online.sendMessage(String.format("§6%s §cdefeated §6%s §cin a fight!", killer, killed));
					else
						online.sendMessage(String.format(plugin.customMessages.get(killer), killer, killed).replace("&", "§"));
					if (plugin.getStreak(killer, killed) != 0)
					{
						int count = plugin.getStreak(killer, killed);
						if (count == 1)
							online.sendMessage("§6Double kill!");
						else if (count == 2)
							online.sendMessage("§6Triple kill!");
						else if (count == 3)
							online.sendMessage("§6§oQuadruple kill!");
						else if (count >= 10)
							online.sendMessage("§6§o§l" + killer
									+ " is Legendary!");
						else if (count > 100)
							online.sendMessage("§6§o§l"
									+ killer
									+ " is either Legendary, or this plugin is broken!");
						else
							online.sendMessage("§6§o§nKill streak: " + count);
					}
					if (plugin.getMaxStreakAgainst(killer) >= 2)
						online.sendMessage("§6§oC-C-Combo breaker!");
				}
			}
		}
		if (event.getEntity().getLastDamageCause() == null
				|| event.getEntity().getLastDamageCause().getCause() == null)
		{
			event.setDeathMessage("§c" + event.getEntity().getName()
					+ " §fdied");
			plugin.resetStreak(killed);
			return;
		}
		if (plugin.getDeathMessage(event.getEntity(), event.getEntity().getLastDamageCause().getCause()) != null)
			event.setDeathMessage(String.format(plugin.getDeathMessage(event.getEntity(), event.getEntity().getLastDamageCause().getCause()), event.getEntity().getName()).replace("&", "§"));
		plugin.resetStreak(killed);
	}

	/**
	 * A player logged off the server - where they in a fight?
	 * 
	 * @param event
	 *            PlayerQuitEvent
	 */
	@EventHandler
	public void onPlayerInFightFlee(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		for (Fight fight : plugin.getAllFights())
		{
			if (fight.isTarget(player))
			{
				if (fight.lastAttack.longValue() + plugin.timeout > System.currentTimeMillis())
				{
					plugin.getCowards().add(player.getName());

					ItemStack[] items = player.getInventory().getContents();
					ItemStack[] armor = player.getInventory().getArmorContents();
					Location dropLoc = player.getLocation();

					player.getInventory().clear();
					player.getInventory().setBoots(null);
					player.getInventory().setLeggings(null);
					player.getInventory().setChestplate(null);
					player.getInventory().setHelmet(null);

					for (ItemStack i : items)
						if (i != null && i.getType() != Material.AIR)
							player.getWorld().dropItemNaturally(dropLoc, i);
					for (ItemStack i : armor)
						if (i != null && i.getType() != Material.AIR)
							player.getWorld().dropItemNaturally(dropLoc, i);

					plugin.getServer().broadcastMessage("§c" + player.getName()
							+ "§6 fled in combat");
					return;
				}
				plugin.getLogger().info("Timed out");
			}
		}
	}

	/**
	 * If we've already determined that a player is a coward for fleeing in a
	 * fight, let's carry out their sentance
	 * 
	 * @param event
	 *            PlayerJoinEvent
	 */
	@EventHandler
	public void onPlayerInFightReturn(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		if (plugin.getCowards().remove(player.getName()))
		{
			player.getInventory().clear();
			player.setHealth(0);
			player.sendMessage("§cYou fled in combat");
		}
	}

}