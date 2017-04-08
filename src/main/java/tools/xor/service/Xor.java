package tools.xor.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;

import tools.xor.Settings;

public interface Xor {
	
	/**
	 * @param   inputObject    The input object from the user in external form
	 * @param   settings       User specified settings
	 * @return The persistence managed object
	 */
	public Object create(Object inputObject, Settings settings);

	/**
	 * This method returns the object in external form such as JSONObject
	 * If the object is desired as a persistence managed object, this can be specified in the settings (TODO)
	 * 
	 * @param inputObject   This can either be the primary key or the managed object
	 * @param settings      User specified settings
	 * @return External model object
	 */
	public Object read(Object inputObject, Settings settings);

	/**
	 * @param   inputObject    The input object from the user in external form
	 * @param   settings       User specified settings
	 * @return The persistence managed object
	 */
	public Object update(Object inputObject, Settings settings);
	
	/**
	 * @param   inputObject  The input object from the user in external form
	 * @param   entityClass  The entity class of the object to be created
	 *                       This is different from the class of the input object as that can be JSONObject
	 * @return The persistence managed object
	 */	
	public Object update(Object inputObject, Class<?> entityClass);

	/**
	 * @param inputObject  The persistence managed object that needs to be deleted
	 * @param settings     User specified settings
	 */
	public void delete(Object inputObject, Settings settings);
	
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
	public List patch(List inputObject, List inputSnapshot, Settings settings);
	
	/**
	 * Get a list of the objects in external form
	 * 
	 * @param inputObject  The input object from the user in external form
	 *                     If the input object has an id then only that object is returned
	 * @param settings User specified settings
	 * @return The result of the query
	 */
	public List<?> query(Object inputObject, Settings settings);

	/**
	 * @param   entity       The input object from the user in managed form
	 * @param   settings     User specified settings
	 * @return A persistence managed object that is a copy of the input entity
	 * @see Xor#patch(List, List,  Settings)
	 */	
	public Object clone(Object entity, Settings settings);	
	
	/**
	 * Exports the data in excel format
	 * @param outputStream OutputStream of the Excel file
	 * @param settings from the user
	 */
	public void exportDenormalized(OutputStream outputStream, Settings settings);

	/**
	 * Imports the denormalized excel
	 * @param is InputStream of the Excel file
	 * @param settings from the user
	 * @throws IOException if an error was encoutered while operating the inputstream
	 */
	public void importDenormalized (InputStream is, Settings settings) throws
		IOException;

	/**
	 * This returns an Excel workbook object that can be used to generate an Excel file
	 * 
	 * @param filePath the file/folder location.
	 * @param inputObject object to export
	 * @param settings from the user
	 * @throws IOException if an error was encoutered while operating the OutputStream
	 */
	public void exportAggregate(String filePath, Object inputObject, Settings settings) throws IOException ;
	
	/**
	 * Import the aggregate expressed in an Excel file
	 * 
	 * @param filePath path to the file/folder
	 * @param settings from the user
	 * @return the id of the created object
	 * @throws IOException  if an error was encoutered while operating the inputstream
	 */
	public Object importAggregate(String filePath, Settings settings) throws IOException;
}
