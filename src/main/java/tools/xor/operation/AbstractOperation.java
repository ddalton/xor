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

package tools.xor.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.BusinessEdge;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.ExtendedProperty.Phase;
import tools.xor.ListType;
import tools.xor.ProcessingStage;
import tools.xor.Property;
import tools.xor.Type;
import tools.xor.event.PropertyElement;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;

import javax.swing.text.html.parser.Entity;

public abstract class AbstractOperation implements Operation {
	private static final Logger owLogger = LogManager.getLogger(Constants.Log.OBJECT_WALKER);

	protected void setResult(Object target) {}
		
	protected boolean supportsCreate(CallInfo callInfo) {
		return true;
	}	
	
	protected boolean supportsUpdate(CallInfo callInfo) {
		return true;
	}	
	
	protected boolean supportsPostLogic(CallInfo callInfo) {
		return callInfo.getSettings().isSupportsPostLogic();
	}

	public void execute(CallInfo callInfo) {
		
		// Create new objects
		if(supportsCreate(callInfo)) {
			owLogger.debug("Processing state: " + ProcessingStage.CREATE);
			this.process(callInfo.setStage(ProcessingStage.CREATE)); 
			callInfo.clearVisitedOutputs();			
		}

		// TODO: get list of revisions for this aggregate and iterate this process for each revision
		// The upgrade and downgrade methods will be called for revision processing instead of the process method
		// Step 1: Create the update actions
		if(supportsUpdate(callInfo)) {
			setVisited(callInfo, true);
			owLogger.debug("Processing state: " + ProcessingStage.UPDATE);
			this.process(callInfo.setStage(ProcessingStage.UPDATE));
			callInfo.clearVisitedOutputs();			
			
			// Step 2: Execute the actions
			if(callInfo.getOutputRoot().getObjectPersister() != null) {
				callInfo.getOutputRoot().getObjectPersister().processActions(callInfo.getSettings());
			}
		}

		// Remove the dummy root node
		if(callInfo.isBulkInput()) {
			callInfo.getOutputObjectCreator().unregister((BusinessObject)callInfo.getInput());
			callInfo.getOutputObjectCreator().unregister((BusinessObject)callInfo.getOutput());
		}
		
		// Now we can persist the objects as all the links are set
		// This might be necessary if the deferred and post logic actions if any
		// are dependent on the identifier of newly created objects
		if(callInfo.getSettings().doPersist()) {
			persist(callInfo);
		}
		
		// Process the open property actions that were deferred
		if(callInfo.getOutputRoot().getObjectPersister() != null) {
			callInfo.getOutputRoot().getObjectPersister().processOpenPropertyActions(callInfo.getSettings());
		}

		// Process post actions
		// Enable only if necessary for performance reasons
		if(supportsPostLogic(callInfo)) {
			owLogger.debug("Processing state: " + ProcessingStage.POSTLOGIC);
			this.process(callInfo.setStage(ProcessingStage.POSTLOGIC));
			callInfo.clearVisitedOutputs();	
		}

		setResult(callInfo.getOutput());
	}
	
	protected void persist(CallInfo callInfo) {
		/*
		 * By default we do not persist. This is overridden by the
		 * appropriate subclasses
		 */
	}

	protected void processPostLogic(CallInfo callInfo) throws Exception
	{
		if(callInfo.getStage() == ProcessingStage.POSTLOGIC) {
			BusinessObject target = (BusinessObject) callInfo.getOutput();
			if(callInfo.isCascadable())
				target.invokePostLogic(callInfo.getSettings());
		}
	}

