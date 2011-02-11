package org.opentox.pol.xml;

import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ParsePolicy {

	//No generics
	ArrayList<Policy> myPols;
	Document dom;


	public ParsePolicy(){
		//create a list to hold the employee objects
		myPols = new ArrayList<Policy>();
	}

	public ArrayList<Policy> runParser(String filename) throws Exception {
		parseXmlFile(filename);
		parseDocument();
//		printData();
		return myPols;
	}
	
	private void parseXmlFile(String filename) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		DocumentBuilder db = dbf.newDocumentBuilder();
		dom = db.parse(filename);

	}

	
	private void parseDocument(){
		Element docEle = dom.getDocumentElement();
		//get a nodelist of <Policy> elements
		NodeList nl = docEle.getElementsByTagName("Policy");
		if(nl != null && nl.getLength() > 0) {
			for(int i = 0 ; i < nl.getLength();i++) {
				Element pol = (Element)nl.item(i);
				Policy p = getPolicy(pol);
				myPols.add(p);
			}
		}
	}


	/**
	 * I take an employee element and read the values in, create
	 * an Employee object and return it
	 * @param empEl
	 * @return
	 */
	private Policy getPolicy(Element polEle) {
		
        // Get Resource Name
		NodeList nl1 = polEle.getElementsByTagName("Rule");
		String name = polEle.getAttribute("name");
		
		ArrayList<String> resourcenames = new ArrayList<String>();
		for (int i=0; i<nl1.getLength(); i++) {
			NodeList nl2 = ((Element) nl1.item(i)).getElementsByTagName("ResourceName");
			resourcenames.add(((Element) nl2.item(0)).getAttribute("name"));
		}
		Policy p = new Policy(name,resourcenames);
		
		return p;
	}




	
	/**
	 * Iterate through the list and print the 
	 * content to console
	 */
//	private void printData(){
//		Iterator it = myPols.iterator();
//		while(it.hasNext()) {
//			System.out.println(it.next().toString());
//		}
//	}

/*
	public static void main(String[] args){
		//create an instance
		ParsePolicy pp = new ParsePolicy();
		
		//call run example
		pp.runExample();
	}
*/

}
