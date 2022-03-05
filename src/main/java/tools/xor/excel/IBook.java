/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations 
 * under the License.
 */

package tools.xor.excel;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Represents an Excel sheet or a JSON sheet used in REST call
 * 
 * @author Dilip Dalton
 *
 */
public interface IBook {
	
    /**
     * Write out this workbook to an Outputstream.
     *
     * @param stream - the java OutputStream you wish to write to
     * @exception IOException if anything can't be written.
     */
    public void write(OutputStream stream) throws IOException;
	
    /**
     * Close the underlying input resource (File or Stream),
     *  from which the Workbook was read. After closing, the
     *  Workbook should no longer be used.
     * <p>This will have no effect newly created Workbooks.
     * @throws IOException when closing file
     */
    public void close() throws IOException;
	
    /**
     * Get sheet with the given name
     *
     * @param name of the sheet
     * @return Sheet with the name provided or <code>null</code> if it does not exist
     */
    public ISheet getSheet(String name);
	
    /**
     * Set whether temp files should be compressed.
     * @param compress true to compress
     */
    public void setCompressTempFiles(boolean compress);
    
    /**
     * Create a new sheet for this Workbook and return the high level representation.
     * Use this to create new sheets.
     * 
     * IllegalArgumentException if the name is null or invalid
     *  or workbook already contains a sheet with this name
     * 
     * @param sheetname sheet name
     * @return Sheet object
     */
    public ISheet createSheet(String sheetname);
}
