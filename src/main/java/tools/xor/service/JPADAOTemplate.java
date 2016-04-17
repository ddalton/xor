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

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class JPADAOTemplate<T> implements DAOTemplate<T> {
    protected Class<? extends T> persistentClass;

	@PersistenceContext
	private EntityManager entityManager;	
	
	@Override
	public T findById(Object id) {
		return entityManager.find(persistentClass, id);
	}

	@Override
	public T findByName(String name) {
		CriteriaQuery<? extends T> cq = entityManager.getCriteriaBuilder().createQuery(persistentClass);
		Root<? extends T> pClass = cq.from(persistentClass);
		cq.where(entityManager.getCriteriaBuilder().equal(pClass.get("name"), name));		
		TypedQuery<? extends T> query = entityManager.createQuery(cq);
		
		return query.getSingleResult();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> findByIds(Collection<Object> ids) {
		CriteriaQuery<? extends T> cq = entityManager.getCriteriaBuilder().createQuery(persistentClass);
		Root<? extends T> pClass = cq.from(persistentClass);
		cq.where(pClass.get("id").in(ids));		
		TypedQuery<? extends T> query = entityManager.createQuery(cq);
		
		return (List<T>) query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> findAll() {
		CriteriaQuery<? extends T> cq = entityManager.getCriteriaBuilder().createQuery(persistentClass);	
		TypedQuery<? extends T> query = entityManager.createQuery(cq);
		
		return (List<T>) query.getResultList();
	}

	@Override
	public T saveOrUpdate(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	@Override
	public void delete(T entity) {
		entityManager.remove(entity);
	}

	@Override
	public void refresh(T entity) {
		entityManager.refresh(entity);
	}

	@Override
	public void clear() {
		entityManager.clear();
	}
	

	@Override
	public void setPersistentClass(T clazz) {
		this.persistentClass = (Class<? extends T>) clazz;
	}	

}
