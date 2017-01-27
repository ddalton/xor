package tools.xor.service.exim;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.ExcelJsonTypeMapper;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.AggregateManager;
import tools.xor.util.Constants;
import tools.xor.util.ExcelJsonCreationStrategy;
import tools.xor.view.AggregateView;

import javax.swing.text.html.parser.Entity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractExportImport implements ExportImport
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    protected AggregateManager am;
    protected Map<String, Integer> propertyColIndex;

    public AbstractExportImport (AggregateManager am)
    {
        this.am = am;
    }

    private void validateImportExport ()
    {
        if (!ExcelJsonTypeMapper.class.isAssignableFrom(am.getTypeMapper().getClass())) {
            throw new RuntimeException("Import/Export can only work with ExcelJsonTypeMapper");
        }
    }

    protected Map<String, JSONObject> parseEntities(String path,
                                                  Map<String, String> entitySheets,
                                                  Map<String, String> collectionSheets) throws
        IOException
    {
        populateMaps(path, entitySheets, collectionSheets);

        Map<String, JSONObject> idMap = new HashMap<String, JSONObject>();
        for (Map.Entry<String, String> entry : entitySheets.entrySet()) {
            processEntitySheet(path, entry.getKey(), idMap);
        }

        return idMap;
    }

    protected void addProperties(String prefix, List attrPath, Map<String, Integer> headerMap) {
        for(Map.Entry<String, Integer> entry : headerMap.entrySet()) {
            String propertyName = entry.getKey();
            //Property property = entityType.getProperty(entry.getKey());
            if(!propertyName.startsWith(Constants.XOR.XOR_PATH_PREFIX) && !propertyName.startsWith(Constants.XOR.OBJECTREF)) {
                attrPath.add(prefix + entry.getKey());
            }
        }
    }

    protected Type getType (String entityInfo)
    {
        // Parse the entity classname from this
        String[] tokens = entityInfo.split(":");
        if (tokens.length != 2) {
            throw new RuntimeException(
                "The entity info column in sheet map is not in <classname>:<property> format: "
                    + entityInfo);
        }
        return am.getDAS().getType(tokens[0]);
    }

    protected Property getProperty (String entityInfo)
    {
        Type type = getType(entityInfo);

        String[] tokens = entityInfo.split(":");
        return type.getProperty(tokens[1]);
    }

    protected void setView(Settings settings, String path) throws IOException
    {
        // set the view
        // Create view based on the CSV header fields
        AggregateView view = new AggregateView("CSV_IMPORT");
        List attrPath = new ArrayList();
        view.setAttributeList(attrPath);
        settings.setView(view);

        // Get the view fields for the entity
        Map<String, Integer> headerMap = getHeader(path, Constants.XOR.EXCEL_ENTITY_SHEET);
        addProperties("", attrPath, headerMap);

        // Get the view fields for the relationships
        addRelationships(path, attrPath);
    }

    @Override
    public Object importAggregate (String filePath, Settings settings) throws IOException
    {
        validateImportExport();
        return null;
    }

    @Override public void exportAggregate (String filePath,
                                           Object inputObject,
                                           Settings settings) throws IOException
    {
        validateImportExport();

        BusinessObject to = am.readBO(inputObject, settings);
        Set<BusinessObject> dataObject = to.getObjectCreator().getDataObjects();

        // Get the container and the containment property and create a sheet of such objects
        Map<String, List<BusinessObject>> sheetBO = new HashMap<String, List<BusinessObject>>();
        for (BusinessObject bo : dataObject) {
            if (bo.getContainer() != null && bo.getContainmentProperty() != null) {
                String key = Constants.XOR.getExcelSheetFullName(
                    bo.getContainer().getType(),
                    bo.getContainmentProperty());
                if (!sheetBO.containsKey(key)) {
                    sheetBO.put(key, new LinkedList<BusinessObject>());
                }
                List<BusinessObject> boList = sheetBO.get(key);
                boList.add(bo);
            }
        }

        processBO(filePath, to, sheetBO);
    }

    /**
     * Generate the Excel sheets based on entities and collections
     * TODO: Should the map be topologically ordered?
     *
     * @param filePath path to the Excel file or the CSV folder
     * @param to      root entity
     * @param sheetBO map of the sheet name and the entities/relationships within that sheet
     * @throws IOException when the file cannot be written to
     */
    protected void processBO (String filePath,
                              BusinessObject to,
                              Map<String, List<BusinessObject>> sheetBO) throws
        IOException
    {
        setupExport(filePath);

        List<BusinessObject> entityBOList = new LinkedList<BusinessObject>();
        entityBOList.add(to);
        writeEntity(Constants.XOR.EXCEL_ENTITY_SHEET, entityBOList, null);

        int sheetNo = 1;
        Map<String, String> sheetMap = new HashMap<String, String>();
        for (Map.Entry<String, List<BusinessObject>> entry : sheetBO.entrySet()) {
            // Create a sheet
            String sheetName = Constants.XOR.EXCEL_SHEET_PREFIX + sheetNo++;
            sheetMap.put(entry.getKey(), sheetName);
            writeEntity(sheetName, entry.getValue(), null);
        }
        writeRelationshipMap(filePath, sheetMap);
    }

    private void writeEntity (String sheetName,
                              List<BusinessObject> boList,
                              BusinessObject owner)
    {
        if(boList == null || boList.size() == 0) {
            return;
        }

        EntityType entityType = null;
        setupEntity(sheetName);

        for (BusinessObject bo : boList) {
            if (bo.getContainmentProperty() != null && bo.getContainmentProperty().isMany()) {
                writeEntity(
                    sheetName,
                    bo.getList(),
                    (BusinessObject)bo.getContainer());
                continue;
            }
            if(entityType == null) {
                entityType = (EntityType)bo.getType();
            }

            List<String> propertyPaths = new ArrayList<String>();

            // Based on polymorphism, the actual instance can be a different subtype
            // so we need to get a fresh property list and calculate the column indexes
            // as new properties might be present and would need to be mapped to additional columns
            if (owner == null && bo.getContainer() != null) {
                owner = (BusinessObject)bo.getContainer();
            }
            if (owner != null) {
                propertyPaths.add(Constants.XOR.OWNER_ID);
            }
            propertyPaths.add(Constants.XOR.ID);
            propertyPaths.add(Constants.XOR.TYPE);

            // We want to have all required columns in the beginning
            List<String> requiredPropertyPaths = new ArrayList<String>();
            for (Property property : bo.getType().getProperties()) {
                if (property.isMany()) {
                    propertyPaths.add(ExcelJsonCreationStrategy.getCollectionTypeKey(property));

                    // Collections are handled separately
                    continue;
                }
                // Skip open content until we come with a default serialized form for empty object
                // Currently it fails validation since empty string does not equal JSONObject
                if (property.isOpenContent()) {
                    continue;
                }
                // Handle embedded objects and expand them if necessary
                propertyPaths.addAll(property.expand(new HashSet<Type>()));
            }

            Set<String> requiredColumns = setupPropertyColumns(propertyPaths, bo.getType());

            // TODO: add columns only if the value is not null
            prepareItem();
            for (String propertyPath : propertyPaths) {
                prepareEntityItemProperty(propertyPath, requiredColumns);
                Object value;
                if (Constants.XOR.OWNER_ID.equals(propertyPath)) {
                    value = owner.getOpenProperty(Constants.XOR.ID);
                }
                else if (Constants.XOR.ID.equals(propertyPath) || propertyPath.startsWith(
                    Constants.XOR.TYPE + Constants.XOR.SEP)) {
                    value = bo.getOpenProperty(propertyPath);
                }
                else if (Constants.XOR.TYPE.equals(propertyPath)) {
                    value = bo.getInstanceClassName();
                }
                else if (propertyPath.startsWith(Constants.XOR.OBJECTREF)) {
                    String path = propertyPath.substring(Constants.XOR.OBJECTREF.length());
                    value = bo.getExistingDataObject(Settings.convertToBOPath(path));
                    if (value != null && value instanceof BusinessObject) {
                        value = ((BusinessObject)value).getOpenProperty(Constants.XOR.ID);
                    }
                    else if (value != null) {
                        throw new RuntimeException(
                            "ObjectRef needs to refer to an Entity: " + value.toString());
                    }
                }
                else {
                    value = bo.getString(propertyPath);
                }
                if (value != null) {
                    writeEntityItemPropertyValue(value.toString());
                }
            }
            finishupItem();
        }

        writeEntityHeader(sheetName, entityType);
    }

    protected void writeRelationshipMap (String filePath, Map<String, String> sheetMap) throws
        IOException
    {
        setupRelationship();

        for (Map.Entry<String, String> entry : sheetMap.entrySet()) {
            prepareItem();
            writeRelationshipItem(entry.getValue(), entry.getKey());
        }

        finishupRelationship();
    }

    protected String getRelationshipHeaderCol1 ()
    {
        return "Sheet name";
    }

    protected String getRelationshipHeaderCol2 ()
    {
        return "Relationship";
    }

    protected String getEntityTypeCol3 ()
    {
        return "Entity Type";
    }

    protected String getCollectionKey (String ownerXorKey, String entityInfo)
    {
        return ownerXorKey + ":" + getProperty(entityInfo).getName();
    }

    protected void addCollectionEntry (Map<String, JSONArray> collectionPropertyMap,
                                     String key,
                                     JSONObject collectionEntryJSON)
    {
        JSONArray collection = null;
        if (collectionPropertyMap.containsKey(key)) {
            collection = collectionPropertyMap.get(key);
        }
        else {
            collection = new JSONArray();
            collectionPropertyMap.put(key, collection);
        }

        collection.put(collectionEntryJSON);
    }

    protected void link (Map<String, JSONObject> idMap,
                       Map<String, JSONArray> collectionPropertyMap)
    {
        // First link the collections to their owners
        for (Map.Entry<String, JSONArray> entry : collectionPropertyMap.entrySet()) {
            String collectionKey = entry.getKey();
            String[] tokens = collectionKey.split(":");
            String ownerId = tokens[0];
            String collectionProperty = tokens[1];

            JSONObject owner = idMap.get(ownerId);
            if (owner == null) {
                throw new RuntimeException(
                    "Unable to find collection owner with XOR.id " + ownerId);
            }
            owner.put(collectionProperty, entry.getValue());
        }

        // Link all the object references
        for (JSONObject entity : idMap.values()) {
            // Iterate through all the object references
            JSONArray fields = entity.names();
            for (int i = 0; i < fields.length(); i++) {
                String property = fields.getString(i);
                if (property.startsWith(Constants.XOR.OBJECTREF)) {
                    JSONObject toOne = idMap.get(entity.getString(property));
                    if (toOne == null) {
                        logger.info(
                            "Unable to find object reference: " + entity.getString(property));
                        continue;
                    }
                    String reference = property.substring(Constants.XOR.OBJECTREF.length());

                    // replace the object reference with actual reference
                    am.setEmbeddableValue(entity, reference, toOne, property);
                }
            }
        }
    }

    /**
     * Re-orders the property paths so that the required columns are at the beginning
     * @param propertyPaths that need to be re-ordered
     * @param type of the entity
     * @return the set of required columns
     */
    protected Set<String> setupPropertyColumns(List<String> propertyPaths, Type type) {
        EntityType entityType = (EntityType) type;

        List<String> requiredPropertyPaths = new ArrayList<>();
        List<String> nullablePropertyPaths = new ArrayList<>();
        // Move required columns to the beginning
        for(String pp: propertyPaths) {
            //Property p = entityType.getProperty(pp);
            //if (p != null && !p.isNullable()) {
            if(!entityType.isNullable(pp)) {
                requiredPropertyPaths.add(pp);
            } else {
                nullablePropertyPaths.add(pp);
            }
        }
        propertyPaths = new ArrayList<>(requiredPropertyPaths);
        propertyPaths.addAll(nullablePropertyPaths);

        propertyColIndex = new HashMap<String, Integer>();

        int colNo = 0;
        for (String propertyPath : propertyPaths) {
            if (!propertyColIndex.containsKey(propertyPath)) {
                propertyColIndex.put(propertyPath, colNo++);
            }
        }

        return new HashSet<String>(requiredPropertyPaths);
    }

    protected abstract void processEntitySheet (String path, String sheetName, Map<String, JSONObject> idMap) throws
        IOException;

    protected abstract void populateMaps(String path, Map<String, String> entitySheets,
                                Map<String, String> collectionSheets) throws IOException;

    protected abstract Map<String, Integer> getHeader(String path, String name) throws IOException;

    protected abstract void addRelationships(String path, List attrPath) throws IOException;

    protected abstract void setupEntity (String name);

    protected abstract void setupRelationship ();

    protected abstract void finishupRelationship ();

    protected abstract void prepareItem ();

    protected abstract void finishupItem ();

    protected abstract void writeRelationshipItem (String name, String entityInfo);

    protected abstract void prepareEntityItemProperty (String propertyPath, Set<String> requiredColumns);

    protected abstract void writeEntityItemPropertyValue (String value);

    protected abstract void writeEntityHeader (String sheetName, EntityType entityType);

    protected abstract void setupExport (String filePath) throws
        IOException;
}
