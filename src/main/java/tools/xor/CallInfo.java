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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.ExtendedProperty.Phase;
import tools.xor.event.PropertyElement;
import tools.xor.operation.Operation;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.Edge;
import tools.xor.util.ObjectCreator;
import tools.xor.util.State;
import tools.xor.util.graph.StateGraph;
import tools.xor.util.graph.StateTree;
import tools.xor.util.graph.TypeGraph;

public class CallInfo {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	private static final int START_DEPTH = 0;	

	private Object           input;
	private Object           output;
	private ExtendedProperty inputProperty;
	private CallInfo         parent;
	private Settings         settings;	
	private int              depth;               // The current depth within the object graph, starting from the aggregate roo
	private Operation        operation;
	private ObjectCreator    inputObjectCreator;  // Ensure we are not sharing objects between the two object creators 	
	private ObjectCreator    outputObjectCreator; // needed to create the root target data object
	private ProcessingStage  stage;

	// attributes for optimization
	private String           propertyPath;
	private State            currentState;
	
	public CallInfo() {
		// Empty constructor, needs to be followed with an init call
	}

	public CallInfo(Object from, Object to, CallInfo parent, ExtendedProperty property) {

		this.init(from, to, parent, property);		
	}
	
	public void init(Object from, Object to, CallInfo parent, ExtendedProperty property) {

		this.input = from;
		this.output = to;
		this.parent = parent;
		this.inputProperty = property;	
		this.propertyPath = null;
		
		if(parent != null) {
				operation = parent.getOperation();
				settings = parent.getSettings();
				stage = parent.getStage();

				this.outputObjectCreator = parent.getOutputObjectCreator();
				this.inputObjectCreator = parent.getInputObjectCreator();		

				// If property is not null, then it should not be toMany
				if(property == null || !property.isMany()) {
					this.depth = parent.depth+1;
				}

		} else {
			this.inputObjectCreator = ((BusinessObject)from).getObjectCreator();
			this.depth = START_DEPTH;		
		}

		validate();			
	}
	
	public void initOperation(Operation operation, Object to, CallInfo parent, ExtendedProperty property) {
		init(null, to, parent, property);

		// The order of the below 2 lines is important. We need to set input before
		// we invoke isDataType()
		this.input = getInputFromParent(operation);
		if(!isDataType()) {
			BusinessObject parentSource = (BusinessObject) getParent().getInput();
			BusinessObject sourceRootDO = (BusinessObject) parentSource.getRootObject();

			Object sourceInstance = this.input;
			this.input = (sourceInstance == null) ? null : sourceRootDO.getObjectCreator().getExistingDataObject(sourceInstance);

			if(this.input != null && ( ((BusinessObject)input).getContainmentProperty() == null || !((BusinessObject)input).getContainmentProperty().isContainment())) {
				if(property.isContainment()) {
					((BusinessObject)input).setContainer(parentSource);
					((BusinessObject)input).setContainmentProperty(property);
				}
			}

			if(sourceInstance != null && input == null) {
				// This is probably not a containment data object so explicitly create one
				this.input = sourceRootDO.getObjectCreator().createDataObject(sourceInstance, property.getType(), parentSource, property);				
			}
		}

		validate();		
	}
	
	public String getIndentString() {
		String propertyPath = getInputPropertyPath();
		
		int depth;
		if(propertyPath == null || "".equals(propertyPath.trim())) {
			depth = 0;
		}
		
		if(propertyPath.indexOf(Settings.PATH_DELIMITER) == -1) {
			depth = 1;
		} else {
			depth = propertyPath.split(Settings.PATH_DELIMITER_REGEX).length;
		}
		
		//return (depth > 0) ? (new String(new char[depth]).replace("\0", Constants.Format.INDENT_STRING)) : "";
		return Constants.Format.getIndentString(depth);
	}	

	public Object getInput() {
		return input;
	}

	public void setInput(Object from) {
		this.input = from;
		validate();
	}

	public Object getOutput() {
		return output;
	}

	public void setOutput(Object to) {
		this.output = to;
		validate();		
	}

	public ExtendedProperty getInputProperty() {
		return inputProperty;
	}

	public BusinessObject getParentOutputEntity ()
	{
		return parent == null ? null : (BusinessObject)parent.getOutput();
	}
	
