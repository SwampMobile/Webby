package com.swampmobile.webby.util.time;

public enum Duration 
{
    IMMEDIATELY(0),
	ONE_MINUTE(60 * 1000),
	ONE_HOUR(60 * 60 * 1000),
	ONE_DAY(24 * 60 * 60 * 1000),
	ONE_WEEK(7 * 24 * 60 * 60 * 1000),
	ETERNITY(-1);
	
	private long span;
	
	private Duration(long span)
	{
		this.span = span;
	}

    public long getValue() { return span; }
	
	/**
	 * Is this duration exceeded by the difference between time1 and time2 (order is irrelevant).
	 * 
	 * @param time1 is either a start or end timestamp for a range of time
	 * @param time2 is the timestamp for the other end of the range of time referenced by time1
	 * @return
	 */
	public boolean isExceeded(long time1, long time2)
	{
		if(span < 0)
			return false; // <- eternity
		else
			return Math.abs(time2 - time1) > span;
	}
}
