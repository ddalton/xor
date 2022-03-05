package tools.xor.service.exim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.Settings;
import tools.xor.service.AggregateManager;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;

public class CSVExportImport extends AbstractExportImport
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    private String filePath;
    private CSVPrinter csvPrinter;
    private List entityRecord;

    public static final CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
    public static final String urlSeparator = "/";

    public CSVExportImport (AggregateManager am)
    {
        super(am);
    }
    
    public static JSONObject getJSON (Map<String, Integer> colMap, CSVRecord row) {
        return getJSON(colMap, row, true);
    }
    
    public static JSONObject getJSON (Map<String, Integer> colMap, CSVRecord row, boolean resolveEmbedded)
    {
        JSONObject entity = new JSONObject();

        if (row != null) {
            for (Map.Entry<String, Integer> entry : colMap.entrySet()) {
                String value = row.get(entry.getValue());
                if (resolveEmbedded && isEmbeddedPath(entry.getKey())) {
                    setEmbeddableValue(entity, entry.getKey(), value);
                } else {
                    // set direct value
                    if (value != null) {
                        // if(NumberUtils.isNumber(value)) {
                        // entity.put(entry.getKey(), NumberUtils.toDouble(value));
                        // } else {
                        entity.put(entry.getKey(), value);
                        // }
                    } else {
                        // entity.put(entry.getKey(), JSONObject.NULL);
                        entity.put(entry.getKey(), "");
                    }
                }
            }
        }

        return entity;
    }    

    protected void setupExport (String filePath) throws
        IOException {

        this.filePath = filePath;

        File file = new File(this.filePath);
        if (file.exists()) {
            throw new RuntimeException("Folder '" + filePath + " exists! Please rename/delete existing folder.");
        }

        file.mkdirs();
    }
    
    @Override
    protected void finishExport (String filePath) throws
    IOException {
        // No-op
    }

    @Override
    protected Map<String, Integer> getHeader (String path, String name) throws IOException
    {        
        try(InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path + name + Constants.XOR.CSV_FILE_SUFFIX);
                Reader entitySheet = new InputStreamReader(is);
                CSVParser parser = new CSVParser(entitySheet, CSVFormat.DEFAULT.withHeader())) {
            Map<String, Integer> headerMap = parser.getHeaderMap();

            return headerMap;
        }
    }

    @Override
    protected boolean setupEntity (String name) {
        boolean result = true;

        if(csvPrinter != null) {
            return false;
        }

        try {
            name += Constants.XOR.CSV_FILE_SUFFIX;

            //initialize FileWriter object
            FileWriter fileWriter = new FileWriter(filePath + name);

            //initialize CSVPrinter object
            csvPrinter = new CSVPrinter(fileWriter, csvFileFormat);
        } catch(IOException ioe) {
            throw ClassUtil.wrapRun(ioe);
        }

        return result;
    }

    @Override
    protected void setupPropertyColumns(EntityStructure entityStructure) {
        super.setupPropertyColumns(entityStructure);

        Object [] FILE_HEADER = new String[propertyColIndex.size()];

        for(Map.Entry<String, Integer> entry: propertyColIndex.entrySet()) {
            FILE_HEADER[entry.getValue()] = entry.getKey();
        }

        try {
            //Create CSV file header
            csvPrinter.printRecord(FILE_HEADER);
        } catch (IOException ioe) {
            throw ClassUtil.wrapRun(ioe);
        }
    }

    @Override
    protected void prepareEntityItemProperty(String propertyPath, Set<String> requiredColumns) {
        // do nothing
    }

    @Override
    protected void prepareItem() {
        entityRecord = new ArrayList();
    }
    
    @Override
    protected void finishItem () {
        try {
            csvPrinter.printRecord(entityRecord);            
        }
        catch (IOException e) {
            throw ClassUtil.wrapRun(e);
        }
    }        

    @Override
    protected void finishEntity () {
        try {
            if(csvPrinter != null) {            
                csvPrinter.close();
                csvPrinter = null;
            }
        }
        catch (IOException e) {
            ClassUtil.wrapRun(e);
        }            
    }

    @Override
    protected void writeRelationshipItem(String name, String entityInfo) {
        entityRecord.add(name);
        entityRecord.add(entityInfo);
        try {
            csvPrinter.printRecord(entityRecord);
        }
        catch (IOException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    @Override
    protected void writeEntityItemPropertyValue(String value) {
        entityRecord.add(value);
    }

    @Override
    protected void writeEntityHeader (String sheetName, EntityStructure entityStructure) {
         // No-op
    }

    @Override
    protected void setupRelationship() {

        //initialize FileWriter object
        try {
            FileWriter fileWriter = new FileWriter(filePath + Constants.XOR.CSV_INDEX_SHEET);

            //initialize CSVPrinter object
            csvPrinter = new CSVPrinter(fileWriter, csvFileFormat);
        }
        catch (IOException e) {
            ClassUtil.wrapRun(e);
        }

        Object [] FILE_HEADER = new String[2];
        FILE_HEADER[0] = getRelationshipHeaderCol1();
        FILE_HEADER[1] = getRelationshipHeaderCol2();
    }

    @Override
    protected void finishupRelationship() {
        if(csvPrinter != null) {
            try {
                csvPrinter.close();
                csvPrinter = null;
            }
            catch (IOException e) {
                ClassUtil.wrapRun(e);
            }
        }
    }

    private boolean hasRelationships(String path) {
        String relationshipFilePath = path + Constants.XOR.CSV_INDEX_SHEET;
        URL resource = Thread.currentThread().getContextClassLoader().getResource(relationshipFilePath);           
        File file = new File(resource.getPath());        

        if(!file.exists()) {
            // Relationships are not required
            logger.info("No relationship file found under path: " + resource.getPath());
            return false;
        }
        logger.info("Relationship file found under path: " + resource.getPath());

        return true;
    }

    @Override
    protected void addRelationships(String path, List attrPath) throws IOException
    {
        if(!hasRelationships(path)) {
            return;
        }

        try(InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path + Constants.XOR.CSV_INDEX_SHEET);
                Reader indexSheet = new InputStreamReader(is);        
                CSVParser parser = new CSVParser(indexSheet, CSVFormat.DEFAULT.withHeader());) {
            for (CSVRecord csvRecord : parser)

            {
                String entityInfo = csvRecord.get(1);
                try (InputStream isc = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(path + csvRecord.get(0) + Constants.XOR.CSV_FILE_SUFFIX);
                        Reader sheet = new InputStreamReader(isc);
                        CSVParser sheetParser = new CSVParser(sheet, CSVFormat.DEFAULT.withHeader());) {
                    Map<String, Integer> sheetHeaderMap = sheetParser.getHeaderMap();

                    addProperties(getProperty(entityInfo).getName() + Settings.PATH_DELIMITER, attrPath,
                            sheetHeaderMap);
                }
            }
        } 
    }

    @Override
    public Object importAggregate (String filePath,
                                   Settings settings) throws IOException
    {
        super.importAggregate(filePath, settings);

        if (filePath == null || "".equals(filePath.trim())) {
            throw new IllegalArgumentException(
                "filePath is required and needs to point to a directory.");
        }

        URL resource = Thread.currentThread().getContextClassLoader().getResource(filePath);
        File folder = new File(resource.getPath());
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException(
                "filePath " + filePath + " should represent a directory name.");
        }

        if (!filePath.endsWith(urlSeparator)) {
            filePath += urlSeparator;
        }

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
            filePath + Constants.XOR.CSV_ENTITY_SHEET);
        Reader entitySheet = new InputStreamReader(is);
        CSVParser parser = new CSVParser(entitySheet, CSVFormat.DEFAULT.withHeader());

        try {
            // Get the entity class name
            Map<String, Integer> colMap = parser.getHeaderMap();
            setView(settings, filePath);

            List<Object> entityBatch = new LinkedList<>();
            for (CSVRecord csvRecord : parser) {
                if (!colMap.containsKey(Constants.XOR.TYPE)) {
                    throw new RuntimeException("XOR.type column is missing");
                }

                String entityClassName = csvRecord.get(colMap.get(Constants.XOR.TYPE));

                try {
                    settings.setEntityClass(Class.forName(entityClassName));
                }
                catch (ClassNotFoundException e) {
                    throw new RuntimeException("Class " + entityClassName
                        + " is not found. Ensure the XOR.type column is populated.");
                }

                /******************************************************
                 * Algorithm
                 *
                 * 1. Create all objects with the XOR.id
                 * 2. Create the collections
                 * 3. Associate the collections to their owners
                 * 4. Then finally call JSONTransformer.unpack to link the objects by XOR.id
                 *
                 ********************************************************/

                // 1. Create all objects with the XOR.id
                Map<String, String> collectionSheets = new HashMap<String, String>();
                Map<String, String> entitySheets = new HashMap<String, String>();
                entitySheets.put(Constants.XOR.EXCEL_ENTITY_SHEET, entityClassName);
                Map<String, JSONObject> idMap = parseEntities(filePath, entitySheets,
                    collectionSheets);

                // 2. Create the collections
                // The key in the collection property map is of the form <owner_xor_id>:<property>
                Map<String, JSONArray> collectionPropertyMap = parseCollections(filePath,
                    collectionSheets, idMap);

                // 3. Associate the collections to their owners
                // Replace all objectref prefix keys with the actual objects
                // Replace all collection properties with the array objects
                link(idMap, collectionPropertyMap);

                // Find the root
                String rootId = csvRecord.get(colMap.get(Constants.XOR.ID));
                JSONObject root = idMap.get(rootId);

                entityBatch.add(root);
            }

            return am.create(entityBatch, settings);
        }
        finally {
            parser.close();
        }
    }

    private Map<String, JSONArray> parseCollections (String path,
                                                     Map<String, String> collectionSheets,
                                                     Map<String, JSONObject> idMap) throws IOException
    {
        Map<String, JSONArray> collectionPropertyMap = new HashMap<String, JSONArray>();
        for (Map.Entry<String, String> entry : collectionSheets.entrySet()) {
            processCollectionSheet(
                path,
                entry.getKey(),
                entry.getValue(),
                collectionPropertyMap,
                idMap);
        }

        return collectionPropertyMap;
    }

    private void processCollectionSheet (
        String path,
        String sheetName,
        String entityInfo, Map<String, JSONArray> collectionPropertyMap,
        Map<String, JSONObject> idMap) throws IOException
    {
        // Ensure we have the XOR.id column in the entity sheet
        try(InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path + sheetName + Constants.XOR.CSV_FILE_SUFFIX);
                Reader entitySheet = new InputStreamReader(is);
                CSVParser parser = new CSVParser(entitySheet, CSVFormat.DEFAULT.withHeader())) {

            // Get the entity class name
            Map<String, Integer> colMap = parser.getHeaderMap();

            // A collection can have value objects, so XOR.ID is not mandatory
            // But a collection entry should have a collection owner
            if (!colMap.containsKey(Constants.XOR.OWNER_ID)) {
                throw new RuntimeException("XOR.owner.id column is missing");
            }

            // process each collection entry
            for (
                CSVRecord csvRecord
                : parser)

            {
                JSONObject collectionEntryJSON = getJSON(colMap, csvRecord);
                String key = getCollectionKey(
                    collectionEntryJSON.getString(Constants.XOR.OWNER_ID),
                    entityInfo);
                addCollectionEntry(collectionPropertyMap, key, collectionEntryJSON);

                // If the collection element is an entity add it to the idMap also
                if (collectionEntryJSON.has(Constants.XOR.ID)) {
                    try {
                        idMap.put(
                            collectionEntryJSON.getString(Constants.XOR.ID),
                            collectionEntryJSON);
                    }
                    catch (Exception e) {
                        String longStr = new Long(collectionEntryJSON.getLong(Constants.XOR.ID)).toString();
                        idMap.put(longStr, collectionEntryJSON);
                    }
                }
            }
        }
    }

    @Override
    protected void populateMaps(String path, Map<String, String> entitySheets,
                 Map<String, String> collectionSheets) throws IOException
    {
        if(!hasRelationships(path)) {
            return;
        }

        // First find all the entity sheets
        try(InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path + Constants.XOR.CSV_INDEX_SHEET);
                Reader indexSheet = new InputStreamReader(is);
                CSVParser parser = new CSVParser(indexSheet, CSVFormat.DEFAULT.withHeader())) {

            for (
                CSVRecord csvRecord
                : parser)

            {
                String entityInfo = csvRecord.get(1);

                logger.info("Relationship entity: " + entityInfo);
                if (getProperty(entityInfo).isMany()) {
                    collectionSheets.put(csvRecord.get(0), entityInfo);
                }
                else {
                    entitySheets.put(csvRecord.get(0), entityInfo);
                }
            }
        }
    }

    @Override
    protected void processEntitySheet (String path, String sheetName, Map<String, JSONObject> idMap) throws
        IOException
    {
        // Ensure we have the XOR.id column in the entity sheet
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path + sheetName + Constants.XOR.CSV_FILE_SUFFIX);
                Reader entitySheet = new InputStreamReader(is);
                CSVParser parser = new CSVParser(entitySheet, CSVFormat.DEFAULT.withHeader())) {


            // Get the entity class name
            Map<String, Integer> colMap = parser.getHeaderMap();
            if (!colMap.containsKey(Constants.XOR.ID)) {
                throw new RuntimeException("XOR.id column is missing");
            }

            for (
                CSVRecord csvRecord
                : parser)

            {
                JSONObject entityJSON = getJSON(colMap, csvRecord);
                idMap.put(entityJSON.getString(Constants.XOR.ID), entityJSON);
            }
        }
    }

}
