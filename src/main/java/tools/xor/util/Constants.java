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

public class Constants {

  // Constants related to logging
  public static class Log {
    public static final String OBJECT_WALKER = "object.graph.walker";
    public static final String STATE_GRAPH   = "state.graph";
    public static final String CYCLE_FINDER  = "cycle.finder";
    public static final String VIEW_BRANCH   = "view.branch";
    
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
	}
  
  
  // Constants related to XOR serialization between JavaScript client and the Java server
  public static class XOR {
	  public static final String XOR_PREFIX = "XOR";
	  public static final String SEP = ":";
	  public static final String XOR_PATH_PREFIX = XOR_PREFIX + Settings.PATH_DELIMITER;
	  
	  // Additional references to an object are represented using the following attribute prefix
	  public static final String OBJECTREF = "|" + XOR_PREFIX + "|";

	  // Synthetic identifier added to each object instance to help identify each object in an object graph
	  public static final String ID = XOR_PATH_PREFIX + "id";

	  // Synthetic identifier to save the type, especially useful if the object is a subtype 
	  public static final String TYPE = XOR_PATH_PREFIX + "type";
	  
	  // Flag to indicate if that entry should be deleted. Mainly used during aggregate update.
	  public static final String DELETE = XOR_PATH_PREFIX + "delete";

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

	  // Property constraints
	  public static final String CONS_LENGTH = "_LENGTH_";
	  public static final String CONS_PRECISION = "_PRECISION_";
	  public static final String CONS_SCALE = "_SCALE_";

	  // Graph related
	  public static final int TOPO_ORDERING_START = 1;
	  
	  public static String getExcelSheetFullName(Type type, Property property) {
		  return type.getName() + ":" + property.getName();
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
