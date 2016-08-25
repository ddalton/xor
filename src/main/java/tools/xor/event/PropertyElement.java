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

package tools.xor.event;

import tools.xor.ExtendedProperty;
import tools.xor.ExtendedProperty.Phase;
import tools.xor.ProcessingStage;
import tools.xor.Settings;

public class PropertyElement implements PropertyEvent {

	private Object   element;
	private Phase    phase;
	private Settings settings;
	private Object   inputElement;
	private Object   inputParentElement;	
	private ProcessingStage stage;

	public PropertyElement(Settings settings, Object element, Object input, Object inputParent, Phase phase, ProcessingStage stage) {
		this.element = element;
		this.phase = phase;
		this.settings = settings;
		this.inputElement = input;
		this.inputParentElement = inputParent;
		this.stage = stage;
	}

	public Object getElement() {
		return element;
	}

	public void setElement(Object element) {
		this.element = element;
	}

	@Override
	public Phase getPhase() {
		return phase;
	}

	@Override
	public Settings getSettings() {
		return settings;
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	@Override
	public Object getOtherElement () {
		return inputElement;
	}
	
	@Override
	public Object getOtherElementParent () {
		return inputParentElement;
	}	

	@Override public ProcessingStage getStage ()
	{
		return this.stage;
	}

	public void setInputElement(Object inputElement) {
		this.inputElement = inputElement;
	}
}
