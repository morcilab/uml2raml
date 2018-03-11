/********************************************************************************
* Copyright (c) 2017 Davide Rossi
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* This Source Code may also be made available under the following Secondary
* Licenses when the conditions for such availability set forth in the Eclipse
* Public License, v. 2.0 are satisfied: GNU General Public License, version 2
* with the GNU Classpath Exception which is
* available at https://www.gnu.org/software/classpath/license.html.
*
* SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
********************************************************************************/
package net.morcilab.uml2raml.raml;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class RamlTypeDeclaration {
	private boolean isArray = false;
	private int arrayMinItems = -1;
	private int arrayMaxItems = -1;
	private boolean arrayIsUnique = false;
	private String name;
	private Map<String, String> facets = new HashMap<>();

	public RamlTypeDeclaration(RamlTypeDeclaration other) {
		this.arrayIsUnique = other.isArray;
		this.arrayMinItems = other.arrayMinItems;
		this.arrayMaxItems = other.arrayMaxItems;
		this.arrayIsUnique = other.arrayIsUnique;
		this.name = other.name;
		this.facets = new HashMap<>(other.facets);
	}
	
	public RamlTypeDeclaration(String name) {
		this.name = name.replace(" ", "");
	}
	
	public RamlTypeDeclaration(String name, boolean isArray) {
		this(name);
		this.isArray = isArray;
	}

	public String getName() {
		return this.name;
	}
	
	public void setArray(boolean isArray) {
		this.isArray = isArray;
	}
	
	public boolean isArray() {
		return this.isArray;
	}
	
	public void makeItArray() {
		this.isArray = true;
	}
	
	public void makeItArray(int arrayMinItems, int arrayMaxItems, boolean arrayIsUnique) {
		this.isArray = true;
		this.arrayMinItems = arrayMinItems;
		this.arrayMaxItems = arrayMaxItems;
		this.arrayIsUnique = arrayIsUnique;
	}
	
	public void addFacet(String name, String value) {
		this.facets.put(name, value);
	}

	public void addFacets(Map<String, String> facets) {
		this.facets.putAll(facets);
	}

	
	public String getFacet(String name) {
		return facets.get(name);
	}

	public void write(Writer writer, int indentDepth) throws IOException {
		StringBuffer indent = new StringBuffer();
		for(int i = 0; i < indentDepth; i++) {
			indent.append("  ");
		}
		write(writer, indent.toString());
	}
	
	public void write(Writer writer, String indent) throws IOException {
		if(this.isArray) {
			writer.write(indent+"type: array\n");
			if(this.arrayMinItems > 0) {
				writer.write(indent+"minItems: "+this.arrayMinItems+"\n");
			}
			if(this.arrayMaxItems >= 0) {
				writer.write(indent+"maxItems: "+this.arrayMaxItems+"\n");
			}
			if(this.arrayIsUnique) {
				writer.write(indent+"uniqueItems: true\n");
			}
			writer.write(indent+"items: ");
			indent = indent+"  ";
		} else {
			writer.write(indent+"type: ");
		}
		writer.write(this.name+"\n");
		for(String facetName : this.facets.keySet()) {
			String facetValue = this.facets.get(facetName);
			writer.write(indent+facetName+": "+facetValue+"\n");
		}
		writer.flush();
	}
}
