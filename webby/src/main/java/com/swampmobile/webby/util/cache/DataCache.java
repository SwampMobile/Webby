package com.swampmobile.webby.util.cache;

import android.net.Uri;

import java.io.IOException;

import com.swampmobile.webby.util.time.Duration;

/**
 * Represents a persistent cache of String resources.  This interface imposes no restriction
 * on how those string values are persisted.  The only implicit restriction imposed by this
 * cache is that text content be able to fit in a single String.  There is no support in
 * this interface for stream-based reading/writing.
 * 
 * @author Matt
 *
 */
public interface DataCache 
{
	boolean containsItem(Uri id);
	boolean isYoungerThan(Uri id, Duration age);
	
	String readFromCacheSync(Uri id) throws CacheReadException;
	void readFromCacheAsync(Uri id, CacheReadCallback callback) throws CacheReadException;
	
	void writeToCacheSync(Uri id, String resource) throws CacheWriteException;
	void writeToCacheAsync(Uri id, String resource, CacheWriteCallback callback) throws CacheWriteException;
	
	void registerResourceObserver(Uri id, CacheObserver observer);
	void unregisterResourceObserver(Uri id, CacheObserver observer);
	void unregisterResourceObserverFromAll(CacheObserver observer);
	
	public interface CacheReadCallback
	{
		void onSuccessfulRead(String resource);
		void onReadError(CacheReadException error);
	}
	
	public interface CacheWriteCallback
	{
		void onSuccessfulWrite();
		void onWriteError(CacheWriteException error);
	}
	
	public interface CacheObserver
	{
		void onResourceChange(Uri id, String value);
		void onResourceDeleted(Uri id);
	}
	
	public class CacheReadException extends IOException
	{
		public enum ReadError
		{
			NO_SUCH_CACHE_RESOURCE, // ex: cache file does not exist
			CANNOT_ACCESS_CACHE, // ex: a text file is locked
			COULD_NOT_READ_CACHE // ex: content exists, access available, but ran into error reading content in
		}
		
		private ReadError problem;
		
		public CacheReadException(ReadError problem, String message)
		{
			super(problem.toString() + ": " + message);
			
			this.problem = problem;
		}
		
		public ReadError getProblem() { return problem; }
	}
	
	public class CacheWriteException extends IOException
	{
		public enum WriteError
		{
			CANNOT_ACCESS_CACHE,
			ERROR_WRITING_TO_CACHE
		}
		
		private WriteError problem;
		
		public CacheWriteException(WriteError problem, String message)
		{
			super(problem.toString() + ": " + message);
			
			this.problem = problem;
		}
		
		public WriteError getProblem() { return problem; }
	}
}
