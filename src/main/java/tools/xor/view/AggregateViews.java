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

package tools.xor.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.service.AggregateManager;

@XmlRootElement(name="AggregateViews")
public class AggregateViews {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	private Set<AggregateView> aggregateView;

	public Set<AggregateView> getAggregateView() {
		return aggregateView;
	}

	public void setAggregateView(Set<AggregateView> value) {
		this.aggregateView = value;
	}

	/**
	 * Obsolete views will be left as is, and new or updated views will be set 
	 * This is an expensive operation
	 * @param am AggregateManager
	 */
	public void sync(AggregateManager am) {

		// This can happen if the AggregateViews.xml file is not configured
		if(this.aggregateView == null) {
			return;
		}
		
		Map<String, List<AggregateView>> avVersions = new HashMap<String, List<AggregateView>>();
		for(AggregateView view: aggregateView) {
			List<AggregateView> viewVersions = avVersions.get(view.getName());
			if(viewVersions == null) {
				viewVersions = new ArrayList<>();
				avVersions.put(view.getName(), viewVersions);
			}
			viewVersions.add(view);
		}
		for(List<AggregateView> versions: avVersions.values()) {
			Collections.sort(versions);
		}
		
		// merge the new views with existing views
		am.getDataModel().sync(avVersions);
	}
	
}
