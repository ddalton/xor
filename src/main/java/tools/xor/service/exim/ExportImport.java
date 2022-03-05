package tools.xor.service.exim;

import java.io.IOException;

import tools.xor.Settings;

public interface ExportImport {

	/**
	 * Exports an aggregate. Currently designed to export a single aggregate.
	 *
	 * @param filePath the file or the folder containing the result of the export
	 * @param inputObject the aggregate to be exported
	 * @param settings object
	 * @throws IOException when the given file/folder cannot be written to
	 */
	public void exportAggregate(String filePath, Object inputObject, Settings settings) throws
		IOException;

	/**
	 * Imports one or more aggregates. An aggregate is an entity including its relationship objects.
	 *
	 * @param filePath the file or the folder containing the aggregate to be imported
	 * @param settings object
	 * @return the result of the import
	 * @throws IOException when the given file/folder cannot be read from
	 */
	public Object importAggregate (String filePath, Settings settings) throws IOException;
}
