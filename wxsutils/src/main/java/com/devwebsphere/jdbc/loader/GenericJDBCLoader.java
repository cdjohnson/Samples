package com.devwebsphere.jdbc.loader;

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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.devwebsphere.wxsutils.WXSUtils;
import com.devwebsphere.wxsutils.jmx.loader.LoaderMBeanImpl;
import com.ibm.websphere.objectgrid.BackingMap;
import com.ibm.websphere.objectgrid.Session;
import com.ibm.websphere.objectgrid.TxID;
import com.ibm.websphere.objectgrid.plugins.Loader;
import com.ibm.websphere.objectgrid.plugins.LoaderException;
import com.ibm.websphere.objectgrid.plugins.LogElement;
import com.ibm.websphere.objectgrid.plugins.LogSequence;
import com.ibm.websphere.objectgrid.plugins.OptimisticCollisionException;
import com.ibm.websphere.projector.annotations.Id;

/**
 * This is a generic JDBC Loader for POJOs. It expects a POJO that has public field attributes. Some
 * of these are annotated the WXS Id annotation to indicate key fields in the database. The database table is specified using
 * the table name attribute. The POJO attribute names must be the same as the database table column names.
 * @author bnewport
 * @see Id
 *
 */
public class GenericJDBCLoader extends BaseJDBCLoader implements Loader 
{
	static Logger logger = Logger.getLogger(GenericJDBCLoader.class.getName());
	
	public String getClassName() {
		return theClass.getName();
	}
	
	public void setClassName(String className) 
	{
		try
		{
			theClass = Class.forName(className);
			createSQLStrings();
		}
		catch(ClassNotFoundException e)
		{
			logger.log(Level.SEVERE, "Unknown class name " + className);
			throw new IllegalArgumentException("Unknown class name: " + className);
		}
	}

	/**
	 * The POJO class to persist. Need this to reflect the annotations
	 */
	private Class theClass;

	/**
	 * This is the insert SQL command 
	 */
	String insertSQL;
	/**
	 * This is the update SQL command
	 */
	String updateSQL;
	/**
	 * This is the delete SQL command
	 */
	String deleteSQL;
	/**
	 * This is the select SQL command
	 */
	String selectSQL;

	ArrayList<String> keyFieldNames;
	ArrayList<Field> keyFields;
	ArrayList<String> normalFieldNames;
	ArrayList<Field> normalFields;
	ArrayList<String> allFieldNames;
	ArrayList<Field> allFields;
	
	/**
	 * This reflects the POJO and builds the SQL strings for insert, select, update and delete commands
	 * @throws LoaderException
	 */
	void createSQLStrings()
	{
		if(insertSQL != null)
			return;
		keyFieldNames = new ArrayList<String>();
		keyFields = new ArrayList<Field>();
		normalFieldNames = new ArrayList<String>();
		normalFields = new ArrayList<Field>();
		allFieldNames = new ArrayList<String>();
		allFields = new ArrayList<Field>();
		Field[] fields = theClass.getDeclaredFields();
		for(Field f : fields)
		{
			// ignore static fields
			if((f.getModifiers() & Modifier.STATIC) == 0)
			{
				// is it a key field?
				if(f.getAnnotation(Id.class) != null)
				{
					keyFieldNames.add(f.getName());
					keyFields.add(f);
				}
				else
				{
					normalFieldNames.add(f.getName());
					normalFields.add(f);
				}
				allFieldNames.add(f.getName());
				allFields.add(f);
			}
		}
		insertSQL = "INSERT INTO " + tableName + " (";
		for(int i = 0; i < allFieldNames.size(); ++i)
		{
			insertSQL += allFieldNames.get(i).toUpperCase() + ((i != allFieldNames.size() - 1) ? "," : "");
		}
		insertSQL += ") VALUES (";
		for(int i = 0; i < allFieldNames.size(); ++i)
		{
			insertSQL += ":" + allFieldNames.get(i) + ((i != allFieldNames.size() - 1) ? "," : "");
		}
		insertSQL += ")";
		deleteSQL = "DELETE FROM " + tableName;
		String whereSQL = " WHERE ";
		for(int i = 0; i < keyFieldNames.size(); ++i)
		{
			whereSQL += keyFieldNames.get(i).toUpperCase() + "=:" + keyFieldNames.get(i) + ((i != keyFieldNames.size() - 1) ? " AND " : "");
		}
		deleteSQL += whereSQL;
		updateSQL = "UPDATE " + tableName + " SET ";
		for(int i = 0; i < normalFieldNames.size(); ++i)
		{
			updateSQL += normalFieldNames.get(i).toUpperCase() + "=:" + normalFieldNames.get(i) + ((i != normalFieldNames.size() - 1) ? "," : "");
		}
		updateSQL += whereSQL;
		selectSQL = "SELECT ";
		for(int i = 0; i < allFieldNames.size(); ++i)
		{
			selectSQL += allFieldNames.get(i) + ((i != allFieldNames.size() - 1) ? "," : "");
		}
		selectSQL +=" FROM " + tableName + whereSQL;
		logger.fine("ISQL: " + insertSQL);
		logger.fine("USQL: " + updateSQL);
		logger.fine("DSQL: " + deleteSQL);
	}
	
