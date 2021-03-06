//
//This sample program is provided AS IS and may be used, executed, copied and
//modified without royalty payment by customer (a) for its own instruction and 
//study, (b) in order to develop applications designed to run with an IBM 
//WebSphere product, either for customer's own internal use or for redistribution 
//by customer, as part of such an application, in customer's own products. "
//
//5724-J34 (C) COPYRIGHT International Business Machines Corp. 2005
//All Rights Reserved * Licensed Materials - Property of IBM
//
package com.devwebsphere.wxssearch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.devwebsphere.wxssearch.jmx.TextIndexMBeanImpl;
import com.devwebsphere.wxsutils.WXSMap;
import com.devwebsphere.wxsutils.WXSUtils;
import com.ibm.websphere.objectgrid.BackingMap;
import com.ibm.websphere.objectgrid.ObjectGrid;
import com.ibm.websphere.objectgrid.ObjectGridException;
import com.ibm.websphere.objectgrid.ObjectGridRuntimeException;
import com.ibm.websphere.objectgrid.ObjectMap;
import com.ibm.websphere.objectgrid.Session;

/**
 * Each index uses 4 dynamic maps named after the index.
 *
 * @param <RK> The actual key of the entities being indexed.
 */
public abstract class Index<C, RK> 
{

	static String DYN_INDEX_MAP_SUFFIX = "Index";
	static String DYN_BAD_SYMBOL_MAP_SUFFIX = "BadSymbol";
	
	static Logger logger = Logger.getLogger(Index.class.getName());
	IndexManager<C, RK> manager;
	
	String indexName;
	String attributeIndexMapName;
	String indexMapName;
	String badSymbolMapName;
	
	int maxMatches;
	
	TextIndexMBeanImpl mbean;

	/**
	 * Creates an index wrapper which only tracks entries with
	 * less than maxMatches duplicates.
	 * @param im
	 * @param indexName
	 * @param maxMatches
	 */
	Index(IndexManager<C,RK> im, String indexName, int maxMatches)
	{
		this.maxMatches = maxMatches;
		this.manager = im;
		this.indexName = indexName;
        attributeIndexMapName = indexName + "_" + DYN_INDEX_MAP_SUFFIX;
		indexMapName = indexName + "_" + DYN_INDEX_MAP_SUFFIX;
		badSymbolMapName = indexName + "_" + DYN_BAD_SYMBOL_MAP_SUFFIX;
		
		mbean = WXSUtils.getIndexMBeanManager().getBean(im.utils.getObjectGrid().getName(), indexName);
		
		try
		{
			// make sure the maps based on the index name are dynamically created
			// a Dynamic Map won't exist unless a client calls Session#getMap using
			// the dynamic map name.
			Session s = manager.utils.getObjectGrid().getSession();
			ObjectMap m = s.getMap(manager.getInternalKeyToRealKeyMapName());
			m = s.getMap(attributeIndexMapName);
			m = s.getMap(indexMapName);
			m = s.getMap(badSymbolMapName);
		}
		catch(Exception e)
		{
			logger.log(Level.SEVERE, "Cannot create dynamic maps for index", e);
			throw new ObjectGridRuntimeException(e);
		}
	}
	
	/**
	 * This adds index entries. It takes the key for the actual record and the
	 * value of the attribute for the record. The index can later be used to retrieve
	 * the keys for all entries with an attribute containing a symbol
	 * @param entries
	 */
    public void insert(Map<RK, String> entries)
    {
    	long start = System.nanoTime();
    	try
    	{
	    	ObjectGrid grid = manager.utils.getObjectGrid();
	
	        HashMap<ByteArrayKey, RK> ik2rk = new HashMap<ByteArrayKey, RK>();
	        HashMap<String, Set<byte[]>> symbolToHashMap = new HashMap<String, Set<byte[]>>();

	        for(Map.Entry<RK, String> e : entries.entrySet())
	        {
	        	byte[] hash = calculateHash(e.getKey());
	        	// need to wrap a byte[] to work with WXS
	        	ByteArrayKey hashKey = new ByteArrayKey(hash);
	            ik2rk.put(hashKey, e.getKey());
	            Set<String> symbols = generate(e.getValue());
	
	            for (String a : symbols)
	            {
	                Set<byte[]> list = symbolToHashMap.get(a);
	                if (list == null)
	                {
	                    list = new HashSet<byte[]>();
		                symbolToHashMap.put(a, list);
	                }
	                list.add(hash);
	            }
	        }
	        BackingMap bmap = grid.getMap(manager.getInternalKeyToRealKeyMapName());
	        // store hash, real key pairs in the grid
	        manager.utils.putAll(ik2rk, bmap);
	
	        Set<String> allSymbols = symbolToHashMap.keySet();
	        Map<String, IndexEntryUpdateAgent> batchAgents = new HashMap<String, IndexEntryUpdateAgent>();
	        for (String symbol : allSymbols)
	        {
	            Set<byte[]> keys = symbolToHashMap.get(symbol);
	            IndexEntryUpdateAgent agent = new IndexEntryUpdateAgent();
	            agent.maxMatches = maxMatches;
	            agent.internalKeyList = new ArrayList<byte[]>(keys.size());
	            agent.internalKeyList.addAll(keys);
	            agent.gridName = manager.utils.getObjectGrid().getName();
	            agent.isAddOperation = true;
	            agent.indexName = indexName;
	            batchAgents.put(symbol, agent);
	        }
	        manager.utils.callMapAgentAll(batchAgents, grid.getMap(attributeIndexMapName));
	        mbean.getInsertMetrics().logTime(System.nanoTime() - start);
    	}
    	catch(Exception e)
    	{
    		mbean.getInsertMetrics().logException(e);
    		logger.log(Level.SEVERE, "Exception inserting index entries", e);
    		throw new ObjectGridRuntimeException(e);
    	}
    }
    
