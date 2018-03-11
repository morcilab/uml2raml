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
import java.util.Iterator;
import java.util.List;

public class RamlResource extends RamlElementBase implements RamlDescriptableElement {
	public enum WriteMode {
		INLINE, 		//expand internal nodes, do not recurse for child resources
		INLINE_NESTED, 	//expand internal nodes, recurse for child resources
		TYPE, 			//output as the body of a resourceType definition
		TYPEREF_NESTED, //output as path + reference to a resourceType
	}
	private RamlModel model;
	private RamlResource parent;
	private List<RamlResource> childResourcesList = new ArrayList<>();
	private List<RamlMethod> methodsList = new ArrayList<>();
	private List<String> uriParameters = new ArrayList<>();
	private String path;
	private String typeName;
	private int depth;
	private String description;
	private String is;
	private String type;
	private String securedBy;

	public RamlResource(String name, RamlModel model, RamlResource parent) {
		super(name, parent);
		this.model = model;
		this.parent = parent;
		if(parent != null) {
			parent.addChildResource(this);
			this.depth = parent.getDepth() + 1;
		} else {
			this.depth = 0;
			model.addResource(this);
		}
	}
	
	@Override
	public String getFQName() {
		return this.model.getFQName()+"_"+this.name;
	}

	public boolean isToplevel() {
		return this.parent == null;
	}

	private void addChildResource(RamlResource childResource) {
		this.childResourcesList.add(childResource);
		childResource.setParent(this);
		addChild(childResource);
	}
	
	public void addMethod(RamlMethod method) {
		this.methodsList.add(method);
		addChild(method);
	}
	
	public void addUriParameter(String parameterName) {
		this.uriParameters.add(parameterName);
	}
	
	public void removeMethod(RamlMethod method) {
		this.methodsList.remove(method);
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public String getFullPath() {
		if(this.parent == null) {
			return this.path;
		} else {
			return parent.getFullPath()+this.path;
		}
	}
	
	public int getDepth() {
		return this.depth;
	}
	
	public RamlModel getModel() {
		return this.model;
	}
	
	public void setParent(RamlResource parent) {
		this.parent = parent;
	}
	
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public List<RamlResource> getChildResourcesList() {
		return this.childResourcesList;
	}

	public List<RamlMethod> getMethods() {
		return this.methodsList;
	}

	public void setIs(String is) {
		this.is = is;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void setSecuredBy(String securedBy) {
		this.securedBy = securedBy;
	}
	
	public String getIs() {
		return is;
	}
	
	public String getType() {
		return type;
	}
	
	public String getSecuredBy() {
		return securedBy;
	}
	
	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "{RamlResource: "+this.path+"}";
	}

	public void write(Writer writer) throws IOException {
		write(writer, WriteMode.INLINE);
	}

	public void write(Writer writer, WriteMode writeMode) throws IOException {
		int depth = this.depth;
		if(writeMode == WriteMode.TYPE) {
			depth = 1;
		}
		StringBuffer indent = new StringBuffer();
		for(int i = 0; i < depth; i++) {
			indent = indent.append("  ");
		}
		int methodDepth = -1; //depends on the depth of the enclosing resource
		if(writeMode == WriteMode.TYPE) {
			writer.write(indent+this.typeName+":\n");
			methodDepth = 2;
		} else {
			writer.write(indent+this.path+":\n");
		}
		indent.append("  ");
		if(writeMode == WriteMode.TYPEREF_NESTED) {
			writer.write(indent+"type: "+this.typeName+"\n");
		} else {
			if(this.typeName != null) {
				writer.write(indent+"displayName: "+this.typeName+"\n");
			}
			if(!this.uriParameters.isEmpty()) {
				writer.write(indent+"uriParameters:\n");
				for(String uriParameter : this.uriParameters) {
					writer.write(indent+"  "+uriParameter+": string\n");
				}
			}
			if(getDescription() != null && !getDescription().trim().equals("")) {
				writer.write(indent+"description: "+getDescription()+"\n");
			}
			if(this.is != null && !this.is.trim().equals("")) {
				writer.write(indent+"is: "+this.is+"\n");
			}
			if(this.type != null && !this.type.trim().equals("")) {
				writer.write(indent+"type: "+this.type+"\n");
			}
			if(this.securedBy != null && !this.securedBy.trim().equals("")) {
				writer.write(indent+"securedBy: "+this.securedBy+"\n");
			}
			for(Iterator<RamlMethod> methodsIterator = this.methodsList.iterator(); methodsIterator.hasNext(); ) {
				RamlMethod ramlMethod = methodsIterator.next();
				ramlMethod.write(writer, methodDepth);
			}
		}

		if(writeMode == WriteMode.INLINE_NESTED || writeMode == WriteMode.TYPEREF_NESTED) {
			if(this.childResourcesList.size() > 0) {
				writer.write("\n");
			}
			for(Iterator<RamlResource> iterator = this.childResourcesList.iterator(); iterator.hasNext(); ) {
				RamlResource childResource = iterator.next();
				childResource.write(writer, writeMode);
				if(iterator.hasNext()) {
					writer.write("\n");
				}
			}
			writer.flush();
		}
	}
}
