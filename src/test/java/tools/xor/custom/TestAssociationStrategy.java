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

package tools.xor.custom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.AggregateAction;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.db.base.Category;
import tools.xor.db.vo.base.CategoryVO;
import tools.xor.util.ClassUtil;
import tools.xor.util.I18NUtils;
import tools.xor.util.ObjectCreator;


public class TestAssociationStrategy extends DefaultAssociationStrategy {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	/**
	 * The strategy used to associate with an existing object based on the source information
	 * @param callInfo
	 * @throws Exception 
	 */
	public Object execute(CallInfo callInfo, ObjectCreator oc) {
		return super.execute(callInfo, oc);
	}

	/**
	 * Method that indicates if the source object should be persisted
	 * @param callInfo
	 */
	public boolean doProcess(CallInfo callInfo) {
		boolean result = super.doProcess(callInfo);

		// special processing for Category association
		if(callInfo.getOutputProperty() != null ) {
			if(Category.class.isAssignableFrom(callInfo.getOutputProperty().getType().getInstanceClass())) {
				// We also retrieve Category objects
				if(callInfo.getSettings().getAction() == AggregateAction.READ)
					return true;

				// If the associated entity is a transient Category, we need to persist it if its type is extensible
				BusinessObject target = (BusinessObject) callInfo.getOutput();

				if(target == null || !((BusinessObject)target).isPersistent()) {
					boolean isExtensible = false;
					String categoryName = null;
					String facetName = null;
					if( ((BusinessObject) callInfo.getInput()).isDomainType() ) {
						Category category = (Category) ClassUtil.getInstance(callInfo.getInput()); // We have to look at the source since this is a transient object
						if(category.getFacet() != null && category.getFacet().isExtensible())
							result = true;
						categoryName = category.getName();
						facetName = (category.getFacet() == null) ? null : category.getFacet().getName();
					} else {
						CategoryVO category = (CategoryVO) ClassUtil.getInstance(callInfo.getInput()); // We have to look at the source since this is a transient object
						if(category.getFacet() != null && category.getFacet().isExtensible())
							result = true;		
						categoryName = category.getName();
						facetName = (category.getFacet() == null) ? null : category.getFacet().getName();						
					}
					if(isExtensible)
						result = true;
					else {
						String[] params = new String[2];
						params[0] = categoryName;
						params[1] = facetName;

						// Uses users resource bundle
						logger.error(I18NUtils.getResource(  "exception.cannotAddUnextensibleCategory",I18NUtils.CORE_RESOURCES, params));
					}				
				}
			} 
		}

		return result;
	}
}