	public BusinessObject getParentInputEntity() {
		return parent == null ? null : (BusinessObject)parent.getInput();
	}

	public boolean isBulkInput ()
	{
		return getParent() == null && (getInput() instanceof BusinessObject
			&& ((BusinessObject)getInput()).getType() instanceof ListType);
	}

	public CallInfo getParent() {
		return parent;
	}

	public int getDepth() {
		return depth;
	}

	public void setParent(CallInfo parent) {
		this.parent = parent;
	}

	public Settings getSettings() {
		return settings;
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	protected void validate() {
		if(!isDataType()) {
			/*
			if(this.input != null && !BusinessObject.class.isAssignableFrom(this.input.getClass()))
				throw new IllegalStateException("Source should be a data object but is not treated as one");
			if(this.output != null && !BusinessObject.class.isAssignableFrom(this.output.getClass()))
				throw new IllegalStateException("Target should be a data object but is not treated as one");
			*/
			if(this.input != null && !(this.input instanceof BusinessObject))
				throw new IllegalStateException("Source should be a data object but is not treated as one");
			if(this.output != null && !(this.output instanceof BusinessObject))
				throw new IllegalStateException("Target should be a data object but is not treated as one");
		} else {
			if(getInputProperty() == null || !getInputProperty().isMany()) {
				if (this.input != null && (this.input instanceof BusinessObject))
					throw new IllegalStateException(
						"Source should NOT be a data object but is treated as one");
				if (this.output != null && (this.output instanceof BusinessObject))
					throw new IllegalStateException(
						"Target should NOT be a data object but is treated as one");
			}
		}		
	}

	public boolean isCascadable() {
		return AbstractProperty.isCascadable(inputProperty);
	}

	public boolean isRequired() {
		return AbstractProperty.isRequired(getInputProperty());	
	}

	public boolean isCollection() {
		return AbstractProperty.isCollection(getInputProperty());
	}

	public boolean isDataType() {
		if(getInputProperty() == null) {
			// This can be null for a collection element, so check the parent
			if(getParent() != null && getParent().getInputProperty() != null && getParent().getInputProperty().getElementType() != null) {
				boolean result = getParent().getInputProperty().getElementType().isDataType();
				return (result) ? getParent().getInputProperty().getElementType().isDataType(input) : false;
			}

			// Using instanceof for performance reasons
			if(input != null)
				return ! (input instanceof BusinessObject);
			if(output != null)
				return ! (output instanceof BusinessObject);

		} else {
			boolean result = getInputProperty().isDataType();
			return (result) ? getInputProperty().getType().isDataType(input) : false;
		}

		return true;
	}
	
	public EntityType getDomainType(EntityType entityType) {
	    TypeMapper typeMapper = null;
	    if(outputObjectCreator != null) {
	        typeMapper = outputObjectCreator.getTypeMapper();
	    } else if(inputObjectCreator != null) {
	        typeMapper = inputObjectCreator.getTypeMapper();
	    }
	    
	    return (EntityType) typeMapper.getDomainShape().getType(entityType.getEntityName());
	}

	private State getCurrentState() {
		if(currentState == null) {
			EntityType entityType = (EntityType)settings.getEntityType();
			TypeGraph sg = settings.getView().getTypeGraph(getDomainType(entityType), settings.getScope());
			if(getParent() == null || getParent().isBulkInput()) {
				currentState = sg.getRootState();
			} else {
				Property property = getInputProperty();
				if(property != null) {
					Edge edge = sg.getOutEdge(getParent().getCurrentState(), property.getName());
					currentState = (State)edge.getEnd();
				} else if(getParent().getParent() != null) {
					property = getParent().getInputProperty();
					if(property != null) {
						Edge edge = sg.getOutEdge(
							getParent().getParent().getCurrentState(),
							property.getName());
						currentState = (State)edge.getEnd();
					} else {
						throw new RuntimeException("Unable to locate property");
					}
				}
			}
		}

		return currentState;
	}

	private void checkView()
	{
		if(settings.getView() == null) {
			throw new RuntimeException("View is not set in settings. Ensure that settings.init() is called");
		}
	}

	public boolean isReference(Type type)
	{
		checkView();

		if(getSettings().getScope() == StateGraph.Scope.EDGE) {
			return getCurrentState().isReference();
		}
		else {
			EntityType entityType = (EntityType)settings.getEntityType();
			TypeGraph sg = settings.getView().getTypeGraph(getDomainType(entityType));

			State state = sg.getVertex(getDomainType((EntityType)type));
			return state.isReference();
		}
	}
	
	public List<Property> getProperties(Type type) {
		checkView();

		EntityType entityType = (EntityType)settings.getEntityType();
		TypeGraph sg = settings.getView().getTypeGraph(getDomainType(entityType), settings.getScope());

		if(logger.isDebugEnabled()) {
			logger.debug("Type: " + getOutputRoot().getType().getName() + ", view: " 
					+ settings.getView().getName() 
					+ " type: " + type.getName()
					+ " domain type: " + getDomainType((EntityType)type).getName());
			logger.debug("State graph is " + ( (sg==null) ? "NOT":"") + " present");
		}

		if(!settings.getView().isExpanded()) {
			settings.getView().expand();
		}

		List<Property> exactProperties = null;

		if(getSettings().getScope() == StateGraph.Scope.EDGE) {
			State state = getCurrentState();
			if (settings.getAction() == AggregateAction.READ) {
				Object obj = ClassUtil.getInstance(getInput());
				if (obj != null) {
					EntityType instanceType = (EntityType)getInputObjectCreator().getShape().getType(
						obj.getClass());

					// Downcast if possible
					State subtypeState = ((StateTree.SubtypeState)state).findState(instanceType);
					if(subtypeState != null) {
						state = subtypeState;
					}
				}
			}

			// The state graph has full blown attributes for the type
			// We need to get only the exact properties
			exactProperties = sg.next(
				state,
				getInputPropertyPath(),
				settings.getView().getExactAttributes());
		} else {
			exactProperties = sg.next(
				getDomainType((EntityType)type),
				getInputPropertyPath(),
				settings.getView().getExactAttributes());
		}
		
		// Get the external property instances, if type is in external form
		if(!((AbstractType)type).isDomainType()) {
			exactProperties = ((AbstractType)type).getProperties(exactProperties);
		}

		// Prune attributes
		if(settings.hasPrunedAssociations()) {
			List<Property> pruned = new ArrayList<>();
			for(Property p: exactProperties) {
				if(!settings.shouldPrune(p.getName())) {
					pruned.add(p);
				}
			}
			exactProperties = pruned;
		}

		if(settings.getView().getRegexAttributes() != null) {
			// Get the RegEx properties
			Set<String> isIncluded = new HashSet<>();
			for (Property p : exactProperties) {
				isIncluded.add(p.getName());
			}
			for (Property p : type.getProperties()) {
				// Is the property already included
				if (isIncluded.contains(p.getName()) || settings.shouldPrune(p.getName())) {
					continue;
				}

				// Evaluate the regex
				String pPath = (getInputPropertyPath() == null || "".equals(getInputPropertyPath())) ?
					p.getName() :
					(getInputPropertyPath() + Settings.PATH_DELIMITER + p.getName());
				if (settings.getView().matches(pPath)) {
					exactProperties.add(p);
				}
			}
		}

		return exactProperties;
	}

	public String getInputPropertyPath() {
		
		if(propertyPath == null) {
			
			propertyPath = (inputProperty != null) ? inputProperty.getName() : "";
			if(this.getParent() != null) {
				String parentPath = this.getParent().getInputPropertyPath();
				if(parentPath != null && !"".equals(parentPath.trim())) {
					if(propertyPath != null && !"".equals(propertyPath.trim())) {
						propertyPath = parentPath + Settings.PATH_DELIMITER + propertyPath;
					} else {
						propertyPath = parentPath;
					}
				}
			}
		}

		return propertyPath;


		/*
		 * We cannot look at the object path since the same object might be referenced from multiple places
		 *
		BusinessObject targetDO = (BusinessObject) getTarget();
		return targetDO.getObjectPath();
		 */
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;

		if(outputObjectCreator == null || inputObjectCreator == null)
			throw new IllegalStateException("The source and target object creators need to be set in order to create the root target object");

		if(output != null)
			throw new IllegalStateException("The root target data object needs to be created before using the operation object");
	}

	public void linkOutputToParent(Object targetValue) {
		EntityType targetType = (EntityType) ((BusinessObject) getParent().getOutput()).getType();
		ExtendedProperty targetProperty = (ExtendedProperty) targetType.getProperty(getInputProperty().getName());
		targetProperty.setValue((BusinessObject)getParent().getOutput(), targetValue);
	}

	public Object getOutputFromParent(Settings settings) {

		EntityType targetType = (EntityType) ((BusinessObject) getParent().getOutput()).getType();
		ExtendedProperty targetProperty = (ExtendedProperty) targetType.getProperty(getInputProperty().getName());
		if(targetProperty == null) {
			logger.warn("Property " + getInputProperty().getName() + " is missing from the type " + targetType.getName());
			return null;
		}
		return targetProperty.getValue((BusinessObject)getParent().getOutput());
	}

	public ExtendedProperty getOutputProperty() {
		if(getInputProperty() == null)
			return null;

		Type targetType = ((BusinessObject) getParent().getOutput()).getType();
		ExtendedProperty targetProperty = (ExtendedProperty) targetType.getProperty(getInputProperty().getName());

		return targetProperty;
	}
	
	protected Object readCustomValue(Operation operation, BusinessObject invokee, ExtendedProperty property) {
		Phase phase = Phase.INPLACEOF;
		ProcessingStage stage = ProcessingStage.UPDATE;
		String[] tags = {AbstractProperty.GETTER_TAG};
		
		List<MethodInfo> customGetter = property.getLambdas(settings, tags, phase, stage);
		if(customGetter != null && customGetter.size() > 0) {
			return property.evaluateLambda(
					new PropertyElement(
						settings,
						operation.getDomain(this),
						operation.getExternal(this),
						operation.getDomainParent(this),
						operation.getExternalParent(this),
						tags,
						phase,
						stage)).getResult();				
		} else {
			return property.getValue(invokee);
		}
	}

	public Object getInputFromParent(Operation operation) {
		return readCustomValue(operation, (BusinessObject) getParent().getInput(), (ExtendedProperty)getInputProperty());
		
		//ExtendedProperty p = (ExtendedProperty)getInputProperty();
		//return p.getValue( (BusinessObject) getSupertype().getInput());
	}

	public BusinessObject getOutputRoot() {
		if(getParent() == null) {
			if(getOutput() == null)
				throw new IllegalStateException("Target root is null, it needs to be created first");

			if(!BusinessObject.class.isAssignableFrom(getOutput().getClass()))
				throw new IllegalStateException("Target root needs to be a data object");

			if ( !((BusinessObject)getOutput()).isRoot() )
				throw new IllegalStateException("Target with null parent needs to be the root data object");

			return (BusinessObject)getOutput();
		} else
			return getParent().getOutputRoot();
	}

	public BusinessObject getInputRoot() {
		if(getParent() == null) {
			if(getInput() == null)
				throw new IllegalStateException("Source root is null, set the source before calling this method.");

			if(!BusinessObject.class.isAssignableFrom(getInput().getClass()))
				throw new IllegalStateException("Source root needs to be a data object");

			if ( !((BusinessObject)getInput()).isRoot() )
				throw new IllegalStateException("Source with null parent needs to be the root data object");

			return (BusinessObject)getInput();
		} else
			return getParent().getInputRoot();

	}

	public void setOutputObjectCreator(ObjectCreator oc) {
		this.outputObjectCreator = oc;
	}

	public ObjectCreator getOutputObjectCreator() {
		return this.outputObjectCreator;
	}

	public ProcessingStage getStage() {
		return stage;
	}

	public CallInfo setStage(ProcessingStage stage) {
		this.stage = stage;
		return this;
	}

	public boolean isPathSuffix(String pathSuffix) {
		// Walk through the parent chain to see that the path meets the pathSuffix
		if(getInputPropertyPath().endsWith(pathSuffix))
			return true;
		else
			return false;
	}

	public boolean isExternal() {
		boolean result = !this.isCollection() && !this.isCascadable();
		
		// We can explicitly tag an association as not external
		if(result) {
			for(AssociationSetting setting: settings.getExpandedAssociations()) {
				if(setting.isAggregatePart(this)) {
					result = false;
					break;
				}
			}
		}
		
		return result;
	}

	public ObjectCreator getInputObjectCreator() {
		return inputObjectCreator;
	}

	public void setInputObjectCreator(ObjectCreator sourceObjectCreator) {
		this.inputObjectCreator = sourceObjectCreator;
	}

	public void clearVisitedOutputs() {
		getOutputObjectCreator().clearVisited();
	}
}
