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
	 * @return object
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
	 * we can query the data optimally and make it managed and update it
	 * 
	 * @param   inputObject    The input object from the user in external form
	 * @param   settings       User specified settings
	 * @return The persistence managed object
	 */	
	public Object patch(Object inputObject, Settings settings);
	
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
	 * @see Xor#patch(Object, Settings)
	 */	
	public Object clone(Object entity, Settings settings);	
	
	/**
	 * Exports the data in excel format
	 * @param outputStream OutputStream of the Excel file
	 * @param settings from the user
	 */
	public void exportDenormalized(OutputStream outputStream, Settings settings);

	/**
	 * Imports a CSV file containing entities. The id should be null.
	 * This method automatically sets up a view based on the fields in the CSV header.
	 *
	 * @param csvData
	 * @param settings
	 * @throws Exception
	 */
	public void importBulk (Reader csvData, Settings settings) throws Exception;

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
	 * @param os OutputStream of the Excel file
	 * @param inputObject object to export
	 * @param settings from the user
	 * @throws IOException if an error was encoutered while operating the OutputStream
	 */
	public void exportAggregate(OutputStream os, Object inputObject, Settings settings) throws IOException ;
	
	/**
	 * Import the aggregate expressed in an Excel file
	 * 
	 * @param is InputStream of the Excel file
	 * @param settings from the user
	 * @return the id of the created object
	 * @throws IOException  if an error was encoutered while operating the inputstream
	 */
	public Object importAggregate(InputStream is, Settings settings) throws IOException;
}
