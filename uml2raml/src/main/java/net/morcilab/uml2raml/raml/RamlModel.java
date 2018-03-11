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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import net.morcilab.uml2raml.m2t.Uml2RamlException;

public class RamlModel extends RamlElementBase implements RamlDescriptableElement {
	public enum WriteMode {
		INLINE,
		TYPEREF
	}
	private List<RamlResource> toplevelResourceList = new ArrayList<>();
	//This is the list of all types references found while parsing the model
	//they will be converted to actual types at the end of the parsing
	private Set<String> typeNameSet = new HashSet<>();
	private Set<RamlType> typeSet = new HashSet<>();
	private String title;
	private String baseUri;
	private String version;
	private String description;
	private String mediaType;
	private String protocols;
	private String documentation;
	private List<String> types;
	private List<String> traits;
	private List<String> resourceTypes;
	private String securitySchemes;
	private String securedBy;
	private List<String> uses;
	private String baseUriParameters;

	public RamlModel(String name) {
		super(name, null);
	}
	
	@Override
	public RamlModel getModel() {
		return this;
	}
	
	public String getVersion() {
		return version;
	}

	public String getMediaType() {
		return mediaType;
	}

	public String getProtocols() {
		return protocols;
	}

	public String getDocumentation() {
		return documentation;
	}

	public List<String> getTypes() {
		return types;
	}

	public List<String> getTraits() {
		return traits;
	}

	public List<String> getResourceTypes() {
		return resourceTypes;
	}

	public String getSecuritySchemes() {
		return securitySchemes;
	}

	public String getSecuredBy() {
		return securedBy;
	}

	public List<String> getUses() {
		return uses;
	}
	
	public String getBaseUriParameters() {
		return baseUriParameters;
	}

	@Override
	public String getFQName() {
		return this.name;
	}

	public void addResource(RamlResource resource) {
		this.toplevelResourceList.add(resource);
		addChild(resource);
	}
	
	public List<RamlResource> getToplevelResources() {
		return toplevelResourceList;
	}
	
	public Collection<RamlResource> getAllResources() {
		Stack<RamlResource> resourcesStack = new Stack<>();
		resourcesStack.addAll(this.toplevelResourceList);
		Set<RamlResource> flatResoucesSet = new HashSet<>();
		while(!resourcesStack.isEmpty()) {
			RamlResource ramlResource = resourcesStack.pop();
			flatResoucesSet.add(ramlResource);
			resourcesStack.addAll(ramlResource.getChildResourcesList());
		}
		return flatResoucesSet;
	}

	public void addTypeName(String typeName) {
		this.typeNameSet.add(typeName);
	}
	
	public Set<String> getTypeNameSet() {
		return typeNameSet;
	}

	public void setTypeNameSet(Set<String> typeNameSet) {
		this.typeNameSet = typeNameSet;
	}

	public void addType(RamlType ramlType) {
		this.typeSet.add(ramlType);
	}
	
	public void setTitle(String name) {
		this.title = name;
	}

	public String getTitle() {
		return this.title;
	}

	public void setBaseUri(String baseUri) {
		this.baseUri = baseUri;
	}
	
