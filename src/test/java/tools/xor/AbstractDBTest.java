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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.db.base.Facet;
import tools.xor.db.base.MetaEntityState;
import tools.xor.db.base.MetaEntityType;
import tools.xor.db.base.Technician;
import tools.xor.db.common.ValueType;
import tools.xor.db.enums.base.FacetEnum;
import tools.xor.db.enums.base.MetaEntityStateEnum;
import tools.xor.db.enums.base.MetaEntityTypeEnum;
import tools.xor.db.enums.common.ValueTypeEnum;
import tools.xor.db.enums.service.ChapterTypeEnum;
import tools.xor.db.vo.base.ChapterTypeVO;
import tools.xor.db.vo.base.FacetVO;
import tools.xor.db.vo.base.MetaEntityStateVO;
import tools.xor.db.vo.base.MetaEntityTypeVO;
import tools.xor.db.vo.common.ValueTypeVO;
import tools.xor.service.AggregateManager;

public class AbstractDBTest {	
	
	@Autowired
	protected AggregateManager aggregateManager;
	
	public static class TypeTest implements Type {
		public String name;
		
		public TypeTest(String name) {
			this.name = name;
		}
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getURI() {
			return null;
		}

		@Override
		public Class<?> getInstanceClass() {
			return null;
		}

		@Override
		public boolean isInstance(Object object) {
			return false;
		}

		@Override
		public List<Property> getProperties() {
			return null;
		}

		@Override
		public Property getProperty(String propertyName) {
			return null;
		}

		@Override
		public boolean isDataType() {
			return false;
		}

		@Override public boolean isDataType (Object object)
		{
			return isDataType();
		}

		@Override
		public boolean isOpen() {
			return false;
		}

		@Override
		public boolean isSequenced() {
			return false;
		}

		@Override
		public boolean isAbstract() {
			return false;
		}

		@Override
		public List<Type> getParentTypes() {
			return null;
		}

		@Override
		public List<Property> getDeclaredProperties() {
			return null;
		}

		@Override
		public List<?> getAliasNames() {
			return null;
		}

		@Override
		public List<?> getInstanceProperties() {
			return null;
		}

		@Override
		public Object get(Property property) {
			return null;
		}

		@Override
		public boolean isLOB() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public TypeKind getKind ()
		{
			return TypeKind.SCALAR;
		}

	}
	
	protected Settings getSettings() {
	  // Be default we treat technician to be part of the aggregate
		Settings settings = new Settings();
		settings.expand(new AssociationSetting(Technician.class));
		settings.expand(new AssociationSetting("auditTask"));
		settings.expand(new AssociationSetting("auditedTask"));
		settings.expand(new AssociationSetting("alternateTask"));
		settings.expand(new AssociationSetting("taskParent"));
		settings.expand(new AssociationSetting("assignedTo"));
		settings.expand(new AssociationSetting("auditTask.name"));
		settings.expand(new AssociationSetting("auditedTask.name"));
	  
	  return settings;
	}	
	
	protected Settings getEmptySettings() {
		Settings settings = new Settings();
		return settings;
	}	
	
	public void setupValueTypeVO(AggregateManager aggregateService){
		FacetVO facetVO = new FacetVO();
		facetVO.setName(FacetEnum.VALUE_TYPE.name());
		facetVO.setDisplayName(FacetEnum.VALUE_TYPE.name());
		facetVO.setExtensible(true);
		aggregateService.create(facetVO, new Settings());
		
		for (ValueTypeEnum v : ValueTypeEnum.values()) {
			ValueTypeVO valueTypeVO = new ValueTypeVO();
			valueTypeVO.setName(v.name());
			valueTypeVO.setFacet(facetVO);
			aggregateService.create(valueTypeVO, getEmptySettings());
		}			
		
		// Ensure the values are seen by queries
		aggregateManager.getDataModelFactory().createDataStore(null).flush();
	}	
	
