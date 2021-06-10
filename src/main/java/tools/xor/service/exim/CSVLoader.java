/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2020, Dilip Dalton
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

package tools.xor.service.exim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.BasicType;
import tools.xor.BusinessObject;
import tools.xor.CounterGenerator;
import tools.xor.DataImporter;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.GeneratorDriver;
import tools.xor.ImmutableBO;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.generator.Generator;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.DataModelFactory;
import tools.xor.service.DomainShape;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.util.Edge;
import tools.xor.util.State;
import tools.xor.util.graph.DirectedSparseGraph;
import tools.xor.util.graph.StateGraph;

/**
 * 
 * CSVLoader is responsible for loading data from CSV files directly into database using JDBC.
 * The CSV file is made up of 3 sections:
 * 
 * Section 1 (1 line): Header section. A comma separated list of the column names 
 *    that need to be populated into the DB
 * Section 2 (1 line): Schema for this CSV file. The schema is in JSON format and
 *    provides information about the data in the CSV file and any foreign key relationships
 *    that need to be satisfied. The schema is detailed later in the section below.
 * Section 3 (remaining lines): lines of Comma separated list of the column values
 * 
 * Sample CSV file below (In line 2, the JSON is formatted across multiple lines for readability):
 * 
 *    *********************************************************  
 *    *      S E C T I O N   1    -   H E A D E R             *
 *    *********************************************************
 *    Icol1, Icol2, Icol3, Icol4, Icol5, Icol6, ... 
 * 
 *    NOTE: the columns that get values are the "columns" key in the JSON schema and not
 *    section 1. This is because there are some columns that represent foreign keys and
 *    need to be resolved using the JSON schema.
 *    If the columns names are not the same as table names then we need to provide aliases
 * 
 *    *********************************************************  
 *    *      S E C T I O N   2    -   S C H E M A             *
 *    *********************************************************
 *    The JSON Schema is needed to interpret the data (Section 3).
 *    NOTE: The schema is represented as a single line in the CSV file  
 *    
 *    Some parts of the schema are optional depending on the table structure and what
 *    columns need to be populated.
 *    
 *    Required fields
 *    ---------------
 *    tableName  - The name of the table
 *    
 *    Optional fields
 *    ---------------
 *    entityName       - Java entity name, typically the fully qualified class name of the entity
 *    columnAliases    - Map the header name to a column name
 *    dateFormat       - If there is at least a single column of type Date
 *    keys             - Represents the fields that unique identify the row in the table
 *                       The columns in the keys cannot comprise foreign key columns
 *                       This field is needed when updating NULL foreign key columns
 *    foreignKeys      - Columns that need to be populated based on foreign key relationships
 *                       These columns are not part of header columns in Section 1
 *                       So this section provides info on how they can be populated using
 *                       the "foreignKeyTable", "select" and "join" information.
 *                  
 *                       In the example below, the foreign key column "col2" is populated
 *                       by selecting the id column from the FKTABLE1 table under the
 *                       join condition(s) restriction.
 *                  
 *                       SELECT f.id FROM FKTABLE1 f WHERE FKTcol3 = :Icol2  
 *                      
 *    columnGenerators - Useful for generating values for missing columns    
 *    dependsOn        - Useful to explicitly state the dependency of loading
 *                       table data. This is useful if the dependency cannot
 *                       be inferred from the foreign keys alone, or
 *                       the data is dynamically created using a SQL query.
 *    entityGenerator  - Useful for generating data for a table that does
 *                       not have data in an existing CSV file.
 *                       Most common usecase is using a QueryGenerator to get
 *                       the data from other tables.
 *                       When this is set, then the dependsOn property is
 *                       also usually set.     
 *    columnGenerator  - The lookup key for resolving a foreign key can have
 *                       a generator, if the data is purely based on generated data.
 *                       i.e., there is no CSV data in the file, but just generators in
 *                       the schema.                   
 *    
 * 
 *    Example:
 *    =======
 *    {
 *      "entityName" : "tools.xor.db.pm.Task",
 *      "tableName"  : "TASK",
 *      "dateFormat" : "yyyy-MM-dd HH:mm:ss", 
 *      "columnAliases" : {
 *          "col1" : "Icol1",
 *          "col2" : "Icol1",
 *          "col3" : "Icol2"
 *      }
 *      "keys" : ["col1"],
 *      "foreignKeys" : [
 *          {
 *             "foreignKey"      : "col2",
 *             "foreignKeyTable" : "FKTABLE1",
 *             "select"          : "id",
 *             "join"            : [
 *                                    { "FKTcol3" : "Icol2" }
 *                                 ]
 *          },
 *          {
 *             "foreignKey"      : "col5",
 *             "foreignKeyTable" : "FKTABLE2",
 *             "select"          : "id",
 *             "join"            : [
 *                                    { "FKTcol2" : "Icol3" },
 *                                    { "FKTcol4" : "Icol4" }
 *                                 ]
 *          },
 *          {
 *             "foreignKey"      : "col7",
 *             "foreignKeyTable" : "FKTABLE3",
 *             "select"          : "id",
 *             "join"            : [
 *                                    { 
 *                                       "FKTcol1" : "Icol9"
 *                                       "columnGenerator" : {
 *                                          "className" : "tools.xor.generator.StringTemplate",
 *                                          "arguments" : ["ID_[VISITOR_CONTEXT]"]
 *                                       } 
 *                                    },
 *                                    { 
 *                                      "FKTcol2" : "Icol11" 
 *                                    }
 *                                 ]
 *          }
 *       ],
 *       "dependsOn" : ["TABLE1", "TABLE2"],
 *       "entityGenerator" : {
 *          "className" : "tools.xor.QueryGenerator",
 *          "arguments" : ["SELECT t1.col1, t1.col2, t2.col3 FROM TABLE1 t1, TABLE2 t2 WHERE t1.t2id = t2.id"]
 *       },
 *       "columnGenerators" : [
 *          {
 *             "column" : "col10",
 *             "className" : "tools.xor.generator.StringTemplate",
 *             "arguments" : ["ID_[VISITOR_CONTEXT]"]
 *          }
 *       ]
 *    }
 * 
 *    *********************************************************  
 *    *      S E C T I O N   3    -   D A T A                 *
 *    *********************************************************
 *    data11, data12, data13, data14 .....
 *    data21, data22, data23, data24 .....
 * 
 * 
 * The data population is done in multiple passes.
 * Initial pass - Build the states by reading the schema
 * Second pass - INSERT
 *    The data is inserted into the tables.
 *    The non-foreign key and NOT NULL foreign key columns are populated during this phase
 *    
 *    In order to populate the NOT NULL foreign key columns, the entities need to be loaded
 *    in a particular order. This is automatically handled by the XOR tool.
 *    
 * Third pass - UPDATE
 *    In this pass all the remaining NULL foreign key columns are updated
 *
 */
