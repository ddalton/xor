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

import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.operation.CloneOperation;
import tools.xor.operation.ModifyOperation;
import tools.xor.util.ClassUtil;
import tools.xor.util.ObjectCreator;

public class MutableBO extends AbstractBO implements BusinessNode {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	private static final long serialVersionUID = 1L;
	
	public MutableBO(Type type, DataObject container,
			Property containmentProperty, ObjectCreator objectCreator) {
		super(type, container, containmentProperty, objectCreator);

		if(isRoot()) 
			objectPersister = new ObjectPersister();
	}

	@Override
	public boolean isPersistent() {
		return persistent;
	}

	@Override
	public void clearVisited() {
		for(BusinessObject dataObject: objectCreator.getDataObjects())
			dataObject.setVisited(false);
	}

	/**
	 * This method takes care of creating the containment graph and also any needed DataObject wrappers for the instance objects
	 * This is especially important if the object is referenced more than once and we don't want to create a copy for each reference
	 * It also demarcates the spanning tree of the graph based on containment relationships
	 */	
	@Override
	public void createAggregate() {

		// Loop through the data object properties and if it is not a data type, then create a Data Object wrapper and recurse
		clearVisited();
		createWrapper(this);

		clearVisited();
	}

	protected void createWrapper(BusinessObject parent) {
		
		for(BusinessObject child: parent.getList()) {
			if(parent.getContainmentProperty().isContainment()) {
				child.setContainer(parent);
				child.setContainmentProperty(parent.getContainmentProperty());
			}
			createWrapper(child);
		}

		for(Property property: parent.getType().getProperties()) {	
			if(!((ExtendedProperty) property).isDataType()) {
				Object propertyInstance = ((ExtendedProperty)property).getValue(parent);
				if(propertyInstance == null)
					continue;

				Object target = objectCreator.getExistingDataObject(propertyInstance);
				if(target != null && !BusinessObject.class.isAssignableFrom(target.getClass()))
					throw new IllegalStateException("Property refers to a DataObject, but the object is not a DataObject");

				BusinessObject child = null;
				if(target != null)
					child = (BusinessObject) target;
				else {
					BusinessObject container = parent;
					Property containmentProperty = property;
					if(!property.isContainment()) {
						container = null;
						containmentProperty = null;
					}
					child = objectCreator.createDataObject(propertyInstance, property.getType(), container, containmentProperty);
				}

				if(child.isVisited())
					continue;
				else
					child.setVisited(true);

				if(!property.isContainment()) 
					continue;

				createWrapper(child);				
			}
		}
	}	

	@Override
	public boolean isDependent() {
		return getContainmentProperty() != null;
	}

	@Override
	public BusinessObject update(Settings settings) {
		// clone the task object using a DataObject
		CallInfo callInfo = new CallInfo(this, null, null, null);
		callInfo.setSettings(settings);	
		callInfo.getSettings().setAction(settings.getAction()); // Since the default global action can be either UPDATE or MERGE
		this.createAggregate();

		// Create an object creator for the target root
		ObjectCreator oc = new ObjectCreator(getObjectCreator().getDAS(), getObjectCreator().getPersistenceOrchestrator(), MapperDirection.DOMAINTOEXTERNAL);
		callInfo.setOutputObjectCreator(oc);
		ModifyOperation operation = new ModifyOperation();
		callInfo.setOperation(operation);
		BusinessObject target = null;
		
		target = (BusinessObject) operation.createTarget(callInfo, settings.getEntityClass());
		oc.setObjectGraph(target);
		callInfo.setOutput(target);
		operation.execute(callInfo);
		
		try {
			oc.persistGraph(settings);
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}

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
		callInfo.getSettings().setAction(AggregateAction.CREATE);
		this.getObjectCreator().setShare(true);
		this.createAggregate();
		Date a = new Date();

		// Create an object creator for the target root
		ObjectCreator oc = new ObjectCreator(getObjectCreator().getDAS(), getObjectCreator().getPersistenceOrchestrator(), MapperDirection.DOMAINTOEXTERNAL);
		callInfo.setOutputObjectCreator(oc);
		ModifyOperation operation = new ModifyOperation();
		callInfo.setOperation(operation);
		BusinessObject target = null;
		
		target = (BusinessObject) operation.createTarget(callInfo, settings.getEntityClass());
		oc.setObjectGraph(target);
		callInfo.setOutput(target);
		operation.execute(callInfo);

		try {
			Date start = new Date();
			oc.persistGraph(settings);
			System.out.println("MutableBO#create.createAggregate took " + ((a.getTime()-s.getTime())/1000) + " seconds");
			System.out.println("MutableBO#create.execute took " + ((start.getTime()-s.getTime())/1000) + " seconds");
			System.out.println("MutableBO#create.persist took " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}

		return target;		
	}

	@Override
	public DataObject clone(Settings settings) {

		// clone the task object using a DataObject
		CallInfo callInfo = new CallInfo(this, null, null, null);
		callInfo.setSettings(settings);	
		callInfo.getSettings().setAction(AggregateAction.CLONE);
		this.createAggregate();

		// Create an object creator for the target root
		ObjectCreator oc = new ObjectCreator(getObjectCreator().getDAS(), getObjectCreator().getPersistenceOrchestrator(), MapperDirection.DOMAINTODOMAIN);
		callInfo.setOutputObjectCreator(oc);
		CloneOperation operation = new CloneOperation();
		callInfo.setOperation(operation);
		BusinessObject target = null;
		
		try {
			target = (BusinessObject) operation.createTarget(callInfo, settings.getEntityClass());
			oc.setObjectGraph(target);
			callInfo.setOutput(target);
			operation.execute(callInfo);
			oc.persistGraph(settings);
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}

		return target;
	}

}