	/* Algorithm
	 * =========
	 * 
	 * 1. Process the "from" data object
	 *    a) If it has properties then process each on in turn
	 *    b) If it does not have properties and is a collection/map then process each element in turn
	 *    c) If it does not have properties then process the data type
	 * 2. If it is a valid property, call ObjectCreator#toDataObject on the "from" property and goto STEP 1
	 * 3. Else set as null in the "to" object and process the next item
	 * 
	 */		
	public void process(CallInfo callInfo) {

		try {
			if(!callInfo.isDataType()) {
				BusinessObject source = (BusinessObject) callInfo.getInput();
				BusinessObject target = (BusinessObject) callInfo.getOutput();
				boolean alreadyVisited = target.isVisited();
				
				// Get the property list for the current API version
				if(callInfo.isBulkInput()) {
					processBulk(callInfo);
				} else {
					List<Property> properties = callInfo.getProperties(source.getType());
					CallInfo next = new CallInfo();

					List<Property> propertyReferences = new ArrayList<Property>();
					for (Property sourceProperty : properties) {
						if(next.isDataType()) {

							// Simple properties are processed first as we need this information
							// to form EntityKey using userKey
							next.initOperation(this, null, callInfo, (ExtendedProperty)sourceProperty);
							processAttribute(next);
						} else {
							propertyReferences.add(sourceProperty);
						}
					}

					// Register the object now, so property references can find this object
					if (target.getType() instanceof EntityType) {
						EntityType entityType = (EntityType)target.getType();
						if (!entityType.isEmbedded() && target.getInstance() != null &&
							callInfo.getStage() == ProcessingStage.UPDATE) {
							target.addEntity(target);
						}
					}

					// Process the property references
					for (Property sourceProperty : propertyReferences) {
						next.initOperation(this, null, callInfo, (ExtendedProperty)sourceProperty);
						processAttribute(next);
					}
				}

				if(!alreadyVisited) {
					processPostLogic(callInfo);
				}
			}

		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		} 
	}

	private void processToMany(CallInfo callInfo) throws Exception {
		if(callInfo.getOutput() == null)
			return;

		processCollection(callInfo);
	}
	
	protected ExtendedProperty getDomainProperty(CallInfo ci) {
		return ci.getOutputProperty();
	}
	
	private boolean executeDataUpdate(CallInfo ci, Phase phase) {
		 
		if(ci.getOutputProperty() != null) {
			ExtendedProperty property = getDomainProperty(ci);
			String[] tags = ci.getSettings().getTags().toArray(new String[ci.getSettings().getTags().size()]);
			if(property.getLambdas(ci.getSettings(), tags, phase, ci.getStage()).size() > 0) {
				return property.evaluateLambda(
					new PropertyElement(
						ci.getSettings(),
						getDomain(ci),
						getExternal(ci),
						getDomainParent(ci),
						getExternalParent(ci),
						tags,
						phase,
						ci.getStage())).isCapture();
			}
		}


		return false;
	}
	
	@Override
	public Object getDomain(CallInfo ci) {
		return ci.getOutput();
	}
	
	@Override
	public Object getExternal(CallInfo ci) {
		return ci.getInput();
	}	
	
	@Override
	public BusinessObject getDomainParent(CallInfo ci) {
		return ci.getParentOutputEntity();
	}
	
	@Override
	public BusinessObject getExternalParent(CallInfo ci) {
		return ci.getParentInputEntity();
	}	

	public String getDebugInput(Object inputValue) {
		if(inputValue.toString().length() > Constants.Log.DEBUG_DATA_SIZE) {
			return inputValue.toString().substring(0, Constants.Log.DEBUG_DATA_SIZE) + "...";
		} 
		return inputValue.toString();
	}
	
	private void rebuildImmutable(CallInfo ci) {
		if(ci.getOutputObjectCreator().getTypeMapper().immutable() && ci.getStage() == ProcessingStage.POSTLOGIC) {
			ci.linkOutputToParent(ci.getOutput());
		}
	}
	
	protected void processDataType(CallInfo ci) throws Exception {
		ci.setOutput(ci.getOutputObjectCreator().createDataType(ci.getInputFromParent(this), ci.getOutputProperty()));
		Object oldTarget = ci.getOutputFromParent(ci.getSettings());
		if( !( oldTarget == ci.getOutput() || (oldTarget != null && oldTarget.equals(ci.getOutput()))) ) {
			ci.linkOutputToParent(ci.getOutput());
		}		
	}
	