public class CSVLoader implements Callable {
    
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());    
    
    private static final int COUNTER_START = 1;
    
    private Shape shape;
    private DirectedSparseGraph<CSVState, Edge<CSVState>> orderingGraph;
    private Map<String, CSVState> tableStateMap;
    private String folderPath;
    private StateGraph.ObjectGenerationVisitor visitor;
    
    // Used in parallel execution
    private Integer jobNo; // starts from 0
    private Settings settings;
    private JDBCDataStore dataStore;
    private Integer numThreads;
    
    private static final String KEY_ENTITY_NAME = "entityName";
    private static final String KEY_TABLE_NAME = "tableName";
    private static final String KEY_DATE_FORMAT = "dateFormat";
    private static final String KEY_COLUMN_ALIASES = "columnAliases";
    private static final String KEY_KEYS = "keys";
    private static final String KEY_FOREIGN_KEYS = "foreignKeys";
    private static final String KEY_FOREIGN_KEY_TABLE = "foreignKeyTable";
    private static final String KEY_FOREIGN_KEY = "foreignKey";
    private static final String KEY_JOIN = "join";
    private static final String KEY_SELECT = "select";
    private static final String KEY_COLUMN_GENERATORS = "columnGenerators";
    private static final String KEY_COLUMN_GENERATOR = "columnGenerator";    
    private static final String KEY_COLUMN = "column";
    private static final String KEY_CLASSNAME = "className";
    private static final String KEY_ARGUMENTS = "arguments";
    private static final String KEY_DEPENDS_ON = "dependsOn";
    private static final String KEY_ENTITY_GENERATOR = "entityGenerator";
    
    public static class CSVState extends State {
        
        private final JSONObject schema; 
        private final CSVLoader loader;
        private final String csvFile;
        private Map<String, Integer> headerMap;
        private List<JSONObject> notNullForeignKeys;
        private List<JSONObject> nullableForeignKeys;
        private SimpleDateFormat dateFormatter;
        private GeneratorDriver entityGenerator;
        private Set<String> dependsOn;
        private CSVPrinter csvPrinter;
        private Map<String, String> columnAliases;

        public CSVState(Type type, JSONObject schema, String csvFile, CSVLoader loader, Shape childShape) {
            // make a copy of the type and add it to the child shape
            super(((EntityType)type).copy(childShape), false);
            
            // update the type to point to the copy
            type = getType();
            
            this.schema = schema;
            this.loader = loader;
            this.csvFile = csvFile;
            this.headerMap = new HashMap<>();
            this.notNullForeignKeys = new ArrayList<>();
            this.nullableForeignKeys = new ArrayList<>();
            this.entityGenerator = new CounterGenerator(-1, 1);
            this.dependsOn = new HashSet<>();
            this.columnAliases = new HashMap<>();          
            
            try(BufferedReader reader = getReader(csvFile)) {
                if (reader.ready()) {
                    String header = reader.readLine();
                    // We are not doing anything with the header at this point
                    
                    CSVParser headerParser = CSVParser.parse(header, CSVFormat.DEFAULT.withHeader());
                    Map<String, Integer> headerColumns = headerParser.getHeaderMap();
                    
                    // Convert the header columns to uppercase
                    for(Map.Entry<String, Integer> entry: headerColumns.entrySet()) {
                        // Normalize the header map by trimming whitespaces and converting
                        // to uppercase
                        headerMap.put(normalize(entry.getKey()), entry.getValue());
                    }
                }
            } catch(IOException e) {
                throw ClassUtil.wrapRun(e);
            }
            
            if(schema.has(KEY_COLUMN_ALIASES)) {
                JSONObject json = schema.getJSONObject(KEY_COLUMN_ALIASES);
                Iterator<String> keyIter = json.keys();
                while(keyIter.hasNext()) {
                    String key = keyIter.next();
                    this.columnAliases.put(normalize(key), normalize(json.getString(key)));
                    logger.info(String.format("Column alias - Key %s, Value %s", key, json.getString(key)));
                }
            }
            
            if(schema.has(KEY_FOREIGN_KEYS)) {
                JSONArray fkeys = this.schema.getJSONArray(KEY_FOREIGN_KEYS);
                for(int i = 0; i < fkeys.length(); i++) {
                    JSONObject fkey = fkeys.getJSONObject(i);
                    String fkColumn = normalize(fkey.getString(KEY_FOREIGN_KEY));
                    Property property = getType().getProperty(fkColumn);
                    if(property == null) {
                        throw new RuntimeException(String.format("Unable to find column %s in table %s. Check if the schema has the correct table name.", fkColumn, schema.getString(KEY_TABLE_NAME)));
                    }
                    
                    if(!property.isNullable()) {
                        notNullForeignKeys.add(fkey);
                    } else {
                        nullableForeignKeys.add(fkey);
                    }
                    
                    extractColumnGenerator(fkey);
                }
            }
            
            if(schema.has(KEY_DATE_FORMAT)) {
                String dateFormat = this.schema.getString(KEY_DATE_FORMAT);
                this.dateFormatter = new SimpleDateFormat(dateFormat);
            }
            
            // initialize generators
            if(schema.has(KEY_COLUMN_GENERATORS)) {
                JSONArray jsonArray = schema.getJSONArray(KEY_COLUMN_GENERATORS);
                for(int i = 0; i < jsonArray.length(); i++) {
                    JSONObject generator = jsonArray.getJSONObject(i);
                    setGenerator(generator, getType(), generator.getString(KEY_COLUMN));
                }
            }
            
            if(schema.has(KEY_DEPENDS_ON)) {
                JSONArray jsonArray = schema.getJSONArray(KEY_DEPENDS_ON);
                for(int i = 0; i < jsonArray.length(); i++) {
                    this.dependsOn.add(normalize(jsonArray.getString(i)));
                }
            }
            
            if(schema.has(KEY_ENTITY_GENERATOR)) {
                JSONObject generator = schema.getJSONObject(KEY_ENTITY_GENERATOR);
                String generatorClassName = generator.getString(KEY_CLASSNAME);
                Object args = buildArguments(generator);
                this.entityGenerator = (GeneratorDriver) createGenerator(generatorClassName, args);
                
                ((EntityType)getType()).addGenerator(entityGenerator);
            }
        }
        
        private void setGenerator(JSONObject generator, Type type, String generatorColumn) {
            String generatorClassName = generator.getString(KEY_CLASSNAME);
            Object args = buildArguments(generator);
            
            Property p = type.getProperty(normalize(generatorColumn));
            if(p != null) {
                logger.info(String.format("Setting generator '%s' for property %s mapped to column '%s' in table %s", generatorClassName, p.getName(), generatorColumn, type.getName()));
                p.setGenerator((Generator) createGenerator(generatorClassName, args));
            } else {
                throw new RuntimeException(String.format("Unable to find property for column %s in table %s", generatorColumn, type.getName()));
            }            
        }
        
        private Shape getShape() {
            return ((EntityType)getType()).getShape();
        }
        
        // If the lookup key has a column generator then extract that here
        private void extractColumnGenerator(JSONObject fkJson) {
            
            // get the type for the foreign key table
            Type type = getShape().getType(normalize(fkJson.getString(KEY_FOREIGN_KEY_TABLE)));
            
            // To be safe, we make a copy so we can manipulate the column generators
            // We need to get the shape of the CSVState's type since that is the correct
            // shape we should make this copy on
            type = ((EntityType)type).copy(getShape());
            
            JSONArray joinArray = fkJson.getJSONArray(KEY_JOIN);
            for (int j = 0; j < joinArray.length(); j++) {
                JSONObject joinJson = joinArray.getJSONObject(j);
                String fkColName = joinJson.names().getString(0);

                if(joinJson.has(KEY_COLUMN_GENERATOR)) {
                    JSONObject generator = joinJson.getJSONObject(KEY_COLUMN_GENERATOR);
                    setGenerator(generator, type, fkColName);
                }
            }            
        }
        
        private Object buildArguments(JSONObject generator) {
            Object args = new String[0];
            if (generator.has(KEY_ARGUMENTS)) {
                JSONArray arguments = generator.getJSONArray(KEY_ARGUMENTS);

                if (arguments.length() == 1 && arguments.get(0) instanceof Number) {
                    args = arguments.getInt(0);
                } else {
                    args = new String[arguments.length()];
                    for (int j = 0; j < arguments.length(); j++) {
                        ((String[]) args)[j] = arguments.getString(j);
                    }
                }
                logger.info("buildArguments: ", args);
            } else {
                logger.info(String.format("Generator for table %s has no arguments", this.getTableName()));
            }

            return args;
        }
        
        private Object createGenerator(String generatorClassName, Object args) {
            // construct the generator
            Class<?> cl;
            try {
                cl = Class.forName(generatorClassName);
                Constructor<?> cons = null;
                
                if (args != null) {
                    if (String[].class.isAssignableFrom(args.getClass())) {
                        cons = cl.getConstructor(String[].class);
                    } else if (Integer.class.isAssignableFrom(args.getClass())) {
                        cons = cl.getConstructor(int.class);
                    } else {
                        throw new RuntimeException(String.format("Unknown argument type for generator %s in table %s",
                                generatorClassName, getTableName()));
                    }
                }
                return cons.newInstance((Object) args);
            } catch (Exception e) {
                throw ClassUtil.wrapRun(e);
            } 
        }
        
        public DateFormat getDateFormatter() {
            return this.dateFormatter;
        }
  
        public GeneratorDriver getEntityGenerator() {
            return entityGenerator;
        }

        public void setEntityGenerator(GeneratorDriver entityGenerator) {
            this.entityGenerator = entityGenerator;
        }        
        
        /*
         * Get properties that either have generators or are NOT NULL
         * The value with will generated for these properties
         */
        public List<Property> getGeneratedProperties() {
            List<Property> result = new ArrayList<>();
            Set<String> propertiesWithGenerator = new HashSet<>();
            for (Property p: getType().getProperties()) {
                if (p == null) {
                    throw new RuntimeException(
                            String.format("Unable to find column %s in table %s", p.getName(), getType().getName()));
                }
                if (p.getGenerator() != null) {
                    result.add(p);
                    propertiesWithGenerator.add(p.getName());
                } 
            }
            
            for(Property p: getMissingRequiredProperties()) {
                if(!propertiesWithGenerator.contains(p.getName())) {
                    result.add(p);
                }
            }

            return result;
        }
        
        public Map<String, Object> loadNotNullFKData(JSONObject entityJSON, JDBCDataStore dataStore) {
            return loadFKData(entityJSON, dataStore, this.notNullForeignKeys, true);
        }
        
        public Map<String, Object> loadNullableFKData(JSONObject entityJSON, JDBCDataStore dataStore) {
            return loadFKData(entityJSON, dataStore, this.nullableForeignKeys, false);
        }        
        
        public Map<String, Object> loadFKData(JSONObject entityJSON, JDBCDataStore dataStore, List<JSONObject> foreignKeysJson, boolean isNotNull) {
            Map<String, Object> result = new HashMap<>();
            
            if(entityJSON == null || dataStore == null) {
                return result;
            }
            
            for(JSONObject fkJson: foreignKeysJson) {
                // get the type for the foreign key table
                Type type = getShape().getType(normalize(fkJson.getString(KEY_FOREIGN_KEY_TABLE)));
                
                BusinessObject bo = new ImmutableBO(type, null, null, null);
                JSONObject fkJsonInstance = new JSONObject();
                bo.setInstance(fkJsonInstance);
                JSONArray joinArray = fkJson.getJSONArray(KEY_JOIN);
                
                Map<String, String> lookupKeys = new HashMap<>(); 
                for(int i = 0; i < joinArray.length(); i++) {
                    JSONObject joinJson = joinArray.getJSONObject(i);
                    String fkColName = joinJson.names().getString(0);
                    String headerName = joinJson.getString(fkColName);
                    
                    // normalize the column names
                    fkColName = normalize(fkColName);
                    headerName = normalize(headerName);
                    String fkValue = entityJSON.getString(headerName);
                    fkJsonInstance.put(fkColName, fkValue);
                    lookupKeys.put(fkColName, fkValue);
                }
                 
                Object value = dataStore.getSessionContext().getSingleResult(bo, fkJson.getString(KEY_SELECT), lookupKeys);
                if (value == null) {
                    if (isNotNull) {
                        StringBuilder keyStr = new StringBuilder();
                        int i = 1;
                        for (Map.Entry<String, String> entry : lookupKeys.entrySet()) {
                            keyStr.append(
                                    String.format("Key %s: %s, Value: %s\n", i++, entry.getKey(), entry.getValue()));
                        }
                        throw new RuntimeException(String.format(
                                "Unable to load required value for column %s in table %s for the following key(s): \n%s",
                                fkJson.getString(KEY_FOREIGN_KEY), getType().getName(), keyStr.toString()));
                    }
                } else {
                    result.put(normalize(fkJson.getString(KEY_FOREIGN_KEY)), value);
                    entityJSON.put(normalize(fkJson.getString(KEY_FOREIGN_KEY)), value);
                }
            }
            
            return result;
        }
        
        public Map<String, Property> getLookupKeyNotNullFKMap() {
            return getHeaderLookupPropertyMap(this.notNullForeignKeys);
        }
        
        public Map<String, Property> getLookupKeyNullableFKMap() {
            return getHeaderLookupPropertyMap(this.nullableForeignKeys);
        }        
        
        private Map<String, Property> getHeaderLookupPropertyMap(List<JSONObject> fkeys) {
            Map<String, Property> result = new HashMap<>();
            for(JSONObject fkJson: fkeys) {
                JSONArray joinArray = fkJson.getJSONArray(KEY_JOIN);
                for (int j = 0; j < joinArray.length(); j++) {
                    JSONObject joinJson = joinArray.getJSONObject(j);
                    String fkColName = joinJson.names().getString(0);
                    
                    String headerName = normalize(joinJson.getString(fkColName));
                    Type type = getShape().getType(normalize(fkJson.getString(KEY_FOREIGN_KEY_TABLE)));
                    Property property = type.getProperty(normalize(fkColName));
                    result.put(headerName, property);
                }
            }
            
            return result;
        }
        
        public static String normalize(String column) {
            return column.trim().toUpperCase();
        }
        
        Map<String, Object> getLookupKeys(JSONObject entityJSON) {
            Map<String, Object> result = new HashMap<>();
            
            JSONArray keys = null;
            try {
                keys = schema.getJSONArray(KEY_KEYS);
            } catch (org.json.JSONException e) {
                throw new RuntimeException(
                        String.format("Unable to find '%s' fragment in the schema for table %s. Message: %s", KEY_KEYS,
                                getTableName(), e.getMessage()));
            }
            for(int i = 0; i < keys.length(); i++) {
                String key = normalize(keys.getString(i));
                if(!this.headerMap.containsKey(key)) {
                    if(this.columnAliases.containsKey(key)) {
                        key = this.columnAliases.get(key);
                    }
                }
                String value = entityJSON.getString(key);
                
                result.put(key, value);
            }
            
            return result; 
        }
        
        public Set<String> getNotNullFKTables() {
            Set<String> notNullFKTables = new HashSet<>();
            
            for(JSONObject json: this.notNullForeignKeys) {
                notNullFKTables.add(normalize(json.getString(KEY_FOREIGN_KEY_TABLE)));
            }
            
            return notNullFKTables;
        }
        
        public Set<String> getNonFKColumns() {
            Set<String> result = new HashSet<>();
            
            // first get all valid columns
            for(String headerColumn: this.headerMap.keySet()) {
                if(getType().getProperty(headerColumn) != null) {
                    result.add(headerColumn);
                }
            }
            
            result.removeAll(getFKColumns());
            
            return result;
        }
        
        public Set<String> getFKColumns() {
            Set<String> result = new HashSet<>();
            
            for(JSONObject json: this.notNullForeignKeys) {
                result.add(normalize(json.getString(KEY_FOREIGN_KEY)));
            }
            for(JSONObject json: this.nullableForeignKeys) {
                result.add(normalize(json.getString(KEY_FOREIGN_KEY)));
            }            
            
            return result;
        }
        
        /**
         * Columns that have data directly specified in the csv file,
         * or columns that can derive the data using the foreign keys
         * @return list of columns with data 
         */
        public Set<String> getColumnsWithData() {
            Set<String> result = getNonFKColumns();
            result.addAll(getFKColumns());
            
            return result;
        }
        
        public String getCSVFile() {
            return this.csvFile;
        }
        
        public String getEntityName() {
            return schema.getString(KEY_ENTITY_NAME);
        }        
        
        public String getTableName() {
            return schema.getString(KEY_TABLE_NAME);
        }
        
        public List<Property> getMissingRequiredProperties() {
            List<Property> result = new ArrayList<>();

            Set<String> columnsWithData = getColumnsWithData();
            for (Property p : getType().getProperties()) {
                if (((ExtendedProperty) p).isDataType() && !p.isNullable()
                        && !columnsWithData.contains(normalize(p.getName()))) {
                    result.add(p);
                }
            }

            return result;
        }

        public void addEdges() {
            // Add edge from the dependedOn table to the dependedBy table
           for(String fkTable: getNotNullFKTables()) {
               CSVState fkState = loader.findState(fkTable);
               if(fkState == null) {
                   logger.warn(String.format("Unable to find csv file for table %s. File not needed if data for this table is already in database.", fkTable));
                   continue;
               }
               if(fkState == this) {
                   logger.info("Avoiding self-loop edge for table %s");
                   continue;
               }
               
               Edge<CSVState> edge = new Edge<>(fkTable, fkState, this);
               addEdge(edge);
           }
           
           for(String dependsOnTable: this.dependsOn) {
               CSVState fkState = loader.findState(dependsOnTable);
               if(fkState == null) {
                   throw new RuntimeException(String.format("Unable to find csv file for table " + dependsOnTable));
               }
               
               Edge<CSVState> edge = new Edge<>(dependsOnTable, fkState, this);
               addEdge(edge);
           }
        }
        
        private void addEdge(Edge<CSVState> edge) {
            loader.orderingGraph.addEdge(edge, edge.getStart(), edge.getEnd());            
        }

        /**
         * Needs to be called everytime when creating records
         * @param filePath of the file that contains the output
         */
        public void createCSVPrinter(String filePath) {
            try {
                // initialize FileWriter object
                BufferedWriter fileWriter = new BufferedWriter(new FileWriter(filePath));

                // initialize CSVPrinter object
                this.csvPrinter = new CSVPrinter(fileWriter, CSVExportImport.csvFileFormat);
            } catch (IOException e) {
                throw ClassUtil.wrapRun(e);
            }
        }

        public void writeToCSV(String filePath, Settings settings, JDBCDataStore dataStore) {
            try {
                createCSVPrinter(filePath);
                loader.createRecords(this, settings, dataStore);
                
                // Don't need to clear the generators since we created a child shape to make generator related changes
            } catch (IOException e) {
                throw ClassUtil.wrapRun(e);
            }
        }
        
        private Set<String> getMissingKeys() {
            Set<String> result = new HashSet<>();

            if(schema.has(KEY_KEYS)) {
                JSONArray keys = schema.getJSONArray(KEY_KEYS);
                for(int i = 0; i < keys.length(); i++) {
                    String key = normalize(keys.getString(i));
                    if(!this.headerMap.containsKey(key) && !this.columnAliases.containsKey(key)) {
                        result.add(key);
                    }
                }
            }
            
            return result;
        }        
    }
    
    public CSVLoader(Shape shape) {
        this.shape = shape;
        this.numThreads = 1;
        this.orderingGraph = new DirectedSparseGraph<>();
        this.tableStateMap = new HashMap<>();
    }
    
    public CSVLoader(Shape shape, String path) {
        this(shape, path, null, null, null, 1);
    }

    public CSVLoader(Shape shape, String path, Integer jobNo, Settings settings, JDBCDataStore dataStore, Integer numThreads) {
        this(shape);
        
        this.folderPath = path;
        this.jobNo = jobNo;
        this.settings = settings;
        this.dataStore = dataStore;
        this.numThreads = numThreads;

        if(path == null || "".equals(path.trim())) {
            throw new RuntimeException("Folder needs to be specified");
        }

        if (!folderPath.endsWith(File.separator)) {
            folderPath += File.separator;
        }

        File folder = new File(path);
        String folderError = String.format("Unable to find the folder '%s' needed to run test1", this.folderPath);
        List<String> files = new ArrayList<>();
        if(folder.isAbsolute()) {
            if (folder.exists()) {
                folderError = null;
                files = Arrays.asList(folder.list());
            }
        } else {
            if (CSVLoader.class.getClassLoader().getResource(this.folderPath) != null) {
                folderError = null;
                try {
                    files = IOUtils.readLines(
                        CSVLoader.class.getClassLoader().getResourceAsStream(this.folderPath),
                        StandardCharsets.UTF_8.name());
                } catch (IOException e) {
                    throw ClassUtil.wrapRun(e);
                }
            }
        }

        if (folderError != null) {
            throw new RuntimeException(folderError);
        }

        List<String> csvFiles = new ArrayList<>();
        for (String file : files) {
            if(file.toUpperCase().endsWith("CSV")) {
                csvFiles.add(folderPath+file);
            }
        }

        try {
            buildStates(csvFiles);
        } catch (IOException e) {
            throw ClassUtil.wrapRun(e);
        }
    }
    
    public void setVisitor(StateGraph.ObjectGenerationVisitor visitor) {
        this.visitor = visitor;
    }
    
    public DirectedSparseGraph<CSVState, Edge<CSVState>> getGraph() {
        return this.orderingGraph;
    }
    
    public void importData(Settings settings, JDBCDataStore dataStore) {
        if(dataStore == null || dataStore.getSessionContext() == null) {
            throw new RuntimeException("Import needs an active transaction");
        }

        boolean orderSQL = dataStore.getSessionContext().isOrderSQL();
        try {
            dataStore.getSessionContext().setOrderSQL(false);
            insertData(settings, dataStore);
            
            updateData(settings, dataStore);
            
            // Don't need to clear the generators since we created a child shape to make generator related changes
            
        } catch (IOException e) {
            throw ClassUtil.wrapRun(e);
        } finally {
            dataStore.getSessionContext().setOrderSQL(orderSQL);
        }
    }
    
    public void importDataParallel(Settings settings, DataModelFactory dataModelFactory, int numThreads) {
        ExecutorService importers = Executors.newFixedThreadPool(numThreads);
        
        List<Future> importJobs = new ArrayList<Future>();
        for (int i = 0; i < numThreads; i++) {
            JDBCDataStore dataStore = (JDBCDataStore)dataModelFactory.createDataStore(settings.getSessionContext());
            importJobs.add(importers.submit(new CSVLoader(this.shape, this.folderPath, i, settings, dataStore, numThreads)));
        }

        // wait for the jobs to complete
        for (Future importJob : importJobs) {
            try {
                importJob.get();
            }
            catch (Exception e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }    
    
    private void buildStates(List<String> csvFiles) throws IOException {
        for(String csvFile: csvFiles) {
            logger.info("Building state for: " + csvFile);
            CSVState csvState = getCSVState(csvFile);
            orderingGraph.addVertex(csvState);
        }
        
        for(CSVState state: orderingGraph.getVertices()) {
            logger.info(String.format("Before topo sort, table %s has id %s", state.getTableName(), orderingGraph.getId(state)));
            state.addEdges();
        }
        
        // Do topological sorting
        this.orderingGraph.toposort(shape);
        // renumber the vertices
        this.orderingGraph.renumber(this.orderingGraph.toposort(shape));
        
        if (logger.isInfoEnabled()) {
            for(Edge<CSVState> edge: orderingGraph.getEdges() ) {
                logger.info(edge.toString());
            }
            
            for (CSVState state : orderingGraph.getVertices()) {
                logger.info(String.format("After topo sort, table %s has id %s", state.getTableName(),
                        orderingGraph.getId(state)));
                state.addEdges();
            }
        }
    }
    
    private void insertData(Settings settings, JDBCDataStore dataStore) throws FileNotFoundException, IOException {
        // iterate on topo sorted order of csv files
        for(int i = orderingGraph.START; i < orderingGraph.START+orderingGraph.getVertices().size(); i++ ) {
            CSVState csvState = orderingGraph.getVertex(i);
            createRecords(csvState, settings, dataStore);
        }
    }
    
    private void updateData(Settings settings, JDBCDataStore dataStore) throws FileNotFoundException, IOException {
        
        for(int i = orderingGraph.START; i < orderingGraph.START+orderingGraph.getVertices().size(); i++ ) {
            CSVState csvState = orderingGraph.getVertex(i);
            
            if(csvState.nullableForeignKeys.size() > 0) {
                updateRecords(csvState, settings, dataStore);
            }
        }
    }  
    
    private Set<String> getColumnsToPopulate(CSVState csvState, List<Property> generatedProperties) {
        // The columns list direct which table columns need to be populated
        Set<String> columns = csvState.getNonFKColumns();
        for(Map.Entry<String, Integer> entry: csvState.headerMap.entrySet()) {
            if(columns.contains(entry.getKey())) {
            }
        }
        
        // Add column aliases
        for(Map.Entry<String, String> entry: csvState.columnAliases.entrySet()) {
            if(csvState.headerMap.containsKey(entry.getValue())) {
                columns.add(entry.getKey());
            } else {
                logger.warn(String.format("Column alias %s in table %s missing in header", entry.getValue(), csvState.getTableName()));
            }
        }
        
        // Add the not-null FK to the column list
        for(JSONObject json: csvState.notNullForeignKeys) {
            columns.add(CSVState.normalize(json.getString(KEY_FOREIGN_KEY)));
        }
        //Add the generated properties to the column list
        for(Property p: generatedProperties) {
            logger.info(String.format("[%s] Genererated property: %s", csvState.getName(), p.getName()));
            columns.add(p.getName());
        }
        
        return columns;
    }
    
    private StateGraph.ObjectGenerationVisitor getOrCreateVisitor(Settings settings) {
        StateGraph.ObjectGenerationVisitor currentVisitor = this.visitor;
        if (currentVisitor == null) {
            currentVisitor = new StateGraph.ObjectGenerationVisitor(null, settings, null);
        }
        
        return currentVisitor;
    }

    private JSONObject getDirectJSON (Object json, GeneratorDriver entityGenerator)
    {
        if (json != null && entityGenerator.isDirect()) {
            if (json instanceof JSONObject) {
                return (JSONObject)json;
            }
            else {
                throw new RuntimeException(String.format(
                    "Entity generator class %s is defined as direct but is not returning a JSON object",
                    entityGenerator.getClass().getName()));
            }
        }

        return null;
    }
    
    private void createRecords(CSVState csvState, Settings settings, JDBCDataStore dataStore) throws IOException {
        /* Get the columns we need to populate
         * 1. All non foreign key columns
         * 2. All not-null foreign key columns
         * 3. All null foreign key columns
         * 
         * Columns in items 1 & 2 is populated as part of INSERT step
         * Columns in item 3 is populated as port of UPDATE step 
         *
         */
        
        // We create a CounterGenerator to drive this process
        // ensure that the entityType does not have any generators
        EntityType entityType = (EntityType) csvState.getType();
        CSVPrinter csvPrinter = csvState.csvPrinter;
        
        logger.info("Creating date for table " + csvState.getTableName());
        StateGraph.ObjectGenerationVisitor currentVisitor = getOrCreateVisitor(settings);
        
        JDBCSessionContext sc = dataStore == null ? null : dataStore.getSessionContext();
        if(isWriteToDB(csvPrinter)) {
            assert sc.getConnection() != null : "Can import only in the context of an existing JDBC connection";
        }
        
        GeneratorDriver entityGenerator = csvState.getEntityGenerator();
        entityGenerator.init(sc == null ? null : sc.getConnection(), currentVisitor);
        entityGenerator.processVisitors();
        Iterator entityIterator = (Iterator) entityGenerator;        

        List<Property> generatedProperties = csvState.getGeneratedProperties();
        Set<String> columns = getColumnsToPopulate(csvState, generatedProperties);
                
        List<Property> dateProperties = new ArrayList<>();
        for(String column: columns) {
            logger.info(String.format("[%s] Column being populated: %s", csvState.getName(), column));
            Property p = csvState.getType().getProperty(column);
            if(Date.class.isAssignableFrom(p.getType().getInstanceClass())) {
                dateProperties.add(p);
            }
        }
        if(dateProperties.size() > 0 && csvState.getDateFormatter() == null) {
            logger.warn("Some of the columns are of type Date, and a 'dateFormat' expression is not set on the schema");
        }
        
        List<String> columnList = new ArrayList<>(columns);
        Set<String> columnsToInsert = new HashSet<>(columnList);
        
        // Read the data for non FK columns
        try(BufferedReader reader = getReader(csvState.getCSVFile())) {
            // skip first 2 lines
            if(reader.ready()) { reader.readLine();}
            if(reader.ready()) { reader.readLine();}
            
            try(CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT)) {
                Iterator<CSVRecord> csvIterator = parser.iterator();
                int i = COUNTER_START, recordNo = i;
                boolean csvPowered = false;
                CSVRecord csvRecord = null;
                while(csvIterator.hasNext() || entityIterator.hasNext()) {
                    csvRecord = csvIterator.hasNext() ? csvIterator.next() : null;

                    // The default entity generator is counter generator
                    if(!entityIterator.hasNext()) {
                        break;
                    }                  
                    
                    // Only if we don't have csv data do we solely rely on entity driver generator powered
                    if(!csvPowered) {
                        if(csvRecord != null) {
                            logger.info(String.format("Data generation for table %s is CSV powered", csvState.getTableName()));
                            csvPowered = true;
                        }
                    } else if(csvRecord == null) {
                        if (csvPowered) {
                            // end of csv powered processing
                            break;
                        }
                    } else if(i == COUNTER_START) {
                        logger.info(String.format("Data generation for table %s is entityGenerator powered", csvState.getTableName()));
                    }
                    
                    // Should support more records as it is a generator
                    // If not then we break
                    Object result = entityIterator.next();  
                    if(!csvPowered && result == null) {
                        break;
                    }
                    
                    // This is a parallel execution
                    // We process only its corresponding slot
                    if(!isMyJob(i++)) {
                        continue;
                    }                    

                    JSONObject entityJSON = getDirectJSON(result, entityGenerator);
                    try {
                        // It is fine to copy everything to the JSON object since we say exactly which columns to insert
                        if(entityJSON == null) {
                            entityJSON = CSVExportImport.getJSON(csvState.headerMap, csvRecord,
                                false);
                        }
                        populateLookupKeyValues(entityJSON, settings, currentVisitor, csvState.getLookupKeyNotNullFKMap());                        
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new RuntimeException(String.format(
                                "Unable to find data for some column(s) in row %s while processing table %s. ArrayIndexOutOfBoundsException on index: %s",
                                i, csvState.getTableName(), e.getMessage()));
                    }
                    
                    csvState.loadNotNullFKData(entityJSON, dataStore); 
                    if(!isWriteToDB(csvPrinter)) {
                        // Since we cannot update a CSV file, we also try and get the values 
                        // for the nullable Foreign keys.
                        // The assumption is that the data is already persisted, if not, it will be null.
                        Set<String> lookupColumns = updateNullableFKFields(entityJSON, csvState, settings, dataStore, currentVisitor).keySet();
                        for(String lookupColumn: lookupColumns) {
                            if(!columnsToInsert.contains(lookupColumn)) {
                                columnsToInsert.add(lookupColumn);
                                columnList.add(lookupColumn);
                            }
                        }
                    }
                    
                    for(Property p: dateProperties) {
                        // This could be a generated property, so it might
                        // not yet be present in the JSON object
                        if(entityJSON.has(p.getName())) {
                            String dateStr = entityJSON.getString(p.getName());
                            if(StringUtils.isNotEmpty(dateStr.trim())) {
                                Date value = null;
                                try {
                                    value = csvState.getDateFormatter().parse(dateStr);
                                } catch (ParseException e) {
                                    throw new RuntimeException(String.format(
                                            "The date value '%s' for property %s in table %s cannot be parsed", dateStr,
                                            p.getName(), csvState.getTableName()));
                                } catch (NullPointerException npe) {
                                    throw new RuntimeException("Date format needs to be specified in the schema for table " + csvState.getTableName());
                                }
                                entityJSON.put(p.getName(), value);
                            }
                        }
                    }
                    
                    // Finally populate the values for those properties whose values are 
                    // to be generated
                    for(Property p: generatedProperties) {
                        entityJSON.put(
                                p.getName(), ((BasicType)p.getType()).generate(
                                    settings,
                                    p,
                                    null,
                                    null,
                                    currentVisitor));
                    }
                    
                    if(!csvPowered) {
                        // If not csv powered we have to add the header columns that are properties
                        // and that are not present in the JSON object
                        for(String columnName: csvState.headerMap.keySet()) {
                            if(entityJSON.has(columnName) || csvState.getType().getProperty(columnName) == null) {
                                continue;
                            }
                            
                            Property p = csvState.getType().getProperty(columnName);
                            entityJSON.put(
                                    p.getName(), ((BasicType)p.getType()).generate(
                                        settings,
                                        p,
                                        null,
                                        null,
                                        currentVisitor));                            
                        }
                    }
                    
                    if(logger.isDebugEnabled()) {
                        logger.debug("entityJSON: " + entityJSON.toString());
                    }

                    if (isWriteToDB(csvPrinter)) {
                        BusinessObject bo = new ImmutableBO(csvState.getType(), null, null, null);
                        bo.setInstance(entityJSON);
                        dataStore.getSessionContext().create(bo, columnList);

                        performFlush(sc, recordNo, false);
                    } else {
                        if(i == COUNTER_START) {
                            csvPrinter.printRecord(columnList);
                        }
                        csvPrinter.printRecord(extractValues(entityJSON, columnList));
                    }
                    
                    // increment counter
                    recordNo++;
                }
                
                if (isWriteToDB(csvPrinter)) {
                    performFlush(sc, recordNo, true);
                }
            }
        } finally {
            if(csvPrinter != null) {
                csvPrinter.close();
                csvPrinter = null;
            }
        }
    }
    
    private List<Object> extractValues(JSONObject json, List<String> columnList) {
        List<Object> result = new ArrayList<>();
        
        for(String column: columnList) {
            result.add(json.get(column));
        }
        
        return result;
    }
    
    private boolean isWriteToDB(CSVPrinter csvPrinter) {
        return csvPrinter == null;
    }
    
    private void updateRecords(CSVState csvState, Settings settings, JDBCDataStore dataStore) throws IOException {
                
        // Read the data for nullable FK columns
        try(BufferedReader reader = getReader(csvState.getCSVFile())) {
            // skip first 2 lines
            if(reader.ready()) { reader.readLine();}
            if(reader.ready()) { reader.readLine();}
            
            StateGraph.ObjectGenerationVisitor currentVisitor = getOrCreateVisitor(settings);  
            
            JDBCSessionContext sc = dataStore == null ? null : dataStore.getSessionContext();
            GeneratorDriver entityGenerator = csvState.getEntityGenerator();
            entityGenerator.init(sc == null ? null : sc.getConnection(), currentVisitor);
            entityGenerator.processVisitors();
            Iterator entityIterator = (Iterator) entityGenerator;            
            
            try(CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT)) {
                Iterator<CSVRecord> csvIterator = parser.iterator();
                int i = COUNTER_START, recordNo = i;
                boolean csvPowered = false;
                CSVRecord csvRecord = null;
                while(csvIterator.hasNext() || entityIterator.hasNext()) {
                    csvRecord = csvIterator.hasNext() ? csvIterator.next() : null;
                    
                    if(!entityIterator.hasNext()) {
                        break;
                    }                  
                    
                    // Only if we don't have csv data do we solely rely on entity driver generator powered
                    if(!csvPowered) {
                        if(csvRecord != null) {
                            logger.info(String.format("Data update for table %s is CSV powered", csvState.getTableName()));
                            csvPowered = true;
                        }
                    } else if(csvRecord == null) {
                        if (csvPowered) {
                            // end of csv powered processing
                            break;
                        }
                    } else if(i == COUNTER_START) {
                        logger.info(String.format("Data update for table %s is entityGenerator powered", csvState.getTableName()));
                    }
                    
                    // Should support more records as it is a generator
                    // If not then we break
                    Object result = entityIterator.next();  
                    if(!csvPowered && result == null) {
                        break;
                    }      
                    
                    // This is a parallel execution
                    // We process only its corresponding slot
                    if(!isMyJob(i++)) {
                        continue;
                    }

                    JSONObject entityJSON = getDirectJSON(result, entityGenerator);
                    if(entityJSON == null) {
                        entityJSON = CSVExportImport.getJSON(csvState.headerMap,
                            csvRecord, false);
                    }
                    Set<String> missingKeys = csvState.getMissingKeys();
                    if(!csvPowered || missingKeys.size() > 0) {
                        // generate the lookup key(s) for the current type
                        // NOTE: The generation of the lookup key values should be idempotent, i.e., they should not change with different invocations
                        if(csvState.schema.has(KEY_KEYS)) {
                            
                            JSONArray keys = csvState.schema.getJSONArray(KEY_KEYS);
                            for(int j = 0; j < keys.length(); j++) {
                                String key = CSVState.normalize(keys.getString(j));
                                missingKeys.add(key);
                            }
                             
                            for(String key: missingKeys) {
                                Property p = csvState.getType().getProperty(key);
                                if(p != null && p.getGenerator() != null) {
                                    logger.info(String.format("Lookup keys: Property %s in table %s is initialized with a generator", p.getName(), csvState.getTableName()));
                                }
                                
                                if(p != null) {
                                    entityJSON.put(
                                            p.getName(), ((BasicType)p.getType()).generate(
                                                settings,
                                                p,
                                                null,
                                                null,
                                                currentVisitor));
                                }
                            }
                        }
                    }

                    Map<String, Object> nullableFKData = updateNullableFKFields(entityJSON, csvState, settings, dataStore, currentVisitor);                       
                    List<String> columnsToUpdate = new ArrayList<>(nullableFKData.keySet());                     
                    
                    if (columnsToUpdate.size() > 0) {
                        // Get the lookup keys for that particular record
                        Map<String, Object> lookupKeys = csvState.getLookupKeys(entityJSON);

                        BusinessObject bo = new ImmutableBO(csvState.getType(), null, null, null);
                        bo.setInstance(entityJSON);
                        dataStore.getSessionContext().update(bo, columnsToUpdate, lookupKeys);

                        performFlush(sc, recordNo++, false);
                    }
                }
                if(i > COUNTER_START) {
                    performFlush(sc, recordNo, true);
                }
            }
        }
    }
    
    public void performFlush(JDBCSessionContext sc, int i, boolean isEnd) {
        if(!isEnd) {
            if (i % DataImporter.COMMIT_SIZE == 0) {
                // flush in batches
                sc.flush();
            }          
        } else {
            sc.flush();
        }
    }     
    
    private boolean isMyJob(int recordNo) {
        if(jobNo == null || recordNo%this.numThreads == jobNo) {
            return true;
        }        
        
        return false;
    }
    
    private Map<String, Object> updateNullableFKFields(JSONObject entityJSON, CSVState csvState, Settings settings, JDBCDataStore dataStore, StateGraph.ObjectGenerationVisitor currentVisitor) {
        populateLookupKeyValues(entityJSON, settings, currentVisitor, csvState.getLookupKeyNullableFKMap());
        
        // Fetch the foreign key value from the DB
        return  csvState.loadNullableFKData(entityJSON, dataStore);
    }
    
    private void populateLookupKeyValues(JSONObject entityJSON, Settings settings, StateGraph.ObjectGenerationVisitor currentVisitor, Map<String, Property> loopKeyPropertyMap) {
        for(Map.Entry<String, Property> entry: loopKeyPropertyMap.entrySet()) {
            logger.info(String.format("populateLookupKeyValues: key: %s, property name: %s", entry.getKey(), (entry.getValue() == null ? "null" : entry.getValue().getName())));

            if(!entityJSON.has(entry.getKey())) {
                entityJSON.put(entry.getKey(), ((BasicType)entry.getValue().getType()).generate(
                        settings,
                        entry.getValue(),
                        null,
                        null,
                        currentVisitor));
            }
        }   
    }
    
    public CSVState findState(String tableName) {
        return tableStateMap.get(tableName);
    }

    /**
     * Creates a CSVState object that contains all the information to populate a table
     * 
     * @param csvFile path relative to classpath
     * @return new CSVState object
     * @throws IOException if there is a problem with reading the CSV file
     */
    public CSVState getCSVState(String csvFile) throws IOException {
        // A child shape is created so that we can manipulate the entity generators for the types
        // Also the child shape for each Type ensures that a copy of the relationship types are also made
        Shape childShape = new DomainShape("test", shape, shape.getDataModel());
        
        try(BufferedReader reader = getReader(csvFile)) {
            // Skip first line as schema is in second line
            if(reader.ready()) {
                reader.readLine();
            }
            
            JSONObject schema = null;
            if (reader.ready()) {
                String jsonSchema = reader.readLine();
                schema = new JSONObject(jsonSchema);
            }
            
            // Create a state object using the json schema and check if there are any not-null
            // foreign keys in the columns. This creates a dependency with the state object of the foreignKeyTable
            String tableName = CSVState.normalize(schema.getString(KEY_TABLE_NAME));
            Type type = childShape.getType(tableName);

            if (type == null) {
                throw new RuntimeException(String.format("Type is missing in shape for table %s", tableName));
            }
            
            CSVState result = new CSVState(type, schema, csvFile, this, childShape);
            this.tableStateMap.put(tableName, result);
            
            return result;
        }         
    }

    private static BufferedReader getReader(String filePath) throws FileNotFoundException
    {
        File file = new File(filePath);
        if(file.isAbsolute()) {
            return new BufferedReader(new FileReader(filePath));
        } else {
            InputStream is = CSVLoader.class.getClassLoader().getResourceAsStream(filePath);
            return new BufferedReader(new InputStreamReader(is));
        }
    }

    @Override
    public Object call() throws Exception {
        JDBCSessionContext sc = dataStore.getSessionContext();
        sc.beginTransaction();
        try {
            this.importData(this.settings, this.dataStore);
        } finally {
            // When running in parallel, since it runs in a separate thread,
            // we need to commit
            sc.commit();
        }
        
        return null;
    }
}
