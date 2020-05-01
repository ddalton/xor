/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations 
 * under the License.
 */

package tools.xor;

import java.util.ArrayList;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.operation.CloneOperation;
import tools.xor.operation.DeleteOperation;
import tools.xor.operation.ModifyOperation;
import tools.xor.util.ClassUtil;
import tools.xor.util.ObjectCreator;

public class MutableBO extends AbstractBO {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	private static final long serialVersionUID = 1L;

	// temporary - used for debugging
	private boolean evicted;
	private boolean reference;

	public boolean isEvicted ()
	{
		return evicted;
	}

	public void setEvicted (boolean evicted)
	{
		this.evicted = evicted;
	}

	@Override
	public boolean isReference() {
		return this.reference;
	}

	public void setReference(boolean reference) {
		this.reference = reference;
	}
	
	public MutableBO(Type type, DataObject container,
			Property containmentProperty, ObjectCreator objectCreator) {
		super(type, container, containmentProperty, objectCreator);

		if(isRoot()) 
			objectPersister = new ObjectPersister();
	}

	protected Type getEntityType(CallInfo callInfo, Settings settings) {
		Type entityType = settings.getEntityType();
		if(callInfo.isBulkInput()) {
			entityType = new ListType(ArrayList.class);
		}

		return entityType;
	}

	@Override
	public BusinessObject update(Settings settings) {
		// clone the task object using a DataObject
		CallInfo callInfo = new CallInfo(this, null, null, null);
		callInfo.setSettings(settings);	
		callInfo.getSettings().setAction(settings.getAction()); // Since the default global action can be either UPDATE or MERGE
		this.getObjectCreator().setShare(true);
		this.createAggregate(settings);

		// Create an object creator for the target root
        TypeMapper typeMapper = getObjectCreator().getTypeMapper().newInstance(MapperSide.DOMAIN);
        ObjectCreator oc = new ObjectCreator(settings, getObjectCreator().getDataStore(), typeMapper);		
		oc.setShare(true);
		callInfo.setOutputObjectCreator(oc);
		ModifyOperation operation = new ModifyOperation();
		callInfo.setOperation(operation);

		BusinessObject target = operation.createTarget(callInfo, getEntityType(callInfo, settings));
		oc.setObjectGraph(target);
		callInfo.setOutput(target);
		settings.setPersist(true);
		operation.execute(callInfo);

		return target;
	}

	@Override
	/**
	 * Always creates a root data object
	 */
	public DataObject create(Settings settings) {

		Date s = new Date();
		// clone the task object using a DataObject
		CallInfo callInfo = new CallInfo(this, null, null, null);
		callInfo.setSettings(settings);
		this.getObjectCreator().setShare(true);
		this.createAggregate(settings);
		Date a = new Date();

		// Create an object creator for the target root
		TypeMapper typeMapper = getObjectCreator().getTypeMapper().newInstance(MapperSide.DOMAIN);
		ObjectCreator oc = new ObjectCreator(settings, getObjectCreator().getDataStore(), typeMapper);
		oc.setShare(true);
		callInfo.setOutputObjectCreator(oc);
		ModifyOperation operation = new ModifyOperation();
		callInfo.setOperation(operation);

		BusinessObject target = operation.createTarget(callInfo, getEntityType(callInfo, settings));
		oc.setObjectGraph(target);
		callInfo.setOutput(target);
		settings.setPersist(true);
		
		try {		
			Date start = new Date();
			operation.execute(callInfo);
			//oc.persistGraph(settings);
			if(logger.isDebugEnabled()) {
				logger.debug("MutableBO#create.createAggregate took " + ((a.getTime()-s.getTime())/1000) + " seconds");
				logger.debug("MutableBO#create.execute took " + ((start.getTime()-s.getTime())/1000) + " seconds");
				logger.debug("MutableBO#create.persist took " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
			}
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}

		return target;		
	}

	@Override
	public void delete(Settings settings) {
		// clone the task object using a DataObject
		CallInfo callInfo = new CallInfo(this, null, null, null);
		callInfo.setSettings(settings);
		callInfo.getSettings().setAction(AggregateAction.DELETE);
		this.getObjectCreator().setShare(true);
		this.createAggregate(settings);

		// Create an object creator for the target root
        TypeMapper typeMapper = getObjectCreator().getTypeMapper().newInstance(MapperSide.DOMAIN);
        ObjectCreator oc = new ObjectCreator(settings, getObjectCreator().getDataStore(), typeMapper);
		oc.setShare(true);
		callInfo.setOutputObjectCreator(oc);
		DeleteOperation operation = new DeleteOperation();
		callInfo.setOperation(operation);

		BusinessObject target = operation.createTarget(callInfo, getEntityType(callInfo, settings));
		if(target == null) {
			throw new RuntimeException("Unable to find the entity to delete");
		}

		oc.setObjectGraph(target);
		callInfo.setOutput(target);
		settings.setPersist(true);
		operation.execute(callInfo);
	}

	@Override
	public DataObject clone(Settings settings) {

		// clone the task object using a DataObject
		CallInfo callInfo = new CallInfo(this, null, null, null);
		callInfo.setSettings(settings);	
		callInfo.getSettings().setAction(AggregateAction.CLONE);
		this.createAggregate(settings);

		// Create an object creator for the target root
        TypeMapper typeMapper = getObjectCreator().getTypeMapper().newInstance(MapperSide.DOMAIN);
        ObjectCreator oc = new ObjectCreator(settings, getObjectCreator().getDataStore(), typeMapper);		
		callInfo.setOutputObjectCreator(oc);
		CloneOperation operation = new CloneOperation();
		callInfo.setOperation(operation);
		BusinessObject target = null;
		
		try {
			target = operation.createTarget(callInfo, settings.getEntityType());
			oc.setObjectGraph(target);
			callInfo.setOutput(target);
			settings.setPersist(true);
			operation.execute(callInfo);
			//oc.persistGraph(settings);
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}

		return target;
	}

}