    /**
     * This retrieves the list of keys for all entries with an attribute
     * containing the symbol anywhere.
     * @param symbol
     * @return
     */
    public SearchResult<RK> contains(String symbol)
	{
    	long start = System.nanoTime();
    	try
    	{
    		SearchResult<RK> rc = null;
			SearchResult<byte[]> rawSR = rawContains(symbol);
			if(!rawSR.tooManyMatches)
			{
				List<byte[]> keys = rawSR.getResults();
				
				if (keys != null)
				{
					rc = new SearchResult<RK>(fetchKeysForInternalByteKeys(keys));
				}
			}
			
			mbean.getContainsMetrics().logTime(System.nanoTime() - start);
			return rc;
    	}
    	catch(Exception e)
    	{
    		mbean.getContainsMetrics().logException(e);
    		logger.log(Level.SEVERE, "Exception looking up substrings", e);
    		throw new ObjectGridRuntimeException(e);
    	}
	}

    public List<RK> fetchKeysForInternalKeys(List<ByteArrayKey> keys)
    {
		ObjectGrid grid = manager.utils.getObjectGrid();
		
		Map<ByteArrayKey, RK> all = manager.utils.getAll(keys, grid.getMap(manager.getInternalKeyToRealKeyMapName()));
		List<RK> rc = new ArrayList<RK>(all.values());
		return rc;
    }
    
    public List<RK> fetchKeysForInternalByteKeys(List<byte[]> keys)
    {
    	List<ByteArrayKey> k = IndexManager.convertToKeys(keys);
    	return fetchKeysForInternalKeys(k);
    }
    
    public SearchResult<byte[]> rawContains(String symbol)
	{
    	try
    	{
			ObjectGrid grid = manager.utils.getObjectGrid();
			Session sess = grid.getSession();
			ObjectMap indexMap = sess.getMap(indexMapName);
			ObjectMap badSymbolMap = sess.getMap(badSymbolMapName);

			SearchResult<byte[]> rc = null;
			if (!badSymbolMap.containsKey(symbol))
			{
				List<byte[]> rl = (List<byte[]>) indexMap.get(symbol);
				rc = new SearchResult<byte[]>(rl);
			}
			else
				rc = new SearchResult<byte[]>();
			return rc;
    	}
    	catch(ObjectGridException e)
    	{
    		logger.log(Level.SEVERE, "Exception looking up substrings", e);
    		throw new ObjectGridRuntimeException(e);
    	}
	}
    
    /**
     * This removes the index entry for the record with the key key and the attribute value
     * attributeValue
     * @param key
     * @param attributeValue
     */
    public void remove(RK key, String attributeValue)
    {
    	long start = System.nanoTime();
    	try
    	{
	    	WXSUtils utils = manager.utils;
	    	WXSMap<String, RK> badSymbolMap = utils.getCache(indexMapName);
	    	ObjectGrid grid = utils.getObjectGrid();
	    	Session sess = grid.getSession();
	    	byte[] hash = calculateHash(key);
	    	ByteArrayKey hashKey = new ByteArrayKey(hash);
	    	Set<String> symbols = generate(attributeValue);
	    	Map<String, IndexEntryUpdateAgent> agentList = new HashMap<String, IndexEntryUpdateAgent>();
	    	ObjectMap names = sess.getMap(manager.getInternalKeyToRealKeyMapName());
	    	// remove the int key -> real key record
	    	names.remove(hashKey);
	    	// for each possible index key
	    	for(String s : symbols)
	    	{
	    		// if key was useful
		    	if (!badSymbolMap.contains(s)) // RPC
		    	{
		    		// remove this records hash from the list of matches
		    		// the agent avoids pulling the whole list to here and
		    		// then writing it back
		    		IndexEntryUpdateAgent agent = new IndexEntryUpdateAgent();
		    		agent.maxMatches = maxMatches;
		    		agent.internalKeyList = Collections.singletonList(hash);
		    		agent.isAddOperation = false;
		    		agent.indexName = indexName;
		    		agent.gridName = grid.getName();
		    		agentList.put(s, agent);
		    	}
	    	}
	    	BackingMap bmap = grid.getMap(indexMapName);
	    	// invoke all agents as efficiently as we can
	    	// this minimizes RPCs
	    	utils.callMapAgentAll(agentList, bmap);
	    	mbean.getRemoveMetrics().logTime(System.nanoTime() - start);
    	}
    	catch(ObjectGridException e)
    	{
    		mbean.getRemoveMetrics().logException(e);
    		logger.log(Level.SEVERE, "Exception removing entries", e);
    		throw new ObjectGridRuntimeException(e);
    	}
    }

    abstract Set<String> generate(String str);
    
    /**
     * Optimized get hash for object. It uses a message digest on the byte[] for the
     * object. Worst case, the RK o is serialized to a byte[]
     * @param o
     * @return
     */
    byte[] calculateHash(RK o)
    {
    	try
    	{
	    	MessageDigest digest = IndexManager.getDigest();
	    	if(o instanceof String)
	    	{
	    		String s = (String)o;
	    		return digest.digest(s.getBytes());
	    	}
	    	else if(o instanceof Serializable)
	    	{
	    		ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    		ObjectOutputStream oos = new ObjectOutputStream(bos);
	    		oos.writeObject(o);
	    		oos.flush();
	    		oos.close();
	    		byte[] b = bos.toByteArray();
	    		return digest.digest(b);
	    	} else
	    		return null;
    	}
    	catch(IOException e)
    	{
    		throw new ObjectGridRuntimeException(e);
    	}
    }
}
