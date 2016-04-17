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

package tools.xor.db.dao;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import tools.xor.service.DAOFactory;
import tools.xor.service.DAOTemplate;

public class AbstractDAOTemplate<T> implements DAOTemplate<T> {
	@Resource(name="DAOFactory")
	private DAOFactory daoFactory; 

	private DAOTemplate<T> daoDelegate; 	

	@SuppressWarnings("unchecked")
	public DAOTemplate<T> getDelegate() {
		final Class<? extends T> persistentClass;
		
		Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		if (type instanceof Class) {
			persistentClass = (Class<? extends T>) type;
		} else if (type instanceof TypeVariable) {
			persistentClass = (Class<? extends T>) ((java.lang.reflect.TypeVariable) type).getBounds()[0];
		} else {
			persistentClass = null;
		}

		if(daoDelegate == null) {
			daoDelegate = daoFactory.create();
			daoDelegate.setPersistentClass((T)persistentClass);
		}

		return daoDelegate;
	}

	@Override
	public T findById(Object id) {
		return getDelegate().findById(id);
	}

	@Override
	public T findByName(String name) {
		return getDelegate().findByName(name);
	}

	@Override
	public List<T> findByIds(Collection<Object> ids) {
		return getDelegate().findByIds(ids);
	}

	@Override
	public List<T> findAll() {
		return getDelegate().findAll();
	}

	@Override
	public T saveOrUpdate(T entity) {
		return getDelegate().saveOrUpdate(entity);
	}

	@Override
	public void delete(T entity) {
		getDelegate().delete(entity);
	}

	@Override
	public void refresh(T entity) {
		getDelegate().refresh(entity);
	}

	@Override
	public void clear() {
		getDelegate().clear();
	}

	@Override
	public void setPersistentClass(T clazz) {
		// This method is used in the delegate and does not have use here
	}
}
