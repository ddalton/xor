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

package tools.xor.action;

import java.util.List;

import tools.xor.AbstractProperty;
import tools.xor.BusinessObject;
import tools.xor.ExtendedProperty;
import tools.xor.ExtendedProperty.Phase;
import tools.xor.MethodInfo;
import tools.xor.ProcessingStage;
import tools.xor.Settings;
import tools.xor.event.PropertyElement;


public final class SetterAction implements Executable {

	private final Settings    settings;
	private final Object      value;
	private final PropertyKey key;
	private final Executable  triggeringAction;

	/**
	 * NOTE: The output object in callInfo may not be the same as the key.getDataObject() - e.g., the backRef code calling this methoed
	 *       in the context of setting the bi-directional link. This is typically indicated by the fact that 
	 *       triggeringAction is not null.
	 *
	 * @param settings user provided settings
	 * @param value the new value
	 * @param key property details
	 * @param triggeringAction action
	 */
	public SetterAction(Settings settings, Object value, PropertyKey key, Executable triggeringAction) {
		this.settings = settings;
		this.value = value;
		this.key = key;
		this.triggeringAction = triggeringAction;
	}

	@Override
	public void execute() {
		setValue();
	}
	
	public void setValue() {
		ExtendedProperty prop = ((ExtendedProperty)key.getProperty());
		
		BusinessObject invokee = key.getDataObject();
		// First check if this property is part of this data object
		if(invokee instanceof BusinessObject) {
			if(((BusinessObject)invokee).getType().getProperty(prop.getName()) == null && prop.getMapPath() != null && prop.getMapPath().contains(Settings.PATH_DELIMITER)) {
				// Get the embedded object
				invokee = (BusinessObject) ((BusinessObject)invokee).get( prop.getMapPath().substring(0, prop.getMapPath().lastIndexOf(Settings.PATH_DELIMITER)));
			}
		}
		
		setCustomValue(invokee);
	}		
	
	protected void setCustomValue(BusinessObject invokee) {
		ExtendedProperty property = (ExtendedProperty)key.getProperty();
		
		Phase phase = Phase.INPLACEOF;
		ProcessingStage stage = ProcessingStage.UPDATE;
		String[] tags = {AbstractProperty.SETTER_TAG};
		
		List<MethodInfo> customGetter = property.getLambdas(settings, tags, phase, stage);
		if(customGetter != null && customGetter.size() > 0) {
			property.evaluateLambda(
					new PropertyElement(
						settings,
						value,
						tags,
						phase,
						stage)).getResult();				
		} else {
			property.setValue(invokee, value);
		}
	}

	@Override
	public PropertyKey getKey() {
		return key;
	}

	@Override
	public Executable getTriggeringAction() {
		return triggeringAction;
	}	

	public Object getValue() {
		return value;
	}		
	
	@Override
	public String toString() {
		BusinessObject invokeOn = key.getDataObject();
		ExtendedProperty prop = ((ExtendedProperty)key.getProperty());
		
		String result = value == null ? "null" : value.toString();
		
		StringBuilder str = new StringBuilder("SetterAction -> setting value [");
		str.append(invokeOn.toString());
		str.append("] on object with type [");
		str.append(invokeOn.getType().getName());
		str.append(", " + result);
		str.append("] against property ");
		str.append(prop.getName());
		
		return str.toString();
	}
}
