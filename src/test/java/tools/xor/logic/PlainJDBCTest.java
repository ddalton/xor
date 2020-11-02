/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2019, Dilip Dalton
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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import tools.xor.EntityType;
import tools.xor.FunctionType;
import tools.xor.JDBCType;
import tools.xor.Settings;
import tools.xor.generator.Generator;
import tools.xor.generator.RangePercent;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.service.Shape;
import tools.xor.view.AggregateView;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-jdbc-test.xml" })
public class PlainJDBCTest
{
	private static final String SHAPE_NAME = "PlainJDBC";

	@Autowired
	protected AggregateManager am;

	@Autowired
	protected DataSource dataSource;

	private void printDatasource() {

		BasicDataSource bds = (BasicDataSource) dataSource;

		System.out.println("**************** DATASOURCE ***************");
		System.out.println("Max Active: " + bds.getMaxTotal());
		System.out.println("Max Idle: " + bds.getMaxIdle());
		System.out.println("Min Idle: " + bds.getMinIdle());
		System.out.println("Num Active: " + bds.getNumActive());
		System.out.println("Num Idle: " + bds.getNumIdle());
	}

	@BeforeEach
	public void init() throws SQLException, ClassNotFoundException, IOException
	{
		long time1 = System.nanoTime();
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();) {

			statement.execute("CREATE TABLE address "
					+ "(id VARCHAR(10) NOT NULL, "
					+ " street VARCHAR(256) NOT NULL, "
					+ " city VARCHAR(96) NOT NULL, "
					+ " county_province VARCHAR(30), "
					+ " zip_or_postcode VARCHAR(30) NOT NULL, "
					+ " country VARCHAR(30) NOT NULL, "
					+ " PRIMARY KEY(id))");

			statement.execute("CREATE TABLE library "
					               + "(id VARCHAR(10) NOT NULL, "
					               + " name VARCHAR(50) NOT NULL, "
					               + " address VARCHAR(10) NOT NULL, "
					               + " PRIMARY KEY(id))");
			statement.execute("ALTER TABLE library ADD CONSTRAINT FK_1__1_entity FOREIGN KEY(address) REFERENCES address(id)");

			statement.execute(
				"CREATE TABLE librarian "
					+ "(id VARCHAR(10) NOT NULL, "
					+ " name VARCHAR(50) NOT NULL,"
					+ " email VARCHAR(50) NOT NULL, "
					+ " library VARCHAR(10) NOT NULL, "
					+ " PRIMARY KEY (id))");
			statement.execute(
				"ALTER TABLE librarian ADD CONSTRAINT FK_1__N_librarians FOREIGN KEY(library) REFERENCES library(id)");

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
			statement.execute("ALTER TABLE librarianassociation ADD CONSTRAINT FK2_1__N_libraryassociations FOREIGN KEY(librarian) REFERENCES librarian(id)");
			statement.execute(
				"ALTER TABLE librarianassociation ADD CONSTRAINT FK3_1__N_libraryassociations FOREIGN KEY(association) REFERENCES association(id)");

			long time2 = System.nanoTime();
			System.out.println("[Create tables - " + ((time2 - time1) / 1000) + " μs");

			populate(connection, statement);
			long time3 = System.nanoTime();
			System.out.println("[Populate tables - " + ((time3 - time2) / 1000) + " μs");

			connection.commit();
			long time4 = System.nanoTime();
			System.out.println("[Commit seed data - " + ((time4 - time3) / 1000) + " μs");

			// build the shape
			am.getDataModel().createShape(SHAPE_NAME);
		}
	}

	protected void populate(Connection connection, Statement statement) throws SQLException
	{
		/*
		long timeA = System.nanoTime();
		statement.executeUpdate("INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A101', '96 Euston Rd', 'London', 'NW1 2DB', 'UK')");
		statement.executeUpdate("INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A102', 'University of Oxford, Broad St', 'Oxford', 'OX1 3BG', 'UK')");
		statement.executeUpdate("INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A103', 'William Brown St', 'Liverpool', 'L3 8EW', 'UK')");
		statement.executeUpdate("INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A104', 'Parliament Square', 'Edinburgh', 'EH1 1RF', 'UK')");
		statement.executeUpdate("INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A105', 'Broad St', 'Oxford', 'OX1 3BG', 'UK')");
		statement.executeUpdate("INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A106', '630 W. 5th Street', 'Los Angeles', '90071', 'USA')");
		statement.executeUpdate("INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A107', '101 Independence Ave SE', 'Washington', '20540', 'USA')");
		statement.executeUpdate("INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A108', 'Cromwell Rd.', 'London', 'SW7 5BD', 'UK')");


		// British libraries
		statement.executeUpdate("INSERT INTO library (id, name, address) VALUES ('L100', 'British Library', 'A101')");
		long timeB = System.nanoTime();
		System.out.println("[Populate 2 (executeUpdate) - " + ((timeB - timeA) / 1000) + " μs");
		statement.executeUpdate("INSERT INTO library (id, name, address) VALUES ('L110', 'Duke Humfrey’s Library', 'A102')");
		statement.executeUpdate("INSERT INTO library (id, name, address) VALUES ('L111', 'Liverpool Central Library', 'A103')");
		statement.executeUpdate("INSERT INTO library (id, name, address) VALUES ('L112', 'Signet Library', 'A104')");
		statement.executeUpdate("INSERT INTO library (id, name, address) VALUES ('L113', 'Bodlein Library', 'A105')");

		// US libraries
		statement.executeUpdate("INSERT INTO library (id, name, address) VALUES ('L201', 'Central Library', 'A106')");
		statement.executeUpdate("INSERT INTO library (id, name, address) VALUES ('L202', 'Library of Congress', 'A107')");

		statement.executeUpdate("INSERT INTO librarian (id, name, email, library) VALUES ('1001','Thomas Bodley', 'tbodley@somewhere.com', 'L113')");
		statement.executeUpdate("INSERT INTO librarian (id, name, email, library) VALUES ('1002','Lewis Carroll', 'lcarroll@somewhere.com', 'L100')");
		statement.executeUpdate("INSERT INTO librarian (id, name, email, library) VALUES ('1003','Alison Bailey', 'abailey@somewhere.com', 'L100')");
		statement.executeUpdate("INSERT INTO librarian (id, name, email, library) VALUES ('1004','Alasdair Ball', 'aball@somewhere.com', 'L100')");
		statement.executeUpdate("INSERT INTO librarian (id, name, email, library) VALUES ('1010','Benjamin Franklin', 'bfranklin@somewhere.com', 'L202')");

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
*/

		/*
		PreparedStatement ps = connection.prepareStatement("INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES(?, ?, ?, ?, ?)");
		ps.setString(1, "A101");ps.setString(2, "96 Euston Rd");ps.setString(3, "London");ps.setString(4, "NW1 2DB");ps.setString(5, "UK");ps.addBatch();
		ps.setString(1, "A102");ps.setString(2, "University of Oxford, Broad St");ps.setString(3, "Oxford");ps.setString(4, "OX1 3BG");ps.setString(5, "UK");ps.addBatch();
		ps.setString(1, "A103");ps.setString(2, "William Brown St");ps.setString(3, "Liverpool");ps.setString(4, "L3 8EW");ps.setString(5, "UK");ps.addBatch();
		ps.setString(1, "A104");ps.setString(2, "Parliament Square");ps.setString(3, "Edinburgh");ps.setString(4, "EH1 1RF");ps.setString(5, "UK");ps.addBatch();
		ps.setString(1, "A105");ps.setString(2, "Broad St");ps.setString(3, "Oxford");ps.setString(4, "OX1 3BG");ps.setString(5, "UK");ps.addBatch();
		ps.setString(1, "A106");ps.setString(2, "630 W. 5th Street");ps.setString(3, "Los Angeles");ps.setString(4, "90071");ps.setString(5, "USA");ps.addBatch();
		ps.setString(1, "A107");ps.setString(2, "101 Independence Ave SE");ps.setString(3, "Washington");ps.setString(4, "20540");ps.setString(5, "USA");ps.addBatch();
		ps.setString(1, "A108");ps.setString(2, "Cromwell Rd.");ps.setString(3, "London");ps.setString(4, "NW1 2DB");ps.setString(5, "UK");ps.addBatch();
		ps.executeBatch();

		ps = connection.prepareStatement("INSERT INTO library (id, name, address) VALUES(?, ?, ?)");
		ps.setString(1, "L100");ps.setString(2, "British Library");ps.setString(3, "A101");ps.addBatch();
		ps.setString(1, "L110");ps.setString(2, "Duke Humfrey’s Library");ps.setString(3, "A102");ps.addBatch();
		ps.setString(1, "L111");ps.setString(2, "Liverpool Central Library");ps.setString(3, "A103");ps.addBatch();
		ps.setString(1, "L112");ps.setString(2, "Signet Library");ps.setString(3, "A104");ps.addBatch();
		ps.setString(1, "L113");ps.setString(2, "Bodlein Library");ps.setString(3, "A105");ps.addBatch();
		ps.setString(1, "L201");ps.setString(2, "Central Library");ps.setString(3, "A106");ps.addBatch();
		ps.setString(1, "L202");ps.setString(2, "Library of Congress");ps.setString(3, "A107");ps.addBatch();
		ps.executeBatch();

		ps = connection.prepareStatement("INSERT INTO librarian (id, name, email, library) VALUES(?, ?, ?, ?)");
		ps.setString(1, "1001");ps.setString(2, "Thomas Bodley");ps.setString(3, "tbodley@somewhere.com");ps.setString(4, "L113");ps.addBatch();
		ps.setString(1, "1002");ps.setString(2, "Lewis Carroll");ps.setString(3, "lcarroll@somewhere.com");ps.setString(4, "L100");ps.addBatch();
		ps.setString(1, "1003");ps.setString(2, "Alison Bailey");ps.setString(3, "abailey@somewhere.com");ps.setString(4, "L100");ps.addBatch();
		ps.setString(1, "1004");ps.setString(2, "Alasdair Ball");ps.setString(3, "aball@somewhere.com");ps.setString(4, "L100");ps.addBatch();
		ps.setString(1, "1010");ps.setString(2, "Benjamin Franklin");ps.setString(3, "bfranklin@somewhere.com");ps.setString(4, "L202");ps.addBatch();
		ps.executeBatch();

		ps = connection.prepareStatement("INSERT INTO association (id, name, state) VALUES(?, ?, ?)");
		ps.setString(1, "ALA");ps.setString(2, "American Library Association");ps.setString(3, "Illinois");ps.addBatch();
		ps.setString(1, "ALISE");ps.setString(2, "Association for Library and Information Science Education");ps.setString(3, "Illinois");ps.addBatch();
		ps.setString(1, "ARLIS");ps.setString(2, "Art Libraries Society of North America");ps.setString(3, "Wisconsin");ps.addBatch();
		ps.setString(1, "MLA");ps.setString(2, "Medical Library Association");ps.setString(3, "Illinois");ps.addBatch();
		ps.setString(1, "AALL");ps.setString(2, "American Association of Law Libraries");ps.setString(3, "Illinois");ps.addBatch();
		ps.setString(1, "CILIP");ps.setString(2, "Chartered Institute of Library and Information Professionals");ps.setString(3, "London");ps.addBatch();
		ps.executeBatch();

		ps = connection.prepareStatement("INSERT INTO librarianassociation (librarian, association) VALUES(?, ?)");
		ps.setString(1, "1002");ps.setString(2, "CILIP");ps.addBatch();
		ps.setString(1, "1002");ps.setString(2, "ALISE");ps.addBatch();
		ps.setString(1, "1003");ps.setString(2, "CILIP");ps.addBatch();
		ps.setString(1, "1004");ps.setString(2, "CILIP");ps.addBatch();
		ps.executeBatch();
*/

		// In HANA DB foreign keys affect insert performance.
		// If possible add foreign keys (i.e., relationships) only at the meta level

		long timeA = System.nanoTime();
		statement.addBatch(
			"INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A101', '96 Euston Rd', 'London', 'NW1 2DB', 'UK')");
		statement.addBatch(
			"INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A102', 'University of Oxford, Broad St', 'Oxford', 'OX1 3BG', 'UK')");
		statement.addBatch(
			"INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A103', 'William Brown St', 'Liverpool', 'L3 8EW', 'UK')");
		statement.addBatch(
			"INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A104', 'Parliament Square', 'Edinburgh', 'EH1 1RF', 'UK')");
		statement.addBatch(
			"INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A105', 'Broad St', 'Oxford', 'OX1 3BG', 'UK')");
		statement.addBatch(
			"INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A106', '630 W. 5th Street', 'Los Angeles', '90071', 'USA')");
		statement.addBatch(
			"INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A107', '101 Independence Ave SE', 'Washington', '20540', 'USA')");
		statement.addBatch(
			"INSERT INTO address (id, street, city, zip_or_postcode, country) VALUES ('A108', 'Cromwell Rd.', 'London', 'SW7 5BD', 'UK')");
		statement.executeBatch();

		// British libraries
		statement.addBatch(
			"INSERT INTO library (id, name, address) VALUES ('L100', 'British Library', 'A101')");
		long timeB = System.nanoTime();
		System.out.println("[Populate 2 (no executeBatch) - " + ((timeB - timeA) / 1000) + " μs");
		statement.executeBatch();
		long timeC = System.nanoTime();
		System.out.println("[Populate 2 (only executeBatch) - " + ((timeC - timeB) / 1000) + " μs");

		long time1 = System.nanoTime();


		statement.addBatch(
			"INSERT INTO library (id, name, address) VALUES ('L110', 'Duke Humfrey’s Library', 'A102')");
		statement.addBatch(
			"INSERT INTO library (id, name, address) VALUES ('L111', 'Liverpool Central Library', 'A103')");
		statement.addBatch(
			"INSERT INTO library (id, name, address) VALUES ('L112', 'Signet Library', 'A104')");
		statement.addBatch(
			"INSERT INTO library (id, name, address) VALUES ('L113', 'Bodlein Library', 'A105')");

		// US libraries
		statement.addBatch(
			"INSERT INTO library (id, name, address) VALUES ('L201', 'Central Library', 'A106')");
		statement.addBatch(
			"INSERT INTO library (id, name, address) VALUES ('L202', 'Library of Congress', 'A107')");

		statement.addBatch(
			"INSERT INTO librarian (id, name, email, library) VALUES ('1001','Thomas Bodley', 'tbodley@somewhere.com', 'L113')");
		statement.addBatch(
			"INSERT INTO librarian (id, name, email, library) VALUES ('1002','Lewis Carroll', 'lcarroll@somewhere.com', 'L100')");
		statement.addBatch(
			"INSERT INTO librarian (id, name, email, library) VALUES ('1003','Alison Bailey', 'abailey@somewhere.com', 'L100')");
		statement.addBatch(
			"INSERT INTO librarian (id, name, email, library) VALUES ('1004','Alasdair Ball', 'aball@somewhere.com', 'L100')");
		statement.addBatch(
			"INSERT INTO librarian (id, name, email, library) VALUES ('1010','Benjamin Franklin', 'bfranklin@somewhere.com', 'L202')");

		statement.addBatch(
			"INSERT INTO association (id, name, state) VALUES ('ALA','American Library Association', 'Illinois')");
		statement.addBatch(
			"INSERT INTO association (id, name, state) VALUES ('ALISE','Association for Library and Information Science Education', 'Illinois')");
		statement.addBatch(
			"INSERT INTO association (id, name, state) VALUES ('ARLIS','Art Libraries Society of North America', 'Wisconsin')");
		statement.addBatch(
			"INSERT INTO association (id, name, state) VALUES ('MLA','Medical Library Association', 'Illinois')");
		statement.addBatch(
			"INSERT INTO association (id, name, state) VALUES ('AALL','American Association of Law Libraries', 'Illinois')");
		statement.addBatch(
			"INSERT INTO association (id, name, state) VALUES ('CILIP','Chartered Institute of Library and Information Professionals', 'London')");

		statement.addBatch(
			"INSERT INTO librarianassociation (librarian, association) VALUES ('1002','CILIP')");
		statement.addBatch(
			"INSERT INTO librarianassociation (librarian, association) VALUES ('1002','ALISE')");
		statement.addBatch(
			"INSERT INTO librarianassociation (librarian, association) VALUES ('1003','CILIP')");
		statement.addBatch("INSERT INTO librarianassociation (librarian, association) VALUES ('1004','CILIP')");

		long time2 = System.nanoTime();
		System.out.println("[Populate (no executeBatch) - " + ((time2 - time1) / 1000) + " μs");

		statement.executeBatch();
		long time3 = System.nanoTime();
		System.out.println("[Populate (only executeBatch) - " + ((time3 - time2) / 1000) + " μs");
	}

	@AfterEach
	public void destroy() throws SQLException, ClassNotFoundException, IOException {

		long time1 = System.nanoTime();
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();) {
			statement.executeUpdate("DROP TABLE librarianassociation");
			statement.executeUpdate("DROP TABLE association");
			statement.executeUpdate("DROP TABLE librarian");
			statement.executeUpdate("DROP TABLE library");
			statement.executeUpdate("DROP TABLE address");
			connection.commit();
		}
		long time2 = System.nanoTime();
		System.out.println("[Drop tables - " + ((time2 - time1) / 1000) + " μs");

		am.getDataModel().removeShape(SHAPE_NAME);
	}

	@Test
	public void jsonSelect() {
		String jsonString = "{ \"normalized\": false, \"view\" : { \"attributeList\" : [\"count\"], \"nativeQuery\" : { \"selectClause\": \"SELECT count(*) FROM librarian\" } } }";

		DataModel das = am.getDataModel();

		am.configure(null);
		JDBCSessionContext sc = ((JDBCDataStore)am.getDataStore()).getSessionContext();
		sc.beginTransaction();

		Settings settings = das.settings().json(jsonString).build();

		Object result = am.dml(settings);

		assert(result instanceof List);

		List list = (List) result;
		assert(list.size() == 2);

		sc.close();
	}

	@Test
	public void selectView() {
		DataModel das = am.getDataModel();
		Shape shape = das.getShape(SHAPE_NAME);

		am.configure(null);
		JDBCSessionContext sc = ((JDBCDataStore)am.getDataStore()).getSessionContext();
		sc.beginTransaction();

		JSONObject address = new JSONObject().put("ID", "A108");

		// create library
		JSONObject json = new JSONObject();
		json.put("ID", "L101");
		json.put("NAME", "Natural History Museum");
		json.put("ADDRESS", new JSONObject().put("ID", "A108"));

		Settings settings = new Settings();
		JDBCSessionContext context = new JDBCSessionContext((JDBCDataStore)am.getDataStore(), null);
		context.process(address, (EntityType) shape.getType("address"));
		settings.setSessionContext(context);

		JDBCType type = (JDBCType) shape.getType("library");
		settings.setEntityType(type);
		settings.init(shape);
		Object obj = am.create(json, settings);

		assert(obj != null);

		//Object jsonObject = am.read(obj, settings);
		List<?> toList = am.query(json, settings);
		assert(toList.size() == 1);
		System.out.println("JSON string: " + json.toString());

		// Need to commit to release the locks on HANA
		sc.close();
	}

	@Test
	public void queryEntity() {
		DataModel das = am.getDataModel();
		Shape shape = das.getShape(SHAPE_NAME);

		am.configure(null);
		JDBCSessionContext sc = ((JDBCDataStore)am.getDataStore()).getSessionContext();
		sc.beginTransaction();

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
		JDBCType type = (JDBCType) shape.getType("librarian");
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

		sc.close();
	}


	@Test
	public void queryCollection() {
		DataModel das = am.getDataModel();
		Shape shape = das.getShape(SHAPE_NAME);

		am.configure(null);

		JDBCSessionContext sc = ((JDBCDataStore)am.getDataStore()).getSessionContext();
		sc.beginTransaction();

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
		JDBCType type = (JDBCType) shape.getType("library");
		settings.setEntityType(type);
		settings.setView(view);
		settings.init(shape);

		List<?> toList = am.query(json, settings);
		assert(toList.size() == 1);
		JSONObject library = (JSONObject)toList.get(0);
		JSONArray librarians = library.getJSONArray("LIBRARIANS");
		assert(librarians != null);
		assert(librarians.length() == 3);

		sc.close();
	}

	@Test
	public void queryManyToMany() {
		DataModel das = am.getDataModel();
		Shape shape = das.getShape(SHAPE_NAME);

		am.configure(null);
		JDBCSessionContext sc = ((JDBCDataStore)am.getDataStore()).getSessionContext();
		sc.beginTransaction();

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
		JDBCType type = (JDBCType) shape.getType("library");
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
		JSONObject la = las.getJSONObject(0);

		JSONObject assoc = la.getJSONObject("ASSOCIATION");
		assert(assoc.getString("ID").equals("ALISE"));

		sc.close();
	}

	@Test
	public void testDeeplyNested() {
		DataModel das = am.getDataModel();
		Shape shape = das.getShape(SHAPE_NAME);

		am.configure(null);
		JDBCSessionContext sc = ((JDBCDataStore)am.getDataStore()).getSessionContext();
		sc.beginTransaction();

		// create library
		JSONObject json = new JSONObject();
		json.put("ID", "L100");


		// Find all the librarians of a library and get the associations and the
		// libraries involved with those associations, and get the country where those libraries
		// are located
		// So for British Library
		AggregateView view = new AggregateView();
		List<String> attributes = new ArrayList<>();
		view.setAttributeList(attributes);
		attributes.add("ID");
		attributes.add("NAME");
//		attributes.add("LIBRARIANS.LIBRARYASSOCIATIONS.ASSOCIATION.LIBRARYASSOCIATIONS.LIBRARIAN.LIBRARY.ADDRESS.COUNTRY");
		attributes.add("LIBRARIANS.LIBRARYASSOCIATIONS.ASSOCIATION.ID");
		attributes.add("LIBRARIANS.LIBRARYASSOCIATIONS.ASSOCIATION.NAME");

		// This helps with object reconstitution. Without this, the query framework should
		// add this to the query
//		attributes.add("LIBRARIANS.ID");

		Settings settings = new Settings();
		JDBCType type = (JDBCType) shape.getType("library");
		settings.setEntityType(type);
		settings.setView(view);
		settings.init(shape);
		view.addFunction(FunctionType.ASC, 1, "LIBRARIANS.NAME");

		List<?> toList = am.query(json, settings);
		assert(toList.size() == 1);
		JSONObject library = (JSONObject)toList.get(0);
		JSONArray librarians = library.getJSONArray("LIBRARIANS");
		assert(librarians != null);
		assert(librarians.length() == 3);

		sc.close();
	}

	@Test
	public void testDeeplyNested2() {
		DataModel das = am.getDataModel();
		Shape shape = das.getShape(SHAPE_NAME);

		am.configure(null);
		JDBCSessionContext sc = ((JDBCDataStore)am.getDataStore()).getSessionContext();
		sc.beginTransaction();

		// create library
		JSONObject json = new JSONObject();
		json.put("ID", "L100");


		// Find all the librarians of a library and get the associations and the
		// libraries involved with those associations, and get the country where those libraries
		// are located
		// So for British Library
		AggregateView view = new AggregateView();
		List<String> attributes = new ArrayList<>();
		view.setAttributeList(attributes);
		attributes.add("ID");
		attributes.add("NAME");
		attributes.add("LIBRARIANS.LIBRARYASSOCIATIONS.ASSOCIATION.LIBRARYASSOCIATIONS.LIBRARIAN.LIBRARY.ADDRESS.COUNTRY");
		//attributes.add("LIBRARIANS.LIBRARYASSOCIATIONS.ASSOCIATION.ID");
		//attributes.add("LIBRARIANS.LIBRARYASSOCIATIONS.ASSOCIATION.NAME");

		// This helps with object reconstitution. Without this, the query framework should
		// add this to the query
		//		attributes.add("LIBRARIANS.ID");

		Settings settings = new Settings();
		JDBCType type = (JDBCType) shape.getType("library");
		settings.setEntityType(type);
		settings.setView(view);
		settings.init(shape);

		List<?> toList = am.query(json, settings);
		assert(toList.size() == 1);
		JSONObject library = (JSONObject)toList.get(0);
		JSONArray librarians = library.getJSONArray("LIBRARIANS");
		assert(librarians != null);
		assert(librarians.length() == 3);

		// Get the first librarian
		JSONObject lib1002 = librarians.getJSONObject(0);
		assert(lib1002.getString("ID").equals("1002"));
		JSONArray lib1002la = lib1002.getJSONArray("LIBRARYASSOCIATIONS");
		assert(lib1002la != null);
		assert(lib1002la.length() == 2);
		JSONObject alise = lib1002la.getJSONObject(0);
		JSONObject cilip = lib1002la.getJSONObject(1);
		validateAlise(alise);
		validateCilip(cilip);

		// Get the second librarian
		JSONObject lib1003 = librarians.getJSONObject(1);
		assert(lib1003.getString("ID").equals("1003"));
		JSONArray lib1003la = lib1003.getJSONArray("LIBRARYASSOCIATIONS");
		assert(lib1003la != null);
		assert(lib1003la.length() == 1);
		cilip = lib1003la.getJSONObject(0);
		validateCilip(cilip);

		// Get the third librarian
		JSONObject lib1004 = librarians.getJSONObject(2);
		assert(lib1004.getString("ID").equals("1004"));
		JSONArray lib1004la = lib1003.getJSONArray("LIBRARYASSOCIATIONS");
		assert(lib1004la != null);
		assert(lib1004la.length() == 1);
		cilip = lib1004la.getJSONObject(0);
		validateCilip(cilip);

		sc.close();
	}

	private void validateAlise(JSONObject la) {
		JSONObject assoc = la.getJSONObject("ASSOCIATION");
		assert(assoc != null);
		assert(assoc.getString("ID").equals("ALISE"));

		JSONArray las = assoc.getJSONArray("LIBRARYASSOCIATIONS");
		assert(las.length() == 1);
		JSONObject libassoc = las.getJSONObject(0);
		JSONObject librarian = libassoc.getJSONObject("LIBRARIAN");
		assert(librarian.getString("ID").equals("1002"));

		JSONObject library = librarian.getJSONObject("LIBRARY");
		assert(library.getString("ID").equals("L100"));

		JSONObject address = library.getJSONObject("ADDRESS");
		assert(address.getString("COUNTRY").equals("UK"));
	}

	private void validateCilip(JSONObject la) {
		JSONObject assoc = la.getJSONObject("ASSOCIATION");
		assert(assoc != null);
		assert(assoc.getString("ID").equals("CILIP"));

		JSONArray las = assoc.getJSONArray("LIBRARYASSOCIATIONS");
		assert(las.length() == 3);

		JSONObject librarian1 = las.getJSONObject(0).getJSONObject("LIBRARIAN");
		JSONObject librarian2 = las.getJSONObject(1).getJSONObject("LIBRARIAN");
		JSONObject librarian3 = las.getJSONObject(2).getJSONObject("LIBRARIAN");

		assert(librarian1.getString("ID").equals("1002"));
		assert(librarian2.getString("ID").equals("1003"));
		assert(librarian3.getString("ID").equals("1004"));
	}

	@Test
	public void testRangePercent() {
		Generator parentgen = new RangePercent(new String[] {"ID_[__]",
															 "0,3:0.06",
															 "4,15:0.18",
															 "16,35:0.30",
															 "36,85:0.36",
															 "86,2576:1.00"
		});

		Short val = parentgen.getShortValue(null);
		System.out.println(val);
	}
}
