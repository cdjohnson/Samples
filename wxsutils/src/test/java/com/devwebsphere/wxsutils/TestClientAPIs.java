//
//This sample program is provided AS IS and may be used, executed, copied and
//modified without royalty payment by customer (a) for its own instruction and
//study, (b) in order to develop applications designed to run with an IBM
//WebSphere product, either for customer's own internal use or for redistribution
//by customer, as part of such an application, in customer's own products. "
//
//5724-J34 (C) COPYRIGHT International Business Machines Corp. 2009
//All Rights Reserved * Licensed Materials - Property of IBM
//
package com.devwebsphere.wxsutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.objectgrid.BackingMap;
import com.ibm.websphere.objectgrid.ObjectGrid;
import com.ibm.websphere.objectgrid.ObjectGridException;
import com.ibm.websphere.objectgrid.ObjectGridRuntimeException;

/**
 * This test connects to a grid running on the same box. Use the gettingstarted example
 * with the xml files in this folder. These xmls just add a third Map which doesn't
 * use client side caching.
 *
 */
public class TestClientAPIs 
{
	static ObjectGrid ogclient;
	static WXSUtils utils;
	static BackingMap bmFarMap3;
	
	@BeforeClass
	public static void setupTest()
	{
		// do everything in one JVM for test
		ogclient = WXSUtils.startTestServer("Grid", "/objectgrid.xml", "/deployment.xml");
		// switch to this to connect to remote grid instead.
//		ogclient = WXSUtils.connectClient("localhost:2809", "Grid", "/objectgrid.xml");
		utils = new WXSUtils(ogclient);
		bmFarMap3 = ogclient.getMap("FarMap3");
	}

	/**
	 * This clears the FarMap3 in preparation for any tests
	 */
	public static void clearMap()
	{
		try
		{
			ogclient.getSession().getMap("FarMap3").clear();
		}
		catch(ObjectGridException e)
		{
			Assert.fail("Exception during clear");
		}
	}
	
	@Test
	public void testPut()
	{
		clearMap();
		WXSMap<String, String> map = utils.getCache(bmFarMap3.getName());
		String value = "B";
		map.put("A", value);
		Assert.assertTrue(map.contains("A"));
		String v = map.get("A");
		Assert.assertEquals(value, v);
	}

	/**
	 * This tests the basic putAll/getAll/removeAll capabilities
	 */
	@Test
	public void testPutAll()
	{
		clearMap();
		for(int k = 0; k < 10; ++k)
		{
			int base = k * 1000;
			Map<String, String> batch = new HashMap<String, String>();
			for(int i = base; i < base + 1000; ++i)
			{
				batch.put("" + i, "V" + i);
			}
			utils.putAll(batch, bmFarMap3);
		}
		
		for(int k = 0; k < 10; ++k)
		{
			int base = k * 1000;
			ArrayList<String> keys = new ArrayList<String>();
			for(int i = base; i < base + 1000; ++i)
			{
				keys.add("" + i);
			}
			Map<String, String> rc = utils.getAll(keys, bmFarMap3);
			
			for(Map.Entry<String, String> e : rc.entrySet())
			{
				Assert.assertEquals("V" + e.getKey(), e.getValue());
			}

			utils.removeAll(keys, bmFarMap3);
			rc = utils.getAll(keys, bmFarMap3);
			
			for(Map.Entry<String, String> e : rc.entrySet())
			{
				Assert.assertNull(e.getValue());
			}
		}
	}
	
	@Test
	public void testCond_PutAll()
	{
		clearMap();
		Map<String, String> original = new HashMap<String, String>();
		for(int i = 0; i < 10; ++i)
		{
			original.put("" + i, "V" + i);
		}
		utils.putAll(original, bmFarMap3);
		
		Map<String, String> newValues = new HashMap<String, String>();
		for(int i = 0; i < 11; ++i)
		{
			newValues.put("" + i, "N" + i);
		}
		WXSMap<String, String> map = utils.getCache(bmFarMap3.getName());
		map.put("4", "DIFFERENT");

		// try with maps different size, orig = 10, new = 11
		try
		{
			Map<String, Boolean> rc = utils.cond_putAll(original, newValues, bmFarMap3);
			Assert.fail("Should have thrown exception");
		}
		catch(ObjectGridRuntimeException e)
		{
			// this is expected
		}
		// now make maps same size
		original.put("10", "DUMMY");
		Map<String, Boolean> rc = utils.cond_putAll(original, newValues, bmFarMap3);
		Assert.assertNotNull(rc);
		for(Map.Entry<String, Boolean> e : rc.entrySet())
		{
			Boolean b = rc.get(e.getKey());
			Assert.assertNotNull(b);
			if(e.getKey().equals("4"))
				Assert.assertFalse(b);
			else
				Assert.assertTrue(b);
		}
		for(int i = 0; i < 11; ++i)
		{
			String v = map.get("" + i);
			if(i != 4)
			{
				Assert.assertEquals("N" + i, v);
			}
			else
			{
				Assert.assertEquals("DIFFERENT", v);
			}
		}
	}
	
	@Test 
	public void testEmptyBulkOperations()
	{
		ArrayList<String> emptyList = new ArrayList<String>();
		utils.getAll(emptyList, bmFarMap3);
		utils.removeAll(emptyList, bmFarMap3);
		Map<String, String> emptyMap = new HashMap<String, String>();
		utils.putAll(emptyMap, bmFarMap3);
	}
	/**
	 * This does a simple stress test against the grid.
	 */
	@Test 
	public void testPutRate()
	{
		clearMap();
		int maxTests = 50;
		// run more than one time to allow JIT to settle
		// for unit test once is enough
		for(int loop = 0; loop < 1; ++loop)
		{
			for(int batchSize = 1000; batchSize <= 32000; batchSize *= 2 )
			{
				Map<String, String> batch = new HashMap<String, String>();
				for(int i = 0; i < batchSize; ++i)
					batch.put(Integer.toString(i), "V" + i);
				
				long start = System.nanoTime();
				for(int test = 0; test < maxTests; ++test)
				{
					utils.putAll(batch, ogclient.getMap("FarMap3"));
				}
				if(false)
				{
					ArrayList<String> keys = new ArrayList<String>();
					for(int i = 0; i < batchSize; ++i)
					{
						keys.add(Integer.toString(i));
					}
					Map<String, String> rc = utils.getAll(keys, ogclient.getMap("FarMap3"));
					
					for(Map.Entry<String, String> e : rc.entrySet())
					{
						Assert.assertEquals("V" + e.getKey(), e.getValue());
					}
				}
				double duration = (System.nanoTime() - start) / 1000000000.0;
				double rate = (double)batchSize * (double)maxTests / duration;
				System.out.println("Batch of " + batchSize + " rate is " + rate + " <" + (batch.size() * maxTests) + ":" + duration + ">");
			}
		}
	}
}
