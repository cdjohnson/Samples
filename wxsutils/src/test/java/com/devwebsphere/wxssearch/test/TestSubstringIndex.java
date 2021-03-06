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
package com.devwebsphere.wxssearch.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.devwebsphere.wxssearch.Index;
import com.devwebsphere.wxssearch.IndexManager;
import com.devwebsphere.wxssearch.PrefixIndexImpl;
import com.devwebsphere.wxssearch.SearchResult;
import com.devwebsphere.wxssearch.type.PrefixIndex;
import com.devwebsphere.wxsutils.WXSMap;
import com.devwebsphere.wxsutils.WXSUtils;
import com.devwebsphere.wxsutils.jmx.MinMaxAvgMetric;
import com.ibm.websphere.objectgrid.ObjectGrid;

public class TestSubstringIndex
{
	static ObjectGrid clientOG;
	static WXSUtils utils;
	static IndexManager<TestBusinessObject, Long> indexManager;
	static Index<TestBusinessObject,Long> firstNameIndex;
//	static Index<TestBusinessObject,Long> middleNameIndex;
	static Index<TestBusinessObject,Long> surnameIndex;
	static WXSMap<Long, TestBusinessObject> realRecordsMap;
	
	@BeforeClass
	static public void initGrid()
	{
		// creates an in JVM complete grid using these xml files for testing and returns
		// a client reference to it.
		clientOG = WXSUtils.startTestServer("Grid", "/search/testog.xml", "/search/testdep.xml");

		// create the utility library for the client grid
		utils = new WXSUtils(clientOG);
		
		// this should be placed in a static or similar device
		indexManager = new IndexManager(utils, TestBusinessObject.class);
		// create the name index. Looking it up creates it.
		firstNameIndex = indexManager.getIndex("firstName");
		Assert.assertNotNull(firstNameIndex);
//		middleNameIndex = indexManager.getIndex("middleName");
//		Assert.assertNotNull(middleNameIndex);
		surnameIndex = indexManager.getIndex("surname");
		Assert.assertNotNull(surnameIndex);
		// create a map for the real records
		realRecordsMap = utils.getCache("RealRecords");
	}

//	@Test
	public void testGeneratePrefix()
		throws IllegalAccessException, NoSuchFieldException
	{
		PrefixIndex p = TestBusinessObject.class.getDeclaredField("middleName").getAnnotation(PrefixIndex.class);
		Assert.assertNotNull(p);
		Set<String> results = PrefixIndexImpl.sgenerate(p, "Billy");
		
		Set<String> correct = new HashSet<String>();
		correct.add("BI"); correct.add("BIL"); correct.add("BILL"); correct.add("BILLY");
		
		Assert.assertEquals(correct, results);
		
		results = PrefixIndexImpl.sgenerate(p, "");
		Assert.assertTrue(results.isEmpty());
	}

	@Test
	public void preloadGrid()
		throws IOException
	{
        InputStream is = IndexManager.class.getResourceAsStream("/search/malenames.txt");
        BufferedReader fr = new BufferedReader(new InputStreamReader(is));
        
        long start = System.currentTimeMillis();
        long count = 0;
        
        Map<Long, TestBusinessObject> entries = new HashMap<Long, TestBusinessObject>();
        while (true)
        {
            String firstname = fr.readLine();
            if (firstname == null)
                break;
            InputStream sis = IndexManager.class.getResourceAsStream("/search/surnames.txt");
            BufferedReader sr = new BufferedReader(new InputStreamReader(sis));
            while (true)
            {
                String surname = sr.readLine();
                if (surname == null)
                    break;
                
                TestBusinessObject bo = new TestBusinessObject();
                bo.firstName = firstname;
                bo.middleName = "";
                bo.surname = surname;
                entries.put(count, bo);
                if (entries.size() > 1000)
                {
                	// insert the real records using a Long key
                	realRecordsMap.putAll(entries);
                	
                	// update the index for each record also. The index just keeps
                	// a reference to the key in RealRecords
                	indexManager.indexAll(entries);
                	entries = new HashMap<Long, TestBusinessObject>();
                }
                count++;
            }
            if(count > 100000)
            	break;
        }
        
        // flush any remaining entries, above loop just does every 1000
        // there will likely be some extra at the end, i.e. there wont
        // be a multiple of a 1000 names, it's unlikely.
        if (entries.size() > 0)
        {
        	indexManager.indexAll(entries);
        	realRecordsMap.putAll(entries);
        	entries = new HashMap<Long, TestBusinessObject>();
        }
        long duration = (System.currentTimeMillis() - start) / 1000;
        System.out.println(Long.toString(count) + " names inserted and indexed in " + duration + " seconds");
	}
	
	@Test
	public void testLookup()
		throws InterruptedException
	{
        SearchResult<Long> matches = null;
        List<byte[]> rawMatches = null;
		for(int loop = 0; loop < 1; ++loop)
        {
            long st_time = System.nanoTime();
            int numIterations = 1000;
            MinMaxAvgMetric m = new MinMaxAvgMetric();
            for (int i = 0; i < numIterations; ++i)
            {
            	long start = System.nanoTime();
            	// get the keys for the records whose 'name' contains EN
            	TestBusinessObject criteria = new TestBusinessObject();
            	criteria.firstName = "JAM"; // anywhere
            	criteria.surname = "ALL"; // exact
            	matches = indexManager.searchMultipleIndexes(criteria, true);
//            	matches = firstNameIndex.contains("JAMES");
//            	rawMatches = firstNameIndex.rawContains("JAMES");
            	m.logTime(System.nanoTime() - start);
            }
//            matches = firstNameIndex.fetchKeysForInternalKeys(IndexManager.convertToKeys(rawMatches));
            System.out.println(m.toString());
            double d = (System.nanoTime() - st_time) / 1000000000.0;
            System.out.println("Throughput is " + Double.toString(numIterations / d) + "/sec");
            m.dumpResponseTimes();
            if(matches.isTooManyMatches())
            	System.out.println("Too many matches found");
            else
            	System.out.println("Found " + matches.getResults().size());
        }
		// print out the records that matches
		if(matches.isTooManyMatches())
		{
			System.out.println("Too many matches");
		}
		else
		{
		    Map<Long, TestBusinessObject> bos = realRecordsMap.getAll(matches.getResults());
		    for (TestBusinessObject bo : bos.values())
		    {
		        System.out.println(bo.firstName + " " + bo.middleName + " " + bo.surname);
		    }
		}
//		Thread.sleep(60*60*1000L); // uncomment to keep JVM running
	}
}
