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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.BasicType;
import tools.xor.BusinessObject;
import tools.xor.CounterGenerator;
import tools.xor.DataImporter;
import tools.xor.EntityType;
import tools.xor.ImmutableBO;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
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
 *    entityName - Java entity name, typically the fully qualified class name of the entity
 *    tableName  - The name of the table
 *    columns    - The columns that need to be populated in the table
 *    
 *    Optional fields
 *    ---------------
 *    keys        - Represents the fields that unique identify the row in the table
 *                  The columns in the keys cannot comprise foreign key columns
 *                  This field is needed when updating NULL foreign key columns
 *    foreignKeys - Columns that need to be populated based on foreign key relationships
 *                  These columns are not part of header columns in Section 1
 *                  So this section provides info on how they can be populated using
 *                  the "foreignKeyTable", "select" and "join" information.
 *                  
 *                  In the example below, the foreign key column "col2" is populated
 *                  by selecting the id column from the FKTABLE1 table under the
 *                  join condition(s) restriction.
 *                  
 *                  SELECT f.id FROM FKTABLE1 f WHERE FKTcol3 = :Icol2              
 *    
 * 
 *    Example:
 *    =======
 *    {
 *      "entityName" : "tools.xor.db.pm.Task",
 *      "tableName"  : "TASK",
 *      "columns" : ["col1", "col2", "col3", "col4", "col5", "col6", "col7" ....],
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
public class CSVLoader {
    
    private Shape shape;
    private DirectedSparseGraph<CSVState, Edge<CSVState>> orderingGraph;
    private Map<String, CSVState> tableStateMap;
    private String folderPath;
    
    private static final String KEY_ENTITY_NAME = "entityName";
    private static final String KEY_TABLE_NAME = "tableName";
    private static final String KEY_COLUMNS = "columns";
    private static final String KEY_KEYS = "keys";
    private static final String KEY_FOREIGN_KEYS = "foreignKeys";
    private static final String KEY_FOREIGN_KEY_TABLE = "foreignKeyTable";
    private static final String KEY_FOREIGN_KEY = "foreignKey";
    private static final String KEY_JOIN = "join";
    private static final String KEY_SELECT = "select";
    
    public static class CSVState extends State {
        
        private final JSONObject schema; 
        private final CSVLoader loader;
        private final String csvFile;
        private Map<String, Integer> headerMap;
        private List<JSONObject> notNullForeignKeys;
        private List<JSONObject> nullableForeignKeys;

        public CSVState(Type type, JSONObject schema, String csvFile, CSVLoader loader) {
            super(type, false);
            
            this.schema = schema;
            this.loader = loader;
            this.csvFile = csvFile;
            this.headerMap = new HashMap<>();
            this.notNullForeignKeys = new ArrayList<>();
            this.nullableForeignKeys = new ArrayList<>();
            
            try(InputStream is = CSVLoader.class.getClassLoader().getResourceAsStream(this.csvFile);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {                    
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
                }
            }
        }
        
        public List<Property> getPropertiesWithGenerator() {
            List<Property> result = new ArrayList<>();
            JSONArray columns = this.schema.getJSONArray(KEY_COLUMNS);
            for (int i = 0; i < columns.length(); i++) {
                String columnName = normalize(columns.getString(i));
                Property p = getType().getProperty(columnName);
                if (p == null) {
                    throw new RuntimeException(
                            String.format("Unable to find column %s in table %s", columnName, getType().getName()));
                }
                if (p.getGenerator() != null) {
                    result.add(p);
                } else {
                    // Check that a NOT NULL column either has data in the csv file or a generator
                    if (!this.headerMap.containsKey(columnName) && !p.isNullable()) {
                        throw new RuntimeException(String.format(
                                "NOT NULL column %s in table %s needs to either have data in the csv file or be configured with a generator",
                                columnName, this.getTableName()));
                    }
                }
            }

            return result;
        }
        
        public Map<String, Object> loadNotNullFKData(CSVRecord csvRecord, JDBCDataStore po) {
            return loadFKData(csvRecord, po, this.notNullForeignKeys, true);
        }
        
        public Map<String, Object> loadNullableFKData(CSVRecord csvRecord, JDBCDataStore po) {
            return loadFKData(csvRecord, po, this.nullableForeignKeys, false);
        }        
        
        public Map<String, Object> loadFKData(CSVRecord csvRecord, JDBCDataStore po, List<JSONObject> foreignKeysJson, boolean isNotNull) {
            Map<String, Object> result = new HashMap<>();
            
            for(JSONObject fkJson: foreignKeysJson) {
                // get the type for the foreign key table
                Type type = loader.shape.getType(fkJson.getString(KEY_FOREIGN_KEY_TABLE));
                
                BusinessObject bo = new ImmutableBO(type, null, null, null);
                JSONObject entityJSON = new JSONObject();
                bo.setInstance(entityJSON);
                JSONArray joinArray = fkJson.getJSONArray(KEY_JOIN);
                
                Map<String, String> lookupKeys = new HashMap<>(); 
                for(int i = 0; i < joinArray.length(); i++) {
                    JSONObject joinJson = joinArray.getJSONObject(i);
                    String fkColName = joinJson.names().getString(0);
                    String headerName = joinJson.getString(fkColName);
                    
                    // normalize the column names
                    fkColName = normalize(fkColName);
                    headerName = normalize(headerName);
                    String fkValue = csvRecord.get(this.headerMap.get(headerName));
                    entityJSON.put(fkColName, fkValue);
                    lookupKeys.put(fkColName, fkValue);
                }
                 
                Object value = po.getSessionContext().getSingleResult(bo, fkJson.getString(KEY_SELECT), lookupKeys);
                if (value == null && isNotNull) {
                    StringBuilder keyStr = new StringBuilder();
                    int i = 1;
                    for (Map.Entry<String, String> entry : lookupKeys.entrySet()) {
                        keyStr.append(String.format("Key %s: %s, Value: %s\n", i++, entry.getKey(), entry.getValue()));
                    }
                    throw new RuntimeException(String.format(
                            "Unable to load required value for column %s in table %s for the following key(s): \n%s",
                            fkJson.getString(KEY_FOREIGN_KEY), getType().getName(), keyStr.toString()));
                }
                result.put(normalize(fkJson.getString(KEY_FOREIGN_KEY)), value);
            }
            
            return result;
        }
        
        private static String normalize(String column) {
            return column.trim().toUpperCase();
        }
        
        Map<String, Object> getLookupKeys(CSVRecord csvRecord) {
            Map<String, Object> result = new HashMap<>();
            
            JSONArray keys = schema.getJSONArray(KEY_KEYS);
            for(int i = 0; i < keys.length(); i++) {
                String key = normalize(keys.getString(i));
                String value = csvRecord.get(this.headerMap.get(key));
                
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
        
        public Set<String> gatherNonFKColumns() {
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
        
        public String getCSVFile() {
            return this.csvFile;
        }
        
        public String getTableName() {
            return schema.getString(KEY_TABLE_NAME);
        }
        
        public void addEdges() {
            // Add edge from the dependedOn table to the dependedBy table
           for(String fkTable: getNotNullFKTables()) {
               CSVState fkState = loader.findState(fkTable);
               if(fkState == null) {
                   throw new RuntimeException(String.format("Unable to find csv file for table " + fkTable));
               }
               
               Edge<CSVState> edge = new Edge<>("fkTable", fkState, this);
               addEdge(edge);
           }
        }
        
        private void addEdge(Edge<CSVState> edge) {
            loader.orderingGraph.addEdge(edge, edge.getStart(), edge.getEnd());            
        }
    }

    public CSVLoader(Shape shape, String path) {
        // A child shape is created so that we can manipulate the entity generators for the types
        this.shape = new DomainShape("test", shape, shape.getDataModel());
        this.orderingGraph = new DirectedSparseGraph<>();
        this.tableStateMap = new HashMap<>();
        this.folderPath = path;

        if (CSVLoader.class.getClassLoader().getResource(this.folderPath) == null) {
            throw new RuntimeException(
                    String.format("Unable to find the folder '%s' needed to run test1", this.folderPath));
        }

        if (!folderPath.endsWith(File.separator)) {
            folderPath += File.separator;
        }

        try {
            List<String> files = IOUtils.readLines(
                    CSVLoader.class.getClassLoader().getResourceAsStream(this.folderPath),
                    StandardCharsets.UTF_8.name());
            
            List<String> csvFiles = new ArrayList<>();
            for (String file : files) {
                if(file.toUpperCase().endsWith("CSV")) {
                    csvFiles.add(folderPath+file);
                }
            }

            buildStates(csvFiles);
        } catch (IOException e) {
            throw ClassUtil.wrapRun(e);
        }
    }
    
    public DirectedSparseGraph<CSVState, Edge<CSVState>> getGraph() {
        return this.orderingGraph;
    }
    
    public void importData(Settings settings, JDBCDataStore po) {
        boolean orderSQL = po.getSessionContext().isOrderSQL();
        try {
            po.getSessionContext().setOrderSQL(false);
            insertData(settings, po);
            
            updateData(settings, po);
        } catch (IOException e) {
            throw ClassUtil.wrapRun(e);
        } finally {
            po.getSessionContext().setOrderSQL(orderSQL);
        }
    }
    
    private void buildStates(List<String> csvFiles) throws FileNotFoundException, IOException {
        for(String csvFile: csvFiles) {
            CSVState csvState = getCSVState(csvFile);
            orderingGraph.addVertex(csvState);
        }
        
        for(CSVState state: orderingGraph.getVertices()) {
            state.addEdges();
        }
        
        // Do topological sorting
        this.orderingGraph.toposort(shape);
    }
    
    private void insertData(Settings settings, JDBCDataStore po) throws FileNotFoundException, IOException {
        // iterate on topo sorted order of csv files
        for(int i = orderingGraph.START; i < orderingGraph.START+orderingGraph.getVertices().size(); i++ ) {
            CSVState csvState = orderingGraph.getVertex(i);
            createRecords(csvState, settings, po);
        }
    }
    
    private void updateData(Settings settings, JDBCDataStore po) throws FileNotFoundException, IOException {
        
        for(int i = orderingGraph.START; i < orderingGraph.START+orderingGraph.getVertices().size(); i++ ) {
            CSVState csvState = orderingGraph.getVertex(i);
            
            if(csvState.nullableForeignKeys.size() > 0) {
                updateRecords(csvState, settings, po);
            }
        }
    }    
    
    private void createRecords(CSVState csvState, Settings settings, JDBCDataStore po) throws IOException {
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
        assert entityType.getGenerators().size() == 0 : "Please clear existing generators on entity: " + entityType.getName();
        
        CounterGenerator counterGen = new CounterGenerator(-1, 1);
        entityType.addGenerator(counterGen);
        
        StateGraph.ObjectGenerationVisitor visitor = new StateGraph.ObjectGenerationVisitor(
                null,
                settings,
                null);
        
        JDBCSessionContext sc = po.getSessionContext();
        assert sc.getConnection() != null : "Can import only in the context of an existing JDBC connection";
        
        counterGen.init(sc.getConnection(), visitor);
        counterGen.processVisitors();
        Iterator iter = counterGen;        
        
        Set<String> columns = csvState.gatherNonFKColumns();
        Map<String, Integer> colPosition = new HashMap<>();
        for(Map.Entry<String, Integer> entry: csvState.headerMap.entrySet()) {
            if(columns.contains(entry.getKey())) {
                colPosition.put(entry.getKey(), entry.getValue());
            }
        }
        
        // All the not-null FK to the column list
        for(JSONObject json: csvState.notNullForeignKeys) {
            columns.add(CSVState.normalize(json.getString(KEY_FOREIGN_KEY)));
        }
        
        List<Property> propWithGenerator = csvState.getPropertiesWithGenerator();
        for(Property p: propWithGenerator) {
            columns.add(p.getName());
        }
        
        
        // Read the data for non FK columns
        try(InputStream is = CSVLoader.class.getClassLoader().getResourceAsStream(csvState.getCSVFile());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {        
            // skip first 2 lines
            if(reader.ready()) { reader.readLine();}
            if(reader.ready()) { reader.readLine();}
            
            try(CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT)) {
                int i = 1;
                for (CSVRecord csvRecord : parser) {     
                    iter.next(); // increment the generator to the next count
                    JSONObject entityJSON = CSVExportImport.getJSON(colPosition, csvRecord);
                    
                    Map<String, Object> notNullFKData = csvState.loadNotNullFKData(csvRecord, po);
                    for(Map.Entry<String, Object> entry: notNullFKData.entrySet()) {                    
                        entityJSON.put(entry.getKey(), entry.getValue());
                    }   
                    
                    // check if any properties have generators and if so execute them
                    for(Property p: propWithGenerator) {
                        entityJSON.put(
                                p.getName(), ((BasicType)p.getType()).generate(
                                    settings,
                                    p,
                                    null,
                                    null,
                                    visitor));
                    }
                    
                    BusinessObject bo = new ImmutableBO(csvState.getType(), null, null, null);
                    bo.setInstance(entityJSON);
                    po.getSessionContext().create(bo, new ArrayList<>(columns));                    
                    
                    DataImporter.performFlush(sc, i++, false);
                }
                
                DataImporter.performFlush(sc, i, true);
            }
        }

        entityType.clearGenerators();
    }
    
    private void updateRecords(CSVState csvState, Settings settings, JDBCDataStore po) throws IOException {
                
        // Read the data for nullable FK columns
        try(InputStream is = CSVLoader.class.getClassLoader().getResourceAsStream(csvState.getCSVFile());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {                
            // skip first 2 lines
            if(reader.ready()) { reader.readLine();}
            if(reader.ready()) { reader.readLine();}
            
            try(CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT)) {
                int i = 1;
                JDBCSessionContext sc = po.getSessionContext();
                for (CSVRecord csvRecord : parser) {                
                    JSONObject entityJSON = new JSONObject();                    
                    
                    // Fetch the foreign key value from the DB
                    Map<String, Object> nullableFKData = csvState.loadNullableFKData(csvRecord, po);    
                    List<String> columnsToUpdate = new ArrayList<>(nullableFKData.keySet());                    
                    for(Map.Entry<String, Object> entry: nullableFKData.entrySet()) {                    
                        entityJSON.put(entry.getKey(), entry.getValue());
                    }
                    
                    // Get the lookup keys for that particular record
                    Map<String, Object> lookupKeys = csvState.getLookupKeys(csvRecord);
                    for(Map.Entry<String, Object> entry: lookupKeys.entrySet()) {                    
                        entityJSON.put(entry.getKey(), entry.getValue());
                    }                    
                        
                    BusinessObject bo = new ImmutableBO(csvState.getType(), null, null, null);
                    bo.setInstance(entityJSON);
                    po.getSessionContext().update(bo, columnsToUpdate, lookupKeys);
                    
                    DataImporter.performFlush(sc, i++, false);
                }
                DataImporter.performFlush(sc, i, true);                
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
     * @throws IOException
     */
    public CSVState getCSVState(String csvFile) throws IOException {
        try(InputStream is = CSVLoader.class.getClassLoader().getResourceAsStream(csvFile);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
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
            Type type = this.shape.getType(tableName);
            
            // We make a copy so we can manipulate the entity generators
            type = ((EntityType)type).copy(this.shape);
            this.shape.addType(type);
            
            CSVState result = new CSVState(type, schema, csvFile, this);
            this.tableStateMap.put(tableName, result);
            
            return result;
        }         
    }
}
