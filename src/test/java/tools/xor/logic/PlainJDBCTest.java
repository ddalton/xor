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

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tools.xor.FunctionType;
import tools.xor.JDBCType;
import tools.xor.Settings;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataAccessService;
import tools.xor.service.Shape;
import tools.xor.view.AggregateView;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring-jdbc-test.xml" })
public class PlainJDBCTest
{
	@Autowired
	protected AggregateManager am;

	@Autowired
	protected DataSource dataSource;

	@Before
	public void init() throws SQLException, ClassNotFoundException, IOException
	{
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();) {
			statement.execute("CREATE TABLE library "
					               + "(id VARCHAR(10) NOT NULL, "
					               + " name VARCHAR(50) NOT NULL, "
					               + " PRIMARY KEY(id))");

			// British libraries
			statement.executeUpdate("INSERT INTO library (id, name) VALUES ('L100', 'British Library')");
			statement.executeUpdate("INSERT INTO library (id, name) VALUES ('L110', 'Duke Humfrey’s Library')");
			statement.executeUpdate("INSERT INTO library (id, name) VALUES ('L111', 'Liverpool Central Library')");
			statement.executeUpdate("INSERT INTO library (id, name) VALUES ('L112', 'Signet Library')");
			statement.executeUpdate("INSERT INTO library (id, name) VALUES ('L113', 'Bodlein Library')");

			// UK libraries
			statement.executeUpdate("INSERT INTO library (id, name) VALUES ('L201', 'Central Library')");
			statement.executeUpdate("INSERT INTO library (id, name) VALUES ('L202', 'Library of Congress')");


			statement.execute("CREATE TABLE librarian "
					               + "(id VARCHAR(10) NOT NULL, "
				                   + " name VARCHAR(50) NOT NULL,"
					               + " email VARCHAR(50) NOT NULL, "
					               + " library VARCHAR(10) NOT NULL, "
					               + " PRIMARY KEY (id))");
			statement.execute("ALTER TABLE librarian ADD CONSTRAINT FK1__librarians FOREIGN KEY(library) REFERENCES library(id)");
			statement.executeUpdate("INSERT INTO librarian (id, name, email, library) VALUES ('1001','Thomas Bodley', 'tbodley@somewhere.com', 'L113')");
			statement.executeUpdate("INSERT INTO librarian (id, name, email, library) VALUES ('1002','Lewis Carroll', 'lcarroll@somewhere.com', 'L100')");
			statement.executeUpdate("INSERT INTO librarian (id, name, email, library) VALUES ('1003','Alison Bailey', 'abailey@somewhere.com', 'L100')");
			statement.executeUpdate("INSERT INTO librarian (id, name, email, library) VALUES ('1004','Alasdair Ball', 'aball@somewhere.com', 'L100')");
			statement.executeUpdate("INSERT INTO librarian (id, name, email, library) VALUES ('1010','Benjamin Franklin', 'bfranklin@somewhere.com', 'L202')");

			// Create a many-to-many relationship between librarian and association
			statement.execute(
				"CREATE TABLE association "
					+ "(id VARCHAR(10) NOT NULL, "
					+ " name VARCHAR(250) NOT NULL,"
					+ " state VARCHAR(50) NOT NULL, "
					+ " PRIMARY KEY (id))");
			statement.execute("CREATE TABLE librarianassociation "
					+ "(librarian VARCHAR(10) NOT NULL, "
					+ " association VARCHAR(10) NOT NULL)");
			statement.execute("ALTER TABLE librarianassociation ADD CONSTRAINT FK2__libraryassociations FOREIGN KEY(librarian) REFERENCES librarian(id)");
			statement.execute("ALTER TABLE librarianassociation ADD CONSTRAINT FK3__libraryassociations FOREIGN KEY(association) REFERENCES association(id)");

			statement.executeUpdate("INSERT INTO association (id, name, state) VALUES ('ALA','American Library Association', 'Illinois')");
			statement.executeUpdate("INSERT INTO association (id, name, state) VALUES ('ALISE','Association for Library and Information Science Education', 'Illinois')");
			statement.executeUpdate("INSERT INTO association (id, name, state) VALUES ('ARLIS','Art Libraries Society of North America', 'Wisconsin')");
			statement.executeUpdate("INSERT INTO association (id, name, state) VALUES ('MLA','Medical Library Association', 'Illinois')");
			statement.executeUpdate("INSERT INTO association (id, name, state) VALUES ('AALL','American Association of Law Libraries', 'Illinois')");
			statement.executeUpdate("INSERT INTO association (id, name, state) VALUES ('CILIP','Chartered Institute of Library and Information Professionals', 'London')");

			statement.executeUpdate("INSERT INTO librarianassociation (librarian, association) VALUES ('1002','CILIP')");
			statement.executeUpdate("INSERT INTO librarianassociation (librarian, association) VALUES ('1002','ALISE')");
			statement.executeUpdate("INSERT INTO librarianassociation (librarian, association) VALUES ('1003','CILIP')");
			statement.executeUpdate("INSERT INTO librarianassociation (librarian, association) VALUES ('1004','CILIP')");


			// TODO: Add address

			connection.commit();
		}
	}

	@After
	public void destroy() throws SQLException, ClassNotFoundException, IOException {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();) {
			statement.executeUpdate("DROP TABLE librarianassociation");
			statement.executeUpdate("DROP TABLE association");
			statement.executeUpdate("DROP TABLE librarian");
			statement.executeUpdate("DROP TABLE library");
			connection.commit();
		}
	}

	@Test
	public void jsonSelect() {
		String jsonString = "{ \"normalized\": false, \"view\" : { \"attributeList\" : [\"count\"], \"nativeQuery\" : { \"selectClause\": \"SELECT count(*) FROM librarian\" } } }";

		DataAccessService das = am.getDAS();

		// Rebuild the types
		das.addShape("_DEFAULT_");

		Settings settings = das.settings().json(jsonString).build();

		Object result = am.dml(settings);

		assert(result instanceof List);

		List list = (List) result;
		assert(list.size() == 2);
	}

	@Test
	public void selectView() {
		DataAccessService das = am.getDAS();
		das.addShape("_DEFAULT_");
		Shape shape = das.getShape();

		// create library
		JSONObject json = new JSONObject();
		json.put("ID", "L101");
		json.put("NAME", "Natural History Museum");

		Settings settings = new Settings();
		JDBCType type = (JDBCType) das.getType("library");
		settings.setEntityType(type);
		settings.init(shape);
		Object obj = am.create(json, settings);

		assert(obj != null);

		//Object jsonObject = am.read(obj, settings);
		List<?> toList = am.query(obj, settings);
		assert(toList.size() == 1);
		System.out.println("JSON string: " + json.toString());
	}

	@Test
	public void queryEntity() {
		DataAccessService das = am.getDAS();
		das.addShape("_DEFAULT_");
		Shape shape = das.getShape();

		JSONObject json = new JSONObject();
		json.put("ID", "1001");

		AggregateView view = new AggregateView();
		List<String> attributes = new ArrayList<>();
		view.setAttributeList(attributes);
		attributes.add("ID");
		attributes.add("NAME");
		attributes.add("EMAIL");
		attributes.add("LIBRARY.ID");
		attributes.add("LIBRARY.NAME");

		Settings settings = new Settings();
		JDBCType type = (JDBCType) das.getType("librarian");
		settings.setEntityType(type);
		settings.setView(view);
		settings.init(shape);

		List<?> toList = am.query(json, settings);
		assert(toList.size() == 1);

		json = (JSONObject)toList.get(0);
		assert(json.getJSONObject("LIBRARY") != null);
		assert(json.getString("NAME").equals("Thomas Bodley"));

		JSONObject library = json.getJSONObject("LIBRARY");
		assert(library.getString("NAME").equals("Bodlein Library"));
	}


	@Test
	public void queryCollection() {
		DataAccessService das = am.getDAS();
		das.addShape("_DEFAULT_");
		Shape shape = das.getShape();

		JSONObject json = new JSONObject();
		json.put("ID", "L100");

		AggregateView view = new AggregateView();
		List<String> attributes = new ArrayList<>();
		view.setAttributeList(attributes);
		attributes.add("ID");
		attributes.add("NAME");
		attributes.add("LIBRARIANS.ID");
		attributes.add("LIBRARIANS.NAME");
		attributes.add("LIBRARIANS.EMAIL");

		Settings settings = new Settings();
		JDBCType type = (JDBCType) das.getType("library");
		settings.setEntityType(type);
		settings.setView(view);
		settings.init(shape);

		List<?> toList = am.query(json, settings);
		assert(toList.size() == 1);
		JSONObject library = (JSONObject)toList.get(0);
		JSONArray librarians = library.getJSONArray("LIBRARIANS");
		assert(librarians != null);
		assert(librarians.length() == 3);
	}

	@Test
	public void queryManyToMany() {
		DataAccessService das = am.getDAS();
		das.addShape("_DEFAULT_");
		Shape shape = das.getShape();

		// create library
		JSONObject json = new JSONObject();
		json.put("ID", "L100");

		AggregateView view = new AggregateView();
		List<String> attributes = new ArrayList<>();
		view.setAttributeList(attributes);
		attributes.add("ID");
		attributes.add("NAME");
		attributes.add("LIBRARIANS.ID");
		attributes.add("LIBRARIANS.NAME");
		attributes.add("LIBRARIANS.EMAIL");
		attributes.add("LIBRARIANS.LIBRARYASSOCIATIONS.ASSOCIATION.ID");
		attributes.add("LIBRARIANS.LIBRARYASSOCIATIONS.ASSOCIATION.NAME");

		Settings settings = new Settings();
		JDBCType type = (JDBCType) das.getType("library");
		settings.setEntityType(type);
		settings.setView(view);
		settings.init(shape);
		settings.addFunction(FunctionType.ASC, 1, "LIBRARIANS.NAME");

		List<?> toList = am.query(json, settings);
		assert(toList.size() == 1);
		JSONObject library = (JSONObject)toList.get(0);
		JSONArray librarians = library.getJSONArray("LIBRARIANS");
		assert(librarians != null);
		assert(librarians.length() == 3);

		JSONObject aball = librarians.getJSONObject(0);
		JSONObject abailey = librarians.getJSONObject(1);
		JSONObject lcarroll = librarians.getJSONObject(2);

		assert(aball.getString("NAME").equals("Alasdair Ball"));
		assert(abailey.getString("NAME").equals("Alison Bailey"));
		assert(lcarroll.getString("NAME").equals("Lewis Carroll"));

		JSONArray las = lcarroll.getJSONArray("LIBRARYASSOCIATIONS");
		assert(las.length() == 2);
		JSONObject la = las.getJSONObject(1);

		JSONObject assoc = la.getJSONObject("ASSOCIATION");
		assert(assoc.getString("ID").equals("ALISE"));
	}
}
