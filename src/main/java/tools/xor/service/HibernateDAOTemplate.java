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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;

public class HibernateDAOTemplate<T> implements DAOTemplate<T> {
    protected Class<? extends T> persistentClass;
    
    @Autowired
    private SessionFactory sessionFactory;    
    
    protected Session getSession() {
        if (this.sessionFactory.getCurrentSession() == null)
            throw new IllegalStateException("Session has not been set on DAO before usage");
        return this.sessionFactory.getCurrentSession();
    }       
	
	@SuppressWarnings("unchecked")
	@Override
	public T findById(Object id) {
        T entity;
        entity = (T) getSession().load(persistentClass, (Serializable) id);
 
        return entity;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T findByName(String name) {
        Criteria crit = getSession().createCriteria(persistentClass);
        crit.add(Restrictions.eq("name", name));
		List<T> names = crit.list();
		if (names != null && !names.isEmpty()) {
			return names.get(0);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> findByIds(Collection ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        Criteria crit = getSession().createCriteria(persistentClass);
        crit.add(Restrictions.in("id", ids));        
        return crit.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> findAll() {
        Criteria crit = getSession().createCriteria(persistentClass);
        return crit.list();
	}

	@Override
	public T saveOrUpdate(T entity) {
        getSession().saveOrUpdate(entity);
        return entity;
	}

	@Override
	public void delete(T entity) {
        getSession().delete(entity);
		
	}

	@Override
	public void refresh(T entity) {
    	if (entity != null && this.getSession() != null) {
    		this.getSession().refresh(entity);
    	}
	}

	@Override
	public void clear() {
    	if (this.getSession() != null) {
    		this.getSession().clear();
    	}
	}

	@Override
	public void setPersistentClass(T clazz) {
		this.persistentClass = (Class<? extends T>) clazz;
	}

}
