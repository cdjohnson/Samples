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
package com.devwebsphere.wxs.fs;

/**
 * This ThreadLocal tracks the InputStreamState for a GridInputStream. There
 * is usually one instance of this PER GridInputStream instance.
 * @author bnewport
 *
 */

public class ThreadLocalInputStreamState extends ThreadLocal<GridInputStreamState>{

	@Override
	protected GridInputStreamState initialValue() {
		GridInputStreamState d = new GridInputStreamState();
		return d;
	}
}
