package tools.xor.service.exim;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.AbstractBO;
import tools.xor.AbstractProperty;
import tools.xor.BusinessObject;
import tools.xor.EntityKey;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.MapperSide;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.SimpleType;
import tools.xor.TypeMapper;
import tools.xor.generator.Generator;
import tools.xor.generator.LinkedChoices;
import tools.xor.generator.Lot;
import tools.xor.generator.RandomSubset;
import tools.xor.service.AggregateManager;
import tools.xor.service.Shape;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.ObjectCreator;
import tools.xor.util.excel.ExcelExporter;
import tools.xor.util.graph.StateGraph;

public class ExcelExportImport extends AbstractExportImport
{
    private Workbook wb;
    private Sheet sh; // We cannot use the streaming version since we write the header after writing the body
    private int entitySheetRowNo;
    private Row row;
    private Cell cell;
    private Set<String> entityInfo = new HashSet<String>();
    private boolean streaming;

    private CellStyle headerStyle;
    private CellStyle requiredStyle;

    public ExcelExportImport (AggregateManager am)
    {
        super(am);
        
        if (ApplicationConfiguration.config().containsKey(Constants.Config.EXCEL_STREAMING)
            && ApplicationConfiguration.config().getBoolean(Constants.Config.EXCEL_STREAMING)) {
            streaming = true;
        } else {
            streaming = false;
        }
    }

    public static void initGenerators(InputStream is, Shape shape) {
        try {
            Workbook wb = WorkbookFactory.create(is);

            Sheet domainSheet = wb.getSheet(Constants.XOR.DOMAIN_TYPE_SHEET);
            if (domainSheet == null) {
                throw new RuntimeException("The Domain types sheet is missing");
            }

            for (int i = 1; i <= domainSheet.getLastRowNum(); i++) {
                Row row = domainSheet.getRow(i);
                String entityTypeName = row.getCell(1).getStringCellValue();
                String sheetName = row.getCell(0).getStringCellValue();
                String incomingProperty = row.getCell(2) == null ? null :
                    row.getCell(2).getStringCellValue();

                EntityType entityType = (EntityType)shape.getType(entityTypeName);
                Sheet entitySheet = wb.getSheet(sheetName);
                processDomainValues(entityType, entitySheet, incomingProperty);
            }

        } catch (Exception e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    private static void processDomainValues (EntityType entityType,
                                             Sheet entitySheet,
                                             String incomingProperty)
        throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
               InvocationTargetException, InstantiationException
    {
        Map<String, Integer> headerMap = ExcelExportImport.getHeaderMap(entitySheet);

        Lot lot = null;
        for(Map.Entry<String, Integer> entry: headerMap.entrySet()) {

            Row row = entitySheet.getRow(1);
            Class generatorClass = Class.forName(row.getCell(entry.getValue()).getStringCellValue());
            List<String> list = new ArrayList<String>();
            for(int i = 2; i <= entitySheet.getLastRowNum(); i++) {
                row = entitySheet.getRow(i);
                Cell cell = row.getCell(entry.getValue());

                String value = null;
                if (cell != null) {
                    try {
                        if (cell.getStringCellValue() != null) {
                            value = cell.getStringCellValue();
                        }
                    }
                    catch (Exception e) {
                        value = Double.toString(cell.getNumericCellValue());
                    }
                }
                if(value == null) {
                    break;
                }
                list.add(value);
            }

            String[] values = list.toArray(new String[list.size()]);
            Constructor cd = generatorClass.getConstructor(String[].class);

            Generator gen = (Generator)cd.newInstance((Object)values);
            if(gen instanceof LinkedChoices) {
                if(lot == null) {
                    lot = new Lot(((LinkedChoices)gen).getValues().length);
                }
                ((LinkedChoices)gen).setLot(lot);
            }

            if(gen instanceof RandomSubset) {
                gen.init(new StateGraph.ObjectGenerationVisitor(null, null, null));
            }

            ExtendedProperty property = (ExtendedProperty)entityType.getProperty(entry.getKey());

            if(incomingProperty == null || "".equals(incomingProperty.trim())) {
                incomingProperty = AbstractProperty.TYPE_GENERATOR;
            }

            // It can happen that that particular property is not present in
            // the entity type's shape
            if(property != null) {
                property.setGenerator(incomingProperty, gen);

                // Have it apply to all subtypes also
                for (EntityType subType : entityType.getSubtypes()) {
                    property = (ExtendedProperty)subType.getProperty(entry.getKey());
                    property.setGenerator(incomingProperty, gen);
                }
            }
        }
    }

    @Override
    protected  Map<String, Integer> getHeader(String path, String name) throws IOException
    {
        Sheet sheet = wb.getSheet(name);
        return getHeaderMap(sheet);
    }

    public static Map<String, Integer> getHeaderMap (Sheet sheet)
    {
        Map<String, Integer> colMap = new HashMap<String, Integer>();
        Row headerRow = sheet.getRow(0);

        // Check if sheet is empty
        if(headerRow == null) {
            return colMap;
        }

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

            Property property = getProperty(entityInfo);
            // if the property is not found or if the sheet is empty continue
            if(property == null || sheetHeaderMap.size() == 0) {
                continue;
            }
            addProperties(property.getName() + Settings.PATH_DELIMITER,
                attrPath,
                sheetHeaderMap
            );
        }
    }

    @Override public Object importAggregate (String filePath, Settings settings) throws IOException
    {
        super.importAggregate(filePath, settings);

        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);            
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
                JSONObject entityJSON = getJSON(colMap, entitySheet.getRow(i));

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

                // 3. Normalize collection entities
                // We need to swizzle the collection entity with the entity to share
                // that entity across collections
                swizzleCollectionElement(idMap, collectionPropertyMap);

                // 4. Associate the collections to their owners
                // Replace all objectref prefix keys with the actual objects
                // Replace all collection properties with the array objects
                link(idMap, collectionPropertyMap);

                // Find the root
                String rootId = entityJSON.getString(Constants.XOR.ID);
                JSONObject root = idMap.get(rootId);

                entityBatch.add(root);
            }

