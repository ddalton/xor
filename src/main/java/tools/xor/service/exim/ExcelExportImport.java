package tools.xor.service.exim;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.service.AggregateManager;
import tools.xor.util.Constants;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ExcelExportImport extends AbstractExportImport
{
    private Workbook wb;
    private SXSSFSheet sh;
    private int entitySheetRowNo;
    private Row row;
    private Cell cell;

    public ExcelExportImport (AggregateManager am)
    {
        super(am);
    }

    @Override
    protected  Map<String, Integer> getHeader(String path, String name) throws IOException
    {
        Sheet sheet = wb.getSheet(name);
        return getHeaderMap(sheet);
    }

    private Map<String, Integer> getHeaderMap (Sheet sheet)
    {
        Map<String, Integer> colMap = new HashMap<String, Integer>();
        Row headerRow = sheet.getRow(0);
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell headerCell = headerRow.getCell(i);
            colMap.put(headerCell.getStringCellValue(), i);
        }

        return colMap;
    }

    private boolean hasRelationships() {
        if(wb.getSheet(Constants.XOR.EXCEL_INDEX_SHEET) == null) {
            return false;
        }

        return true;
    }

    @Override
    protected void addRelationships(String path, List attrPath) throws IOException
    {
        Sheet relationshipSheet = wb.getSheet(Constants.XOR.EXCEL_INDEX_SHEET);
        if(!hasRelationships()) {
            return;
        }

        for (int i = 1; i <= relationshipSheet.getLastRowNum(); i++) {
            Row row = relationshipSheet.getRow(i);
            String entityInfo = row.getCell(1).getStringCellValue();

            Map<String, Integer> sheetHeaderMap = getHeader(
                path,
                row.getCell(0).getStringCellValue());

            addProperties(getProperty(entityInfo).getName() + Settings.PATH_DELIMITER,
                attrPath,
                sheetHeaderMap
            );
        }
    }

    @Override public Object importAggregate (String filePath, Settings settings) throws IOException
    {
        super.importAggregate(filePath, settings);

        try {
            FileInputStream is = new FileInputStream(filePath);
            this.wb = WorkbookFactory.create(is);

            Sheet entitySheet = wb.getSheet(Constants.XOR.EXCEL_ENTITY_SHEET);
            if (entitySheet == null) {
                throw new RuntimeException("The entity sheet is missing");
            }

            // Get the entity class name
            Map<String, Integer> colMap = getHeaderMap(entitySheet);
            setView(settings, filePath);

            List<Object> entityBatch = new LinkedList<>();
            for (int i = 1; i <= entitySheet.getLastRowNum(); i++) {
                JSONObject entityJSON = am.getJSON(colMap, entitySheet.getRow(i));

                if(!entityJSON.has(Constants.XOR.TYPE)) {
                    throw new RuntimeException("XOR.type column is missing");
                }
                String entityClassName = entityJSON.getString(Constants.XOR.TYPE);

                try {
                    settings.setEntityClass(Class.forName(entityClassName));
                }
                catch (ClassNotFoundException e) {
                    throw new RuntimeException("Class " + entityClassName + " is not found");
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
                Map<String, JSONObject> idMap = parseEntities(filePath, entitySheets, collectionSheets);

                // 2. Create the collections
                // The key in the collection property map is of the form <owner_xor_id>:<property>
                Map<String, JSONArray> collectionPropertyMap = parseCollections(
                    wb,
                    collectionSheets,
                    idMap);

                // 3. Associate the collections to their owners
                // Replace all objectref prefix keys with the actual objects
                // Replace all collection properties with the array objects
                link(idMap, collectionPropertyMap);

                // Find the root
                String rootId = entityJSON.getString(Constants.XOR.ID);
                JSONObject root = idMap.get(rootId);

                entityBatch.add(root);
            }

            return am.create(entityBatch, settings);
        }
        catch (EncryptedDocumentException e) {
            throw new RuntimeException("Document is encrypted, provide a decrypted inputstream");
        }
        catch (InvalidFormatException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void populateMaps(String path, Map<String, String> entitySheets,
                                Map<String, String> collectionSheets) throws IOException
    {
        // First find all the entity sheets
        Sheet sheetMap = wb.getSheet(Constants.XOR.EXCEL_INDEX_SHEET);
        if(!hasRelationships()) {
            return;
        }

        // SheetName is in first column
        // Entity type and property is in second column
        for (int i = 1; i <= sheetMap.getLastRowNum(); i++) {
            Row row = sheetMap.getRow(i);
            String entityInfo = row.getCell(1).getStringCellValue();

            if (getProperty(entityInfo).isMany()) {
                collectionSheets.put(row.getCell(0).getStringCellValue(), entityInfo);
            }
            else {
                entitySheets.put(row.getCell(0).getStringCellValue(), entityInfo);
            }
        }
    }

    private Map<String, JSONArray> parseCollections (Workbook wb,
                                                     Map<String, String> collectionSheets,
                                                     Map<String, JSONObject> idMap)
    {
        Map<String, JSONArray> collectionPropertyMap = new HashMap<String, JSONArray>();
        for (Map.Entry<String, String> entry : collectionSheets.entrySet()) {
            processCollectionSheet(
                wb,
                entry.getKey(),
                entry.getValue(),
                collectionPropertyMap,
                idMap);
        }

        return collectionPropertyMap;
    }

    @Override
    protected void processEntitySheet (String path, String sheetName, Map<String, JSONObject> idMap) throws
        IOException
    {
        // Ensure we have the XOR.id column in the entity sheet
        Sheet entitySheet = wb.getSheet(sheetName);
        Map<String, Integer> colMap = getHeaderMap(entitySheet);
        if (!colMap.containsKey(Constants.XOR.ID)) {
            throw new RuntimeException("XOR.id column is missing");
        }

        // process each entity
        for (int i = 1; i <= entitySheet.getLastRowNum(); i++) {
            JSONObject entityJSON = am.getJSON(colMap, entitySheet.getRow(i));
            idMap.put(entityJSON.getString(Constants.XOR.ID), entityJSON);
        }
    }

    private void processCollectionSheet (
        Workbook wb,
        String sheetName,
        String entityInfo, Map<String, JSONArray> collectionPropertyMap,
        Map<String, JSONObject> idMap)
    {
        // Ensure we have the XOR.id column in the entity sheet
        Sheet collectionSheet = wb.getSheet(sheetName);
        Map<String, Integer> colMap = getHeaderMap(collectionSheet);

        // A collection can have value objects, so XOR.ID is not mandatory
        // But a collection entry should have a collection owner
        if (!colMap.containsKey(Constants.XOR.OWNER_ID)) {
            throw new RuntimeException("XOR.owner.id column is missing");
        }

        // process each collection entry
        for (int i = 1; i <= collectionSheet.getLastRowNum(); i++) {
            JSONObject collectionEntryJSON = am.getJSON(colMap, collectionSheet.getRow(i));
            String key = getCollectionKey(
                collectionEntryJSON.getString(Constants.XOR.OWNER_ID),
                entityInfo);
            addCollectionEntry(collectionPropertyMap, key, collectionEntryJSON);

            // If the collection element is an entity add it to the idMap also
            if (collectionEntryJSON.has(Constants.XOR.ID)) {
                try {
                    idMap.put(collectionEntryJSON.getString(Constants.XOR.ID), collectionEntryJSON);
                }
                catch (Exception e) {
                    String longStr = new Long(collectionEntryJSON.getLong(Constants.XOR.ID)).toString();
                    idMap.put(longStr, collectionEntryJSON);
                }
            }
        }
    }

    @Override
    protected void setupExport(String filePath) throws FileNotFoundException
    {
        wb = new SXSSFWorkbook();
        ((SXSSFWorkbook)wb).setCompressTempFiles(true);
    }

    @Override
    protected void setupEntity(String sheetName) {
        entitySheetRowNo = 1;
        sh = (SXSSFSheet)wb.getSheet(sheetName);
        if (sh == null) {
            sh = (SXSSFSheet)wb.createSheet(sheetName);
        }
        else {
            entitySheetRowNo = sh.getLastRowNum() + 1;
        }
    }

    @Override
    protected void prepareItem() {
        row = sh.createRow(entitySheetRowNo++);
    }

    @Override
    protected void finishupItem () {
        // do nothing
    }

    @Override
    protected void writeRelationshipItem(String name, String entityInfo) {
        Cell sheetNameCell = row.createCell(0);
        Cell propertyNameCell = row.createCell(1);
        sheetNameCell.setCellValue(name);
        propertyNameCell.setCellValue(entityInfo);
    }

    @Override
    protected void prepareEntityItemProperty(String propertyPath) {
        cell = row.createCell(propertyColIndex.get(propertyPath));
    }

    @Override
    protected void writeEntityItemPropertyValue(String value) {
        cell.setCellValue(value.toString());
    }

    @Override
    protected void writeEntityHeader ()
    {
        Row row = sh.getRow(0);
        if (row != null) {
            // Column names have already been populated
            return;
        }

        row = sh.createRow(0);
        for (Map.Entry<String, Integer> entry : propertyColIndex.entrySet()) {
            Cell cell = row.createCell(entry.getValue());
            cell.setCellValue(entry.getKey());
            sh.autoSizeColumn(entry.getValue());
        }
    }

    @Override
    protected void setupRelationship() {
        sh = (SXSSFSheet)wb.createSheet(Constants.XOR.EXCEL_INDEX_SHEET);

        // Write header
        Row headerRow = sh.createRow(0);
        Cell sheetName = headerRow.createCell(0);
        Cell propertyName = headerRow.createCell(1);
        sheetName.setCellValue(getRelationshipHeaderCol1());
        propertyName.setCellValue(getRelationshipHeaderCol2());

        // Reset the row counter
        entitySheetRowNo = 1;
    }

    @Override
    protected void finishupRelationship() {
        sh.autoSizeColumn(0);
        sh.autoSizeColumn(1);
        wb.setSheetOrder(Constants.XOR.EXCEL_INDEX_SHEET, 1);
    }

    @Override
    protected void writeRelationshipMap(String filePath, Map<String, String> relationshipMap) throws
        IOException
    {
        super.writeRelationshipMap(filePath, relationshipMap);

        FileOutputStream os = new FileOutputStream(filePath);

        wb.write(os);
        os.close();
        wb.close();
    }
}
