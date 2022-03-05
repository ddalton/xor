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

package tools.xor.view;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.AggregateManager;
import tools.xor.util.ClassUtil;
import tools.xor.util.DFAtoRE;
import tools.xor.util.InterQuery;
import tools.xor.view.AggregateTree.QueryKey;

public class AggregateViewFactory {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	private static String AGGREGATE_VIEW_FILE = "AggregateViews.xml";
	private static String REGEN_SUFFIX = "REGEN";

	public void load(AggregateManager am) {
		load(AGGREGATE_VIEW_FILE, am);
	}
	
	public static AggregateViews load(String fileName) {
		AggregateViews views = new AggregateViews();

		try {
			InputStream stream = AggregateViewFactory.class.getClassLoader().getResourceAsStream(fileName);
			if(stream == null) {
				logger.warn("Unable to find the view configuration file: " + fileName);
				return views;
			}
			
			JAXBContext jaxbContext = JAXBContext.newInstance(AggregateViews.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			views = (AggregateViews) jaxbUnmarshaller.unmarshal(stream);
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to read " + fileName, e);
		} 
		
		return views;
	}
	
	public void load(String fileName, AggregateManager am) {
		AggregateViews views = load(fileName);
		views.sync(am);
	}
	
	public void save(String fileName, AggregateViews views) throws JAXBException {	
		JAXBContext jaxbContext = JAXBContext.newInstance( AggregateViews.class );
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		
		jaxbMarshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
		System.out.println("Filename: " + fileName);
		jaxbMarshaller.marshal( views, new File( fileName ) );
	}

	public List<String> extractRecursiveAttributes(AggregateView av) {
		List<String> result = new LinkedList<String>();
		
		List<String> recursiveDeficient = new LinkedList<String>();
		for(String attr: av.attributeList) {
			if(attr.contains(DFAtoRE.RECURSE_SYMBOL)) {
				result.add(attr);
			} else {
				recursiveDeficient.add(attr);
			}
		}
		// Sort the attributes
		Collections.sort(recursiveDeficient);
		Collections.sort(result);
		
		av.setAttributeList(recursiveDeficient);
		
		return result;
	}	
	
	private void groupByPackage(Type type, View av, Map<String, AggregateViews> viewsByPackage) {
		String packageName = type.getInstanceClass().getPackage().getName();
		
		AggregateViews views = viewsByPackage.get(packageName);
		if(views == null) {
			views = new AggregateViews();
			viewsByPackage.put(packageName, views);
		}
		
		if(views.getAggregateView() == null) {
			views.setAggregateView(new HashSet<AggregateView>());
		}
		views.getAggregateView().add((AggregateView)av);
	}
	
	private void writeToFile(AggregateManager am, Map<String, AggregateViews> viewsByPackage) {
		File f = am.getGeneratedViewsDirectory();
		
		for(Map.Entry<String, AggregateViews> entry: viewsByPackage.entrySet()) {
			String fileName = f.getPath() + File.separator + Settings.encodeParam(entry.getKey()) + ".xml";
			
			// This will overwrite any existing file
			try {
				(new AggregateViewFactory()).save(fileName, entry.getValue());
				
			} catch (JAXBException e) {
				ClassUtil.wrapRun(e);
			}
		}
	}

	public void generateQueries(AggregateManager am) {
		
		// The views have the paths populated
		List<View> views = am.getDataModel().getShape().getViews();
		
		// Categorize the views by package
		Map<String, AggregateViews> viewsByPackage = new HashMap<String, AggregateViews>();		
		
		// Now generate the query for each views
		for(View av: views) {
			av = av.copy();

			// Before creating the QueryView, save away all the loop based attributes 
			// and put them in a new view called AggregateView.REGEX
			av.expand();
			Map<String, Pattern> regexAttributes = av.getRegexAttributes();
			
			// skip views not related to a type
			if(av.getTypeName() == null) {
				continue;
			}

			Type type = am.getDataModel().getShape().getType(av.getTypeName());
			groupByPackage(type, av, viewsByPackage);
			
			QueryKey viewKey = new QueryKey(type, av.getName());

			AggregateTree<QueryTree, InterQuery<QueryTree>> queryTree = new AggregateTree(av);
			new FragmentBuilder(queryTree).build(new QueryTree((EntityType)viewKey.type, av));
			
			// Extract system generated OQL query
			List<AggregateView> parallelViews = new ArrayList<>();
			if(av.getExactAttributes() != null) {
				av.setAttributeList(new ArrayList<String>(av.getExactAttributes()));
				parallelViews = queryTree.extractViews(am);
				((AggregateView)av).setSystemOQLQuery(
					(new OQLQuery()).generateQuery(
						am,
						queryTree,
						queryTree.getRoot()));
			}
			
			// Get a list of all the AggregateViews from the QueryView instance
			// and append the recursive vew to this list
			if(regexAttributes != null && regexAttributes.size() > 0) {
				// Create a new Aggregate view based on the recursive attributes
				AggregateView regexView = new AggregateView();
				regexView.setName(type.getName() + TraversalView.REGEX);
				regexView.setAttributeList(new ArrayList<>(regexAttributes.keySet()));
				parallelViews.add(regexView);
			}
			
			// Add the children views to the aggregate view
			((AggregateView)av).setChildren(parallelViews);
		}
		
		writeToFile(am, viewsByPackage);
	}	
	
	/**
	 * Refer https://docs.oracle.com/javase/tutorial/jaxp/dom/readingXML.html
	 *       https://docs.oracle.com/javase/tutorial/jaxp/xslt/writingDom.html
	 * @param am AggregateManager
	 */
	public void testDOMRewrite(AggregateManager am) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException pe) {
			ClassUtil.wrapRun(pe);
		}

		File dir = am.getGeneratedViewsDirectory();
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				try {
					Document doc = db.parse(child);

					// Use a Transformer for output
					TransformerFactory tFactory = TransformerFactory
							.newInstance();
					Transformer transformer = tFactory.newTransformer();

					String childPath =
						child.getPath().endsWith(REGEN_SUFFIX + ".xml")
							? child.getPath()
							: child.getPath() + REGEN_SUFFIX + ".xml";

					File f = new File(childPath);
					DOMSource source = new DOMSource(doc);
					StreamResult result = new StreamResult(f);
					transformer.transform(source, result);
					
				} catch (SAXException e) {
					ClassUtil.wrapRun(e);
				} catch (IOException e) {
					ClassUtil.wrapRun(e);
				} catch (TransformerConfigurationException tce) {
					System.out.println("* Transformer Factory error");
					System.out.println(" " + tce.getMessage());

					Throwable x = tce;
					if (tce.getException() != null)
						x = tce.getException();
					x.printStackTrace();
				} catch (TransformerException te) {
					System.out.println("* Transformation error");
					System.out.println(" " + te.getMessage());

					Throwable x = te;
					if (te.getException() != null)
						x = te.getException();
					x.printStackTrace();
				}
			}
		}
	}
}
