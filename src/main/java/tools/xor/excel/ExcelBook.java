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
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Represents an Excel sheet or a JSON sheet used in REST call
 * 
 * @author Dilip Dalton
 *
 */
public class ExcelBook implements IBook {
	
	private SXSSFWorkbook wb;
	
	public ExcelBook(InputStream is) {
		try {
			wb = (SXSSFWorkbook) WorkbookFactory.create(is);
		} catch (EncryptedDocumentException | IOException e) {
			throw new RuntimeException("The provided inputstream is not valid. " + e.getMessage());
		}		
	}
	
    /**
     * Write out this workbook to an Outputstream.
     *
     * @param stream - the java OutputStream you wish to write to
     * @exception IOException if anything can't be written.
     */
    public void write(OutputStream stream) throws IOException {
    	wb.write(stream);
    }
	
    /**
     * Close the underlying input resource (File or Stream),
     *  from which the Workbook was read. After closing, the
     *  Workbook should no longer be used.
     * <p>This will have no effect newly created Workbooks.
     */
    public void close() throws IOException {
    	wb.close();
    }
	
    /**
     * Get sheet with the given name
     *
     * @param name of the sheet
     * @return Sheet with the name provided or <code>null</code> if it does not exist
     */
    public ISheet getSheet(String name) {
    	Sheet sheet = wb.getSheet(name);
    	return new ExcelSheet(sheet);
    }
	
    /**
     * Set whether temp files should be compressed.
     * @param compress set to true for compression
     */
    public void setCompressTempFiles(boolean compress) {
    	wb.setCompressTempFiles(compress);
    }
    
    /**
     * Create a new sheet for this Workbook and return the high level representation.
     * Use this to create new sheets.
     * 
     * @param sheetname name of the excel sheet
     * @return Sheet object
     */
    public ISheet createSheet(String sheetname) {
    	Sheet sheet = wb.createSheet(sheetname);
    	return new ExcelSheet(sheet);
    }
}