	/**
	 * This is called to write changes at the end of a transaction or a flush to the backend this
	 * Loader works with which in this case in a database. We use purequery to flush everything
	 * in a single batch statement. Purequery does the POJO attribute to SQL parameter work
	 * for us.
	 */
	public void batchUpdate(TxID tx, LogSequence ls)
			throws LoaderException, OptimisticCollisionException 
	{
		mapName = ls.getMapName();
		LoaderMBeanImpl mbean = WXSUtils.getLoaderMBeanManager().getBean(tx.getSession().getObjectGrid().getName(), ls.getMapName());
		mbean.getBatchSizeMetrics().logTime(ls.size());
		long startNS = System.nanoTime();
		// start a hetero batch.
		// make empty lists for the objects that are inserted or updated or deleted
		ArrayList<Object> iList = new ArrayList<Object>(1000);
		ArrayList<Object> uList = new ArrayList<Object>(1000);
		ArrayList<Object> dList = new ArrayList<Object>(1000);
		try
		{
			// get the Data instance for this transaction
			Connection conn = getConnection(tx);
//			data.startBatch(HeterogeneousBatchKind.heterogeneousModify__);
			// get the changes in this transaction
			Iterator<LogElement> iter = ls.getPendingChanges();
			// the key object. We may need to wrap primitive POJOs (Long, Integer etc) with a wrapped
			// with a key attribute for purequery.
			Object key;
			while(iter.hasNext())
			{
				// get next transaction change
				LogElement e = iter.next();
				switch(e.getType().getCode())
				{
				case LogElement.CODE_INSERT: // add to insert POJO list
					iList.add(e.getCurrentValue());
					break;
				case LogElement.CODE_UPDATE:
					uList.add(e.getCurrentValue()); // add to update POJO list
					break;
				case LogElement.CODE_DELETE: // add to delete POJO list
					key = e.getCacheEntry().getKey();
					dList.add(key);
					break;
				}
			}
			// execute the statements within the batch for each type of operation
			if(iList.size() > 0)
			{
				if(logger.isLoggable(Level.FINE))
					logger.fine("Inserting " + ls.getMapName() + " " + iList.toString());
				copyPojoListToBatch(conn, insertSQL, iList, allFields);
			}
			if(uList.size() > 0)
			{
				if(logger.isLoggable(Level.FINE))
					logger.fine("Updating " + ls.getMapName() + " " + uList.toString());
				copyPojoListToBatch(conn, updateSQL, uList, allFields);
			}
			if(dList.size() > 0)
			{
				if(logger.isLoggable(Level.FINE))
					logger.fine("Deleting " + ls.getMapName() + " " + dList.toString());
				copyPojoListToBatch(conn, deleteSQL, dList, keyFields);
			}
			mbean.recordOperationRows(iList.size(), uList.size(), dList.size());
			mbean.getBatchUpdateMetrics().logTime(System.nanoTime() - startNS);
		}
		catch(SQLException e)
		{
			logger.log(Level.SEVERE, "SQL exception processing changes", e);
			mbean.getBatchUpdateMetrics().logException(e);
			throw new LoaderException("Map " + ls.getMapName(), e);
		}
		catch(Exception e)
		{
			logger.log(Level.SEVERE, "Unexpected exception processing update", e);
			mbean.getBatchUpdateMetrics().logException(e);
			throw new LoaderException("Map " + ls.getMapName(), e);
		}
	}

	/**
	 * This is called when a client looks up a key which isn't contained in the grid. The grid
	 * then calls this method to fetch the values for the keys. If a key doesn't exist in the
	 * backend then Loader.KEY_NOT_FOUND is returned.
	 */
	@SuppressWarnings("unchecked")
	public List get(TxID tx, List keys, boolean arg2) throws LoaderException 
	{
		LoaderMBeanImpl mbean = WXSUtils.getLoaderMBeanManager().getBean(tx.getSession().getObjectGrid().getName(), mapName);
		try
		{
			mbean.getGetSizeMetrics().logTime(keys.size());
			long startNS = System.nanoTime();
			// Get a Data instance for these database selects
			Connection conn = getConnection(tx);
			// list for the results
			ArrayList rc = new ArrayList();
			Iterator iter = keys.iterator();
			Object key = theClass.newInstance();
			Field keyField = theClass.getField("keyz");
			
			NamedParameterStatement s = new NamedParameterStatement(conn, selectSQL);
			// for each key in the list
			while(iter.hasNext())
			{
				Object keyValue = iter.next();
				Object value = null;
				// wrap with a Key object for purequery if needed
				if(keyValue.getClass().isPrimitive() || keyValue instanceof String)
				{
					keyField.set(key, keyValue);
					copyPojoToStatement(s, key, keyFields);
				}
				else
				{
					copyPojoToStatement(s, keyValue, keyFields);
				}
				ResultSet rs = s.executeQuery();
				if(rs.first())
				{
					value = copyResultSetToPojo(rs, theClass, allFields);
				}
				// if we found the value then add it otherwise add KEY_NOT_FOUND
				if(value != null)
				{
					rc.add(value);
					if(logger.isLoggable(Level.FINE))
						logger.fine("Found " + keyValue + " in " + tableName + " = " + value);
				}
				else
				{
					rc.add(Loader.KEY_NOT_FOUND);
					if(logger.isLoggable(Level.FINE))
						logger.fine("Cant find " + keyValue + " in " + tableName);
				}
			}
			mbean.getGetMetrics().logTime(System.nanoTime() - startNS);
			return rc;
		}
		catch(Exception e)
		{
			logger.log(Level.SEVERE, "Unexpected exception in get", e);
			mbean.getGetMetrics().logException(e);
			throw new LoaderException(e);
		}
	}

	public void preloadMap(Session sess, BackingMap map)
			throws LoaderException {
		// TODO Auto-generated method stub

	}
}