	public void processAttribute(CallInfo ci) throws Exception {

	
		if(executeDataUpdate(ci, Phase.PRE)) {
			return;
		}

		if(owLogger.isDebugEnabled()) {
			String hasInput = "[HAS NO DATA]";
			if(ci.getInput() != null) {
				hasInput = "[" + getDebugInput(ci.getInput()) + "]";
			}
			owLogger.debug(ci.getIndentString() + (ci.getInputProperty() == null ? "": ci.getInputProperty().getName()) + hasInput);
		}
		
		if(ci.getInput() == null) {
			if ( ci.getOutputFromParent(ci.getSettings()) != null && shouldUnlink(ci)) {
				processNullValue(ci);
			}

			return;
		}		

		// Skip populating the identifier for JPA object copy, as we want this to be null
		// Subclass can override this behavior
		if(isIdentifier(ci)) 
			return;		

		// If the property to be copied is specified then we perform the copy only for that property
		if(ci.getInputProperty() != null) { // Owner is not a collection

			// Simply copy the value
			if(ci.isDataType()) {
				processDataType(ci);
				return;
			}
			
			// TODO: Pre DataUpdate

			// if a reference was returned then we need to not process it anymore
			 // TODO: check if processed flag is necessary - What about temp objects with reference relationship?
			ci.setOutput(ci.getOutputObjectCreator().getExistingDataObject(ci.getInput()));

			if(ci.getOutput() != null) {
				setPropertyTarget(ci, ci.getOutput());
				
				if( ((BusinessObject)ci.getOutput()).isVisited() ) {
					// Rebuild an immutable object that is now fully populated by the builder
					rebuildImmutable(ci);
					return;				
				}
			} else {
				// Do the copy here if necessary
				ci.setOutput(setExistingOrNewCopy(ci));
			}

			// Add edge to any open property objects
			// Collection edges are handled later using the build method
			BusinessObject invokee = (BusinessObject)ci.getParentOutputEntity();
			if(invokee != null && invokee.getObjectCreator().getObjectGraph() != null && ci.getStage() == ProcessingStage.UPDATE) {
				BusinessObject value = (BusinessObject) ci.getOutput();
				BusinessEdge<BusinessObject> edge = new BusinessEdge<BusinessObject>(invokee, value, ci.getOutputProperty());
				invokee.getObjectCreator().getObjectGraph().addEdge(edge, invokee, value);
			}		

			// TODO: Post DataUpdate
		}
		executeDataUpdate(ci, Phase.POST);

		if(!((BusinessObject)ci.getOutput()).isVisited()) {
			setVisited(ci, true);
			postVisited(ci);
		} else
			return;

		if(ci.getInputProperty() != null && ci.getInputProperty().isMany()) { // Process the collection/map
			processToMany(ci);		
		} else {
			process(ci);
		}
		
		// Rebuild an immutable object that is now fully populated by the builder
		//rebuildImmutable(ci);
	}	
	
	protected void postVisited(CallInfo ci) {
	}
	
	private void setVisited(CallInfo callInfo, boolean value) {
		((BusinessObject)callInfo.getOutput()).setVisited(true); // marked nodes we processed
		((BusinessObject)callInfo.getInput()).setVisited(true); // This flag on the source is used by the conflict resolution logic		
	}

	@Override
	public boolean isExternalAssociationLink(CallInfo ci) {
		boolean result = ci.isExternal();

		return result;
	}	

	protected Object setExistingOrNewCopy(CallInfo ci) throws Exception {
		Object propertyTarget = null;

		// If this is a collection, then we need to skip creating copy if an existing persistent collection is found
		Object value = ci.getOutputFromParent(ci.getSettings());
		if(value != null) {

			// Existing embedded/collection objects need to be wrapped in a data object
			if(ci.getInputProperty() != null && 
					(ci.getInputProperty().isMany() || ((EntityType)ci.getInputProperty().getType()).isEmbedded()) && 
					!(value instanceof BusinessObject))
				propertyTarget = createTarget(ci, value, null);
		}

		// Create a new object
		if(propertyTarget == null) {
			propertyTarget =  createTarget(ci, null); // clear the existing id
		}

		propertyTarget = setPropertyTarget(ci, propertyTarget);

		return propertyTarget;
	}	

	public Object getExistingTarget(CallInfo ci) {
		return ci.getOutputObjectCreator().getExistingDataObject(ci.getInput());
	}

