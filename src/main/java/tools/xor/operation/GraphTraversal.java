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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.AggregateAction;
import tools.xor.BusinessEdge;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.ExtendedProperty.Phase;
import tools.xor.MutableBO;
import tools.xor.ProcessingStage;
import tools.xor.Property;
import tools.xor.event.PropertyElement;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;

/**
 * Operations that work on a Graph (Java object).
 * Uses DFS (Depth First Search) to navigate the object.
 * Uses an ObjectGraph instance as the main data structure to resolve the object based on either the StateGraph or StateTree scope.
 * 
 * @author Dilip Dalton
 *
 */
public abstract class GraphTraversal extends AbstractOperation {
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

	@Override
	public void execute(CallInfo callInfo) {
		
		// Create new objects
		try {
			if (supportsCreate(callInfo)) {
				owLogger.debug("Processing state: " + ProcessingStage.CREATE);
				this.process(callInfo.setStage(ProcessingStage.CREATE));
				callInfo.clearVisitedOutputs();
			}

			// Step 1: Create the update actions
			if (supportsUpdate(callInfo)) {
				setVisited(callInfo, true);
				owLogger.debug("Processing state: " + ProcessingStage.UPDATE);
				this.process(callInfo.setStage(ProcessingStage.UPDATE));
				callInfo.clearVisitedOutputs();

				// Step 2: Execute the actions
				if (callInfo.getOutputRoot().getObjectPersister() != null) {
					callInfo.getOutputRoot().getObjectPersister().processActions(callInfo.getSettings());
				}
			}

			// Remove the dummy root node
			if (callInfo.isBulkInput()) {
				callInfo.getOutputObjectCreator().unregister((BusinessObject)callInfo.getInput());
				callInfo.getOutputObjectCreator().unregister((BusinessObject)callInfo.getOutput());
			}

			// Now we can persist the objects as all the links are set
			// This might be necessary if the deferred and post logic actions if any
			// are dependent on the identifier of newly created objects
			if (callInfo.getSettings().doPersist()) {
				persist(callInfo);
			}

			// Process the open property actions that were deferred
			if (callInfo.getOutputRoot().getObjectPersister() != null) {
				callInfo.getOutputRoot().getObjectPersister().processOpenPropertyActions(callInfo.getSettings());
			}

			// Process post actions
			// Enable only if necessary for performance reasons
			if (supportsPostLogic(callInfo)) {
				owLogger.debug("Processing state: " + ProcessingStage.POSTLOGIC);
				this.process(callInfo.setStage(ProcessingStage.POSTLOGIC));
				callInfo.clearVisitedOutputs();
			}

			setResult(callInfo.getOutput());
		} catch(Exception e) {
			owLogger.error(e.getMessage());
			throw e;
		}
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
					createElements(callInfo);
				} else {

					// If this is a CREATE action on a persistent instance (loaded from DB),
					// then we don't modify it.
					// Only UPDATE action can modify existing persistent instances.
					if(callInfo.getSettings().getAction() == AggregateAction.CREATE &&
						target.isPersistent()) {
						return;
					}

					// Instances of States that are explicitly marked as reference cannot be
					// updated
					if (callInfo.getStage() == ProcessingStage.UPDATE
						&& callInfo.isReference(source.getType())) {
						return;
					}

					List<Property> properties = callInfo.getProperties(source.getType());
					List<Property> nonKeyProperties = new ArrayList<Property>();
					for (Property sourceProperty : properties) {
						CallInfo next = new CallInfo();
						// Properties comprising the natural key are processed first
						// as we need this information to form EntityKey using userKey
						next.initOperation(this, null, callInfo, (ExtendedProperty)sourceProperty);
						if( ((ExtendedProperty)sourceProperty).isPartOfNaturalKey() ) {
							processAttribute(next);
						} else {
							nonKeyProperties.add(sourceProperty);
						}
					}

					// Register the object now, so property references can find this object
					if (target.getType() instanceof EntityType) {
						EntityType entityType = (EntityType)target.getType();
						if (!entityType.isEmbedded() && target.getInstance() != null &&
							callInfo.getStage() == ProcessingStage.CREATE) {

							// Register the object
							// If there are multiple input objects with the same key,
							// then this step does the de-duplication
							BusinessObject existing = target.getObjectCreator().register(target, callInfo.getInput(), true, null);
							if(existing != target && existing instanceof MutableBO) {

								// If the existing object is not a reference then it is the
								// actual object, so stop processing this object further
								if( !((MutableBO)existing).isReference()) {
									return;
								}
							}

							// We don't return at this point since this object may have
							// reference to other new objects, that also need to be
							// registered.
						}
					}

					// Process the property references
					for (Property sourceProperty : nonKeyProperties) {
						CallInfo next = new CallInfo();
						next.initOperation(
							this,
							null,
							callInfo,
							(ExtendedProperty)sourceProperty);
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

	public String getDebugInput(Object inputValue) {
		/* This can throw a StackOverflowError
		 *if(inputValue.toString().length() > Constants.Log.DEBUG_DATA_SIZE) {
		 *	return inputValue.toString().substring(0, Constants.Log.DEBUG_DATA_SIZE) + "...";
		 *}
		 * return inputValue.toString();
		 */
		 return inputValue.getClass().getName();
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

		// Read only properties should not be modified
		if (ci.getOutputProperty() != null && ci.getOutputProperty().isReadOnly() && !(
			ci.getSettings().getAction() == AggregateAction.READ ||
				ci.getSettings().getAction() == AggregateAction.LOAD ||
				ci.getSettings().getAction() == AggregateAction.TO_EXTERNAL
		)) {
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
		if(isIdentifier(ci)) {
			return;
		}

		// If the property to be copied is specified then we perform the copy only for that property
		if(ci.getInputProperty() != null) { // Owner is not a collection

			// Simply copy the value
			if(ci.isDataType()) {
				try {
					processDataType(ci);
				} catch(Exception e) {
					// For a migrate operation we tolerate errors as we do a best effort migrate
					// since this can be due to validation checks in the business logic
					// As the migrate by design is not reliable we have to go with best effort
					// If an exception is desired then we can control it with a parameter
					if(ci.getSettings().getMainAction() != AggregateAction.MIGRATE) {
						throw e;
					}
				}
				return;
			}

			// Set the output value if one already exists for that input
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
			BusinessObject invokee = ci.getParentOutputEntity();
			if(invokee != null && invokee.getObjectCreator().getObjectGraph() != null && ci.getStage() == ProcessingStage.UPDATE) {
				BusinessObject value = (BusinessObject) ci.getOutput();
				BusinessEdge<BusinessObject> edge = new BusinessEdge<BusinessObject>(invokee, value, ci.getOutputProperty());
				invokee.getObjectCreator().getObjectGraph().addEdge(edge, invokee, value);
			}
		}

		if(!((BusinessObject)ci.getOutput()).isVisited()) {
			// If this a reference association object, then we don't want to mark it as
			// processed in case the real object has not yet been processed
			if( !((BusinessObject)ci.getInput()).isReference() ) {
				setVisited(ci, true);
				postVisited(ci);
			}
		} else {
			return;
		}

		if(ci.getInputProperty() != null && ci.getInputProperty().isMany()) { // Process the collection/map
			processToMany(ci);		
		} else {
			process(ci);
		}
		
		// Rebuild an immutable object that is now fully populated by the builder
		//rebuildImmutable(ci);

		executeDataUpdate(ci, Phase.POST);
	}	
	
	protected void postVisited(CallInfo ci) {
	}
	
	private void setVisited(CallInfo callInfo, boolean value) {
		((BusinessObject)callInfo.getOutput()).setVisited(true); // marked nodes we processed
		((BusinessObject)callInfo.getInput()).setVisited(true); // This flag on the source is used by the conflict resolution logic		
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

	protected boolean shouldUpdate (CallInfo ci)
	{
		/*
		 * We update only during two times
		 * 1. During CREATE stage, we update only the natural keys
		 * 2. During UPDATE stage
		 */
		return ci.getStage() == ProcessingStage.UPDATE || (ci.getStage() == ProcessingStage.CREATE
			&& ci.getInputProperty() != null && ci.getInputProperty().isPartOfNaturalKey());
	}

	protected Object setPropertyTarget(CallInfo ci, Object propertyTarget) {
		/*
		// Set only if updating
		if(ci.getStage() != ProcessingStage.UPDATE)
			return propertyTarget;
			*/
		if(!shouldUpdate(ci)) {
			return propertyTarget;
		}

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

		// Baseline objects need to have their id populated
		if(ci.getSettings().doBaseline()) {
			return false;
		}

		if(ci.getInputProperty().getName().equals( sourceType.getIdentifierProperty().getName()) )
			return true;

		return false;
	}

	protected void processNullValue(CallInfo ci) throws Exception {
		// Do not modify if not updating it
		if(ci.getStage() != ProcessingStage.UPDATE) {
			return;
		}

		// Set the null value - unlink the relationship from this side	
		ci.linkOutputToParent(null);				
	}	


	/**
	 * The Data objects for the source collection should have been created
	 * So use the source collection, retrieve the data objects and populate the
	 * target collection
	 * @param callInfo object
	 * @param elements the elements dataobjects that need to be added to the collection
	 */
	private void addElements (CallInfo callInfo, List<CallInfo> elements) {
		if(callInfo.getStage() != ProcessingStage.UPDATE) {
			return;
		}

		if (callInfo.getInputProperty().isSet() || callInfo.getInputProperty().isList()) {
			for (CallInfo ci : elements) {
				Object element = ClassUtil.getInstance(ci.getOutput());
				callInfo.getOutputProperty().addElement(
					((BusinessObject)callInfo.getOutput()),
					element);
			}
		} else if(callInfo.getInputProperty().isMap()) {
			Map sourceMap = (java.util.Map)((BusinessObject)callInfo.getInput()).getInstance();

			for(Object source: sourceMap.entrySet()) {
				Object targetKey = ((Map.Entry)source).getKey();
				BusinessObject targetValue = callInfo.getOutputObjectCreator().getExistingDataObject(((Map.Entry)source).getValue());
				callInfo.getOutputProperty().addMapEntry(callInfo.getOutput(),
						targetKey, targetValue.getInstance());
			}				
		}
	}

	protected List<CallInfo> createElements (CallInfo callInfo) throws Exception
	{
		List boList = null;
		if(callInfo.getParent() == null) {
			boList = ((BusinessObject)callInfo.getInput()).getBulkList(callInfo.getSettings());
		} else {
			boList = ((BusinessObject)callInfo.getInput()).getList(callInfo.getInputProperty());
		}

		List<CallInfo> collectionCallFrames = new ArrayList<>();
		for (Object nextSource : boList) {
			CallInfo next = new CallInfo();
			next.init(nextSource, null, callInfo, null);
			if(next.isCascadable()) {
				next.setOutput(getExistingTarget(next));
				if( next.getOutput() == null )
					next.setOutput(createTarget(next, null));
				processAttribute(next);
			} else {
				next.setOutput(createTarget(next, ClassUtil.getInstance(nextSource), null));
			}

			if( callInfo.getStage() == ProcessingStage.UPDATE) {
				if (callInfo.isBulkInput()) {
					Object outputInstance = ((BusinessObject)next.getOutput()).getInstance();
					((List)((BusinessObject)callInfo.getOutput()).getInstance()).add(outputInstance);
				}
				else {
					collectionCallFrames.add(next);
				}
			}
		}

		return collectionCallFrames;
	}

	protected void processCollection(CallInfo callInfo) throws Exception {

		if(callInfo.isDataType())
			return;

		addElements(callInfo, createElements(callInfo));
	}
}
