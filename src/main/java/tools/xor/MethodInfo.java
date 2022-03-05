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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import tools.xor.ExtendedProperty.Phase;
import tools.xor.util.ClassUtil;


public class MethodInfo extends VersionInfo {

	private final Method method;
	private final boolean capture;
	private final AggregateAction[] actions;
	private final String[] tags;
	private final Phase phase;
	private final ProcessingStage stage;
	
	public MethodInfo(int order, int fromVersion, int untilVersion, Method method, boolean capture, AggregateAction[] actions, String[] tags, Phase phase, ProcessingStage stage) {
		super(fromVersion, untilVersion);
		
		this.capture = capture;
		this.method = method;
		
		if(actions == null || Arrays.asList(actions).size() == 0) {
			this.actions = AggregateAction.values();
		} else {
			this.actions = actions;
		}
		
		if(tags == null || Arrays.asList(tags).size() == 0) {
			this.tags = new String[] { AbstractProperty.EMPTY_TAG };
		} else {
			this.tags = tags;
		}
		
		if(phase == null) {
			this.phase = Phase.POST;
		} else {
			this.phase = phase;
		}
		
		if(this.getUntilVersion() < this.getFromVersion()) {
			throw new RuntimeException("untilVersion cannot be less than the fromVersion");
		}

		if(stage == null) {
			this.stage = ProcessingStage.UPDATE;
		} else {
			this.stage = stage;
		}
	}
	
	public boolean intersectsActions(AggregateAction[] otherActions) {
		Set<AggregateAction> commonActions = new HashSet<AggregateAction>(Arrays.asList(actions));
		commonActions.retainAll(new HashSet<AggregateAction>(Arrays.asList(otherActions)) );
		if(commonActions.isEmpty()) {
			// applies to different action so they do not overlap
			return false;
		}
		
		return true;
	}
	
	public boolean doOverlap(MethodInfo method) {
		if(!ClassUtil.intersectsTags(tags, method.tags)) {
			return false;
		}
		
		if(!intersectsActions(method.actions)) {
			return false;
		}
		
		if(getFromVersion() < method.getFromVersion() && getUntilVersion() >= method.getFromVersion()) {
			return true;
		}
		
		if(getFromVersion() >= method.getFromVersion() && getFromVersion() <= method.getUntilVersion()) {
			return true;
		}
		
		return false;
	}
	
	public boolean isRelevant(Settings settings) {
		return isRelevant(settings, settings.getTags().toArray(new String[settings.getTags().size()]), Phase.POST, null);
	}
	
	public boolean isRelevant(Settings settings, String[] tags, Phase phase, ProcessingStage stage) {
		if(!ClassUtil.intersectsTags(this.tags, tags)) {
			return false;
		}
		
		if(!intersectsActions(new AggregateAction[] { settings.getAction() })) {
			return false;
		}
		
		// This method is no longer supported for the current version
		if(settings.getCurrentApiVersion() > getUntilVersion()) {
			return false;
		}
		
		// This method was introduced later than the current version
		if(settings.getCurrentApiVersion() < getFromVersion()) {
			return false;
		}
		
		if(phase != this.phase) {
			return false;
		}

		if (stage != null && this.stage != stage) {
			return false;
		}
		
		return true;
	}
	

	public Method getMethod() {
		return method;
	}

	public boolean isCapture() {
		return capture;
	}

	public AggregateAction[] getActions() {
		return actions;
	}

	public String[] getTags() {
		return tags;
	}

}