	public void setupMetaEntityTypeVO(AggregateManager aggregateService){
		FacetVO facetVO = new FacetVO();
		facetVO.setName(FacetEnum.META_ENTITY_TYPE.name());
		facetVO.setDisplayName(FacetEnum.META_ENTITY_TYPE.name());
		facetVO.setExtensible(true);
		aggregateService.create(facetVO, new Settings());
		
		for (MetaEntityTypeEnum v : MetaEntityTypeEnum.values()) {
			MetaEntityTypeVO type = new MetaEntityTypeVO();
			type.setName(v.name());
			type.setFacet(facetVO);
			aggregateService.create(type, getEmptySettings());
		}	
		
		// Ensure the values are seen by queries
		aggregateManager.getDataModelFactory().createDataStore(null).flush();
	}	
	
	public void setupMetaEntityStateVO(AggregateManager aggregateService){
		FacetVO facetVO = new FacetVO();
		facetVO.setName(FacetEnum.META_ENTITY_STATE.name());
		facetVO.setDisplayName(FacetEnum.META_ENTITY_STATE.name());
		facetVO.setExtensible(true);
		aggregateService.create(facetVO, new Settings());
		
		for (MetaEntityStateEnum v : MetaEntityStateEnum.values()) {
			MetaEntityStateVO state = new MetaEntityStateVO();
			state.setName(v.name());
			state.setFacet(facetVO);
			aggregateService.create(state, getEmptySettings());
		}	
		
		// Ensure the values are seen by queries
		aggregateManager.getDataModelFactory().createDataStore(null).flush();
	}	
	
	public void setupChapterTypeVO(AggregateManager aggregateService){
		FacetVO facetVO = new FacetVO();
		facetVO.setName(FacetEnum.CHAPTER_TYPE.name());
		facetVO.setDisplayName(FacetEnum.CHAPTER_TYPE.name());
		facetVO.setExtensible(true);
		aggregateService.create(facetVO, new Settings());
		
		for (ChapterTypeEnum v : ChapterTypeEnum.values()) {
			ChapterTypeVO chapterType = new ChapterTypeVO();
			chapterType.setName(v.name());
			chapterType.setFacet(facetVO);
			aggregateService.create(chapterType, getEmptySettings());
		}
		
		// Ensure the values are seen by queries
		aggregateManager.getDataModelFactory().createDataStore(null).flush();
	}		
	
	public void setupValueType(AggregateManager aggregateService){
		Facet facet = new Facet();
		facet.setName(FacetEnum.VALUE_TYPE.name());
		facet.setDisplayName(FacetEnum.VALUE_TYPE.name());
		facet.setExtensible(true);
		aggregateService.create(facet, new Settings());
		
		for (ValueTypeEnum v : ValueTypeEnum.values()) {
			ValueType valueType = new ValueType();
			valueType.setName(v.name());
			valueType.setFacet(facet);
			aggregateService.create(valueType, getEmptySettings());
		}			
	}	
	
	public void setupMetaEntityType(AggregateManager aggregateService){
		Facet facet = new Facet();
		facet.setName(FacetEnum.META_ENTITY_TYPE.name());
		facet.setDisplayName(FacetEnum.META_ENTITY_TYPE.name());
		facet.setExtensible(true);
		aggregateService.create(facet, new Settings());
		
		for (MetaEntityTypeEnum v : MetaEntityTypeEnum.values()) {
			MetaEntityType type = new MetaEntityType();
			type.setName(v.name());
			type.setFacet(facet);
			aggregateService.create(type, getEmptySettings());
		}		
	}	
	
	public void setupMetaEntityState(AggregateManager aggregateService){
		Facet facet = new Facet();
		facet.setName(FacetEnum.META_ENTITY_STATE.name());
		facet.setDisplayName(FacetEnum.META_ENTITY_STATE.name());
		facet.setExtensible(true);
		aggregateService.create(facet, new Settings());
		
		for (MetaEntityStateEnum v : MetaEntityStateEnum.values()) {
			MetaEntityState state = new MetaEntityState();
			state.setName(v.name());
			state.setFacet(facet);
			aggregateService.create(state, getEmptySettings());
		}		
	}	
}
