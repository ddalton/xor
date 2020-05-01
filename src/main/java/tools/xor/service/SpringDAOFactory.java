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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

@Component("DAOFactory")
public class SpringDAOFactory implements DAOFactory {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	@Autowired
	protected AutowireCapableBeanFactory beanFactory;

	public <T> DAOTemplate<T> create() {
			DAOTemplate<T> result = null;		
			
			try { // JPADataAccessService
				result = new JPADAOTemplate<T>();
				beanFactory.autowireBean(result);
				return result;					
			} catch (BeanCreationException e) {
				logger.warn("JPA configuration not found, hence cannot create a JPADAOTemplate instance");
			}
			
			return null;
	}

}
