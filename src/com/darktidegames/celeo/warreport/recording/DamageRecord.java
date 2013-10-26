package com.darktidegames.celeo.warreport.recording;

import org.bukkit.entity.Player;

/**
 * 
 * @author Celeo
 */
public class DamageRecord
{

	private final Player from;
	private final Player to;
	private final int amount;

	/**
	 * 
	 * @param from
	 *            Player
	 * @param to
	 *            Player
	 * @param amount
	 *            int
	 */
	public DamageRecord(Player from, Player to, int amount)
	{
		this.from = from;
		this.to = to;
		this.amount = amount;
	}

	public Player getFrom()
	{
		return from;
	}

	public Player getTo()
	{
		return to;
	}

	public int getAmount()
	{
		return amount;
	}

}