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

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;
import org.jboss.jandex.UnsupportedVersion;

public class HibernateUtil {

	private static SessionFactory sessionFactory;
	private static Configuration  configuration;

	static
	{
		try
		{
			configuration = new Configuration();
			configuration.configure("hibernate.cfg.xml");
			StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
			sessionFactory = configuration.buildSessionFactory(ssrb.build());

		}
		catch (HibernateException he)
		{
			System.err.println("Error creating Session: " + he);
			throw new ExceptionInInitializerError(he);
		}
	}

	public static SessionFactory getSessionFactory()
	{
		return sessionFactory;
	} 
	
	public static Configuration getConfiguration()
	{
		return configuration;
	} 	

	public static CollectionPersister getCollectionPersister(SessionFactory sessionFactory, CollectionType collType) {

		try {
			return (new Hibernate4Support()).getCollectionPersister(sessionFactory, collType);
		} catch(NoClassDefFoundError e) {
			throw new UnsupportedVersion("You are probably using an older hibernate version and will need to upgrade");
		} catch(NoSuchMethodError nme) {
			throw new UnsupportedVersion("You are probably using an older hibernate version and will need to upgrade");
		}
	}

	public static boolean isCascaded(Configuration configuration, String entityName, String inputPropertyName) {	
		try {
			return (new Hibernate4Support()).isCascaded(configuration, entityName, inputPropertyName);
		} catch(NoClassDefFoundError e) {
			throw new UnsupportedVersion("You are probably using an older hibernate version and will need to upgrade");
		} catch(NoSuchMethodError nme) {
			throw new UnsupportedVersion("You are probably using an older hibernate version and will need to upgrade");
		}
	}

	public static Type getEntityType(SessionFactory sessionFactory, String entityName) {
		try {
			return (new Hibernate4Support()).getEntityType(sessionFactory, entityName);
		} catch(NoClassDefFoundError e) {
			throw new UnsupportedVersion("You are probably using an older hibernate version and will need to upgrade");
		} catch(NoSuchMethodError nme) {
			throw new UnsupportedVersion("You are probably using an older hibernate version and will need to upgrade");		
		}
	}


}