	@Override
	public BusinessObject createTarget(CallInfo ci, Type domainType) {	
		return createTarget(ci, null, domainType);
	}

	@Override
	public BusinessObject createTarget(CallInfo ci, Object targetInstance, Type domainType) {
		targetInstance = ClassUtil.getInstance(targetInstance);
		BusinessObject target = ci.getOutputObjectCreator().createTarget(ci, targetInstance, domainType);

		return target;
	}	

	protected Object setPropertyTarget(CallInfo ci, Object propertyTarget) {
		// Set only if updating
		if(ci.getStage() != ProcessingStage.UPDATE)
			return propertyTarget;

		// Set the target property value
		ci.linkOutputToParent(propertyTarget);			

		return propertyTarget;
	}		

	protected boolean shouldUnlink(CallInfo ci) {
		return true;	
	}	

	protected boolean isIdentifier(CallInfo ci) {
		if(ci.getInputProperty() == null)
			return false;

		EntityType sourceType = (EntityType) ((BusinessObject) ci.getParent().getInput()).getType();
		if(sourceType.isEmbedded()) // embedded type
			return false;

		if(ci.getInputProperty().getName().equals( sourceType.getIdentifierProperty().getName()) )
			return true;

		return false;
	}

	protected void processNullValue(CallInfo ci) throws Exception {
		if(ci.getStage() != ProcessingStage.UPDATE) // Do not modify if not updating it
			return;

		// Set the null value - unlink the relationship from this side	
		ci.linkOutputToParent(null);				
	}	


	/**
	 * The Data objects for the source collection should have been created
	 * So use the source collection, retrieve the data objects and populate the
	 * target collection
	 * @param callInfo object
	 */
	public void cloneToMany(CallInfo callInfo) {
		if(callInfo.getStage() != ProcessingStage.UPDATE)
			return;

		Object object = ((BusinessObject) callInfo.getInput()).getInstance();
		java.util.Collection sourceCollection = null;
		Map sourceMap = null;

		if(callInfo.getInputProperty().isSet() || callInfo.getInputProperty().isList()) {
			sourceCollection = (java.util.Collection) object;

			for(Object source: sourceCollection) {
				BusinessObject target = (BusinessObject) callInfo.getOutputObjectCreator().getExistingDataObject(source);
				callInfo.getOutputProperty().addElement(((BusinessObject) callInfo.getOutput()), target.getInstance());
			}			
		} else if(callInfo.getInputProperty().isMap()) {
			sourceMap = (java.util.Map) object;

			for(Object source: sourceMap.entrySet()) {
				Object targetKey = ((Map.Entry)source).getKey();
				BusinessObject targetValue = (BusinessObject) callInfo.getOutputObjectCreator().getExistingDataObject(((Map.Entry)source).getValue());
				callInfo.getOutputProperty().addMapEntry(((BusinessObject) callInfo.getOutput()), 
						targetKey, targetValue.getInstance());
			}				
		}
	}

	protected void processBulk(CallInfo callInfo) throws Exception
	{
		List boList = null;
		if(callInfo.getParent() == null) {
			boList = ((BusinessObject)callInfo.getInput()).getBulkList(callInfo.getSettings());
		} else {
			boList = ((BusinessObject)callInfo.getInput()).getList(callInfo.getInputProperty());
		}

		CallInfo next = new CallInfo();
		for (Object nextSource : boList) {
			next.init(nextSource, null, callInfo, null);
			if(next.isCascadable()) {
				next.setOutput(getExistingTarget(next));
				if( next.getOutput() == null )
					next.setOutput(createTarget(next, null));
				processAttribute(next);
			} else {
				next.setOutput(createTarget(next, ClassUtil.getInstance(nextSource), null));
			}

			if(callInfo.isBulkInput()) {
				Object outputInstance = ((BusinessObject)next.getOutput()).getInstance();
				((List)((BusinessObject)callInfo.getOutput()).getInstance()).add(outputInstance);
			}
		}
	}

	protected void processCollection(CallInfo callInfo) throws Exception {

		if(callInfo.isDataType())
			return;

		processBulk(callInfo);
		cloneToMany(callInfo);
	}
	
}
