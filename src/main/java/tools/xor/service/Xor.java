package tools.xor.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import tools.xor.Settings;
import tools.xor.TypeMapper;

public interface Xor {
	
	/**
	 * @param   inputObject    The input object from the user in external form
	 * @param   settings       User specified settings
	 * @return The persistence managed object
	 */
	Object create(Object inputObject, Settings settings);

	/**
	 * Performs object conversion by taking an object based on the External model
	 * and converting it to an object based on the Domain model.
	 * @param entity object based on the external model
	 * @param settings The user provided settings
	 * @return Domain model object
	 */
	Object toDomain(Object entity, Settings settings);

	/**
	 * Performs object conversion by taking an object based on the Domain model
	 * and converting it to an object based on the External model.
	 * @param entity object based on the domain model
	 * @param settings The user provided settings
	 * @return External model object
	 */
	Object toExternal (Object entity, Settings settings);

	/**
	 * This method returns the object in external form such as JSONObject
	 * If the object is desired as a persistence managed object, this can be specified in the settings (TODO)
	 * 
	 * @param inputObject   This can either be the primary key or the managed object
	 * @param settings      User specified settings
	 * @return External model object
	 */
	Object read(Object inputObject, Settings settings);

	/**
	 * @param   inputObject    The input object from the user in external form
	 * @param   settings       User specified settings
	 * @return The persistence managed object
	 */
	Object update(Object inputObject, Settings settings);

	/**
	 * Migrates entities from the source database to the (current) target database.
	 * The entities that need to be migrated are specified in the xor.properties file
	 * under the key entities_to_migrate as a comma separated list of fully qualified
	 * entity names.
	 *
	 * @param source database
	 * @param settings containing user provided data
	 */
	void migrate(AggregateManager source, Settings settings);

	/**
	 * @param inputObject  The persistence managed object that needs to be deleted
	 * @param settings     User specified settings
	 */
	void delete(Object inputObject, Settings settings);
	
	/**
	 * Optimized form of update where the user can specify the how much of an object to update
	 * Similar to Update but we do not walk a managed object and patch it, rather
	 * we can query the data optimally and make it managed and update it.
	 *
	 * This approach has a greater chance of getting Stale exception since the latest
	 * version information is not used, rather the user provided version information is used.
	 * Alternatively it can take a snapshot object as input instead of version information.
	 * The snapshot is the last known valid state of the object by the caller.
	 *
	 * Do not use this method if the values being updated are influenced/influences business
	 * logic decisions, update method is the preferred approach in this case. The patch method
	 * is provided purely from a performance optimization perspective. It is best to run
	 * the patch operation in its own transaction.
	 *
	 * patch only supports first level (x = y) updates, 2nd (x.y = z) and higher level updates
	 * are not supported.
	 * Very efficient to use this in bulk update mode, as the updates will be batched by the
	 * underlying persistence mechanism.
	 * 
	 * @param   inputObject    List of input objects from the user in external form
	 * @param   inputSnapshot  List of input snapshot objects.
	 *                         Snapshot refers to object state fetched by the user before modification.
	 *                         We have to set the snapshot if the object is not in cache because
	 *                         the persistence manager needs a baseline/snapshot for dirty checking
	 *                         and this will do it, if the version property is not being used for
	 *                         this entity.
	 *                         This parameter is optional.
	 * @param   settings       User specified settings
	 * @return list of persistence managed objects
	 */	
	List patch(List inputObject, List inputSnapshot, Settings settings);
	
	/**
	 * Get a list of the objects in external form
	 * For JDBC, it is more efficient to use multiple queries then a read, as the
	 * read is based on the objects queried earlier.
	 * 
	 * @param inputObject  The input object from the user in external form
	 *                     If the input object has an id then only that object is returned
	 * @param settings User specified settings
	 * @return The result of the query
	 */
	List<?> query(Object inputObject, Settings settings);

	/**
	 * Executes DML (INSERT, UPDATE, SELECT and DELETE) queries against the DB
	 * @param settings object
	 * @return list for query and int for the rest
	 */
	Object dml(Settings settings);

	/**
	 * @param   entity       The input object from the user in managed form
	 * @param   settings     User specified settings
	 * @return A persistence managed object that is a copy of the input entity
	 * @see Xor#patch(List, List,  Settings)
	 */	
	Object clone(Object entity, Settings settings);	
	
	/**
	 * Exports the data in excel format
	 * @param outputStream OutputStream of the Excel file
	 * @param settings from the user
	 */
	void exportDenormalized(OutputStream outputStream, Settings settings);

	/**
	 * Imports the denormalized excel
	 * @param is InputStream of the Excel file
	 * @param settings from the user
	 * @throws IOException if an error was encoutered while operating the inputstream
	 */
	void importDenormalized (InputStream is, Settings settings) throws
		IOException;

	/**
	 * This returns an Excel workbook object that can be used to generate an Excel file
	 * 
	 * @param filePath the file/folder location.
	 * @param inputObject object to export
	 * @param settings from the user
	 * @throws IOException if an error was encoutered while operating the OutputStream
	 */
	void exportAggregate(String filePath, Object inputObject, Settings settings) throws IOException ;
	
	/**
	 * Import the aggregate expressed in an Excel file
	 * 
	 * @param filePath path to the file/folder
	 * @param settings from the user
	 * @return the id of the created object
	 * @throws IOException  if an error was encoutered while operating the inputstream
	 */
	Object importAggregate(String filePath, Settings settings) throws IOException;

	/**
	 * Generate data based on the shape. The import will occur on 
	 * separate threads to handle parallel load.
	 * 
	 * @param name of the shape
	 * @param types in topological order
	 * @param settings user settings
	 */
	void generate(String name, List<String> types, Settings settings);
	
    /**
     * Generate data based on the shape using the same thread and hence
     * the same JDBC connection.
     * This allows transaction rollback to affect the generated data.
     * 
     * @param name of the shape
     * @param types in topological order
     * @param settings user settings
     */
    void generateSameTX(String name, List<String> types, Settings settings);
    
    /**
     * A Transaction object is created to interact using the JDBC API.
     * If no JDBC connection is provided, then this method helps to
     * obtain one using the configured JDBC data source.
     *  
     * @param settings user settings
     * @return Transaction object
     */
    Transaction createTransaction(Settings settings);    
	
	/**
	 * Any configuration needed for the DataStore is done at this step.
	 * The Xor instance is then initialized with an instance of the DataStore for the current thread.
	 * 
	 * @param settings user provided settings
	 */
	void configure (Settings settings);	
	
	/**
	 * Return the DataStore instance corresponding to the DataModel.
	 * A prescribed DataModel may not have a DataStore instance.
	 *  
	 * @return DataStore instance
	 */
	DataStore getDataStore ();	
	
	/**
	 * Return the DataModel instance exposed by this interface
	 * @return DataModel instance.
	 */
	DataModel getDataModel ();
	
	/** 
	 * Set the factory object responsible for creating the DataModel instance.
	 * 
	 * @param factory DataModel creator
	 */
	void setDataModelFactory (DataModelFactory factory);	
	
	/**
	 * Initialize the XOR framework the the TypeMapper instance.
	 * The TypeMapper is the glue between 2 XOR instances.
	 * 
	 * @param typeMapper instance
	 */
	void setTypeMapper (TypeMapper typeMapper);
}
