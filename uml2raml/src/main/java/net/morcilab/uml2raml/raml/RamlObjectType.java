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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RamlObjectType extends RamlType implements RamlDescriptableElement {
	public class RamlTypeProperty {
		public static final int UNBOUNDED = -1; 
		private String name;
		private RamlTypeDeclaration typeDeclaration;
		private boolean optional;

		public RamlTypeProperty(String name, RamlTypeDeclaration type, boolean optional) {
			this.name = name;
			this.typeDeclaration = type;
			this.optional = optional;
		}
		
		public RamlTypeProperty(String name, RamlTypeDeclaration type) {
			this(name, type, false);
		}

		public String getName() {
			return this.name;
		}

		public RamlTypeDeclaration getTypeDeclaration() {
			return this.typeDeclaration;
		}
		
		public boolean isOptional() {
			return this.optional;
		}
	}
	
	private List<RamlTypeProperty> properties = new ArrayList<>();
	private String description;
	private boolean isXMLSchema = false;
	private boolean isJSONSchema = false;
	private String schema;
	private String dfault;
	private String example;
	private String examples;
	
	public RamlObjectType(String name, RamlModel parent) {
		super(name, parent);
	}

	public void setJSONSchema(String schema) {
		this.isJSONSchema = true;
		this.schema = schema;
	}
	
	public void setXMLSchema(String schema) {
		this.isXMLSchema = true;
		this.schema = schema;
	}
	
	public void setDefault(String dfault) {
		this.dfault = dfault;
	}
	
	public void setExample(String example) {
		this.example = example;
	}
	
	public void setExamples(String examples) {
		this.examples = examples;
	}
	
	public String getSchema() {
		return this.schema;
	}

	@Override
	public String getFQName() {
		return this.name;
	}

	/*
	 * Names for ApiModel classes are set to their FQN
	 * When later asking for the name as a type we only
	 * return the non-qualified name without spaces
	 */
	public String getName() {
		String name = this.name;
		int cutAfter = name.lastIndexOf("::");
		if(cutAfter != -1) {
			name = name.substring(cutAfter+2).replace(" ", "");
		}
		return name;
	}
	
	@Override
	public String getDescription() {
		return this.description;
	}
	
	@Override
	public void setDescription(String description) {
		this.description = description;
	}
	
	public List<RamlTypeProperty> getProperties() {
		return this.properties;
	}
	
	public RamlTypeProperty getProperty(String name) {
		Optional<RamlTypeProperty> property = this.properties.stream().filter(prop -> prop.getName().equals(name)).findAny();
		if(property.isPresent()) {
			return property.get();
		} else {
			return null;
		}
	}
	
	public void addProperty(String name, RamlTypeDeclaration type) {
		addProperty(name, type, false);
	}

	public void addProperty(String name, RamlTypeDeclaration type, boolean optional) {
		this.properties.add(new RamlTypeProperty(name, type, optional));
	}
	
	public void write(Writer writer) throws IOException {
		String name = this.getName();
		writer.write("  "+name+":\n");
		if(getDescription() != null && !getDescription().trim().equals("")) {
			writer.write("    description: "+getDescription()+"\n");
		}
		if((this.isJSONSchema || this.isXMLSchema) && this.schema != null) {
			writer.write("    type: !include "+this.schema+"\n");
		} else {
			if(this.dfault != null) {
				writer.write("    default: "+this.dfault+"\n");
			}
			if(this.example != null) {
				writer.write("    example: "+this.example+"\n");
			}
			if(this.examples != null) {
				writer.write("    examples: "+this.examples+"\n");
			}
			writer.write("    type: object\n");
			if(!this.getProperties().isEmpty()) {
				writer.write("    properties:\n");
			}
			for(RamlTypeProperty property : this.getProperties()) {
				String propertyName = property.getName();
				RamlTypeDeclaration propertyType = property.getTypeDeclaration();
				writer.write("      "+propertyName);
				if(property.isOptional()) {
					writer.write("?");
				}
				writer.write(":\n");
				propertyType.write(writer, "        ");
			}
		}
		writer.flush();
	}
	
	@Override
	public String toString() {
		return "RamType: "+this.name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof RamlObjectType) {
			return ((RamlObjectType)obj).getFQName().equals(this.name);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}
}
