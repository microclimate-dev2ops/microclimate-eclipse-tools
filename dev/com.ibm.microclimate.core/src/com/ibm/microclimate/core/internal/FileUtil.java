/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {
	
    public static boolean makeDir(String path) {
        boolean result = true;

        if (path != null) {
            try {
                File fp = new File(path);
                if (!fp.exists() || !fp.isDirectory()) {
                    // Create the directory.
                    result = fp.mkdirs();
                }
            } catch (Exception e) {
                MCLogger.logError("Failed to create directory: " + path, e);
                result = false;
            }
        }
        return result;
    }
    
    public static void copyFile(InputStream inStream, String path) throws IOException, FileNotFoundException {
    	FileOutputStream outStream = null;
    	try {
    		outStream = new FileOutputStream(path);
    		byte[] bytes = new byte[1024];
    		int bytesRead = 0;
    		while ((bytesRead = inStream.read(bytes)) > 0) {
    			outStream.write(bytes, 0, bytesRead);
    		}
    	} finally {
    		if (outStream != null) {
    			try {
					outStream.close();
				} catch (IOException e) {
					// Ignore
				}
    		}
    	}
    }

}
