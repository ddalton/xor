package tools.xor.service.exim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
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

public abstract class AbstractExportImport implements ExportImport
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    public static final String PROPERTY_TYPE_DELIM = ":";

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
    
    protected static boolean isEmbeddedPath (String propertyPath)
    {
        if (propertyPath.indexOf(Settings.PATH_DELIMITER) != -1 &&
            !propertyPath.startsWith(Constants.XOR.XOR_PATH_PREFIX)) {
            return true;
        }

        return false;
    }    
    
    protected static void setEmbeddableValue (JSONObject base, String path, String value)
    {
        setEmbeddableValue(base, path, value, null);
    }

    public static void setEmbeddableValue (JSONObject base,
                                            String path,
                                            Object value,
                                            String replacedProperty)
    {
        JSONObject embeddable = base;

        // Loop through each part of the path
        while (path.indexOf(Settings.PATH_DELIMITER) != -1) {
            String rootpart = path.substring(0, path.indexOf(Settings.PATH_DELIMITER));
            if (base.has(rootpart)) {
                embeddable = base.getJSONObject(rootpart);
            }
            else {
                embeddable = new JSONObject();
                base.put(rootpart, embeddable);
            }

            path = path.substring(rootpart.length() + 1);
        }
        embeddable.put(path, value);
        if (replacedProperty != null) {
            embeddable.remove(replacedProperty);
        }
    } 
    
    public static JSONObject getJSON (Map<String, Integer> colMap, Row row)
    {
        JSONObject entity = new JSONObject();

        for (Map.Entry<String, Integer> entry : colMap.entrySet()) {
            Cell cell = row.getCell(entry.getValue(), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (isEmbeddedPath(entry.getKey())) {
                if(cell != null) {
                    setEmbeddableValue(entity, entry.getKey(), cell.getStringCellValue());
                }
            }
            else {
                // set direct value
                if (cell != null) {
                    try {
                        entity.put(entry.getKey(), cell.getStringCellValue());
                    }
                    catch (Exception e) {
                        // Numeric entry
                        entity.put(entry.getKey(), cell.getNumericCellValue());
                    }
                }
                else {
                    // Skip processing null values
                }
            }
        }

        return entity;
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
            if(!propertyName.startsWith(Constants.XOR.XOR_PATH_PREFIX) && !propertyName.startsWith(Constants.XOR.IDREF)) {
                attrPath.add(prefix + entry.getKey());
            }
        }
    }

    protected Type getType (String entityInfo)
    {
        // Parse the entity classname from this
        String[] tokens = entityInfo.split(PROPERTY_TYPE_DELIM);
        if (tokens.length != 2) {
            throw new RuntimeException(
                "The entity info column in sheet map is not in <classname>" + PROPERTY_TYPE_DELIM +
                    "<property> format: "
                    + entityInfo);
        }
        return am.getDataModel().getShape().getType(tokens[0]);
    }

    protected Property getProperty (String entityInfo)
    {
        Type type = getType(entityInfo);

        String[] tokens = entityInfo.split(PROPERTY_TYPE_DELIM);
        return type.getProperty(tokens[1]);
    }

    protected void setView(Settings settings, String path) throws IOException
    {
        // set the view only if the user has not provided a view
        if(settings.getView() != null) {
            return;
        }

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
                String key = Constants.XOR.getRelationshipName(
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
        writeRelationshipMap(sheetMap);
        
        finishExport(filePath);
    }

    /**
     * This class is used to capture the header fields including the required fields
     * that make up the Entity.
     * This is especially useful to capture all the fields for a type hierarchy.
     * Conceptually similar to JPA's SINGLE_TABLE per class hieararchy
     */
    static class EntityStructure {

        List<String> properties;
        Set<String> required;
        Map<String, EntityType> subTypeMap;

        public Map<String, EntityType> getSubTypeMap() {
            return subTypeMap;
        }

        public void setSubTypeMap(Map<String, EntityType> subTypeMap) {
            this.subTypeMap = subTypeMap;
        }

        public Set<String> getRequired ()
        {
            return required;
        }

        public void setRequired (Set<String> required)
        {
            this.required = required;
        }

        public List<String> getProperties ()
        {
            return properties;
        }

        public void setProperties (List<String> properties)
        {
            this.properties = properties;
        }

        private void extractSubTypes(List<BusinessObject> boList, Map<String, EntityType> subTypeMap) {

            for (BusinessObject bo : boList) {
                if (bo.getContainmentProperty() != null && bo.getContainmentProperty().isMany()) {
                    extractSubTypes(bo.getList(), subTypeMap);
                } else {
                    EntityType type = (EntityType) bo.getType();
                    if(!subTypeMap.containsKey(type.getName())) {
                        subTypeMap.put(type.getName(), type);
                    }
                }
            }
        }

        private void process(Map<String, EntityType> subTypeMap) {

            this.properties = new ArrayList<>();
            Map<Type, Set<String>> propertyMap = new HashMap<>();
            for(Type type: subTypeMap.values()) {
                if(type instanceof EntityType) {

                    Set<String> typeProperties = new HashSet<>();
                    for (Property property : type.getProperties()) {
                        if (property.isMany()) {
                            typeProperties.add(ExcelJsonCreationStrategy.getCollectionTypeKey(property));

                            // Collections are handled separately
                            continue;
                        }
                        // Skip open content until we come with a default serialized form for empty object
                        // Currently it fails validation since empty string does not equal JSONObject
                        if (property.isOpenContent()) {
                            continue;
                        }
                        // Handle embedded objects and expand them if necessary
                        typeProperties.addAll(property.expand(new HashSet<Type>()));
                    }

                    propertyMap.put(type, typeProperties);
                }
            }

            Set<String> nullableProperties = new HashSet<>();
            this.required = new HashSet<>();
            for (Map.Entry<Type, Set<String>> entry : propertyMap.entrySet()) {
                for(String propertyName: entry.getValue()) {
                    if (!((EntityType)entry.getKey()).isNullable(propertyName)) {
                        this.required.add(propertyName);
                    }
                    else {
                        nullableProperties.add(propertyName);
                    }
                }
            }
            properties.addAll(this.required);
            properties.addAll(nullableProperties);

            // Meta fields
            properties.add(Constants.XOR.OWNER_ID);
            properties.add(Constants.XOR.ID);
            properties.add(Constants.XOR.TYPE);
        }

        public static EntityStructure construct(List<BusinessObject> boList) {
            EntityStructure es = new EntityStructure();

            Map<String, EntityType> subTypeMap = new HashMap<String, EntityType>(); 
            es.setSubTypeMap(subTypeMap);
            es.extractSubTypes(boList, subTypeMap);
            es.process(subTypeMap);

            return es;
        }
    }

    private void writeEntity (String sheetName,
                              List<BusinessObject> boList,
                              BusinessObject owner)
    {
        if(boList == null || boList.size() == 0) {
            return;
        }

        EntityStructure entityStructure = EntityStructure.construct(boList);

        // Get all the header columns.
        // Since subtypes might have additional columns we check the type of each BusinessObject instance
        EntityType entityType = null;
        if (setupEntity(sheetName) ) {
            setupPropertyColumns(entityStructure);
            writeEntityHeader(sheetName, entityStructure);            
        }

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

            /*
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
            */

            // NOTE: nice to have to add columns only if the value is not null
            prepareItem();
            for (String propertyPath : entityStructure.getProperties()) {
                prepareEntityItemProperty(propertyPath, entityStructure.getRequired());
                Object value;
                if (Constants.XOR.OWNER_ID.equals(propertyPath) && owner != null) {
                    value = owner.getOpenProperty(Constants.XOR.ID);
                }
                else if (Constants.XOR.ID.equals(propertyPath) || propertyPath.startsWith(
                    Constants.XOR.TYPE + Constants.XOR.SEP)) {
                    value = bo.getOpenProperty(propertyPath);
                }
                else if (Constants.XOR.TYPE.equals(propertyPath)) {
                    value = bo.getInstanceClassName();
                }
                else if (propertyPath.startsWith(Constants.XOR.IDREF)) {
                    String path = propertyPath.substring(Constants.XOR.IDREF.length());
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
        }
        finishEntity();        

        if(entityType == null) {
            logger.warn("EntityType is missing - Check if all data has been loaded/read from DB.");
        }
    }

    protected void writeRelationshipMap (Map<String, String> sheetMap) throws
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

    /**
     * Swizzle duplicate object references to a single object
     * @param idMap entity map keyed by id
     * @param collectionPropertyMap collections
     */
    protected void swizzleCollectionElement (Map<String, JSONObject> idMap,
                         Map<String, JSONArray> collectionPropertyMap) {

        // Create a new collection pointing to the right entity JSONObject instance
        for (Map.Entry<String, JSONArray> entry : collectionPropertyMap.entrySet()) {
            JSONArray swizzled = new JSONArray();
            JSONArray original = entry.getValue();
            for(int i = 0; i < original.length(); i++) {
                JSONObject json = original.getJSONObject(i);
                if(json.has(Constants.XOR.ID)) {
                    JSONObject entity = idMap.get(getId(json));
                    if(entity == null) {
                        throw new RuntimeException("Cannot find collection entity with id: " + json.get(Constants.XOR.ID));
                    }
                    swizzled.put(entity);
                } else {
                    swizzled.put(json);
                }
            }

            // replace the collection
            collectionPropertyMap.put(entry.getKey(), swizzled);
        }
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
                if (property.startsWith(Constants.XOR.IDREF)) {
                    JSONObject toOne = idMap.get(entity.getString(property));
                    if (toOne == null) {
                        logger.info(
                            "Unable to find object reference: " + entity.getString(property));
                        continue;
                    }
                    String reference = property.substring(Constants.XOR.IDREF.length());

                    // replace the object reference with actual reference
                    setEmbeddableValue(entity, reference, toOne, property);
                }
            }
        }
    }

    /**
     * Re-orders the property paths so that the required columns are at the beginning
     * @param entityStructure structure that contains the properties for the entity
     */
    protected void setupPropertyColumns(EntityStructure entityStructure) {

        propertyColIndex = new HashMap<String, Integer>();

        int colNo = 0;
        for (String propertyPath : entityStructure.getProperties()) {
            if (!propertyColIndex.containsKey(propertyPath)) {
                propertyColIndex.put(propertyPath, colNo++);
            }
        }
    }

    protected String getId (JSONObject json)
    {
        String result = null;

        try {
            result = json.getString(Constants.XOR.ID);
        }
        catch (Exception e) {
            result = new Long(json.getLong(Constants.XOR.ID)).toString();
        }

        return result;
    }

    protected abstract void processEntitySheet (String path, String sheetName, Map<String, JSONObject> idMap) throws
        IOException;

    protected abstract void populateMaps(String path, Map<String, String> entitySheets,
                                Map<String, String> collectionSheets) throws IOException;

    protected abstract Map<String, Integer> getHeader(String path, String name) throws IOException;

    protected abstract void addRelationships(String path, List attrPath) throws IOException;

    /**
     * Sets up the Entity sheet/file.
     * @param name of the Entity sheet/file to setup
     * @return false if setup for the given name has already been performed, true otherwise
     */
    protected abstract boolean setupEntity (String name);
    
    protected abstract void finishEntity ();    

    protected abstract void setupRelationship ();

    protected abstract void finishupRelationship ();

    protected abstract void prepareItem ();

    protected abstract void finishItem ();

    protected abstract void writeRelationshipItem (String name, String entityInfo);

    protected abstract void prepareEntityItemProperty (String propertyPath, Set<String> requiredColumns);

    protected abstract void writeEntityItemPropertyValue (String value);

    protected abstract void writeEntityHeader (String sheetName, EntityStructure entityStructure);

    protected abstract void setupExport (String filePath) throws
        IOException;
    
    protected abstract void finishExport (String filePath) throws
    IOException;    
}
