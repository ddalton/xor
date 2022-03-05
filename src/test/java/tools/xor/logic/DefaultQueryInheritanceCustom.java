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

package tools.xor.logic;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.FunctionType;
import tools.xor.Settings;
import tools.xor.db.base.Citation;
import tools.xor.db.base.MetaEntityState;
import tools.xor.db.base.MetaEntityType;
import tools.xor.db.base.Patent;
import tools.xor.db.enums.base.MetaEntityStateEnum;
import tools.xor.db.enums.base.MetaEntityTypeEnum;
import tools.xor.db.vo.base.CitationVO;
import tools.xor.db.vo.base.MetaEntityTypeVO;
import tools.xor.db.vo.base.MetaEntityVO;
import tools.xor.db.vo.base.PatentVO;
import tools.xor.service.AggregateManager;
import tools.xor.util.ClassUtil;
import tools.xor.view.expression.FunctionHandler;

public class DefaultQueryInheritanceCustom extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateService;

	@BeforeAll
	public static void executeOnceBeforeAll() {
		ClassUtil.setParallelDispatch(false);
	}

	@AfterAll
	public static void executeOnceAfterAll() {
		ClassUtil.setParallelDispatch(true);
	}
	
	private MetaEntityState getState(String state) {
		MetaEntityState entityState = new MetaEntityState();
		entityState.setName(state); 
		return entityState;
	}

	private MetaEntityType getType(String type) {	
		MetaEntityType entityType = new MetaEntityType();
		entityType.setName(type);
		return entityType;
	}	
		
	public void queryPatent() {
		
		setupMetaEntityStateVO(aggregateService);
		setupMetaEntityTypeVO(aggregateService);
		
		// create artifact item
		Patent patent = new Patent();
		patent.setName("DICTIONARY");
		patent.setDisplayName("Great illustrated dictionary");
		patent.setDescription("A large dictionary covering item from various fields");
		patent.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));	
		
		patent = (Patent) aggregateService.create(patent, new Settings());
		
		// Create a binding
		Citation citation = new Citation();
		citation.setName("GEOGRAPHY_SOURCE");
		citation.setDisplayName("Geography source");
		citation.setDescription("The source for the geography related information in the dictionary.");
		citation.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		citation.setMetaEntityType(getType(MetaEntityTypeEnum.CITATION.name()));
		
		Set<Citation> citations = new HashSet<Citation>();
		citations.add(citation);
		patent.setCitations(citations);

		patent = (Patent) aggregateService.update(patent, new Settings());
		
		PatentVO patentVO = new PatentVO();
		MetaEntityTypeVO typeVO = new MetaEntityTypeVO();
		typeVO.setName(patent.getMetaEntityType().getName());
		patentVO.setId(patent.getId());
		patentVO.setMetaEntityType(typeVO);
		
		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("CITATIONINFO"));			
		settings.setPreFlush(true);		
		List<?> toList = aggregateService.query(patentVO, settings);

		assert(toList.size() == 1);

		if(PatentVO.class.isAssignableFrom(toList.get(0).getClass()))
			patentVO = (PatentVO) toList.get(0);
		
		assert(patentVO != null && patentVO.getCitations().size() == 1);
		
		CitationVO result = patentVO.getCitations().iterator().next();
		
		assert(result != null);
		assert(result.getName().equals("GEOGRAPHY_SOURCE"));
		assert(result.getDisplayName().equals("Geography source"));
		assert(result.getMetaEntityType().getName().equals(MetaEntityTypeEnum.CITATION.name()));		
	}	
	
	
	public void listPatents() {
		setupMetaEntityStateVO(aggregateService);
		setupMetaEntityTypeVO(aggregateService);
		
		// Create first artifact
		Patent patent1 = new Patent();
		patent1.setName("ARTIFACT1");
		patent1.setDisplayName("Defects");
		patent1.setDescription("User story to address product defects");
		patent1.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent1.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));		
		patent1 = (Patent) aggregateService.create(patent1, new Settings());				

		// Create second artifact
		Patent patent2 = new Patent();
		patent2.setName("ARTIFACT2");
		patent2.setDisplayName("Enhancements");
		patent2.setDescription("User story to address product enhancements");
		patent2.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent2.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));		
		patent2 = (Patent) aggregateService.create(patent2, new Settings());	
				
		// query the task object
		Settings settings = new Settings();
		settings.addFunction(FunctionHandler.ILIKE, "name", "name");
		settings.addFunction(FunctionHandler.IN, "state", "state");
		settings.addFunction(FunctionHandler.EQUAL, "ownedBy.name", "owner");
		settings.addFunction(FunctionHandler.GE, "createdOn", "createdSince");
		settings.addFunction(FunctionHandler.GE, "updatedOn", "updatedSince");
		settings.addFunction(FunctionType.ASC, "name");
		
		
		settings.setView(aggregateService.getView("ARTIFACTINFO"));	
		MetaEntityVO input = new MetaEntityVO();
		MetaEntityTypeVO typeVO = new MetaEntityTypeVO();
		typeVO.setName(MetaEntityTypeEnum.PATENT.name());
		input.setMetaEntityType(typeVO);		
		List<?> toList = aggregateService.query(input, settings);

		assert(toList.size() == 2);		

		Object obj = toList.get(0);
		assert(PatentVO.class.isAssignableFrom(obj.getClass()));
	}	
	
	public void listPatentsByName() {
		setupMetaEntityStateVO(aggregateService);
		setupMetaEntityTypeVO(aggregateService);
		
		// Create first artifact
		Patent patent1 = new Patent();
		patent1.setName("PATENT1");
		patent1.setDisplayName("Defects");
		patent1.setDescription("User story to address product defects");
		patent1.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent1.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));		
		patent1 = (Patent) aggregateService.create(patent1, new Settings());				

		// Create second artifact
		Patent patent2 = new Patent();
		patent2.setName("PATENT2");
		patent2.setDisplayName("Enhancements");
		patent2.setDescription("User story to address product enhancements");
		patent2.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent2.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));		
		patent2 = (Patent) aggregateService.create(patent2, new Settings());		
				
		// query the task object
		Settings settings = new Settings();
		settings.addFunction(FunctionHandler.ILIKE, "name", "name");
		settings.addFunction(FunctionHandler.IN, "state", "state");
		settings.addFunction(FunctionHandler.EQUAL, "ownedBy.name", "owner");
		settings.addFunction(FunctionHandler.GE, "createdOn", "createdSince");
		settings.addFunction(FunctionHandler.GE, "updatedOn", "updatedSince");
		settings.addFunction(FunctionType.ASC, "name");
		
		// Filter by name
		settings.setParam("name", "PATENT1");
		
		settings.setView(aggregateService.getView("ARTIFACTINFO"));	
		MetaEntityVO input = new MetaEntityVO();
		MetaEntityTypeVO typeVO = new MetaEntityTypeVO();
		typeVO.setName(MetaEntityTypeEnum.PATENT.name());
		input.setMetaEntityType(typeVO);		
		List<?> toList = aggregateService.query(input, settings);

		assert(toList.size() == 1);		

		Object obj = toList.get(0);
		assert(PatentVO.class.isAssignableFrom(obj.getClass()));
		
		PatentVO a1 = (PatentVO) obj;
		assert(a1.getName().equals("PATENT1"));
	}	
	
	public void listPatentsByState() {
		setupMetaEntityStateVO(aggregateService);
		setupMetaEntityTypeVO(aggregateService);
		
		Date today = new Date();
		Date yesterday = new Date();
		yesterday.setTime( today.getTime() - 1*1000*60*60*24 );		
		
		// Create first patent
		Patent patent1 = new Patent();
		patent1.setName("PATENT1");
		patent1.setDisplayName("Defects");
		patent1.setDescription("User story to address product defects");
		patent1.setState(getState(MetaEntityStateEnum.RETIRED.name()));
		patent1.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));
		patent1.setCreatedOn(yesterday);
		patent1 = (Patent) aggregateService.create(patent1, new Settings());				

		// Create second patent
		Patent patent2 = new Patent();
		patent2.setName("PATENT2");
		patent2.setDisplayName("Enhancements");
		patent2.setDescription("User story to address product enhancements");
		patent2.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent2.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));	
		patent2.setCreatedOn(today);
		patent2 = (Patent) aggregateService.create(patent2, new Settings());		
				
		// query the task object
		Settings settings = new Settings();
		settings.addFunction(FunctionHandler.ILIKE, "name", "name");
		settings.addFunction(FunctionHandler.IN, "state.name", "state");
		settings.addFunction(FunctionHandler.EQUAL, "ownedBy.name", "owner");
		settings.addFunction(FunctionHandler.GE, "createdOn", "createdSince");
		settings.addFunction(FunctionHandler.GE, "updatedOn", "updatedSince");
		settings.addFunction(FunctionType.ASC, "name");
		
		// Filter by name
		settings.setParam("state", "ACTIVE");
		
		settings.setView(aggregateService.getView("ARTIFACTINFO"));	
		MetaEntityVO input = new MetaEntityVO();
		MetaEntityTypeVO typeVO = new MetaEntityTypeVO();
		typeVO.setName(MetaEntityTypeEnum.PATENT.name());
		input.setMetaEntityType(typeVO);		
		List<?> toList = aggregateService.query(input, settings);

		assert(toList.size() == 1);		

		Object obj = toList.get(0);
		assert(PatentVO.class.isAssignableFrom(obj.getClass()));
		
		PatentVO a1 = (PatentVO) obj;
		assert(a1.getName().equals("PATENT2"));
	}	

	public void listPatentsBeforeDate() {
		setupMetaEntityStateVO(aggregateService);
		setupMetaEntityTypeVO(aggregateService);
		
		Date today = new Date();
		Date yesterday = new Date();
		yesterday.setTime( today.getTime() - 1*1000*60*60*24 );
		
		// Create first patent
		Patent patent1 = new Patent();
		patent1.setName("PATENT1");
		patent1.setDisplayName("Defects");
		patent1.setDescription("User story to address product defects");
		patent1.setState(getState(MetaEntityStateEnum.RETIRED.name()));
		patent1.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));	
		patent1.setCreatedOn(yesterday);
		patent1 = (Patent) aggregateService.create(patent1, new Settings());				

		// Create second patent
		Patent patent2 = new Patent();
		patent2.setName("PATENT2");
		patent2.setDisplayName("Enhancements");
		patent2.setDescription("User story to address product enhancements");
		patent2.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent2.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));
		patent2.setCreatedOn(today);
		patent2 = (Patent) aggregateService.create(patent2, new Settings());		
				
		// query the task object
		Settings settings = new Settings();
		settings.addFunction(FunctionHandler.ILIKE, "name", "name");
		settings.addFunction(FunctionHandler.IN, "state", "state");
		settings.addFunction(FunctionHandler.EQUAL, "ownedBy.name", "owner");
		settings.addFunction(FunctionHandler.LT, "createdOn", "createdBefore");
		settings.addFunction(FunctionType.ASC, "name");
		
		// Filter by name
		settings.setParam("createdBefore", today);
		
		settings.setView(aggregateService.getView("ARTIFACTINFO"));	
		MetaEntityVO input = new MetaEntityVO();
		MetaEntityTypeVO typeVO = new MetaEntityTypeVO();
		typeVO.setName(MetaEntityTypeEnum.PATENT.name());
		input.setMetaEntityType(typeVO);		
		List<?> toList = aggregateService.query(input, settings);

		assert(toList.size() == 1);

		Object obj = toList.get(0);
		assert(PatentVO.class.isAssignableFrom(obj.getClass()));
		
		PatentVO a1 = (PatentVO) obj;
		assert(a1.getName().equals("PATENT1"));
	}	

	public void listPatentsBetweenDate() {
		setupMetaEntityStateVO(aggregateService);
		setupMetaEntityTypeVO(aggregateService);
		
		Date today = new Date();
		Date TwodaysAgo = new Date();
		Date FifteendaysAgo = new Date();
		TwodaysAgo.setTime( today.getTime() - 2*1000*60*60*24 );
		FifteendaysAgo.setTime( today.getTime() - 15*1000*60*60*24 );
		
		// Create first patent
		Patent patent1 = new Patent();
		patent1.setName("PATENT1");
		patent1.setDisplayName("Defects");
		patent1.setDescription("User story to address product defects");
		patent1.setState(getState(MetaEntityStateEnum.RETIRED.name()));
		patent1.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));	
		patent1.setCreatedOn(FifteendaysAgo);
		patent1 = (Patent) aggregateService.create(patent1, new Settings());				

		// Create second patent
		Patent patent2 = new Patent();
		patent2.setName("PATENT2");
		patent2.setDisplayName("Enhancements");
		patent2.setDescription("User story to address product enhancements");
		patent2.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent2.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));
		patent2.setCreatedOn(TwodaysAgo);
		patent2 = (Patent) aggregateService.create(patent2, new Settings());		
		
		Patent patent3 = new Patent();
		patent3.setName("PATENT3");
		patent3.setDisplayName("Use cases");
		patent3.setDescription("User story to address use cases");
		patent3.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent3.setMetaEntityType(getType(MetaEntityTypeEnum.TASK.name()));
		patent3.setCreatedOn(today);
		patent3 = (Patent) aggregateService.create(patent3, new Settings());			
				
		// query the task object
		Settings settings = new Settings();
		settings.addFunction(FunctionHandler.ILIKE, "name", "name");
		settings.addFunction(FunctionHandler.IN, "state", "state");
		settings.addFunction(FunctionHandler.EQUAL, "ownedBy.name", "owner");
		settings.addFunction(FunctionHandler.BETWEEN, "createdOn", "createdFrom", "createdTo");
		settings.addFunction(FunctionType.ASC, "name");
		
		// Filter by date
		Date yesterday = new Date();
		yesterday.setTime( today.getTime() - 1*1000*60*60*24 );
		Date fiveDaysAgo = new Date();
		fiveDaysAgo.setTime( today.getTime() - 5*1000*60*60*24 );		
		settings.setParam("createdFrom", fiveDaysAgo);
		settings.setParam("createdTo", yesterday);
		
		settings.setView(aggregateService.getView("ARTIFACTINFO"));	
		MetaEntityVO input = new MetaEntityVO();
		MetaEntityTypeVO typeVO = new MetaEntityTypeVO();
		typeVO.setName(MetaEntityTypeEnum.PATENT.name());
		input.setMetaEntityType(typeVO);		
		List<?> toList = aggregateService.query(input, settings);

		assert(toList.size() == 1);		

		Object obj = toList.get(0);
		assert(PatentVO.class.isAssignableFrom(obj.getClass()));
		
		PatentVO a1 = (PatentVO) obj;
		assert(a1.getName().equals("PATENT2"));
	}	
	
	public void limitPatents() {
		setupMetaEntityStateVO(aggregateService);
		setupMetaEntityTypeVO(aggregateService);
		
		Date today = new Date();
		Date TwodaysAgo = new Date();
		Date FifteendaysAgo = new Date();
		TwodaysAgo.setTime( today.getTime() - 2*1000*60*60*24 );
		FifteendaysAgo.setTime( today.getTime() - 15*1000*60*60*24 );
		
		// Create first patent
		Patent patent1 = new Patent();
		patent1.setName("PATENT1");
		patent1.setDisplayName("Defects");
		patent1.setDescription("User story to address product defects");
		patent1.setState(getState(MetaEntityStateEnum.RETIRED.name()));
		patent1.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));	
		patent1.setCreatedOn(FifteendaysAgo);
		patent1 = (Patent) aggregateService.create(patent1, new Settings());				

		// Create second patent
		Patent patent2 = new Patent();
		patent2.setName("PATENT2");
		patent2.setDisplayName("Enhancements");
		patent2.setDescription("User story to address product enhancements");
		patent2.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent2.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));
		patent2.setCreatedOn(TwodaysAgo);
		patent2 = (Patent) aggregateService.create(patent2, new Settings());		
		
		Patent patent3 = new Patent();
		patent3.setName("PATENT3");
		patent3.setDisplayName("Use cases");
		patent3.setDescription("User story to address use cases");
		patent3.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent3.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));
		patent3.setCreatedOn(today);
		patent3 = (Patent) aggregateService.create(patent3, new Settings());			
				
		// query the task object
		Settings settings = new Settings();
		settings.addFunction(FunctionType.ASC, 1, "name");
		
		settings.setView(aggregateService.getView("ARTIFACTINFO"));	
		MetaEntityVO input = new MetaEntityVO();
		MetaEntityTypeVO typeVO = new MetaEntityTypeVO();
		typeVO.setName(MetaEntityTypeEnum.PATENT.name());
		input.setMetaEntityType(typeVO);		
		List<?> toList = aggregateService.query(input, settings);

		assert(toList.size() == 3);		

		settings = new Settings();
		settings.addFunction(FunctionType.ASC, 1, "name");
		settings.setView(aggregateService.getView("ARTIFACTINFO"));	
		settings.setLimit(2);
		toList = aggregateService.query(input, settings);
		
		assert(toList.size() == 2);
	}	
	
	
}
