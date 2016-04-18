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

import tools.xor.BusinessObject;
import tools.xor.ExtendedProperty;
import tools.xor.ProcessingStage;
import tools.xor.Settings;
import tools.xor.event.PropertyElement;


public final class SetterAction implements Executable {

	private final Object      value;
	private final PropertyKey key;
	private final Executable  triggeringAction;
	private final Settings    settings;
	private final Object      input;

	/**
	 * NOTE: The output object in callInfo may not be the same as the key.getDataObject() - e.g., the backRef code calling this methoed
	 *       in the context of setting the bi-directional link. This is typically indicated by the fact that 
	 *       triggeringAction is not null.
	 *
	 * @param value
	 * @param key
	 * @param triggeringAction
	 * @param settings
	 * @param input
	 */
	public SetterAction(Object value, PropertyKey key, Executable triggeringAction, Settings settings, Object input) {
		this.value = value;
		this.key = key;
		this.triggeringAction = triggeringAction;
		this.settings = settings;
		this.input = input;
		
		//(new Exception()).printStackTrace();
	}

	@Override
	public void execute() {
		setValue();
	}
	
	public void setValue() {
		ExtendedProperty prop = ((ExtendedProperty)key.getProperty());
		
		BusinessObject invokeOn = key.getDataObject();
		// First check if this property is part of this data object
		if(invokeOn instanceof BusinessObject) {
			if(((BusinessObject)invokeOn).getType().getProperty(prop.getName()) == null && prop.getMapPath() != null && prop.getMapPath().contains(Settings.PATH_DELIMITER)) {
				// Get the embedded object
				invokeOn = (BusinessObject) ((BusinessObject)invokeOn).get( prop.getMapPath().substring(0, prop.getMapPath().lastIndexOf(Settings.PATH_DELIMITER)));
			}
		}
		
		setCustomValue(invokeOn);
	}		
	
	/**
	 * This method also takes case of invoking any business logic specific method
	 * @param invokeOn
	 */
	protected void setCustomValue(BusinessObject invokeOn) {

		ExtendedProperty property = ((ExtendedProperty)key.getProperty());
		
		if(property.getDataUpdater(settings, ExtendedProperty.Phase.LOGIC, ProcessingStage.UPDATE) != null) {
			property.propertyUpdate(invokeOn, new PropertyElement(settings, value, input));
		} else {
			property.setValue(invokeOn, value);	
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
		
		String result = value == null ? "null" : ((BusinessObject)value).getInstance().toString();
		
		StringBuilder str = new StringBuilder("SetterAction -> setting value [");
		str.append(invokeOn.getInstance().toString());
		str.append("] on object with type [");
		str.append(invokeOn.getType().getName());
		str.append(", " + result);
		str.append("] against property ");
		str.append(prop.getName());
		
		return str.toString();
	}
}
