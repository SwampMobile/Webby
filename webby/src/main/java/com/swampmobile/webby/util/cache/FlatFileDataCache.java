package com.swampmobile.webby.util.cache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.swampmobile.webby.util.logging.WebbyLog;
import com.swampmobile.webby.util.time.Duration;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * A {@link com.swampmobile.webby.util.cache.DataCache} which persists text data in flat file form.  Specifically,
 * FlatFileDataCache creates text files in the "cache directory" belonging to the
 * {@link android.content.Context} which is provided at instantiation time.
 * 
 * The naming of the files should be irrelevant given that those names are opaque
 * to the functionality of this cache, but the names of the cache files are identical
 * to the ID's of the resources they correspond to.
 * 
 * @author Matt
 *
 */
public class FlatFileDataCache implements DataCache
{
	private static final String TAG = "FlatFileDataCache";
	
	private Context context;
	private File cacheDir;
	
	private Map<Uri, Set<CacheObserver>> resourceObserverMap;
	
	public FlatFileDataCache(Context context)
	{
		this.context = context;
		this.cacheDir = context.getCacheDir();
		
		resourceObserverMap = Collections.synchronizedMap(new HashMap<Uri, Set<CacheObserver>>());
	}
	
	private String convertIdToFilename(Uri id)
	{
		//return request.getUri().hashCode() + "";
		
		char fileSep = '/'; // ... or do this portably.
		char escape = '%'; // ... or some other legal char.
		String s = id.toString();
		int len = s.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
		    char ch = s.charAt(i);
		    if (ch < ' ' || ch >= 0x7F || ch == fileSep  // add other illegal chars
		        || (ch == '.' && i == 0) // we don't want to collide with "." or ".."!
		        || ch == escape) {
		        sb.append(escape);
		        if (ch < 0x10) {
		            sb.append('0');
		        }
		        sb.append(Integer.toHexString(ch));
		    } else {
		        sb.append(ch);
		    }
		}
		
		return sb.toString();
	}
	
	private File getFileFromId(Uri id)
	{
		return new File(cacheDir.getAbsolutePath() + File.separator + convertIdToFilename(id) );
	}

	private String readStreamToString(InputStream in) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String newLine;
		
		while((newLine = br.readLine()) != null)
		{
			sb.append(newLine);
		}
		
		return sb.toString();
	}
	
	@Override
	public boolean containsItem(Uri id)
	{
		File resourceFile = getFileFromId(id);
		
		if(!resourceFile.exists())
			return false;
		
		return true;
	}
	
	@Override
	public boolean isYoungerThan(Uri id, Duration age)
	{
		if(!containsItem(id))
			throw new RuntimeException("The resource you requested does not exist in the cache: " + id);
		
		File resource = getFileFromId(id);
		
		return !age.isExceeded(resource.lastModified(), System.currentTimeMillis());
	}
	
	@Override
	public String readFromCacheSync(Uri id) throws CacheReadException
	{
		WebbyLog.d(TAG, "Reading from cache with id: " + id);
		
		File resourceFile = getFileFromId(id);
		
		if(!resourceFile.exists())
			return "";
		
		try {
			String resource = readStreamToString(new FileInputStream(resourceFile));
			
			return resource;
		} catch (FileNotFoundException e) {
			return "";
		} catch (IOException e) {
			throw new CacheReadException(CacheReadException.ReadError.COULD_NOT_READ_CACHE, e.getMessage());
		}
	}

	@Override
	public void readFromCacheAsync(final Uri id, final CacheReadCallback callback)
	{
		new Thread(new Runnable() 
		{
			@Override
			public void run()
			{
				try {
					String resource = readFromCacheSync(id);
					callback.onSuccessfulRead(resource);
				} catch (CacheReadException e) {
					callback.onReadError(e);
				}
			}
		}).start();
	}

	private void writeResourceToFile(String resource, File resourceFile) throws FileNotFoundException, IOException
	{
		FileOutputStream out = new FileOutputStream(resourceFile);
		
		out.write(resource.getBytes());
		out.flush();
		out.close();
	}
	
	@Override
	public void writeToCacheSync(Uri id, String resource) throws CacheWriteException
	{
		WebbyLog.d(TAG, "Writing to cache with filename: " + id +  '\n' + resource);
		
		File resourceFile = getFileFromId(id);
		resourceFile.getParentFile().mkdirs();
		
		try {
			resourceFile.createNewFile();
			writeResourceToFile(resource, resourceFile);
		} catch (FileNotFoundException e) {
			throw new CacheWriteException(CacheWriteException.WriteError.CANNOT_ACCESS_CACHE, e.getMessage());
		} catch (IOException e) {
			throw new CacheWriteException(CacheWriteException.WriteError.ERROR_WRITING_TO_CACHE, e.getMessage());
		}
	}

	@Override
	public void writeToCacheAsync(final Uri id, final String resource, final CacheWriteCallback callback)
	{
		new Thread(new Runnable() 
		{
			@Override
			public void run()
			{
				try {
					writeToCacheSync(id, resource);
					callback.onSuccessfulWrite();
				} catch (CacheWriteException e) {
					callback.onWriteError(e);
				}
			}
		}).start();
	}

	@Override
	public void registerResourceObserver(Uri id, CacheObserver observer) {
		if(resourceObserverMap.get(id) == null)
			resourceObserverMap.put(id, Collections.synchronizedSet(new HashSet<CacheObserver>()));
		
		Set<CacheObserver> observers = resourceObserverMap.get(id);
		observers.add(observer);
	}

	@Override
	public void unregisterResourceObserver(Uri id, CacheObserver observer) {
		if(resourceObserverMap.get(id) == null)
			return;
		
		resourceObserverMap.get(id).remove(observer);
	}

	@Override
	public void unregisterResourceObserverFromAll(CacheObserver observer) {
		for(Entry<Uri, Set<CacheObserver>> entry : resourceObserverMap.entrySet())
		{
			entry.getValue().remove(observer);
		}
	}
	
	protected void notifyObserversOfChange(Uri id, String resource)
	{
		for(CacheObserver observer : resourceObserverMap.get(id))
		{
			observer.onResourceChange(id, resource);
		}
	}
	
	protected void notifyObserversOfDeletion(Uri id, String resource)
	{
		for(CacheObserver observer : resourceObserverMap.get(id))
		{
			observer.onResourceDeleted(id);
		}
	}

}
