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
	private final String entityName;
	private final String pathSuffix;
	private final Boolean createIfMissing;
	private final Boolean exact;

	/**
	 * If including a type to be part of the view, then
	 * it will automatically include all the data types
	 * 
	 * @param entityClass the class of the Type
	 */
	public AssociationSetting(Class<?> entityClass) {
		this(entityClass.getName(), Boolean.TRUE, Boolean.FALSE);
	}

	public static AssociationSetting getExact(String entityName) {
		return new AssociationSetting(entityName, Boolean.TRUE, Boolean.TRUE);
	}

	public AssociationSetting(String entityName, Boolean createIfMissing, Boolean exact) {
		this.entityName = entityName;
		this.matchType = MatchType.TYPE;
		this.pathSuffix = null;
		this.createIfMissing = createIfMissing;
		this.exact = exact;
	}

	public AssociationSetting(String pathSuffix) {
		this.pathSuffix = pathSuffix;
		this.matchType = MatchType.ABSOLUTE_PATH;
		this.entityName = null;
		this.createIfMissing = Boolean.TRUE;
		this.exact = Boolean.FALSE;
	}

	/**
	 * Currently used only for prune behavior, e.g., skipping version fields
	 * @param path of the attribute
	 * @param matchType either absolute or relative
	 */
	public AssociationSetting(String path, MatchType matchType) {
		this.pathSuffix = path;
		this.matchType = matchType;
		this.entityName = null;
		this.createIfMissing = Boolean.TRUE;
		this.exact = Boolean.FALSE;
	}
	
	public String getEntityName() {
		return entityName;
	}

	public String getPathSuffix() {
		return pathSuffix;
	}

	public MatchType getMatchType() {
		return matchType;
	}

	public Boolean doCreateIfMissing ()
	{
		return createIfMissing;
	}

	public Boolean isExact() { return this.exact; }


	public boolean isAggregatePart(CallInfo ci) {
		if(matchType == MatchType.TYPE) {
			BusinessObject domainObject = (BusinessObject)ci.getOutput();
			if(ci.getSettings().getAction() == AggregateAction.READ) {
				domainObject = (BusinessObject)ci.getInput();
			}

			boolean result = false;
			if(domainObject != null) {
				EntityType instanceType = (EntityType)domainObject.getDomainType();
				EntityType entityType = (EntityType)instanceType.getShape().getType(entityName);
				result = entityType.isSameOrSupertypeOf(instanceType);
			}

			return result;
		}

		if(matchType == MatchType.ABSOLUTE_PATH && ci.isPathSuffix(this.pathSuffix))
			return true;

		return false;
	}
	
	public String toString() {
		return matchType + " - " + ((matchType == MatchType.TYPE) ? entityName : pathSuffix );
	}
}
