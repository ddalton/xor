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

package tools.xor.service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 
 * File system structure
 * ===================== 
 * class1Name/datafile.bin
 *           /index/property1Name.bin
 *           /index/property2Name.bin
 * class2Name/datafile.bin
 *           /index/property1Name.bin
 * 
 * Data file structure:
 * Global Meta data
 * ================
 * First free block
 * 
 * Extent meta data
 * ================
 * Data Size       (Size of the data content excluding the header data size)
 * block size      (Size of this block, including the block meta data)
 * next block      (If the data size is greater than the extent size)
 * Next free block (populated if this is a free block)
 * 
 * XOR data block size (starting block size (512 bytes - 2KB), double every link )
 *   - An aggregate is stored in a single block. 
 *   The format is compressed JSON and the jackson tool along with JsonZip is used for this purpose.
 *
 * XOR index block size = 2KB - Use BTREE data structure
 * 
 * @author Dilip Dalton
 *
 */
public class FileSystemManager {
	
	// getCurrentSession: return a FileSystemSession object for the current thread that will 
	// do the following operations

	// The manager has a list of data files
	
	private long getFreeBlockPosition() {
		// read the global meta data in the file
		return 0;
	}
	
	public void saveOrUpdate(String id, byte[] data) {
		
	}
	
	public void delete(String id) {
		
	}
	
	public byte[] findById(String id) {
		return null;
	}
	
	public void flush() {
		// We do not queue modification actions, so there is no need to flush.
		// At the most we can flush the data file buffer if one is present  
	}

	public static void main(String[] args) {
		String s = "I was here!\n";
		byte data[] = s.getBytes();
		ByteBuffer out = ByteBuffer.wrap(data);

		ByteBuffer copy = ByteBuffer.allocate(12);

		try {
			//Path path = FileSystems.getDefault().getPath("data", "data.txt");
			//FileChannel fc = (FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE));
			RandomAccessFile raf = new RandomAccessFile("data/data.txt", "rw");
			FileChannel fc = raf.getChannel();
			
			// Read the first 12
			// bytes of the file.
			int nread;
			do {
				nread = fc.read(copy);
			} while (nread != -1 && copy.hasRemaining());

			// Write "I was here!" at the beginning of the file.
			fc.position(0);
			while (out.hasRemaining())
				fc.write(out);
			out.rewind();

			// Move to the end of the file.  Copy the first 12 bytes to
			// the end of the file.  Then write "I was here!" again.
			long length = fc.size();
			fc.position(length-1 + 300);
			copy.flip();
			while (copy.hasRemaining())
				fc.write(copy);
			while (out.hasRemaining())
				fc.write(out);
		} catch (IOException x) {
			System.out.println("I/O Exception: " + x);
		}
	}
}