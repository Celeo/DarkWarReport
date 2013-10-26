package com.darktidegames.celeo.warreport.recording;

import java.util.List;

/**
 * Fight record-keeping object for fights that have already occurred. References
 * to Player objects are replaced with references to String objects,
 * representing their Player object values.
 * 
 * @author Celeo
 */
public class PastFight
{

	public String target;
	public Long start;
	public Long finish;
	public List<String> killers;
	public List<String> drops;

	/**
	 * Calculates the minutes ellapsed between the start time and finish time
	 * 
	 * @param start
	 *            long
	 * @param finish
	 *            long
	 * @return double - minutes ellapsed
	 */
	public static double calculateLength(long start, long finish)
	{
		double ret = (finish - start);
		if (ret < 0)
			throw new IllegalArgumentException("The finish variable is less than the start variable");
		// milliseconds
		ret /= 1000;
		// seconds
		ret /= 60;
		// minutes
		return ret;
	}

	public PastFight setStart(Long start)
	{
		this.start = start;
		return this;
	}

	public PastFight setFinish(Long finish)
	{
		this.finish = finish;
		return this;
	}

}