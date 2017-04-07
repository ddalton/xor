package tools.xor.service.exim;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.EncryptedDocumentException;
import org.json.JSONArray;
import org.json.JSONObject;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.AggregateManager;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CSVExportImport extends AbstractExportImport
{
    private String filePath;
    private CSVPrinter csvPrinter;
    private List entityRecord;

    private static final CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");

    public CSVExportImport (AggregateManager am)
    {
        super(am);
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
    protected Map<String, Integer> getHeader (String path, String name) throws IOException
    {
        try(Reader entitySheet = new FileReader(path + name + Constants.XOR.CSV_FILE_SUFFIX);
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
    protected Set<String> setupPropertyColumns(List<String> propertyPaths, Type type) {
        Set<String> result = super.setupPropertyColumns(propertyPaths, type);

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

        return result;
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
    protected void finishupItem () {
        try {
            csvPrinter.printRecord(entityRecord);
        }
        catch (IOException e) {
            throw ClassUtil.wrapRun(e);
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
    protected void writeEntityHeader (String sheetName, EntityType entityType) {
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
        File file = new File(relationshipFilePath);
        if(!file.exists()) {
            // Relationships are not required
            return false;
        }

        return true;
    }

    @Override
    protected void addRelationships(String path, List attrPath) throws IOException
    {
        if(!hasRelationships(path)) {
            return;
        }

        Reader indexSheet = new FileReader(path + Constants.XOR.CSV_INDEX_SHEET);
        CSVParser parser = new CSVParser(indexSheet, CSVFormat.DEFAULT.withHeader());

        try {
            for (
                CSVRecord csvRecord
                : parser)

            {
                String entityInfo = csvRecord.get(1);
                Reader sheet = new FileReader(
                    path + csvRecord.get(0) + Constants.XOR.CSV_FILE_SUFFIX);
                CSVParser sheetParser = new CSVParser(sheet, CSVFormat.DEFAULT.withHeader());
                Map<String, Integer> sheetHeaderMap = sheetParser.getHeaderMap();

                addProperties(
                    getProperty(entityInfo).getName() + Settings.PATH_DELIMITER,
                    attrPath,
                    sheetHeaderMap
                );
            }
        } finally {
            parser.close();
        }
    }

    @Override public Object importAggregate (String filePath, Settings settings) throws IOException
    {
        super.importAggregate(filePath, settings);

        try {
            if(filePath == null || "".equals(filePath.trim())) {
                throw new IllegalArgumentException("filePath is required and needs to point to a directory.");
            }

            File folder = new File(filePath);
            if(!folder.isDirectory()) {
                throw new IllegalArgumentException("filePath " + filePath + " should represent a directory name.");
            }

            if(!filePath.endsWith(File.separator)) {
                filePath += File.separator;
            }

            Reader entitySheet = new FileReader(filePath + Constants.XOR.CSV_ENTITY_SHEET);
            CSVParser parser = new CSVParser(entitySheet, CSVFormat.DEFAULT.withHeader());

            try {
                // Get the entity class name
                Map<String, Integer> colMap = parser.getHeaderMap();
                setView(settings, filePath);

                List<Object> entityBatch = new LinkedList<>();
                for (
                    CSVRecord csvRecord
                    : parser)

                {
                    if(!colMap.containsKey(Constants.XOR.TYPE)) {
                        throw new RuntimeException("XOR.type column is missing");
                    }

                    String entityClassName = csvRecord.get(colMap.get(Constants.XOR.TYPE));

                    try {
                        settings.setEntityClass(Class.forName(entityClassName));
                    }
                    catch (ClassNotFoundException e) {
                        throw new RuntimeException("Class " + entityClassName + " is not found. Ensure the XOR.type column is populated.");
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
                    Map<String, JSONObject> idMap = parseEntities(
                        filePath,
                        entitySheets,
                        collectionSheets);

                    // 2. Create the collections
                    // The key in the collection property map is of the form <owner_xor_id>:<property>
                    Map<String, JSONArray> collectionPropertyMap = parseCollections(
                        filePath,
                        collectionSheets,
                        idMap);

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
            } finally {
                parser.close();
            }
        }
        catch (EncryptedDocumentException e) {
            throw new RuntimeException("Document is encrypted, provide a decrypted inputstream");
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
        // Ensure we have the XOR.id column in the entity sheet
        try(Reader entitySheet = new FileReader(path + sheetName + Constants.XOR.CSV_FILE_SUFFIX);
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
                JSONObject collectionEntryJSON = am.getJSON(colMap, csvRecord);
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
        try(Reader indexSheet = new FileReader(path + Constants.XOR.CSV_INDEX_SHEET);
            CSVParser parser = new CSVParser(indexSheet, CSVFormat.DEFAULT.withHeader())) {

            for (
                CSVRecord csvRecord
                : parser)

            {
                String entityInfo = csvRecord.get(1);

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
        try (Reader entitySheet = new FileReader(path + sheetName + Constants.XOR.CSV_FILE_SUFFIX);
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
                JSONObject entityJSON = am.getJSON(colMap, csvRecord);
                idMap.put(entityJSON.getString(Constants.XOR.ID), entityJSON);
            }
        }
    }

}
