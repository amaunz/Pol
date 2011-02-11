package org.opentox.pol.xml;

import java.util.*;

public class Policy {

	private String name;
    private ArrayList<String> resourceNames;

	public Policy(){
	}
	
	public Policy(String name, ArrayList<String> resourceNames) {
		this.name = name;
		this.resourceNames = resourceNames;
	}

	public void addResourceName(String name) {
		this.resourceNames.add(name) ;
	}
	
	public String getName() {
		return this.name;
	}

	public ArrayList<String> getResources() {
		return this.resourceNames;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Policy Details - ");
		sb.append("Name:" + this.name);
		sb.append(", ");
		sb.append("Resourcename:" + this.resourceNames.toString());
		sb.append(".");
		
		return sb.toString();
	}
}
