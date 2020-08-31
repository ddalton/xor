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

package tools.xor.util;

import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.exim.AbstractExportImport;

public class Constants {

  /*
   *  Constants related to logging
   *  If there are problems with the log4j with an application using XOR, then
   *  turn on the following option to see if there are any problems with parsing the configuration
   *  -Dorg.apache.logging.log4j.simplelog.StatusLogger.level=DEBUG
   *  
   *  Usually xml config file has more problems due to unsupported features
   */
   
  public static class Log {
    public static final String OBJECT_WALKER = "object.graph.walker";
    public static final String STATE_GRAPH   = "state.graph";
    public static final String CYCLE_FINDER  = "cycle.finder";
    public static final String VIEW_BRANCH   = "view.branch";
	  public static final String QUERY_TRANSFORMER   = "query.transformer";
    
    public static final int DEBUG_DATA_SIZE = 64;
  }
  
  
  // Constants related to formatting
  public static class Format {
    public static final String INDENT_STRING = "   ";
    
    public static String getIndentString(int indentLevel) {
    	return (indentLevel > 0) ? (new String(new char[indentLevel]).replace("\0", Constants.Format.INDENT_STRING)) : "";
    }
  }

	// Constants related to XOR configuration
	public static class Config {
		public static final String TOPO_VISUAL = "toposort.visual";
		public static final String SQL_STACK = "sql.stacktrace";
		public static final String TOPO_SKIP = "toposort.skip";
		public static final String INCLUDE_EMBEDDED = "include.embedded";
		public static final String ACTIVATE_DETECTORS = "detectors.activate";
		public static final String GENERATOR_LINK_EXISTING = "generator.link.existing";
		public static final String REPULSION_MULTIPLIER = "graph.visual.repulsion";
		public static final String MAX_STRING_LEN = "max.string.length";
		public static final String MIGRATE_ENTITIES = "entities.to.migrate";
		public static final String MIGRATE_RELATIONSHIPS = "migrate.relationships.entities";
		public static final String MIGRATE_BATCH_SIZE = "migrate.batch.size";
		public static final String MIGRATE_FILTER_PARTITION = "migrate.filter.partition";
		public static final String INCLUDE_SUBCLASS = "include.subclass";
		public static final String BATCH_SKIP = "batch.skip";
		public static final String BATCH_COMMIT_SIZE = "batch.commit.size";
        public static final String IMPORTER_POOL_SIZE = "importer.pool.size";		
		public static final String QUERY_POOL_SIZE = "query.pool.size";
		public static final String QUERY_JOIN_TABLE = "query.join.table";
		public static final String EXCEL_STREAMING = "excel.streaming";
	}
  
  
  // Constants related to XOR serialization between JavaScript client and the Java server
  public static class XOR {
	  public static final String XOR_PREFIX = "XOR";
	  public static final String SEP = ":";
	  public static final String XOR_PATH_PREFIX = XOR_PREFIX + Settings.PATH_DELIMITER;

	  // boolean field used to represent if the object is a reference association
	  // i.e., if a dynamic external model object has only the natural key values and/or the id
	  // property it is considered to a reference association object. This flag will be set to
	  // true for such an object.
	  public static final String KEYREF = XOR_PATH_PREFIX + "keyref";

	  // Additional references to an object are represented using the following attribute prefix
	  public static final String IDREF = "|" + XOR_PREFIX + "|";

	  // Synthetic identifier added to each object instance to help identify each object in an object graph
	  public static final String ID = XOR_PATH_PREFIX + "id";

	  // Synthetic identifier to save the type, especially useful if the object is a subtype 
	  public static final String TYPE = XOR_PATH_PREFIX + "type";
	  
	  // Flag to indicate if that entry should be deleted. Mainly used during aggregate update.
	  public static final String DELETE = XOR_PATH_PREFIX + "delete";

	  // In migration, this field captures the source object's surrogate key value
	  public static final String SURROGATEID = XOR_PATH_PREFIX + "surrogateid";

	  // Represents the sheet containing the root object in the aggregate 
	  public static final String EXCEL_ENTITY_SHEET = "Entity";	  
	  public static final String EXCEL_SHEET_PREFIX = "Sheet";
	  public static final String EXCEL_INDEX_SHEET = "Relationships";
	  public static final String EXCEL_INFO_SHEET = "Info";

	  // Represents file names for CSV import
	  public static final String CSV_FILE_SUFFIX = ".csv";
	  public static final String CSV_ENTITY_SHEET = EXCEL_ENTITY_SHEET + CSV_FILE_SUFFIX;
	  public static final String CSV_INDEX_SHEET = EXCEL_INDEX_SHEET + CSV_FILE_SUFFIX;

	  // Domain values for data generation
	  public static final String DOMAIN_TYPE_SHEET = "Domain types";
	  
	  // Synthetic identifier added to each object instance to help identify each object in an object graph
	  public static final String OWNER_ID = XOR_PATH_PREFIX + "owner.id";
	  
	  public static final String CALLBACK = "_CALLBACK_";
	  public static final String GEN_PATH = "_PATH_";
	  public static final String GEN_PARENT = "_PARENT_";

	  // Property constraints
	  public static final String CONS_LENGTH = "_LENGTH_";
	  public static final String CONS_PRECISION = "_PRECISION_";
	  public static final String CONS_SCALE = "_SCALE_";

	  // REST related constants
	  public static final String REST_SETTINGS = "settings";
	  public static final String REST_ENTITY = "entity";

	  // Graph related
	  public static final int TOPO_ORDERING_START = 1;
	  
	  public static String getRelationshipName (Type type, Property property) {
		  return type.getName() + AbstractExportImport.PROPERTY_TYPE_DELIM + property.getName();
	  }

	  public static String walkDown(String path, Property property) {
		  if(property == null) {
			  return path;
		  }

		  if(path == null || "".equals(path)) {
			  return property.getName();
		  } else {
			  return path + Settings.PATH_DELIMITER + property.getName();
		  }
	  }
  }
}
