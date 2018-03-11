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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Dependency;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.MultiplicityElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.TypedElement;

import net.morcilab.uml2raml.raml.RamlTypeDeclaration;
import net.morcilab.uml2raml.raml.RamlMethodEnum;

/*
 * Utility class with static RAML-aware UML helper methods
 */
public class RamlUmlUtils {

	public static boolean hasBasicType(Element element) {
		if(element instanceof TypedElement) {
			String typeQN = ((TypedElement)element).getType().getQualifiedName();
			return typeQN.startsWith(UML_TYPE_PREFIX);
		}
		return false;
	}
	
	public static boolean isNumberRamlType(String type) {
		return type.equals("number") || type.equals("integer");
	}

	public static boolean isIntegerRamlType(String type) {
		return type.equals("integer");
	}

	public static boolean isStringRamlType(String type) {
		if(type.equals("string")) {
			return true;
		}
		return false;
	}

	public static boolean isFileRamlType(String type) {
		if(type.equals("file")) {
			return true;
		}
		return false;
	}
	
	public static String getRAMLType(Element element) {
		if(element instanceof TypedElement) {
			String typeQN = ((TypedElement)element).getType().getQualifiedName();
			if(typeQN.startsWith(RAML_TYPE_PREFIX)) {
				String typeName = typeQN.substring(RAML_TYPE_PREFIX.length());
				return typeName;
			} else {
				return null;
			}
		}
		return null;
	}
	
	/*
	 * Returns the facets for a given element
	 * If the facets are applied to a different element (e.g. when the element is the property of an enclosing faceted DataType)
	 * the ramlType of that element can be passed with ramlType
	 */
	public static Map<String, String> getScalarFacets(Element element, String ramlType) {
		Map<String, String> facets = new HashMap<>();
		if(ramlType == null) {
			ramlType = getRAMLType(element);
		}
		//add facets for stereotyped RamlTypes
		if(isNumberRamlType(ramlType) && element.getAppliedStereotype(RAMLPROFILE_FACETED_NUMBER_FQN) != null) {
			Double minimum = UmlUtils.getDoubleStereotypeProperty(element, RAMLPROFILE_FACETED_NUMBER_FQN, RAMLPROFILE_FACETED_NUMBER_MINIMUM);
			if(minimum != null) {
				String minimumString = minimum.toString();
				if(isIntegerRamlType(ramlType)) {
					if(minimum % 1 != 0) {
						throw new Uml2RamlException("minimum facet must be whole when applied to integer types");
					}
					minimumString = Integer.toString(minimum.intValue());
				}
				facets.put(RAMLPROFILE_FACETED_NUMBER_MINIMUM, minimumString);
			}
			Double maximum = UmlUtils.getDoubleStereotypeProperty(element, RAMLPROFILE_FACETED_NUMBER_FQN, RAMLPROFILE_FACETED_NUMBER_MAXIMUM);
			if(maximum != null) {
				String maximumString = maximum.toString();
				if(isIntegerRamlType(ramlType)) {
					if(maximum % 1 != 0) {
						throw new Uml2RamlException("maximum facet must be whole when applied to integer types");
					}
					maximumString = Integer.toString(maximum.intValue());
				}
				facets.put(RAMLPROFILE_FACETED_NUMBER_MAXIMUM, maximumString);
			}
			Object format = UmlUtils.getStereotypeProperty(element, RAMLPROFILE_FACETED_NUMBER_FQN, RAMLPROFILE_FACETED_NUMBER_FORMAT);
			if(format != null) {
				facets.put(RAMLPROFILE_FACETED_NUMBER_FORMAT, ((EnumerationLiteral)format).getName());
			}
			Double multipleOf = UmlUtils.getDoubleStereotypeProperty(element, RAMLPROFILE_FACETED_NUMBER_FQN, RAMLPROFILE_FACETED_NUMBER_MULTIPLEOF);
			if(multipleOf != null) {
				String multipleOfString = multipleOf.toString();
				if(isIntegerRamlType(ramlType)) {
					if(multipleOf % 1 != 0) {
						throw new Uml2RamlException("multipleOf facet must be whole when applied to integer types");
					}
					multipleOfString = Integer.toString(multipleOf.intValue());
				}
				facets.put(RAMLPROFILE_FACETED_NUMBER_MULTIPLEOF, multipleOfString);
			}
		} else if(isStringRamlType(ramlType) && element.getAppliedStereotype(RAMLPROFILE_FACETED_STRING_FQN) != null) {
			String pattern = UmlUtils.getStringStereotypeProperty(element, RAMLPROFILE_FACETED_STRING_FQN, RAMLPROFILE_FACETED_STRING_PATTERN);
			if(pattern != null) {
				facets.put(RAMLPROFILE_FACETED_STRING_PATTERN, pattern);
			}
			Integer minLength = UmlUtils.getIntegerStereotypeProperty(element, RAMLPROFILE_FACETED_STRING_FQN, RAMLPROFILE_FACETED_STRING_MINLENGTH);
			if(minLength != null) {
				facets.put(RAMLPROFILE_FACETED_STRING_MINLENGTH, minLength.toString());
			}
			Integer maxLength = UmlUtils.getIntegerStereotypeProperty(element, RAMLPROFILE_FACETED_STRING_FQN, RAMLPROFILE_FACETED_STRING_MAXLENGTH);
			if(maxLength != null) {
				facets.put(RAMLPROFILE_FACETED_STRING_MAXLENGTH, maxLength.toString());
			}
			String enm = UmlUtils.getStringStereotypeProperty(element, RAMLPROFILE_FACETED_STRING_FQN, RAMLPROFILE_FACETED_STRING_ENUM);
			if(enm != null) {
				facets.put(RAMLPROFILE_FACETED_STRING_ENUM, enm);
			}
		} else if(isFileRamlType(ramlType) && element.getAppliedStereotype(RAMLPROFILE_FACETED_FILE_FQN) != null) {
			String fileTypes = UmlUtils.getStringStereotypeProperty(element, RAMLPROFILE_FACETED_FILE_FQN, RAMLPROFILE_FACETED_FILE_FILETYPES);
			if(fileTypes != null) {
				facets.put(RAMLPROFILE_FACETED_FILE_FILETYPES, fileTypes);
			}
			Integer minLength = UmlUtils.getIntegerStereotypeProperty(element, RAMLPROFILE_FACETED_FILE_FQN, RAMLPROFILE_FACETED_FILE_MINLENGTH);
			if(minLength != null) {
				facets.put(RAMLPROFILE_FACETED_FILE_MINLENGTH, minLength.toString());
			}
			Integer maxLength = UmlUtils.getIntegerStereotypeProperty(element, RAMLPROFILE_FACETED_FILE_FQN, RAMLPROFILE_FACETED_FILE_MAXLENGTH);
			if(maxLength != null) {
				facets.put(RAMLPROFILE_FACETED_FILE_MAXLENGTH, maxLength.toString());
			}
		}
		return facets;
	}
	
