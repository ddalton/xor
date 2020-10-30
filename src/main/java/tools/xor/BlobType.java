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

package tools.xor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import tools.xor.util.ClassUtil;
import tools.xor.util.graph.StateGraph;

public class BlobType extends SimpleType {
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    String generatedPicture;

    public BlobType(Class<?> clazz) {
        super(clazz);
    }

    @Override
    public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
                           StateGraph.ObjectGenerationVisitor visitor) {

        if(generatedPicture == null) {
            generatedPicture = Base64.getEncoder().encodeToString(generateImage(640, 640));
        }

        return generatedPicture;
    }

    private byte[] generateImage(int width, int height) {

        //create buffered image object img
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        //file object
        File f = null;
        //create random image pixel by pixel
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                int p = 0;
                if( (x/16 % 2 == 0) ^ (y/16 % 2 == 0)) {
                    p = 0x1a1a1a;
                } else {
                    p = 0xffffff;
                }

                img.setRGB(x, y, p);
            }
        }
        //write image
        try{
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();

                ByteArrayOutputStream baos = new ByteArrayOutputStream(37628);
                ImageOutputStream ios =  ImageIO.createImageOutputStream(baos);
                writer.setOutput(ios);
                try {
                    writer.write(img);
                } finally {
                    writer.dispose();
                    ios.flush();
                }

                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw ClassUtil.wrapRun(e);
        }

        return null;
    }
    
    @Override
    public String getJsonType() {
        return MutableJsonType.JSONSCHEMA_STRING_TYPE;
    }    
}
