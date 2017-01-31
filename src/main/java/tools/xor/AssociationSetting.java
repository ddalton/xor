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

public final class AssociationSetting {
	private final MatchType matchType;
	private final Class<?> entityClass;
	private final String pathSuffix;
	private final Boolean createIfMissing;

	/**
	 * If including a type to be part of the view, then
	 * it will automatically include all the data types
	 * 
	 * @param entityClass the class of the Type
	 */
	public AssociationSetting(Class<?> entityClass) {
		this(entityClass, Boolean.TRUE);
	}

	public AssociationSetting(Class<?> entityClass, Boolean createIfMissing) {
		this.entityClass = entityClass;
		this.matchType = MatchType.TYPE;
		this.pathSuffix = null;
		this.createIfMissing = createIfMissing;
	}

	public AssociationSetting(String pathSuffix) {
		this.pathSuffix = pathSuffix;
		this.matchType = MatchType.PATH;
		this.entityClass = null;
		this.createIfMissing = Boolean.TRUE;
	}
	
	public Class<?> getEntityClass() {
		return entityClass;
	}

	public String getPathSuffix() {
		return pathSuffix;
	}

	public MatchType getMatchType() {
		return matchType;
	}

	public Boolean getCreateIfMissing ()
	{
		return createIfMissing;
	}


	public boolean isAggregatePart(CallInfo ci) {
		if(matchType == MatchType.TYPE) {
			BusinessObject domainObject = (BusinessObject)ci.getOutput();
			if(ci.getSettings().getAction() == AggregateAction.READ) {
				domainObject = (BusinessObject)ci.getInput();
			}
			Object associatedInstance = domainObject != null ? domainObject.getInstance() : null;
			boolean result = associatedInstance != null ? entityClass.isAssignableFrom(associatedInstance.getClass()) : false;
			if(!result) {
				// Check the type
				result = entityClass.isAssignableFrom(domainObject.getDomainType().getInstanceClass());
			}
			return result;
		}

		if(matchType == MatchType.PATH && ci.isPathSuffix(this.pathSuffix))
			return true;

		return false;
	}
	
	public String toString() {
		return matchType + " - " + ((matchType == MatchType.TYPE) ? entityClass.getName() : pathSuffix );
	}
}
