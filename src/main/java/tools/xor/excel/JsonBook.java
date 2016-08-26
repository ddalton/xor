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
import java.io.OutputStreamWriter;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Represents an Excel sheet or a JSON sheet used in REST call
 * 
 * @author Dilip Dalton
 *
 */
public class JsonBook implements IBook {
	
	private JSONObject jsonBook;
	
	public JsonBook(InputStream is) {
		jsonBook = new JSONObject();		
	}
	
    /**
     * Write out this workbook to an Outputstream.
     *
     * @param stream - the java OutputStream you wish to write to
     * @exception IOException if anything can't be written.
     */
    public void write(OutputStream stream) throws IOException {
    	jsonBook.write(new OutputStreamWriter(stream));
    }
	
    @Override
    public void close() throws IOException {
        /*
         * Not relevant for JsonBook
         */
    }
	
    /**
     * Get sheet with the given name
     *
     * @param name of the sheet
     * @return Sheet with the name provided or <code>null</code> if it does not exist
     */
    public ISheet getSheet(String name) {
    	if(jsonBook.has(name)) {
    		return (ISheet) jsonBook.get(name);
    	}
    	
    	return null;
    }
	
    @Override
    public void setCompressTempFiles(boolean compress) {
        /*
         * Not relevant for JsonBook.
         * 
         * @param compress
         */    	
    }
    
    @Override
    public ISheet createSheet(String sheetname) {
    	if(jsonBook.has(sheetname)) {
    		throw new IllegalArgumentException( "The json already contains a sheet of this name: " + sheetname);
    	}
    	JsonSheet sheet = new JsonSheet(new JSONArray());
    	jsonBook.put(sheetname, sheet);

    	return sheet;
    }
}
