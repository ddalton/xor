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

package tools.xor.service;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;

import tools.xor.util.ClassUtil;

@Alternative
public class CDIDataModelFactory extends AbstractDataModelFactory {
	
	@Inject private BeanManager beanManager;

	@Override
	public void injectDependencies(Object bean, String name) {
	    InjectionTarget injectionTarget = beanManager.createInjectionTarget(beanManager.createAnnotatedType(ClassUtil.getUnEnhanced(bean.getClass())));		
	    injectionTarget.inject(bean, beanManager.createCreationalContext(null));
	    injectionTarget.postConstruct(bean);
	}
}