	/*
	 * Returns a RAML type declaration for the UML element properties and parameters
	 * The typename for <<Resource>> elements is <elementname>Resource
	 */
	//TODO: do we really need to call all resources <name>Resource?
	public static RamlTypeDeclaration typeMapper(Element element) {
		if(element instanceof TypedElement) {
			Type type = ((TypedElement)element).getType();
			String typeName = type.getName();
			String typeQN = type.getQualifiedName();
			RamlTypeDeclaration typeDeclaration;
			if(hasBasicType(element)) {
				if(typeQN.endsWith("::Real") || typeQN.endsWith("::UnlimitedNatural")) {
					typeDeclaration = new RamlTypeDeclaration("number");
				} else {
					typeDeclaration = new RamlTypeDeclaration(typeName.toLowerCase());
				}
			} else if(getRAMLType(element) != null) { //not a basic type, check if it is a RAML type
				String ramlType = getRAMLType(element);
				typeDeclaration = new RamlTypeDeclaration(ramlType);
				//add facets
				Map<String, String> facets = getScalarFacets(element, null);
				typeDeclaration.addFacets(facets);
			} else if(isApiModel(type)) {
				//not a basic type, not a RAML type, this should be a <<ApiModel>> reference
				typeDeclaration = new RamlTypeDeclaration(typeName);
			} else {
				throw new IllegalArgumentException("Illegal type "+typeQN+" - only basic UML types, RAMLTypes and <<ApiModel>> references are supported");
			}
			if(element instanceof MultiplicityElement) {
				MultiplicityElement multiplicityElement = (MultiplicityElement)element;
				if(multiplicityElement.isMultivalued()) {
					typeDeclaration.makeItArray(multiplicityElement.getLower(), multiplicityElement.getUpper(), multiplicityElement.isUnique());
				}
			}
			return typeDeclaration;
		} else if(element instanceof org.eclipse.uml2.uml.Class) {
			String typeName = ((org.eclipse.uml2.uml.Class)element).getName();
			if(RamlUmlUtils.isResource(element) && typeName != null) {
				return new RamlTypeDeclaration(typeName+"Resource");
			} else {
				return new RamlTypeDeclaration(typeName);
			}
		} else {
			throw new IllegalArgumentException("Only instances of Class and TypedElement are supported");
		}
	}
	
