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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
 *    dateFormat - If there is at least a single column of type Date
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
 *      "dateFormat" : "yyyy-MM-dd HH:mm:ss", 
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
 *       ],
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
public class CSVLoader {
    
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());    
    
    private Shape shape;
    private DirectedSparseGraph<CSVState, Edge<CSVState>> orderingGraph;
    private Map<String, CSVState> tableStateMap;
    private String folderPath;
    
    private static final String KEY_ENTITY_NAME = "entityName";
    private static final String KEY_TABLE_NAME = "tableName";
    private static final String KEY_DATE_FORMAT = "dateFormat";
    private static final String KEY_COLUMNS = "columns";
    private static final String KEY_KEYS = "keys";
    private static final String KEY_FOREIGN_KEYS = "foreignKeys";
    private static final String KEY_FOREIGN_KEY_TABLE = "foreignKeyTable";
    private static final String KEY_FOREIGN_KEY = "foreignKey";
    private static final String KEY_JOIN = "join";
    private static final String KEY_SELECT = "select";
    private static final String KEY_COLUMN_GENERATORS = "columnGenerators";
    private static final String KEY_COLUMN = "column";
    private static final String KEY_CLASSNAME = "className";
    private static final String KEY_ARGUMENTS = "arguments";
    
    public static class CSVState extends State {
        
        private final JSONObject schema; 
        private final CSVLoader loader;
        private final String csvFile;
        private Map<String, Integer> headerMap;
        private List<JSONObject> notNullForeignKeys;
        private List<JSONObject> nullableForeignKeys;
        private SimpleDateFormat dateFormatter;
        private Object context;
        private GeneratorDriver entityGenerator;

        public CSVState(Type type, JSONObject schema, String csvFile, CSVLoader loader) {
            super(type, false);
            
            this.schema = schema;
            this.loader = loader;
            this.csvFile = csvFile;
            this.headerMap = new HashMap<>();
            this.notNullForeignKeys = new ArrayList<>();
            this.nullableForeignKeys = new ArrayList<>();
            this.entityGenerator = new CounterGenerator(-1, 1);
            
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
            
            if(schema.has(KEY_DATE_FORMAT)) {
                String dateFormat = this.schema.getString(KEY_DATE_FORMAT);
                this.dateFormatter = new SimpleDateFormat(dateFormat);
            }
            
            // initialize generators
            if(schema.has(KEY_COLUMN_GENERATORS)) {
                JSONArray jsonArray = schema.getJSONArray(KEY_COLUMN_GENERATORS);
                for(int i = 0; i < jsonArray.length(); i++) {
                    JSONObject generator = jsonArray.getJSONObject(i);
                    String generatorColumn = generator.getString(KEY_COLUMN);
                    String generatorClassName = generator.getString(KEY_CLASSNAME);
                    
                    String[] args = null;
                    if(generator.has(KEY_ARGUMENTS)) {
                        JSONArray arguments = generator.getJSONArray(KEY_ARGUMENTS);
                        args = new String[arguments.length()];
                        for(int j = 0; j < arguments.length(); j++) {
                            args[j] = arguments.getString(j);
                        }
                    }
                    
                    Property p = getType().getProperty(normalize(generatorColumn));
                    if(p != null) {
                        // construct the generator
                        Class<?> cl;
                        try {
                            cl = Class.forName(generatorClassName);
                            Constructor<?> cons = cl.getConstructor(String[].class);
                            p.setGenerator((Generator) cons.newInstance((Object) args));
                        } catch (Exception e) {
                            throw ClassUtil.wrapRun(e);
                        }
                    } else {
                        throw new RuntimeException(String.format("Unable to find property for column %s in table %s", generatorColumn, getType().getName()));
                    }
                }
            }
        }
        
        public DateFormat getDateFormatter() {
            return this.dateFormatter;
        }
        
        public void setContext(Object context) {
            this.context = context;
        }
        
        public Object getContext() {
            return this.context;
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
            JSONArray columns = this.schema.getJSONArray(KEY_COLUMNS);
            
            Set<String> propertiesWithGenerator = new HashSet<>();
            for (int i = 0; i < columns.length(); i++) {
                String columnName = normalize(columns.getString(i));
                Property p = getType().getProperty(columnName);
                if (p == null) {
                    throw new RuntimeException(
                            String.format("Unable to find column %s in table %s", columnName, getType().getName()));
                }
                if (p.getGenerator() != null) {
                    result.add(p);
                    propertiesWithGenerator.add(p.getName());
                } else {
                    // Check that a NOT NULL column either has data in the csv file or a generator
                    if (!this.headerMap.containsKey(columnName) && !p.isNullable()) {
                        throw new RuntimeException(String.format(
                                "NOT NULL column %s in table %s needs to either have data in the csv file or be configured with a generator",
                                columnName, this.getTableName()));
                    }
                }
            }
            
            for(Property p: getMissingRequiredProperties()) {
                if(!propertiesWithGenerator.contains(p.getName())) {
                    result.add(p);
                }
            }

            return result;
        }
        
        public Map<String, Object> loadNotNullFKData(CSVRecord csvRecord, JDBCDataStore dataStore) {
            return loadFKData(csvRecord, dataStore, this.notNullForeignKeys, true);
        }
        
        public Map<String, Object> loadNullableFKData(CSVRecord csvRecord, JDBCDataStore dataStore) {
            return loadFKData(csvRecord, dataStore, this.nullableForeignKeys, false);
        }        
        
        public Map<String, Object> loadFKData(CSVRecord csvRecord, JDBCDataStore dataStore, List<JSONObject> foreignKeysJson, boolean isNotNull) {
            Map<String, Object> result = new HashMap<>();
            
            if(csvRecord == null || dataStore == null) {
                return result;
            }
            
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
                    String fkValue = csvRecord.get(this.headerMap.get(headerName)).trim();
                    entityJSON.put(fkColName, fkValue);
                    lookupKeys.put(fkColName, fkValue);
                }
                 
                Object value = dataStore.getSessionContext().getSingleResult(bo, fkJson.getString(KEY_SELECT), lookupKeys);
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
                   throw new RuntimeException(String.format("Unable to find csv file for table " + fkTable));
               }
               
               Edge<CSVState> edge = new Edge<>("fkTable", fkState, this);
               addEdge(edge);
           }
        }
        
        private void addEdge(Edge<CSVState> edge) {
            loader.orderingGraph.addEdge(edge, edge.getStart(), edge.getEnd());            
        }
        
        public void writeToCSV(String filePath, Settings settings, JDBCDataStore dataStore) {
            try {
                //initialize FileWriter object
                FileWriter fileWriter = new FileWriter(filePath );

                //initialize CSVPrinter object                
                CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVExportImport.csvFileFormat);
                
                try {
                    loader.createRecords(this, settings, dataStore, csvPrinter);
                } finally {
                    csvPrinter.close();
                }
            } catch (IOException e) {
                ClassUtil.wrapRun(e);
            }
        }
    }
    
    public CSVLoader(Shape shape) {
        // A child shape is created so that we can manipulate the entity generators for the types
        this.shape = new DomainShape("test", shape, shape.getDataModel());
        this.orderingGraph = new DirectedSparseGraph<>();
        this.tableStateMap = new HashMap<>();
    }

    public CSVLoader(Shape shape, String path) {
        this(shape);
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
            createRecords(csvState, settings, po, null);
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
    
    private Set<String> getColumnsToPopulate(CSVState csvState, Map<String, Integer> colPosition, List<Property> generatedProperties) {
        // The columns list direct which table columns need to be populated
        Set<String> columns = csvState.getNonFKColumns();
        for(Map.Entry<String, Integer> entry: csvState.headerMap.entrySet()) {
            if(columns.contains(entry.getKey())) {
                colPosition.put(entry.getKey(), entry.getValue());
            }
        }
        // Add the not-null FK to the column list
        for(JSONObject json: csvState.notNullForeignKeys) {
            columns.add(CSVState.normalize(json.getString(KEY_FOREIGN_KEY)));
        }
        //Add the generated properties to the column list
        for(Property p: generatedProperties) {
            columns.add(p.getName());
        }
        
        return columns;
    }
    
    private void createRecords(CSVState csvState, Settings settings, JDBCDataStore dataStore, CSVPrinter csvPrinter) throws IOException {
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
        
        StateGraph.ObjectGenerationVisitor visitor = new StateGraph.ObjectGenerationVisitor(
                null,
                settings,
                null);
        // set any additional context
        visitor.setContext(0, csvState.getContext());
        
        JDBCSessionContext sc = dataStore == null ? null : dataStore.getSessionContext();
        if(isWriteToDB(csvPrinter)) {
            assert sc.getConnection() != null : "Can import only in the context of an existing JDBC connection";
        }
        
        GeneratorDriver entityGenerator = csvState.getEntityGenerator();
        entityType.addGenerator(entityGenerator);
        entityGenerator.init(sc == null ? null : sc.getConnection(), visitor);
        entityGenerator.processVisitors();
        Iterator entityIterator = (Iterator) entityGenerator;        

        Map<String, Integer> colPosition = new HashMap<>();
        List<Property> generatedProperties = csvState.getGeneratedProperties();
        Set<String> columns = getColumnsToPopulate(csvState, colPosition, generatedProperties);
                
        List<Property> dateProperties = new ArrayList<>();
        for(String column: columns) {
            Property p = csvState.getType().getProperty(column);
            if(Date.class.isAssignableFrom(p.getType().getInstanceClass())) {
                dateProperties.add(p);
            }
        }
        if(dateProperties.size() > 0 && csvState.getDateFormatter() == null) {
            throw new RuntimeException("Some of the columns are of type Date, and a 'dateFormat' expression is not set on the schema");
        }
        
        List<String> columnList = new ArrayList<>(columns);
        
        // Read the data for non FK columns
        try(InputStream is = CSVLoader.class.getClassLoader().getResourceAsStream(csvState.getCSVFile());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {        
            // skip first 2 lines
            if(reader.ready()) { reader.readLine();}
            if(reader.ready()) { reader.readLine();}
            
            try(CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT)) {
                Iterator<CSVRecord> csvIterator = parser.iterator();
                int i = 1;
                boolean csvPowered = false;
                CSVRecord csvRecord = null;
                while(csvIterator.hasNext() || entityIterator.hasNext()) {
                    csvRecord = csvIterator.hasNext() ? csvIterator.next() : null;
                    
                    // Only if we don't have csv data do we solely rely on entity driver generator powered
                    if(!csvPowered) {
                        if(csvRecord != null) {
                            csvPowered = true;
                        }
                    } else if(csvRecord == null) {
                        // end of csv powered processing
                        break;
                    }
                    
                    // Should support more records as it is a generator
                    entityIterator.next(); 
                    
                    JSONObject entityJSON = CSVExportImport.getJSON(colPosition, csvRecord);
                    
                    Map<String, Object> notNullFKData = csvState.loadNotNullFKData(csvRecord, dataStore);
                    for(Map.Entry<String, Object> entry: notNullFKData.entrySet()) {                    
                        entityJSON.put(entry.getKey(), entry.getValue());
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
                                    visitor));
                    }
                    
                    if(logger.isDebugEnabled()) {
                        logger.debug("entityJSON: " + entityJSON.toString());
                    }
                    if (isWriteToDB(csvPrinter)) {
                        BusinessObject bo = new ImmutableBO(csvState.getType(), null, null, null);
                        bo.setInstance(entityJSON);
                        dataStore.getSessionContext().create(bo, columnList);

                        DataImporter.performFlush(sc, i, false);
                    } else {
                        if(i == 1) {
                            csvPrinter.printRecord(columnList);
                        }
                        csvPrinter.printRecord(extractValues(entityJSON, columnList));
                    }
                    
                    // increment counter
                    i++;
                }
                
                if (isWriteToDB(csvPrinter)) {
                    DataImporter.performFlush(sc, i, true);
                }
            }
        }

        entityType.clearGenerators();
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
        try(InputStream is = CSVLoader.class.getClassLoader().getResourceAsStream(csvState.getCSVFile());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {                
            // skip first 2 lines
            if(reader.ready()) { reader.readLine();}
            if(reader.ready()) { reader.readLine();}
            
            try(CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT)) {
                int i = 1;
                JDBCSessionContext sc = dataStore.getSessionContext();
                for (CSVRecord csvRecord : parser) {                
                    JSONObject entityJSON = new JSONObject();                    
                    
                    // Fetch the foreign key value from the DB
                    Map<String, Object> nullableFKData = csvState.loadNullableFKData(csvRecord, dataStore);    
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
                    dataStore.getSessionContext().update(bo, columnsToUpdate, lookupKeys);
                    
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
     * @throws IOException if there is a problem with reading the CSV file
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
