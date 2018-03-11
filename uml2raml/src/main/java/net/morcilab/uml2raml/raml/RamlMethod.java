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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RamlMethod extends RamlElementBase implements RamlDescriptableElement {
	private String displayName;
	private String methodName = "";
	private RamlResource ramlResource;
	private RamlMethodEnum method;
	private Map<String, RamlTypeDeclaration> requestBodyMap = new HashMap<>();
	private Map<Integer, Map<String, RamlTypeDeclaration>> responseMap = new HashMap<>();
	private Map<String, RamlTypeDeclaration> queryParameterMap = new HashMap<>();
	private String description;
	private String queryParameters;
	private String is;
	private String protocols;

	public RamlMethod(String name, RamlMethodEnum method, RamlResource parent) {
		super(name, parent);
		this.ramlResource = parent;
		this.method = method;
		parent.addMethod(this);
	}
	
	@Override
	public String getFQName() {
		String fqName = this.name;
		if(this.ramlResource != null) {
			fqName = this.ramlResource.getFQName()+"_"+fqName;
		}
		return fqName;
	}
	
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public String getDisplayName() {
		return this.displayName;
	}
	
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	
	public String getMethodName() {
		return this.methodName;
	}

	public RamlMethodEnum getMethod() {
		return this.method;
	}
	
	public void setMethod(RamlMethodEnum method) {
		this.method = method;
	}
	
	public void setIs(String is) {
		this.is = is;
	}
	
	public void setProtocols(String protocols) {
		this.protocols = protocols;
	}
	
	public void setQueryParameters(String queryParameters) {
		this.queryParameters = queryParameters;
	}

	public String getQueryParameters() {
		return queryParameters;
	}
	
	public String getIs() {
		return is;
	}

	public String getProtocols() {
		return protocols;
	}
	
	public Map<String, RamlTypeDeclaration> getRequestBodyMap() {
		return this.requestBodyMap;
	}

	public RamlTypeDeclaration getRequestBodyType() {
		return this.requestBodyMap.get("");
	}
	
	public RamlTypeDeclaration getRequestBodyType(String mediaType) {
		return this.requestBodyMap.get(mediaType);
	}
	
	public void addRequestBodyType(String mediaType, RamlTypeDeclaration bodyType) {
		//TODO: support inline inherited types [typea, typeb]
		this.requestBodyMap.put(mediaType, bodyType);
	}
	
	public RamlResource getRamlResource() {
		return this.ramlResource;
	}
	
	public Map<String, RamlTypeDeclaration> getQueryParameterMap() {
		return this.queryParameterMap;
	}
	
	public Map<Integer, Map<String, RamlTypeDeclaration>> getResponses() {
		return this.responseMap;
	}
	
	public Collection<Integer> getResponseCodes() {
		return this.responseMap.keySet();
	}
	
	public Collection<String> getResponseMediaTypes(int code) {
		Map<String, RamlTypeDeclaration> mediaTypes = this.responseMap.get(code);
		if(mediaTypes != null) {
			return mediaTypes.keySet();
		} else {
			return null;
		}
	}
	
	public RamlTypeDeclaration getResponseType(int code, String mediaType) {
		Map<String, RamlTypeDeclaration> mediaTypes = this.responseMap.get(code);
		if(mediaTypes != null) {
			return mediaTypes.get(mediaType);
		} else {
			return null;
		}
	}
	
	public void putResponse(int code, RamlTypeDeclaration type, String mediaType) {
		Map<String, RamlTypeDeclaration> response = this.responseMap.get(code);
		if(response == null) {
			response = new HashMap<>();
			this.responseMap.put(code,  response);
		}
		response.put(mediaType, type);
	}

	public void putQueryParameter(String name, RamlTypeDeclaration type) {
		this.queryParameterMap.put(name, type);
	}
	
	public boolean hasRequestBody() {
		if(this.method == RamlMethodEnum.PATCH || 
		   this.method == RamlMethodEnum.POST ||
		   this.method == RamlMethodEnum.PUT) {
			return true;
		} else {
			return false;
		}
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
		String msg = (this.displayName != null ? this.displayName : "noname-method")+"(";
		msg += "QS: ";
		String separator = "";
		for(String paramName : this.queryParameterMap.keySet()) {
			RamlTypeDeclaration paramType = this.queryParameterMap.get(paramName);
			msg += separator+paramName+":"+paramType;
			separator = ", ";
		}
		if(this.requestBodyMap != null) {
			msg += " BODY:"+this.requestBodyMap;
			
		}
		for(Integer response : this.responseMap.keySet()) {
			msg += " [response"+response+":"+this.responseMap.get(response)+"]";
		}
		return msg+")";
	}
	
	public void write(Writer writer) throws IOException {
		write(writer, -1);
	}
	
	public void write(Writer writer, int depthOffset) throws IOException {
		StringBuffer indent = new StringBuffer();
		int depth = this.ramlResource.getDepth()+1;
		if(depthOffset >= 0) {
			depth = depthOffset;
		}
		for(int i = 0; i < depth; i++) {
			indent.append("  ");
		}
		writer.write(indent+this.methodName.toLowerCase()+":\n");
		if(this.displayName != null) {
			writer.write(indent+"  displayName: "+this.displayName+"\n");
		}
		if(getDescription() != null && !getDescription().trim().equals("")) {
			writer.write(indent+"  description: "+getDescription()+"\n");
		}
		if(this.queryParameters != null) {
			writer.write(indent+"  queryParameters: "+this.queryParameters+"\n");
		}
		if(this.is != null) {
			writer.write(indent+"  is: "+this.is+"\n");
		}
		if(this.protocols != null) {
			writer.write(indent+"  protocols: "+this.protocols+"\n");
		}
		if(!this.requestBodyMap.isEmpty()) {
			writer.write(indent+"  body:\n");
			for(String mediaType : this.requestBodyMap.keySet()) {
				RamlTypeDeclaration bodyType = this.requestBodyMap.get(mediaType);
				String moreIndent = "  ";
				if(mediaType.equals("")) {
					moreIndent = "";
				} else {
					writer.write(indent+"    "+mediaType+":\n");
				}
				bodyType.write(writer, indent+moreIndent+"    ");
			}
		}
		if(!this.queryParameterMap.isEmpty()) {
			writer.write(indent+"  queryParameters:\n");
			for(String parameterName : this.queryParameterMap.keySet()) {
				RamlTypeDeclaration parameterType = this.queryParameterMap.get(parameterName);
				writer.write(indent+"    "+parameterName+":\n");
				parameterType.write(writer, indent+"      ");
			}
		}
		writer.write(indent+"  responses:\n");
		if(!this.responseMap.isEmpty()) {
			for(Integer responseCode : this.responseMap.keySet()) {
				Map<String, RamlTypeDeclaration> typeMediatypes = this.responseMap.get(responseCode);
				writer.write(indent+"    "+responseCode+":\n");
				writer.write(indent+"      body:\n");
				String moreIndent = "";
				for(String mediaType : typeMediatypes.keySet()) {
					RamlTypeDeclaration type = typeMediatypes.get(mediaType);
					if(!mediaType.equals("")) {
						moreIndent = "  ";
						writer.write(indent+"        "+mediaType+":\n");
					}
					type.write(writer, indent+moreIndent+"        ");
				}
			}
		} else {
			writer.write(indent+"    200:\n");
			writer.write(indent+"      body:\n");
			writer.write(indent+"        text/plain: !!null\n");
		}
		writer.flush();
	}
}
