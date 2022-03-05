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

import tools.xor.BusinessObject;
import tools.xor.ExtendedProperty.Phase;
import tools.xor.ProcessingStage;
import tools.xor.Settings;

public class PropertyElement implements PropertyEvent {

	private Settings settings;
	private Object domain;	
	private Object external;
	private BusinessObject domainParent;
	private BusinessObject externalParent;
	private String[] tags;
	private Phase    phase;	
	private ProcessingStage stage;
	private Object value; // Will be injected as XorResult

	public PropertyElement(Settings settings, Object value, String[] tags, Phase phase, ProcessingStage stage) {
		this.settings = settings;
		this.value = value;
		this.tags = tags;
		this.phase = phase;
		this.stage = stage;
	}

	public PropertyElement(Settings settings, Object domain, Object external, BusinessObject domainParent, BusinessObject externalParent, String[] tags, Phase phase, ProcessingStage stage) {
		this.domain = domain;
		this.settings = settings;
		this.external = external;
		this.domainParent = domainParent;
		this.externalParent = externalParent;
		this.tags = tags;
		this.phase = phase;
		this.stage = stage;
	}

	public Object getDomain() {
		return domain;
	}

	public void setElement(Object element) {
		this.domain = element;
	}
	
	public Object getExternal() {
		return external;
	}

	public void setExternal(Object element) {
		this.external = element;
	}	

	public String[] getTags() {
		return tags;
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
	public BusinessObject getDomainParent () {
		return domainParent;
	}
	
	@Override
	public BusinessObject getExternalParent () {
		return externalParent;
	}		

	@Override public ProcessingStage getStage ()
	{
		return this.stage;
	}
	
	@Override
	public Object getValue() {
		return value;
	}
}
