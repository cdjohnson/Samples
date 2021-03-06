package com.devwebsphere.wxsutils.multijob;
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


import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.websphere.objectgrid.BackingMap;
import com.ibm.websphere.objectgrid.ObjectGrid;
import com.ibm.websphere.objectgrid.ObjectGridException;
import com.ibm.websphere.objectgrid.ObjectGridRuntimeException;
import com.ibm.websphere.objectgrid.Session;
import com.ibm.websphere.objectgrid.datagrid.AgentManager;

/**
 * This takes a MultiPartTask and works with it to execute
 * a series of SinglePartTasks on each partition in the grid.
 * @author bnewport
 *
 * @param <R> The user consumable type returned from each SinglePartTask
 * @param <V> The raw value returned directly from SinglePartTask
 */
public class JobExecutor <V,R>
{
	static Logger logger = Logger.getLogger(JobExecutor.class.getName());
	MultipartTask<V, R> mtask;
	ObjectGrid ogclient;
	int currentPartitionID;
	SinglePartTask<V, R> currTask;
	String routingMapName = "RouterKeyI32";

	/**
	 * This constructs an instance that will use the specified client grid
	 * and MultiPartTask
	 * @param ogclient
	 * @param m
	 */
	public JobExecutor(ObjectGrid ogclient, MultipartTask<V, R> m)
	{
		this.mtask = m;
		this.ogclient = ogclient;
		String aMapName = (String)ogclient.getListOfMapNames().get(0);
		BackingMap bmap = ogclient.getMap(aMapName);
		currentPartitionID = bmap.getPartitionManager().getNumOfPartitions() - 1;
		currTask = null;
	}

	/**
	 * This is called to get the next set of results. It's possible for implementations
	 * to return empty collections as a return value for this method. This does not mean
	 * there is no more data. You should only stop iterating on getNextResult then it
	 * returns null.
	 * @return The next block or null if no more data
	 */
	public R getNextResult()
	{
		try
		{
			// this counts down from max partition to 0.
			// while there are partitions left to 'process'
			if(currentPartitionID >= 0)
			{
				// create the next task for this partition
				currTask = mtask.createTaskForPartition(currTask);
				// all blocks for current partition are retrieved
				if(currTask == null)
				{
					// time to move to next partition
					--currentPartitionID;
					// if partition == -1 then we're done
					if(currentPartitionID >= 0)
					{
						currTask = mtask.createTaskForPartition(null);
					}
					else
						return null;
				}
				// currTask needs to be sent to current partition
				Object key = new Integer(currentPartitionID);
				Session sess = ogclient.getSession();
				AgentManager amgr = sess.getMap(routingMapName).getAgentManager();
				JobAgent<V,R> agent = new JobAgent<V,R>(currTask);
				// invoke the SingleTaskPart on this specified partition
				Map<Integer, V> agent_result = amgr.callMapAgent(agent, Collections.singleton(key));
				// extract the user exposed object from the return value
				R r = currTask.extractResult(agent_result.get(key));
				return r;
			}
			else
				return null;
		}
		catch(ObjectGridException e)
		{
			throw new ObjectGridRuntimeException(e);
		}
	}
}
