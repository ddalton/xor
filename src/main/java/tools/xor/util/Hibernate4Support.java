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

package tools.xor.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Property;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

public class Hibernate4Support implements HibernateSupport {
	
	@Override
	public CollectionPersister getCollectionPersister(SessionFactory sessionFactory, CollectionType collType) {
		return ((SessionFactoryImplementor) sessionFactory).getCollectionPersister( collType.getRole() );
	}

	@Override
	public boolean isCascaded(Configuration configuration, String entityName, String inputPropertyName) {
		
		if(configuration.getClassMapping(entityName) == null)
			return false;

		Property property = configuration.getClassMapping(entityName).getProperty(inputPropertyName);
		CascadeStyle style = property.getCascadeStyle();

		return style.doCascade(CascadingActions.SAVE_UPDATE) 
				|| style.doCascade(CascadingActions.PERSIST);
	}

	@Override
	public Type getEntityType(SessionFactory sessionFactory, String entityName) {
		return sessionFactory.getTypeHelper().entity(entityName);
	}

	@Override
	public Session getCurrentSession(SessionFactory sessionFactory) {
		return sessionFactory.getCurrentSession();
	}		
}
