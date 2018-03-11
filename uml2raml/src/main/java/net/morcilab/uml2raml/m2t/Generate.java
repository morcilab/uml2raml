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
package net.morcilab.uml2raml.m2t;

import static net.morcilab.uml2raml.m2t.ProfileNames.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.ParameterDirectionKind;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import net.morcilab.uml2raml.raml.RamlTypeDeclaration;
import net.morcilab.uml2raml.raml.RamlMethod;
import net.morcilab.uml2raml.raml.RamlMethodEnum;
import net.morcilab.uml2raml.raml.RamlModel;
import net.morcilab.uml2raml.raml.RamlObjectType;
import net.morcilab.uml2raml.raml.RamlResource;
import net.morcilab.uml2raml.raml.RamlSimpleType;
import net.morcilab.uml2raml.raml.RamlType;

/*
 * The main generation class. For the various element types (API, Resource,
 * method, ...) a couple of methods are provided: process<Type> and
 * setup<Type>. Process creates the elements in the RAML model and
 * attaches them to the tree, setup<Type> fills the properties reading
 * values from the UML model
 */
public class Generate {
	
	private static Logger LOG = Logger.getGlobal();

	private boolean generateDescriptionFiles = false;
	private String descriptionPath = "";
	private boolean arraysAsTypes = false;
	
	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
	}
	
	public Generate() {
		LOG.setLevel(Level.OFF);
	}

	public static void usage() {
		System.out.println("Usage: Generate [-v] [-a <apiname>] [-d] [-dp <path>] file.uml [file.raml]");
		System.out.println("\t-v: verbose");
		System.out.println("\t-a <apiname>: create the RAML model from a specific API package");
		System.out.println("\t-d: create md description files where description is set to '!'");
		System.out.println("\t-dp <path>: path in which the md description files are created");
		System.out.println("\t-at <path>: arrays in parameters and properties generate new types");
	}

	public static void main(String[] args) throws IOException, Uml2RamlException {
		if(args.length == 0) {
			usage();
		} else {
			Generate generate = new Generate();
			int index = 0;
			String apiName = null;
			while(args[index].startsWith("-")) {
				if(args[index].equals("-v")) {
					generate.setVerbose(true);
				}
				if(args[index].equals("-d")) {
					generate.setGenerateDescriptionFiles(true);
				}
				if(args[index].contentEquals("-a")) {
					index++;
					apiName = args[index];
				}
				if(args[index].contentEquals("-dp")) {
					index++;
					generate.setDescriptionPath(args[index]);
				}
				if(args[index].contentEquals("-at")) {
					index++;
					generate.setArraysAsTypes(true);
				}
				index++;
			}
			String inFilename = null;
			if(index >= args.length) {
				usage();
			} else {
				inFilename = args[index];
				String outFilename = null;
				if(args.length > index+1) {
					outFilename = args[index+1];
				}
				generate.generateFile(inFilename, outFilename, apiName);
			}
		}
	}

	public void setVerbose(boolean verbose) {
		if(verbose) {
			LOG.setLevel(Level.ALL);
		} else {
			LOG.setLevel(Level.OFF);
		}
	}
	
	public void setGenerateDescriptionFiles(boolean generateDescriptionFiles) {
		this.generateDescriptionFiles = generateDescriptionFiles;
	}
	
	public void setDescriptionPath(String descriptionPath) {
		if(descriptionPath != "" && !descriptionPath.endsWith("/")) {
			descriptionPath += "/";
		}
		this.descriptionPath = descriptionPath;
	}
	
	public void setArraysAsTypes(boolean arraysAsTypes) {
		this.arraysAsTypes = arraysAsTypes;
	}

	public Map<String, RamlModel> generate(String umlFilename, String... apiNames) throws IOException {
		if(apiNames.length == 0) {
			return generate(umlFilename, (Collection<String>)null);
		} else {
			return generate(umlFilename, Arrays.asList(apiNames));
		}
	}

	public Map<String, RamlModel> generate(String umlFilename, Collection<String> apiNames) throws IOException {
		//load UML models
		ResourceSet set = new ResourceSetImpl();
		UMLResourcesUtil.init(set);

		set.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		set.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
		Resource resource = set.getResource(URI.createFileURI(new File(umlFilename).getAbsolutePath()), true);
		resource.load(null);
		for(Diagnostic error : resource.getErrors()) {
			LOG.warning(error.getMessage());
		}
		for(Diagnostic warning : resource.getWarnings()) {
			LOG.warning(warning.getMessage());
		}
		UMLResource umlResource = (UMLResource)resource;

		//process the model
		return processModel(umlResource, apiNames);
	}

	public RamlModel generateOne(String umlFilename, String apiName) throws IOException, Uml2RamlException {
		return generate(umlFilename, Arrays.asList(new String [] { apiName })).values().iterator().next();
	}

	public RamlModel generateOne(String umlFilename) throws IOException, Uml2RamlException {
		Map<String, RamlModel> modelsMap = generate(umlFilename);
		if(modelsMap.size() == 0) {
			throw new Uml2RamlException("No API found in the model");
		} else if(modelsMap.size() > 1) {
			throw new Uml2RamlException("generateOne only supports models with 1 API");
		}
		return modelsMap.values().iterator().next();
	}
	
	public void generateFile(String umlFilename, String ramlFilename, String apiName) throws IOException, Uml2RamlException {
		RamlModel model = apiName == null ? generateOne(umlFilename) : generateOne(umlFilename, apiName);
		if(this.generateDescriptionFiles) {
			LOG.info("Creating description files");
			FileManager.generateDescriptionFiles(model, this.descriptionPath, ramlFilename);
		}
		Writer writer;
		boolean closeWriter = false;
		if(ramlFilename == null || ramlFilename.equals("-")) {
			writer = new OutputStreamWriter(System.out);
		} else {
			writer = new FileWriter(ramlFilename);
			closeWriter = true;
		}
		model.write(writer);
		writer.flush();
		if(closeWriter) {
			writer.close();
		}
	}

	private void setupAPI(RamlModel ramlModel, Map<String, String> resourcePropsMap, Package packageElement) {
		//RAML: title
		ramlModel.setTitle(resourcePropsMap.get(RAMLPROFILE_API_TITLE));
		//RAML: description?
		ramlModel.setDescription(resourcePropsMap.get(RESTPROFILE_API_DESCRIPTION));
		//RAML: version?
		ramlModel.setVersion(resourcePropsMap.get(RAMLPROFILE_API_VERSION));
		//RAML: baseUri?
		ramlModel.setBaseUri(resourcePropsMap.get(RESTPROFILE_API_BASEURI));
		//RAML: baseUriParameters?
		ramlModel.setBaseUriParameters(resourcePropsMap.get(RAMLPROFILE_API_BASE_URI_PARAMETERS));
		//RAML: protocols?
		ramlModel.setProtocols(resourcePropsMap.get(RAMLPROFILE_API_PROTOCOLS));
		//RAML: mediaType?
		ramlModel.setMediaType(resourcePropsMap.get(RESTPROFILE_API_MEDIATYPE));
		//RAML: documentation?
		//only inclusion is supported
		ramlModel.setDocumentation(resourcePropsMap.get(RAMLPROFILE_API_DOCUMENTATION));
		//RAML: types?
		//support only includes (i.e. array of strings)
		ramlModel.setTypes(UmlUtils.getStringListStereotypeProperty(packageElement, RAMLPROFILE_RAML_API_FQN, RAMLPROFILE_API_TYPES));
		//RAML: traits?
		ramlModel.setTraits(UmlUtils.getStringListStereotypeProperty(packageElement, RAMLPROFILE_RAML_API_FQN, RAMLPROFILE_API_TRAITS));
		//RAML: resourceTypes?
		ramlModel.setResourceTypes(UmlUtils.getStringListStereotypeProperty(packageElement, RAMLPROFILE_RAML_API_FQN, RAMLPROFILE_API_RESOURCE_TYPES));
		//RAML: annotationTypes?
		//TODO: unsupported
		//RAML: (<annotationName>)?
		//TODO: unsupported
		//RAML: securitySchemes?
		ramlModel.setSecuritySchemes(resourcePropsMap.get(RAMLPROFILE_API_SECURITY_SCHEMES));
		//RAML: securedBy?
		ramlModel.setSecuredBy(resourcePropsMap.get(RAMLPROFILE_API_SECURED_BY));
		//RAML: uses?
		ramlModel.setUses(UmlUtils.getStringListStereotypeProperty(packageElement, RAMLPROFILE_RAML_API_FQN, RAMLPROFILE_API_USES));
	}

	private void setupResource(RamlResource ramlResource, Map<String, String> resourcePropsMap, org.eclipse.uml2.uml.Class resourceElement) {
		ramlResource.setTypeName(resourcePropsMap.get(PROFILE_NAME));
		ramlResource.setPath(resourcePropsMap.get(RESTPROFILE_PATH));
		//RAML: description?
		ramlResource.setDescription(resourcePropsMap.get(RESTPROFILE_RESOURCE_DESCRIPTION));
		//RAML: is?
		ramlResource.setIs(resourcePropsMap.get(RESTPROFILE_RESOURCE_IS));
		//RAML: type
		ramlResource.setType(resourcePropsMap.get(RESTPROFILE_RESOURCE_TYPE));
		//RAML: securedBy
		ramlResource.setSecuredBy(resourcePropsMap.get(RESTPROFILE_RESOURCE_SECURED_BY));
		//RAML: uriParameters
		//TODO: unsupported
	}
	
	private void setupMethod(RamlMethod ramlMethod, Operation operation) {
		//set method name from applied stereotype
		Stereotype methodStereotype = UmlUtils.getStereotypeOrSubstereotype(operation, RESTPROFILE_METHOD_FQN);
		ramlMethod.setMethodName(methodStereotype.getName().toLowerCase());
		//TODO: annotationName? headers? protocols? is? securedBy?
		//RAML: description? - mapped to description stereotype property
		String description = UmlUtils.getStringStereotypeProperty(operation, RESTPROFILE_METHOD_FQN, RAMLPROFILE_METHOD_DESCRIPTION);
		if(description != null) {
			ramlMethod.setDescription(description);
		}
		//RAML: displayName? - mapped to operation name
		String displayName = operation.getName();
		if(displayName != null) {
			ramlMethod.setDisplayName(displayName);
		}
		//RAML: is?
		ramlMethod.setIs(UmlUtils.getStringStereotypeProperty(operation, RESTPROFILE_METHOD_FQN, RAMLPROFILE_METHOD_IS));
		//RAML: protocols?
		ramlMethod.setProtocols(UmlUtils.getStringStereotypeProperty(operation, RESTPROFILE_METHOD_FQN, RAMLPROFILE_METHOD_PROTOCOLS));
		//RAML: queryParameters?
		ramlMethod.setQueryParameters(UmlUtils.getStringStereotypeProperty(operation, RESTPROFILE_METHOD_FQN, RAMLPROFILE_METHOD_QUERY_PARAMETERS));
		//RAML: body? - mapped to operation in and inout parameters
		//retrieve all in/input parameters that are not query parameters
		if(ramlMethod.hasRequestBody()) {
			List<Parameter> inParameters = operation.getOwnedParameters().stream()
					.filter(parameter -> 
						parameter.getDirection() == ParameterDirectionKind.IN_LITERAL || 
						parameter.getDirection() == ParameterDirectionKind.INOUT_LITERAL
					).filter(parameter ->
						UmlUtils.getStereotypeOrSubstereotype(parameter, RESTPROFILE_QUERY_PARAMETER_FQN) == null
					).collect(Collectors.toList());
			for(Parameter parameter : inParameters) {
				String mediaType = UmlUtils.getStringStereotypeProperty(parameter, RESTPROFILE_HTTPREQUEST_FQN, RESTPROFILE_REQUEST_MEDIATYPE);
				if(mediaType == null) {
					mediaType = "";
				}
				RamlTypeDeclaration bodyType = RamlUmlUtils.parameterTypeMapper(parameter);
				//TODO ugly workaround for inline array type declarations in raml-to-jax-rs
				if(this.arraysAsTypes && bodyType.isArray()) {
					bodyType = createArrayTypeAndReturnReference(bodyType, ramlMethod.getModel());
				}
				ramlMethod.addRequestBodyType(mediaType, bodyType);
			}
		}
		//RAML: queryParameters? - mapped to stereotyped in and inout parameters; 
		//the stereotype is not needed for methods that do not support a request body
		List<Parameter> queryParameters = operation.getOwnedParameters().stream()
				.filter(parameter -> 
					parameter.getDirection() == ParameterDirectionKind.IN_LITERAL || 
					parameter.getDirection() == ParameterDirectionKind.INOUT_LITERAL
				).filter(parameter -> 
					UmlUtils.getStereotypeOrSubstereotype(parameter, RESTPROFILE_QUERY_PARAMETER_FQN) != null ||
					!ramlMethod.hasRequestBody()
				).collect(Collectors.toList());
		for(Parameter parameter : queryParameters) {
			RamlTypeDeclaration parameterType = RamlUmlUtils.typeMapper(parameter);
			String dfault = UmlUtils.getStringStereotypeProperty(parameter, RESTPROFILE_QUERY_PARAMETER_FQN, RAMLPROFILE_QUERY_PARAMETER_DEFAULT);
			if(dfault != null && !dfault.trim().equals("")) {
				parameterType.addFacet(RAMLPROFILE_QUERY_PARAMETER_DEFAULT, dfault);
			}
			String example = UmlUtils.getStringStereotypeProperty(parameter, RESTPROFILE_QUERY_PARAMETER_FQN, RAMLPROFILE_QUERY_PARAMETER_EXAMPLE);
			if(example != null && !example.trim().equals("")) {
				parameterType.addFacet(RAMLPROFILE_QUERY_PARAMETER_EXAMPLE, example);
			}
			ramlMethod.putQueryParameter(parameter.getName(), parameterType);
		}
		//RAML: responses? - mapped to operation return, out and inout parameters
		List<Parameter> outParameters = operation.getOwnedParameters().stream()
				.filter(parameter -> 
					parameter.getDirection() == ParameterDirectionKind.RETURN_LITERAL || 
					parameter.getDirection() == ParameterDirectionKind.OUT_LITERAL ||
					parameter.getDirection() == ParameterDirectionKind.INOUT_LITERAL
				).collect(Collectors.toList());
		for(Parameter parameter : outParameters) {
			Integer responseCode = UmlUtils.getIntegerStereotypeProperty(parameter, RESTPROFILE_HTTPRESPONSE_FQN, RESTPROFILE_RESPONSE_CODE);
			if(responseCode == null) {
				responseCode = 200;
			}
			String mediaType = UmlUtils.getStringStereotypeProperty(parameter, RESTPROFILE_HTTPRESPONSE_FQN, RESTPROFILE_RESPONSE_MEDIATYPE);
			if(mediaType == null) {
				mediaType = "";
			}
			RamlTypeDeclaration bodyType = RamlUmlUtils.parameterTypeMapper(parameter);
			//TODO ugly workaround for inline array type declarations in raml-to-jax-rs
			if(this.arraysAsTypes && bodyType.isArray()) {
				bodyType = createArrayTypeAndReturnReference(bodyType, ramlMethod.getModel());
			}
			ramlMethod.putResponse(responseCode, bodyType, mediaType);
		}
	}
	
	private RamlTypeDeclaration createArrayTypeAndReturnReference(RamlTypeDeclaration bodyType, RamlModel model) {
		//produce a new type with the declaration of the parameter
		String arrayTypeName = bodyType.getName()+"Array";
		RamlSimpleType arrayType = new RamlSimpleType(arrayTypeName, model);
		arrayType.setTypeDeclaration(bodyType);
		arrayType.setProcessed(true);
		model.addType(arrayType);
		//let the parameter reference the new type
		bodyType = new RamlTypeDeclaration(arrayTypeName);
		return bodyType;
	}

	private void processMethod(RamlResource resource, Operation operation) {
		if(operation.getName() == null || operation.getName().trim().equals("")) {
			throw new RuntimeException("Operation "+operation.getQualifiedName()+" must have a name");
		}
		LOG.info("Processing Method operation: "+operation.getQualifiedName());
		RamlMethodEnum methodEnum = RamlUmlUtils.getMethodEnumFromOperation(operation);
		RamlMethod ramlMethod = new RamlMethod(operation.getName(), methodEnum, resource);
		setupMethod(ramlMethod, operation);
		
		//add ApiModel parameters to the model's types list
		operation.getOwnedParameters().stream()
			.filter(parameter -> 
				RamlUmlUtils.isApiModel(parameter.getType())
			).forEach(parameter -> 
				resource.getModel().addTypeName(parameter.getType().getQualifiedName())
			);
	}

	private void processResource(RamlModel model, RamlResource parent, org.eclipse.uml2.uml.Class resourceElement) {
		LOG.info("Processing Resource class: "+resourceElement.getQualifiedName());
		//name (becomes RAML displayName?)
		Map<String, String> resourcePropsMap = UmlUtils.getProfileProperties(resourceElement, RESTPROFILE_RESOURCE_FQN);
		String resourceName = RamlUmlUtils.typeMapper(resourceElement).getName();
		if(resourceName == null || resourceName.trim().equals("")) {
			throw new IllegalArgumentException("One of the REST resources has no name");
		}
		resourcePropsMap.put(PROFILE_NAME, resourceName);
		//path
		String path = resourcePropsMap.get(RESTPROFILE_PATH);
		if(path == null) {
			path = RamlUmlUtils.getPath(resourceElement, true);
			if(path == null || path.trim().equals("")) {
				throw new IllegalArgumentException("Resource "+resourceName+" has no path");				
			}
		}
		resourcePropsMap.put(RESTPROFILE_PATH, path);
		RamlResource ramlResource = new RamlResource(resourceName, model, parent);
		setupResource(ramlResource, resourcePropsMap, resourceElement);
		//setup URI parameters from profile
		Pattern pattern = Pattern.compile("\\{[^}]*\\}");
		Matcher matcher = pattern.matcher(path);
		while(matcher.find()) {
			String uriParameter = matcher.group().replace("{", "").replace("}", "");
			ramlResource.addUriParameter(uriParameter);
		}

		//search for REST operations and keep track of types
		resourceElement.getOwnedOperations().stream().
			filter(operation -> 
				RamlUmlUtils.isHTTPMethod(operation)
			).forEach(operation -> 
				processMethod(ramlResource, operation)
			);

		//look for explicit dependencies towards ApiModel classes and add them to model's types list
		//that allows a ApiModel to be produced in the RAML model even in not explicitly referenced by parameters
		resourceElement.getClientDependencies().stream().
			filter(dependency -> 
				RamlUmlUtils.isApiModel(dependency)
			).forEach(dependency -> 
				model.addTypeName(dependency.getQualifiedName())
			);

		//recursively process linked resources
		RamlUmlUtils.getLinkedResources(resourceElement).stream().
			forEach(linkedResource -> 
				processResource(model, ramlResource, linkedResource)
			);
	}

	private RamlModel processAPI(Package packageElement) {
		LOG.info("Processing API package: "+packageElement.getQualifiedName());
		Map<String, String> profilePropsMap = UmlUtils.getProfileProperties(packageElement, RESTPROFILE_API_FQN);
		if(profilePropsMap.get(RAMLPROFILE_API_TITLE) == null) {
			if(packageElement.getName() == null || packageElement.getName().trim().equals("")) {
				throw new Uml2RamlException("API package with no name and to title property set");
			}
			profilePropsMap.put(RAMLPROFILE_API_TITLE, packageElement.getName());
		}
		RamlModel ramlModel = new RamlModel(profilePropsMap.get(RAMLPROFILE_API_TITLE));
		setupAPI(ramlModel, profilePropsMap, packageElement);
	
		//we loop for top-level resources, linked resources are processed recursively
		RamlUmlUtils.getToplevelResources(packageElement).stream().
			forEach(resourceElement -> 
				processResource(ramlModel, null, resourceElement)
			);
		
		return ramlModel;
	}

	/*
	 * RamlObjectTypes are initialized by setting their name to the QN of the related APIModel classes.
	 * processObjectType fills in all the details.
	 */
	private void processRamlType(RamlType ramlType, Model model) {
		if(ramlType.isProcessed()) {
			return;
		}
		String typeFQN = ramlType.getFQName();
		if(ramlType instanceof RamlSimpleType) {
			DataType dataType = (DataType)UmlUtils.getElementByQN(model, typeFQN);
			List<Property> properties = dataType.getAllAttributes().stream().collect(Collectors.toList());
			if(properties.size() != 1) {
				throw new Uml2RamlException("DataType "+typeFQN+" must contain only 1 property");
			}
			Property property = properties.get(0);
			if(RamlUmlUtils.getRAMLType(property) == null) {
				throw new Uml2RamlException("Property "+property.getName()+" of "+typeFQN+" must be a RamlType");
			}
			RamlTypeDeclaration propertyType = RamlUmlUtils.typeMapper(property);
			Map<String, String> facets = RamlUmlUtils.getScalarFacets(dataType, RamlUmlUtils.getRAMLType(property));
			propertyType.addFacets(facets);
			((RamlSimpleType)ramlType).setTypeDeclaration(propertyType);
		} else {
			RamlObjectType ramlObject = (RamlObjectType)ramlType;
			org.eclipse.uml2.uml.Class clazz = UmlUtils.getClassByQN(model, typeFQN);
			if(RamlUmlUtils.isJSONModel(clazz)) {
				String schema = UmlUtils.getStringStereotypeProperty(clazz, APIMODELPROFILE_JSON_SCHEMA_MODEL_FQN, RAMLPROFILE_MODEL_SCHEMA);
				if(schema == null || schema.trim().equals("")) {
					throw new Uml2RamlException("JsonModel "+typeFQN+" must set the stereotype property "+RAMLPROFILE_MODEL_SCHEMA);
				}
				ramlObject.setJSONSchema(schema);
			} else if(RamlUmlUtils.isXMLModel(clazz)) {
				String schema = UmlUtils.getStringStereotypeProperty(clazz, APIMODELPROFILE_XML_SCHEMA_MODEL_FQN, RAMLPROFILE_MODEL_SCHEMA);
				if(schema == null || schema.trim().equals("")) {
					throw new Uml2RamlException("XmlModel "+typeFQN+" must set the stereotype property "+RAMLPROFILE_MODEL_SCHEMA);
				}
				ramlObject.setXMLSchema(schema);			
			} else {
				String dfault = UmlUtils.getStringStereotypeProperty(clazz, APIMODELPROFILE_API_MODEL_FQN, RAMLPROFILE_API_MODEL_DEFAULT);
				if(dfault != null && !dfault.trim().equals("")) {
					ramlObject.setDefault(dfault);
				}
				String example = UmlUtils.getStringStereotypeProperty(clazz, APIMODELPROFILE_API_MODEL_FQN, RAMLPROFILE_API_MODEL_EXAMPLE);
				if(example != null && ! example.trim().equals("")) {
					ramlObject.setExample(example);
				}
				String examples = UmlUtils.getStringStereotypeProperty(clazz, APIMODELPROFILE_API_MODEL_FQN, RAMLPROFILE_API_MODEL_EXAMPLES);
				if(examples != null && ! examples.trim().equals("")) {
					ramlObject.setExamples(examples);
				}
				EList<Property> properties = clazz.allAttributes();
				for(Property property : properties) {
					try {
						RamlTypeDeclaration type = RamlUmlUtils.typeMapper(property);
						String name = property.getName();
						boolean isOptional = false;
						if(property.getLower() == 0 && property.getUpper() == 1) {
							isOptional = true;
						}
						ramlObject.addProperty(name, type, isOptional);
					} catch(IllegalArgumentException e) {
						//a reference to an invalid type, skip it
						LOG.warning("Reference to invalid type while processing "+typeFQN+", skipping it");
					}
				}
			}
		}
		ramlType.setProcessed(true);
	}

	private static void computeTypesSetClosure(RamlModel ramlModel, Model model) {
		Set<String> typeNameSet = ramlModel.getTypeNameSet();
		if(typeNameSet.size() == 0) {
			return;
		}
		//retrieve all referenced types names
		Set<String> processedTypeNames = new HashSet<>();
		Set<String> deltaTypeNames = new HashSet<>(typeNameSet);
		do {
			String ramlTypeName = deltaTypeNames.iterator().next();
			processedTypeNames.add(ramlTypeName);
			String typeFQN = ramlTypeName;
			org.eclipse.uml2.uml.Class clazz = UmlUtils.getClassByQN(model, typeFQN);
			if(clazz != null) { //if it is a class it is an ApiModel, otherwise is a Faceted DataType
				//concatenate all associations end types and properties types and, 
				//for each type that is an apiModel add it to typesSet
				Stream.concat(
					clazz.getAssociations().stream()
						.map(association -> 
							association.getEndTypes()
						).flatMap(endTypes -> 
							endTypes.stream()
						),
					clazz.getAllAttributes().stream()
						.map(property -> 
							property.getType()
						)
					)
					.filter(type -> 
						RamlUmlUtils.isApiModel(type)
					).forEach(type -> {
						typeNameSet.add(type.getQualifiedName());
					});
			}
			deltaTypeNames = new HashSet<>(typeNameSet);
			deltaTypeNames.removeAll(processedTypeNames);
		} while(!deltaTypeNames.isEmpty());
		ramlModel.setTypeNameSet(processedTypeNames);
	}

	private Map<String, RamlModel> processModel(UMLResource umlResource, Collection<String> apiNames) {
		Map<String, RamlModel> ramlModels = new HashMap<>();
		Element rootElement = (Element)umlResource.getAllContents().next();
		//for each API package in the model
		List<Package> listElements = UmlUtils.findElements(rootElement, Package.class, RESTPROFILE_API_FQN);
		for(Package packageElement : listElements) {
			if(apiNames == null || apiNames.contains(packageElement.getName())) {
				RamlModel ramlModel = processAPI(packageElement);
				//process referred ApiModels
				computeTypesSetClosure(ramlModel, rootElement.getModel());
				for(String ramlTypeName : ramlModel.getTypeNameSet()) {
					NamedElement namedElement = UmlUtils.getElementByQN(rootElement.getModel(), ramlTypeName);
					RamlType ramlType;
					if(namedElement instanceof org.eclipse.uml2.uml.Class) { //type is a class, must be an ApiModel
						ramlType = new RamlObjectType(ramlTypeName, ramlModel);
					} else { //type must be a Faceted DataType
						ramlType = new RamlSimpleType(ramlTypeName, ramlModel);
					}
					processRamlType(ramlType, rootElement.getModel());
					ramlModel.addType(ramlType);
				}
				ramlModels.put(ramlModel.getName(), ramlModel);
			}
		}
		return ramlModels;
	}
}
