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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;

/*
 * Utility class with static UML helper UML models
 */
public class UmlUtils {

	public static Object getStereotypeProperty(Element element, String sterotypeName, String stereotypeProperty) {
		Stereotype stereotype = getStereotypeOrSubstereotype(element, sterotypeName);
		if(stereotype != null) {
			if(!element.hasValue(stereotype, stereotypeProperty)) {
				return null;
			}
			Object property = element.getValue(stereotype, stereotypeProperty);
			return property;
		}
		return null;
	}
	
	public static List<String> getStringListStereotypeProperty(Element element, String sterotypeName, String stereotypeProperty) {
		Object values = getStereotypeProperty(element, sterotypeName, stereotypeProperty);
		if(values == null) {
			return null;
		}
		if(values instanceof EList) {
			List<String> items = new ArrayList<String>();
			for(Object value : EList.class.cast(values)) {
				if(value instanceof String) {
					items.add((String)value);
				}
			}
			return items.size() > 0 ? items : null;
		} else {
			return null;
		}
	}

	public static String getStringStereotypeProperty(Element element, String sterotypeName, String stereotypeProperty) {
		Object property = getStereotypeProperty(element, sterotypeName, stereotypeProperty);
		if(property != null && property instanceof String) {
			return (String)property;
		} else {
			return null;
		}
	}

	public static Integer getIntegerStereotypeProperty(Element element, String sterotypeName, String stereotypeProperty) {
		Object property = getStereotypeProperty(element, sterotypeName, stereotypeProperty);
		if(property != null && property instanceof Integer) {
			return (Integer)property;
		} else {
			return null;
		}
	}

	public static Double getDoubleStereotypeProperty(Element element, String sterotypeName, String stereotypeProperty) {
		Object property = getStereotypeProperty(element, sterotypeName, stereotypeProperty);
		if(property != null && property instanceof Double) {
			return (Double)property;
		} else {
			return null;
		}
	}