            return am.update(entityBatch, settings);
        }
        catch (EncryptedDocumentException e) {
            throw new RuntimeException("Document is encrypted, provide a decrypted inputstream");
        }
    }
    
    private static Object getCellValue (Cell cell)
    {
        if (cell != null) {
            try {
                return cell.getStringCellValue();
            }
            catch (Exception e) {
                // Numeric entry
                return cell.getNumericCellValue();
            }
        }
        else {
            return "";
        }
    }    
    
    public void importDenormalized (InputStream is, Settings settings) throws
        IOException
    {

        try {
            Workbook wb = WorkbookFactory.create(is);

            // First create the object based on the denormalized values
            // The callInfo should have root BusinessObject
            // clone the task object using a DataObject

            // Create an object creator for the target root
            TypeMapper typeMapper = am.getDataModel().getTypeMapper().newInstance(MapperSide.EXTERNAL);
            ObjectCreator oc = new ObjectCreator(
                settings,
                am.getDataStore(),
                typeMapper);            

            // The Excel should have a single sheet containing the denormalized data
            // Create a JSONObject for each row
            Sheet entitySheet = wb.getSheetAt(0);
            Map<String, Integer> colMap = getHeaderMap(wb.getSheetAt(0));

            Map<BusinessObject, Object> roots = new IdentityHashMap<BusinessObject, Object>();
            for (int i = 1; i <= entitySheet.getLastRowNum(); i++) {
                Row row = entitySheet.getRow(i);

                Property idProperty = ((EntityType)settings.getEntityType()).getIdentifierProperty();
                String idName = idProperty.getName();
                if(!colMap.containsKey(idName)) {
                    throw new RuntimeException("The Excel sheet needs to have the entity identifier column");
                }

                // TODO: Create a JSON object and then extract the value with the correct type
                Object idValue = getCellValue(row.getCell(colMap.get(idName)));
                if(idProperty.getType() instanceof SimpleType) {
                    idValue = ((SimpleType)idProperty.getType()).unmarshall(idValue.toString());
                }

                // Get a child business object of the same type
                // TODO: Get by user key
                //EntityKey ek = oc.getTypeMapper().getEntityKey(idValue, settings.getEntityType());
                EntityKey ek = oc.getTypeMapper().getSurrogateKey(idValue, settings.getEntityType());
                BusinessObject bo = oc.getByEntityKey(ek, settings.getEntityType());
                if (bo == null) {
                    
                    bo = oc.createDataObject(
                        AbstractBO.createInstance(oc, idValue, settings.getEntityType()),
                        settings.getEntityType(),
                        null,
                        null);
                    BusinessObject potentialRoot = (BusinessObject)bo.getRootObject();
                    if(!roots.containsKey(potentialRoot)) {
                        roots.put(potentialRoot, null);
                    }
                }

                for (Map.Entry<String, Integer> entry : colMap.entrySet()) {
                    Cell cell = row.getCell(entry.getValue());
                    Object cellValue = getCellValue(cell);
                    Property property = (settings.getEntityType()).getProperty(entry.getKey());
                    cellValue = ((SimpleType)property.getType()).unmarshall(cellValue.toString());
                    bo.set(entry.getKey(), cellValue);
                }
            }
            for(BusinessObject root: roots.keySet()) {
                am.update(root.getInstance(), settings);
            }

        }
        catch (EncryptedDocumentException e) {
            throw new RuntimeException("Document is encrypted, provide a decrypted inputstream", e);
        }
        catch (InvalidFormatException e) {
            throw new RuntimeException("The provided inputstream is not valid. ", e);
        }
        catch (Exception e) {
            throw new RuntimeException("An error occurred during update.", e);
        }
    }

    public void exportDenormalized (OutputStream outputStream, Settings settings)
    {
        // Make sure this is a denormalized query
        settings.setDenormalized(true);
        List<?> result = am.query(null, settings);

        // Currently only address one sheet, additional sheets will handle
        // dependencies
        ExcelExporter e = new ExcelExporter(outputStream, settings);

        // The first row is the column names
        e.writeRow(result.get(0));

        for (int i = 1; i < result.size(); i++) {
            e.writeRow(result.get(i));
        }
        // TODO: add validation support
        e.writeValidations();

        e.finish();
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

            Property property = getProperty(entityInfo);
            if(property == null) {
                // Meta-data has changed between the time import and export was done
                // or the configuration during import is not the same as it was during export
                continue;
            }
            if (property.isMany()) {
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
            JSONObject entityJSON = getJSON(colMap, entitySheet.getRow(i));
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

        // empty sheet
        if(colMap.size() == 0) {
            return;
        }

        // A collection can have value objects, so XOR.ID is not mandatory
        // But a collection entry should have a collection owner
        if (!colMap.containsKey(Constants.XOR.OWNER_ID)) {
            throw new RuntimeException("XOR.owner.id column is missing in sheet: " + sheetName);
        }

        // process each collection entry
        for (int i = 1; i <= collectionSheet.getLastRowNum(); i++) {
            Row row = collectionSheet.getRow(i);
            if(row == null) {
                // skip empty rows
                continue;
            }
            JSONObject collectionEntryJSON = getJSON(colMap, row);
            String key = getCollectionKey(
                collectionEntryJSON.getString(Constants.XOR.OWNER_ID),
                entityInfo);
            addCollectionEntry(collectionPropertyMap, key, collectionEntryJSON);

            // If the collection element is an entity add it to the idMap also
            if (collectionEntryJSON.has(Constants.XOR.ID)) {
                idMap.put(getId(collectionEntryJSON), collectionEntryJSON);
            }
        }
    }

    @Override
    protected void setupExport(String filePath) throws FileNotFoundException
    {
        if(streaming) {
            wb = new SXSSFWorkbook();
        } else {
            wb = new XSSFWorkbook();
        }

        requiredStyle = wb.createCellStyle();
        requiredStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        requiredStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(requiredStyle, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT.getIndex());

        headerStyle = wb.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(headerStyle, BorderStyle.THIN, IndexedColors.AUTOMATIC.getIndex());
        Font font= wb.createFont();
        font.setFontHeightInPoints((short)11);
        font.setFontName("Times New Roman");
        font.setColor(IndexedColors.BLACK.getIndex());
        font.setBold(true);
        font.setItalic(false);
        headerStyle.setFont(font);
    }

    private void setBorder (CellStyle style, BorderStyle borderType,
                            short borderColor)
    {
        style.setBorderLeft(borderType);
        style.setBorderTop(borderType);
        style.setBorderBottom(borderType);
        style.setBorderRight(borderType);
        style.setBottomBorderColor(borderColor);
        style.setTopBorderColor(borderColor);
        style.setLeftBorderColor(borderColor);
        style.setRightBorderColor(borderColor);
    }

    @Override
    protected boolean setupEntity(String sheetName) {
        boolean result = false;

        entitySheetRowNo = 1;
        sh = wb.getSheet(sheetName);
        if (sh == null) {
            sh = wb.createSheet(sheetName);
            result = true;
        }
        else {
            entitySheetRowNo = sh.getLastRowNum() + 1;
        }

        return result;
    }

    @Override
    protected void prepareItem() {
        row = sh.createRow(entitySheetRowNo++);
    }
    
    @Override
    protected void finishItem () {
        // do nothing
    }    

    @Override
    protected void finishEntity () {
        // do nothing
    }

    @Override
    protected void writeRelationshipItem(String name, String entityInfo) {
        Cell sheetNameCell = row.createCell(0);
        Cell propertyNameCell = row.createCell(1);
        Cell entityTypeCell = row.createCell(2);
        sheetNameCell.setCellValue(name);
        propertyNameCell.setCellValue(entityInfo);

        Property property = getProperty(entityInfo);
        String entityTypeName = property.getType().getName();
        if(((ExtendedProperty)property).isMany()) {
            entityTypeName = ((ExtendedProperty)property).getElementType().getName();
        }
        entityTypeCell.setCellValue(entityTypeName);
    }

    @Override
    protected void prepareEntityItemProperty(String propertyPath, Set<String> requiredColumns) {
        cell = row.createCell(propertyColIndex.get(propertyPath));
        if(requiredColumns.contains(propertyPath)) {
            cell.setCellStyle(requiredStyle);
        }
    }

    @Override
    protected void writeEntityItemPropertyValue(String value) {
        cell.setCellValue(value.toString());
    }

    @Override
    protected void writeEntityHeader (String sheetName, EntityStructure entityStructure)
    {
        writeEntityHeader(sh, sheetName, propertyColIndex);
        
        writeInfo(sheetName, entityStructure);
    }

    protected void writeEntityHeader (Sheet sh, String sheetName, Map<String, Integer> header) {
        Row row = sh.getRow(0);
        if (row != null) {
            // Column names have already been populated
            return;
        }

        row = sh.createRow(0);
        for (Map.Entry<String, Integer> entry : header.entrySet()) {
            Cell cell = row.createCell(entry.getValue());
            cell.setCellValue(entry.getKey());
            cell.setCellStyle(headerStyle);
            if(!streaming) {
                sh.autoSizeColumn(entry.getValue());
            }
        }
    }

    private void writeInfo(String sheetName, EntityStructure entityStructure) {
        for (EntityType entityType : entityStructure.getSubTypeMap().values()) {
            if (entityInfo.contains(entityType.getName())) {
                return;
            } else {
                entityInfo.add(entityType.getName());
            }

            Sheet infoSheet = wb.getSheet(Constants.XOR.EXCEL_INFO_SHEET);
            if (infoSheet == null) {
                CellStyle blankBG = wb.createCellStyle();
                blankBG.setFillForegroundColor(IndexedColors.WHITE.getIndex());
                blankBG.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                CellStyle separator = wb.createCellStyle();
                separator.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                separator.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                infoSheet = wb.createSheet(Constants.XOR.EXCEL_INFO_SHEET);
                wb.setSheetOrder(Constants.XOR.EXCEL_INFO_SHEET, 0);

                Row infoRow = infoSheet.createRow(0);
                infoRow.setRowStyle(blankBG);
                infoRow = infoSheet.createRow(1);
                infoRow.setRowStyle(blankBG);
                Cell cell = infoRow.createCell(1);
                cell.setCellValue("Legend");

                infoRow = infoSheet.createRow(2);
                infoRow.setRowStyle(blankBG);
                cell = infoRow.createCell(1);
                cell.setCellStyle(requiredStyle);
                cell = infoRow.createCell(2);
                cell.setCellValue("Required");

                infoRow = infoSheet.createRow(3);
                infoRow.setRowStyle(blankBG);
                infoRow = infoSheet.createRow(4);
                infoRow.setRowStyle(separator);
            }

            int startRow = infoSheet.getLastRowNum() + 2;
            Row sheetDetailsRow = infoSheet.createRow(startRow);
            infoSheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, 1));
            cell = sheetDetailsRow.createCell(0);
            cell.setCellValue("Entity: " + entityType.getName());
            /*
             * infoSheet.addMergedRegion(new CellRangeAddress(startRow, startRow,0,1)); Cell
             * cell = sheetDetailsRow.createCell(0); cell.setCellValue("Sheet Name: " +
             * sheetName);
             * 
             * infoSheet.addMergedRegion(new CellRangeAddress(startRow, startRow,2,4)); cell
             * = sheetDetailsRow.createCell(2); cell.setCellValue("Entity: " +
             * entityType.getName());
             */

            // Create header area
            startRow++;
            sheetDetailsRow = infoSheet.createRow(startRow);
            cell = sheetDetailsRow.createCell(0);
            cell.setCellValue("Property");
            cell.setCellStyle(headerStyle);
            cell = sheetDetailsRow.createCell(1);
            cell.setCellValue("Type");
            cell.setCellStyle(headerStyle);
            cell = sheetDetailsRow.createCell(2);
            cell.setCellValue("Vector");
            cell.setCellStyle(headerStyle);
            cell = sheetDetailsRow.createCell(3);
            cell.setCellValue("Required");
            cell.setCellStyle(headerStyle);
            cell = sheetDetailsRow.createCell(4);
            cell.setCellValue("isOpen");
            cell.setCellStyle(headerStyle);

            EntityType domainType = (EntityType) am.getTypeMapper().getDomainShape()
                    .getType(entityType.getEntityName());
            for (Property property : domainType.getProperties()) {
                startRow++;
                sheetDetailsRow = infoSheet.createRow(startRow);
                cell = sheetDetailsRow.createCell(0);
                cell.setCellValue(property.getName());
                cell = sheetDetailsRow.createCell(1);

                if (property.isMany()) {
                    String typeName = ((ExtendedProperty) property).getElementType() == null
                            ? property.getType().getName()
                            : ((ExtendedProperty) property).getElementType().getName();
                    cell.setCellValue(typeName);
                } else {
                    cell.setCellValue(property.getType().getName());
                }
                cell = sheetDetailsRow.createCell(2);
                cell.setCellValue(property.isMany() ? "true" : "");
                cell = sheetDetailsRow.createCell(3);
                cell.setCellValue(property.isNullable() ? "" : "true");
                cell = sheetDetailsRow.createCell(4);
                cell.setCellValue(property.isOpenContent() ? "true" : "");
            }

            if (!streaming) {
                infoSheet.autoSizeColumn(0);
                infoSheet.autoSizeColumn(1);
                infoSheet.autoSizeColumn(2);
                infoSheet.autoSizeColumn(3);
                infoSheet.autoSizeColumn(4);
            }
        }
    }

    @Override
    protected void setupRelationship() {
        sh = wb.createSheet(Constants.XOR.EXCEL_INDEX_SHEET);

        // Write header
        Row headerRow = sh.createRow(0);
        Cell sheetName = headerRow.createCell(0);
        Cell propertyName = headerRow.createCell(1);
        Cell entityType = headerRow.createCell(2);
        sheetName.setCellValue(getRelationshipHeaderCol1());
        propertyName.setCellValue(getRelationshipHeaderCol2());
        entityType.setCellValue(getEntityTypeCol3());

        // set header style
        sheetName.setCellStyle(headerStyle);
        propertyName.setCellStyle(headerStyle);
        entityType.setCellStyle(headerStyle);

        // Reset the row counter
        entitySheetRowNo = 1;
    }

    @Override
    protected void finishupRelationship() {
        if(!streaming) {
            sh.autoSizeColumn(0);
            sh.autoSizeColumn(1);
            sh.autoSizeColumn(2);
        }            
        wb.setSheetOrder(Constants.XOR.EXCEL_INDEX_SHEET, 1);
    }

    @Override
    protected void writeRelationshipMap(Map<String, String> relationshipMap) throws
        IOException
    {
        super.writeRelationshipMap(relationshipMap);
    }
    
    protected void finishExport (String filePath) throws
    IOException
    {
        // Write the file
        FileOutputStream os = new FileOutputStream(filePath);
        wb.write(os);
        os.close();
        wb.close();          
    }
}