	/*
	 * Returns the RAML type for parameters in <<HTTPMethod>> operations
	 */
	//TODO: should we support optional parameters somehow?
	public static RamlTypeDeclaration parameterTypeMapper(Parameter parameter) {
		RamlTypeDeclaration typeDeclaration = RamlUmlUtils.typeMapper(parameter);
		if(parameter.isMultivalued()) { //it is an array
			typeDeclaration.makeItArray(parameter.getLower(), parameter.getUpper(), parameter.isUnique());
		}
		return typeDeclaration;
	}

	/*
	 * Returns a collection of resources linked to the resource parameter
	 * via <<ResourcePath>> dependency links
	 */
	public static Collection<org.eclipse.uml2.uml.Class> getLinkedResources(org.eclipse.uml2.uml.Class resource) {
		if(RamlUmlUtils.isResource(resource)) {
			return resource.getClientDependencies().stream()
						.filter(dependency -> 
							UmlUtils.getStereotypeOrSubstereotype(dependency, RESTPROFILE_RESOURCEPATH_FQN) != null
						).map(dependency -> 
							dependency.getTargets()
						).flatMap(targets -> 
							targets.stream()
						).map(org.eclipse.uml2.uml.Class.class::cast)
						.collect(Collectors.toCollection(HashSet::new)
					);
		} else {
			return new LinkedList<>();
		}
	}

	public static List<org.eclipse.uml2.uml.Class> getToplevelResources(Package thePackage) {
		return UmlUtils.findElements(thePackage, org.eclipse.uml2.uml.Class.class, RESTPROFILE_RESOURCE_FQN).stream()
				.map(org.eclipse.uml2.uml.Class.class::cast)
				.filter(clazz -> 
					RamlUmlUtils.isTopLevelResource(clazz)
				).collect(Collectors.toList());
	}

