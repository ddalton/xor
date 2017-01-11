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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.ParameterMode;
import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.AggregateAction;
import tools.xor.Settings;
import tools.xor.db.base.Person;
import tools.xor.service.AggregateManager;
import tools.xor.view.AggregateView;
import tools.xor.view.ParameterMapping;
import tools.xor.view.StoredProcedure;

public class DefaultStoredProcedure extends AbstractDBTest {
	
	@Autowired
	protected AggregateManager aggregateService;

	final String NAME = "GEORGE_WASHINGTON";
	final String DISPLAY_NAME = "George Washington";
	final String DESCRIPTION = "First President of the United States of America";
	final String USER_NAME = "georgewashington";

	protected void singleReadSP() throws UnsupportedEncodingException, JAXBException {
		AggregateView view = aggregateService.getView("BASICINFO_SP");
		outputSP(view);
/*
		Person person = new Person();
		person.setName(NAME);
		person.setDisplayName(DISPLAY_NAME);
		person.setDescription(DESCRIPTION);
		person.setUserName(USER_NAME);

		person = (Person) aggregateService.create(person, new Settings());

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setEntityType(aggregateService.getDAS().getType(Person.class));
		settings.setDenormalized(true);
		settings.setView(aggregateService.getView("BASICINFO_SP"));
		List result = aggregateService.query(new Person(), settings);
*/
		assert(view.getStoredProcedure().size() == 2);
	}
	
	private StoredProcedure getReadSP() {
		StoredProcedure sp = new StoredProcedure();
		sp.setName("basicInfoRead");
		sp.setAction(AggregateAction.READ);

		ParameterMapping pm = new ParameterMapping();
		pm.setAttribute("id");
		pm.setMode(ParameterMode.IN);

		List<ParameterMapping> pmList = new ArrayList<ParameterMapping>();
		pmList.add(pm);
		sp.setParameterList(pmList);
		
		return sp;
	}
	
	private StoredProcedure getCreateSP() {
		StoredProcedure sp = new StoredProcedure();
		sp.setName("basicInfoCreate");
		sp.setAction(AggregateAction.CREATE);

		// id
		ParameterMapping pm = new ParameterMapping();
		pm.setAttribute("id");
		pm.setMode(ParameterMode.OUT);

		List<ParameterMapping> pmList = new ArrayList<ParameterMapping>();
		pmList.add(pm);
		sp.setParameterList(pmList);
		
		// name
		pm = new ParameterMapping();
		pm.setAttribute("name");
		pmList.add(pm);
		
		// displayName
		pm = new ParameterMapping();
		pm.setAttribute("displayName");
		pmList.add(pm);
		
		// description
		pm = new ParameterMapping();
		pm.setAttribute("description");
		pmList.add(pm);	
		
		// iconUrl
		pm = new ParameterMapping();
		pm.setAttribute("iconUrl");
		pmList.add(pm);	
		
		// detailedDescription
		pm = new ParameterMapping();
		pm.setAttribute("detailedDescription");
		pmList.add(pm);		
		
		// extra1
		pm = new ParameterMapping();
		pm.setName("extra1");
		pm.setType("VARCHAR");
		pm.setDefaultValue("ABC");
		pm.setMode(ParameterMode.IN);
		pmList.add(pm);		
		
		// extra2
		pm = new ParameterMapping();
		pm.setName("extra2");
		pm.setType("DATE");
		pm.setDefaultValue("2013-10-21T13:28:06.419Z");
		pm.setMode(ParameterMode.OUT);
		pmList.add(pm);	
		
		// extra2
		pm = new ParameterMapping();
		pm.setName("extra3");
		pm.setType("DATE");
		pm.setMode(ParameterMode.OUT);
		pmList.add(pm);			
		
		return sp;
	}
	
	private void outputSP(AggregateView view) throws JAXBException, UnsupportedEncodingException {
		javax.xml.bind.JAXBContext jaxbCtx = javax.xml.bind.JAXBContext.newInstance(AggregateView.class);
		javax.xml.bind.Marshaller marshaller = jaxbCtx.createMarshaller();
		marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8"); //NOI18N
		marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(view, System.out);

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		marshaller.marshal( view, bOut );
		bOut.toString("UTF-8");		
	}

	/** Create the following Stored procedure
	 *	<storedProcedure>
	 *		<name>basicInfoRead</name>
	 *		<action>READ</action>
	 *		<parameterList attribute="id" mode="IN" />
	 *	</storedProcedure>
	 * @throws JAXBException 
	 * @throws UnsupportedEncodingException 
	 */
	protected void testXmlSP() throws JAXBException, UnsupportedEncodingException {
		AggregateView view = new AggregateView();

		StoredProcedure sp = getReadSP();

		List<StoredProcedure> spList = new ArrayList<StoredProcedure>();
		spList.add(sp);
		view.setStoredProcedure(spList);

		outputSP(view);

	}
	
	protected void testXmlSPExtra() throws JAXBException, UnsupportedEncodingException {
		AggregateView view = new AggregateView();

		StoredProcedure sp = getCreateSP();			
		
		List<StoredProcedure> spList = new ArrayList<StoredProcedure>();
		spList.add(sp);
		view.setStoredProcedure(spList);

		outputSP(view);	
	}
	
	protected void testXmlSPMulti() throws JAXBException, UnsupportedEncodingException {
		AggregateView view = new AggregateView();

		StoredProcedure sp1 = getReadSP();
		StoredProcedure sp2 = getCreateSP();			
		
		List<StoredProcedure> spList = new ArrayList<StoredProcedure>();
		spList.add(sp1);
		spList.add(sp2);
		view.setStoredProcedure(spList);

		outputSP(view);	
	}	
}