	//TODO: this implementation with recurse forever if we have a generalization loop!
	public static boolean stereotypeGeneralizationsInclude(Stereotype stereotype, String stereotypeFQName) {
		if(stereotype.getQualifiedName().equals(stereotypeFQName)) {
			return true;
		} else {		
			List<Stereotype> stereotypeGeneralizations = stereotype.getGeneralizations().stream()
				.filter(generalization -> 
					generalization.getGeneral() instanceof Stereotype
				).map(generalization -> 
					(Stereotype)(generalization.getGeneral())
				).collect(Collectors.toList());
			for(Stereotype stereotypeGeneralization : stereotypeGeneralizations) {
				if(stereotypeGeneralizationsInclude(stereotypeGeneralization, stereotypeFQName)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/*
	 * Returns a stereotype that matches stereotypeFQName if it is applied to element.
	 * If this is not the case searches for an applied stereotype that is a specialization of
	 * stereotypeFQName and returns it.
	 */
	public static Stereotype getStereotypeOrSubstereotype(Element element, String stereotypeFQName) {
		Stereotype stereotype;
		stereotype = element.getAppliedStereotype(stereotypeFQName);
		if(stereotype != null) {
			return stereotype;
		} else {
			EList<Stereotype> appliedStereotypes = element.getAppliedStereotypes();
			//for each applied stereotype: if the inheritance closure of the stereotype includes the one we are looking for return it
			for(Stereotype appliedStereotype : appliedStereotypes) {
				if(stereotypeGeneralizationsInclude(appliedStereotype, stereotypeFQName)) {
					return appliedStereotype;
				}
			}
		}
		return null;
			
//			Stack<Stereotype> stereotypesStack = new Stack<>();
//			stereotypesStack.addAll(appliedStereotypes);
//			Set<Stereotype> stereotypeHierarchy = new HashSet<>();
//			while(!stereotypesStack.empty()) {
//				stereotype = stereotypesStack.pop();
//				stereotypeHierarchy.add(stereotype);
//				//retrieve list of generalizations that are stereotypes
//				List<Stereotype> generalizationStereotypes = stereotype.getGeneralizations().stream()
//					.filter(generalization -> 
//						generalization.getGeneral() instanceof Stereotype
//					).map(generalization -> 
//						generalization.getGeneral()
//					).map(Stereotype.class::cast)
//					.collect(Collectors.toList()
//				);
//				for(Stereotype generalizationStereotype : generalizationStereotypes) {
//					if(!stereotypeHierarchy.contains(generalizationStereotype)) {
//						stereotypesStack.add(generalizationStereotype);
//					}
//				}
//			}
//String msg = ((NamedElement)element).getQualifiedName()+ " -> ";
//msg += stereotypeHierarchy.stream().map(stereo -> stereo.getQualifiedName()).collect(Collectors.toList());
//System.err.println(msg);
//			Optional<Stereotype> filteredSteretype = stereotypeHierarchy.stream().filter(stereo -> stereo.getQualifiedName().equals(stereotypeFQName)).findAny();
//			if(filteredSteretype.isPresent()) {
//				return filteredSteretype.get();
//			} else {
//				return null;
//			}

//			Stereotype applicableStereotype = element.getApplicableStereotype(stereotypeFQName);
//			EList<Stereotype> substereotypesList = element.getAppliedSubstereotypes(applicableStereotype);
//			if(substereotypesList.isEmpty()) {
//				return null;
//			} else {
//substereotypesList.get(0).getGeneralizations().stream().forEach(generalization -> System.err.println(generalization.getGeneral().getQualifiedName()));
//				return substereotypesList.get(0);
//			}
//		}
	}

	public static <T extends Element> List<T> findElements(Element root, java.lang.Class<T> clazz, String stereotypeFQName) {
		return root.getOwnedElements().stream()
				.filter(element -> 
					clazz.isInstance(element)
				).map(clazz::cast)
				.filter(element -> 
					getStereotypeOrSubstereotype(element, stereotypeFQName) != null
				).collect(Collectors.toList());
	}

	public static Property getProfileProperty(Stereotype stereotype, String propName) {
		for(Property property : stereotype.getAllAttributes()) {
			if(property.getName().equals(propName)) {
				return property;
			}
		}
		return null;
	}

	public static String getProfilePropertyField(Element element, Stereotype stereotype, String propName, String fieldName) {
		 EObject eObject = element.getStereotypeApplication(stereotype);
		 EClass eClass = eObject.eClass();
		 EStructuralFeature eStructuralFeature = eClass.getEStructuralFeature(propName);
		 if(eStructuralFeature != null) {
			 Object value = eObject.eGet(eStructuralFeature);
			 if(value != null) {
				 eObject = (EObject)value;
				 eClass = eObject.eClass();
				 eStructuralFeature = eClass.getEStructuralFeature(fieldName);
				 if(eStructuralFeature != null) {
					 value = eObject.eGet(eStructuralFeature);
					 if(value != null && value instanceof String) {
						 return (String)value;
					 }
				 }
			 }
		 }
		 return null;
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, String>> getProfilePropertyMap(Element element, Stereotype stereotype, String propName) {
		 EObject eObject = element.getStereotypeApplication(stereotype);
		 EClass eClass = eObject.eClass();
		 EStructuralFeature eStructuralFeature = eClass.getEStructuralFeature(propName);
		 List<EObject> features;
		 if(eStructuralFeature.isMany()) {
			 features = (List<EObject>)eObject.eGet(eStructuralFeature);
		 } else {
			 features = new ArrayList<EObject>();
			 EObject feature = (EObject)eObject.eGet(eStructuralFeature);
			 if(feature != null) {
				 features.add(feature);
			 }
		 }
		 List<Map<String, String>> propsList = new ArrayList<Map<String,String>>();
		 for(EObject feature : features) {
			 Map<String, String> propsMap = new HashMap<String, String>();
			 eClass = feature.eClass();
			 for(EAttribute eAttribute : eClass.getEAllAttributes()) {
				 EStructuralFeature attributeFeature = eClass.getEStructuralFeature(eAttribute.getName());
				 if(attributeFeature != null) {
					 Object valueObject = feature.eGet(attributeFeature);
					 if(valueObject != null && valueObject instanceof String) {
						 propsMap.put(eAttribute.getName(), (String)valueObject);
					 }
				 }
			 }
			 propsList.add(propsMap);
		 }	
		 return propsList;
	}

	public static Map<String, String> getProfilesProperties(Element element, String... stereotypeFQNames) {
		Map<String, String> propsMap = new HashMap<>();
		for(String stereotypeFQName : stereotypeFQNames) {
			propsMap.putAll(getProfileProperties(element, stereotypeFQName));
		}
		return propsMap;
	}
	
	public static Map<String, String> getProfileProperties(Element element, String stereotypeFQName) {
		Map<String, String> propsMap = new HashMap<>();
		Stereotype stereotype = getStereotypeOrSubstereotype(element, stereotypeFQName);
		if(stereotype != null) {
			EList<Property> propsList = stereotype.getAllAttributes();
			for(Iterator<Property> iterator = propsList.iterator(); iterator.hasNext(); ) {
				Property property = iterator.next();
				String value = UmlUtils.getStringStereotypeProperty(element, stereotype.getQualifiedName(), property.getName());
				if(value != null) {
					propsMap.put(property.getName(), value);
				}
			}
		}
		return propsMap;
	}

	public static Class getClassByQN(Model model, String classQN) {
		for(Element element : model.allOwnedElements()) {
			if(element instanceof Class) {
				if(((Class)element).getQualifiedName().equals(classQN)) {
					return (Class)element;
				}
			}
		}
		return null;
	}

	public static NamedElement getElementByQN(Model model, String classQN) {
		for(Element element : model.allOwnedElements()) {
			if(element instanceof NamedElement) {
				if(classQN.equals(((NamedElement)element).getQualifiedName())) {
					return (NamedElement)element;
				}
			}
		}
		return null;
	}
}