	/*
	 * Checks if the given <<Resource>> class has no <<ResourcePath>> incoming links
	 */
	public static boolean isTopLevelResource(org.eclipse.uml2.uml.Class resource) {
		if(RamlUmlUtils.isResource(resource)) {
			Package thePackage = resource.getNearestPackage(); 
			List<org.eclipse.uml2.uml.Class> resourceList = UmlUtils.findElements(thePackage, org.eclipse.uml2.uml.Class.class, RESTPROFILE_RESOURCE_FQN);
			for(Iterator<org.eclipse.uml2.uml.Class> iterator = resourceList.iterator(); iterator.hasNext(); ) {
				Element element = iterator.next();
				if(RamlUmlUtils.isResource(element) && element != resource) {
					org.eclipse.uml2.uml.Class clazz = (org.eclipse.uml2.uml.Class)element;
					EList<Dependency> dependencies = clazz.getClientDependencies();
					for(Iterator<Dependency> dependenciesIterator = dependencies.iterator(); dependenciesIterator.hasNext(); ) {
						Dependency dependency = dependenciesIterator.next();
						if(UmlUtils.getStereotypeOrSubstereotype(dependency, RESTPROFILE_RESOURCEPATH_FQN) != null && dependency.getTargets().contains(resource)) {
							return false;
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	/*
	 * Returns the path for a given <<Resource>> class. If the resource has the stereotype
	 * path properties set, that is returned. Otherwise a path of <<ResourcePath>> dependency
	 * links is returned
	 */
	public static String getPath(org.eclipse.uml2.uml.Class resource, boolean relative) {
		if(RamlUmlUtils.isResource(resource)) {
			String path = UmlUtils.getStringStereotypeProperty(resource, RESTPROFILE_RESOURCE_FQN, RESTPROFILE_PATH);
			if(path != null) {
				return path;
			} else {
				//retrieve the containing package
				//for all resources in the package look if resource is in their client dependencies
				Package thePackage = resource.getNearestPackage(); 
				EList<Element> packageElements = thePackage.allOwnedElements();
				for(Element element : packageElements) {
					if(RamlUmlUtils.isResource(element)) {
						org.eclipse.uml2.uml.Class clazz = (org.eclipse.uml2.uml.Class)element;
						EList<Dependency> dependencies = clazz.getClientDependencies();
						for(Dependency dependency : dependencies) {
							if(UmlUtils.getStereotypeOrSubstereotype(dependency, RESTPROFILE_RESOURCEPATH_FQN) != null && dependency.getTargets().contains(resource)) {
								String dependencyPath = UmlUtils.getStringStereotypeProperty(dependency, RESTPROFILE_RESOURCEPATH_FQN, RESTPROFILE_PATH);
								if(dependencyPath == null) {
									dependencyPath = dependency.getName();
								}
								if(dependencyPath == null || dependencyPath.trim().equals("")) {
									throw new Uml2RamlException("ResourcePath links must have name or profile attribute path set");
								}
								if(relative) {
									path = (dependencyPath.startsWith("/") ? "" : "/")+dependencyPath;
								} else {
									path += getPath(clazz, false)+(dependencyPath.startsWith("/") ? "" : "/")+dependencyPath;
								}
							}
						}
					}
				}
				return path;
			}
		}
		return "";
	}

	static RamlMethodEnum getMethodEnumFromOperation(Operation operation) {
		RamlMethodEnum methodEnum;
		if(UmlUtils.getStereotypeOrSubstereotype(operation, RESTPROFILE_DELETE_METHOD_FQN) != null) {
			methodEnum = RamlMethodEnum.DELETE;
		} else if(UmlUtils.getStereotypeOrSubstereotype(operation, RESTPROFILE_GET_METHOD_FQN) != null) {
			methodEnum = RamlMethodEnum.GET;
		} else if(UmlUtils.getStereotypeOrSubstereotype(operation, RESTPROFILE_HEAD_METHOD_FQN) != null) {
			methodEnum = RamlMethodEnum.HEAD;
		} else if(UmlUtils.getStereotypeOrSubstereotype(operation, RESTPROFILE_OPTIONS_METHOD_FQN) != null) {
			methodEnum = RamlMethodEnum.OPTIONS;
		} else if(UmlUtils.getStereotypeOrSubstereotype(operation, RESTPROFILE_PATCH_METHOD_FQN) != null) {
			methodEnum = RamlMethodEnum.PATCH;
		} else if(UmlUtils.getStereotypeOrSubstereotype(operation, RESTPROFILE_POST_METHOD_FQN) != null) {
			methodEnum = RamlMethodEnum.POST;
		} else if(UmlUtils.getStereotypeOrSubstereotype(operation, RESTPROFILE_PUT_METHOD_FQN) != null) {
			methodEnum = RamlMethodEnum.PUT;
		} else {
			throw new RuntimeException("Operation stereotype must reference a concrete method");
		}

		return methodEnum;
	}

	public static boolean isResource(Element element) {
		if(element instanceof org.eclipse.uml2.uml.Class && UmlUtils.getStereotypeOrSubstereotype(element, RESTPROFILE_RESOURCE_FQN) != null) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isApiModel(Element element) {
		if(element instanceof org.eclipse.uml2.uml.Class && UmlUtils.getStereotypeOrSubstereotype(element, APIMODELPROFILE_API_MODEL_FQN) != null) {
			return true;
		} else if(element instanceof DataType && UmlUtils.getStereotypeOrSubstereotype(element, APIMODELPROFILE_FACETED_SCALAR_FQN) != null) {
			return true;
		}
		return false;
	}

	public static boolean isApiModelObject(Element element) {
		if(element instanceof org.eclipse.uml2.uml.Class && UmlUtils.getStereotypeOrSubstereotype(element, APIMODELPROFILE_API_MODEL_FQN) != null) {
			return true;
		}
		return false;
	}

	public static boolean isApiModelSimple(Element element) {
		if(element instanceof DataType && UmlUtils.getStereotypeOrSubstereotype(element, APIMODELPROFILE_FACETED_SCALAR_FQN) != null) {
			return true;
		}
		return false;
	}

	public static boolean isJSONModel(Element element) {
		if(element instanceof org.eclipse.uml2.uml.Class && UmlUtils.getStereotypeOrSubstereotype(element, APIMODELPROFILE_JSON_SCHEMA_MODEL_FQN) != null) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isXMLModel(Element element) {
		if(element instanceof org.eclipse.uml2.uml.Class && UmlUtils.getStereotypeOrSubstereotype(element, APIMODELPROFILE_XML_SCHEMA_MODEL_FQN) != null) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isHTTPMethod(Element element) {
		if(element instanceof Operation) {
			if(UmlUtils.getStereotypeOrSubstereotype(element, RESTPROFILE_METHOD_FQN) != null) {
				return true;
			}
		}
		return false;
	}
}
