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
package com.devwebsphere.wxsutils;

public class StartSingleJVMGrid {

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		int i = 0;
		String gridName = args[i++];
		String objectgridxml = args[i++];
		String deploymentxml = args[i++];
		
		System.out.println("Grid: " + gridName);
		System.out.println("ObjectGrid xml: " + objectgridxml);
		System.out.println("Deployment xml:" + deploymentxml);
		WXSUtils.startTestServer(gridName, objectgridxml, deploymentxml);
	}

}