	public void setBaseUriParameters(String baseUriParameters) {
		this.baseUriParameters = baseUriParameters;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String getDescription() {
		return this.description;
	}
	
	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}
	
	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}
	
	public void setProtocols(String protocols) {
		this.protocols = protocols;
	}
	
	public void setTypes(List<String> types) {
		this.types = types;
	}
	
	public void setTraits(List<String> traits) {
		this.traits = traits;
	}
	
	public void setResourceTypes(List<String> resourceTypes) {
		this.resourceTypes = resourceTypes;
	}
	
	public void setSecuritySchemes(String securitySchemes) {
		this.securitySchemes = securitySchemes;
	}
	
	public void setSecuredBy(String securedBy) {
		this.securedBy = securedBy;
	}
	
	public void setUses(List<String> uses) {
		this.uses = uses;
	}
	
	public Set<RamlType> getTypeSet() {
		return this.typeSet;
	}
	
	public void write(Writer writer) throws IOException {
		write(writer, WriteMode.TYPEREF);
	}

	public String getBaseUri() {
		return baseUri;
	}
	
	public void writeProperty(Writer writer, String property) {
		try {
			Method getter = this.getClass().getDeclaredMethod("get"+property.substring(0, 1).toUpperCase()+property.substring(1));
			String value = (String)getter.invoke(this);
			writer.write(value+"\n");
		} catch(Exception e) {
			throw new Uml2RamlException(e);
		}
	}

	public void write(Writer writer, WriteMode writeMode) throws IOException {
		//TODO: write protocols, documentation, types, traits, resourceTypes, securitySchemes, securedBy, uses
		writer.write("#%RAML 1.0\n---\n");
		writer.write("title: "+this.title+"\n");
		
		if(this.baseUri != null) {
			writer.write("baseUri: "+this.baseUri+"\n");
		}
		if(this.baseUriParameters != null) {
			writer.write("baseUriParameters: "+this.baseUriParameters+"\n");
		}
		if(this.version != null) {
			writer.write("version: "+this.version+"\n");
		}
		if(getDescription() != null && !getDescription().trim().equals("")) {
			writer.write("description: "+getDescription()+"\n");
		}
		if(this.mediaType != null) {
			writer.write("mediaType: "+this.mediaType+"\n");
		}
		if(this.protocols != null) {
			writer.write("protocols: "+this.protocols+"\n");
		}
		if(this.documentation != null) {
			writer.write("documentation: "+this.documentation+"\n");			
		}
		if(this.resourceTypes != null && this.resourceTypes.size() > 0) {
			writer.write("resourceTypes:\n");
			for(String resourceType : this.resourceTypes) {
				writer.write("  "+resourceType+"\n");
			}
		}
		if(this.securitySchemes != null) {
			writer.write("securitySchemes: "+this.securitySchemes+"\n");			
		}
		if(this.securedBy != null) {
			writer.write("securedBy: "+this.securedBy+"\n");			
		}
		if(this.uses != null && this.uses.size() > 0) {
			writer.write("uses:\n");
			for(String use : this.uses) {
				writer.write("  "+use+"\n");
			}
		}
		if(this.traits != null && this.traits.size() > 0) {
			writer.write("traits:\n");
			for(String type : this.traits) {
				writer.write("  "+type+"\n");
			}
		}
		boolean typesWritten = false;
		if(this.types != null && this.types.size() > 0) {
			typesWritten = true;
			writer.write("types:\n");
			for(String type : this.types) {
				writer.write("  "+type+"\n");
			}
		}
		writer.flush();
		if(!this.getTypeSet().isEmpty()) {
			if(!typesWritten) {
				writer.write("\ntypes:\n");
			}
			//we first write simple types, then object types
			for(RamlType ramlType : this.getTypeSet()) {
				if(ramlType instanceof RamlSimpleType) {
					ramlType.write(writer);
				}
			}
			for(RamlType ramlType : this.getTypeSet()) {
				if(ramlType instanceof RamlObjectType) {
					ramlType.write(writer);
				}
			}
		}
		RamlResource.WriteMode pathWriteMode = RamlResource.WriteMode.INLINE_NESTED;
		if(!this.toplevelResourceList.isEmpty()) {
			if(writeMode == WriteMode.TYPEREF) {
				//write resource types
				writer.write("\nresourceTypes:\n");
				//flatten the resources tree
				Stack<RamlResource> resourcesStack = new Stack<>();
				resourcesStack.addAll(this.toplevelResourceList);
				Set<RamlResource> flatResoucesSet = new HashSet<>();
				while(!resourcesStack.isEmpty()) {
					RamlResource ramlResource = resourcesStack.pop();
					flatResoucesSet.add(ramlResource);
					resourcesStack.addAll(ramlResource.getChildResourcesList());
				}
				for(RamlResource ramlResource : flatResoucesSet) {
					ramlResource.write(writer, RamlResource.WriteMode.TYPE);
				}
				pathWriteMode = RamlResource.WriteMode.TYPEREF_NESTED;
			}
			for(RamlResource ramlResource : this.toplevelResourceList) {
				writer.write("\n");
				ramlResource.write(writer, pathWriteMode);
			}
		}
		writer.flush();
	}
}
