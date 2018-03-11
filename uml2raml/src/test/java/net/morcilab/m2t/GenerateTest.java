package net.morcilab.m2t;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import net.morcilab.uml2raml.m2t.Generate;
import net.morcilab.uml2raml.m2t.Uml2RamlException;
import net.morcilab.uml2raml.raml.RamlTypeDeclaration;
import net.morcilab.uml2raml.raml.RamlMethod;
import net.morcilab.uml2raml.raml.RamlModel;
import net.morcilab.uml2raml.raml.RamlObjectType;
import net.morcilab.uml2raml.raml.RamlResource;
import net.morcilab.uml2raml.raml.RamlSimpleType;
import net.morcilab.uml2raml.raml.RamlType;

public class GenerateTest {
	@SuppressWarnings("rawtypes")
	String yamlGet(Map map, String path) {
		List<String> path_elements = new ArrayList<>(Arrays.asList(path.split("/")));
		while(path_elements.size() > 1) {
			map = (Map)map.get(path_elements.remove(0));
		}
		return (String)map.get(path_elements.get(0));
	}
	
	@SuppressWarnings("unused")
	private void printModel(RamlModel model) throws IOException {
		StringWriter stringWriter = new StringWriter();
		model.write(stringWriter);
		System.err.println(stringWriter.toString());
	}
	
	@Test
	void generateFileRecipes() throws IOException {
		Generate generate = new Generate();
		generate.generateFile("src/test/resources/uml/recipes.uml", "src/test/resources/tmp/recipes.raml", null);
	}
	
	//Empty model
	@Test
	void generateFromEmptyModelTest() throws IOException {
		Generate generate = new Generate();
		Map<String, RamlModel> modelsMap = generate.generate("src/test/resources/uml/Empty.uml");
		assertEquals(0, modelsMap.size());
	}

	//No <<API>> package
	@Test
	void generateFromNoApiModelTest() throws IOException {
		Generate generate = new Generate();
		Map<String, RamlModel> modelsMap = generate.generate("src/test/resources/uml/NoApiPackage.uml");
		assertEquals(0, modelsMap.size());
	}

	//generateOne with Two <<API>> packages -> Exception
	@Test
	void generateOneFromTwoApiModelsTest() throws IOException {
		Generate generate = new Generate();
		Throwable exception = assertThrows(Uml2RamlException.class, () -> {
			generate.generateOne("src/test/resources/uml/TwoAPIs.uml");
		});
		assertTrue(exception instanceof Uml2RamlException);
	}

	//generate with Two <<API>> packages -> 2 models
	@Test
	void generateFromTwoApiModelsTest() throws IOException {
		Generate generate = new Generate();
		Map<String, RamlModel> modelsMap = generate.generate("src/test/resources/uml/TwoAPIs.uml");
		assertEquals(2, modelsMap.size());
	}

	//All <<Resources>> in <<API>>
	@Test
	void generateOneAllResourcesInApi() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		List<String> expectedResourceNames = Arrays.asList(new String[] {"AlphaResource", "BetaResource", "GammaResource", "DeltaResource"});
		List<String> resourceNames = model.getAllResources().stream().map(RamlResource::getName).collect(Collectors.toList());
		assertTrue(resourceNames.containsAll(expectedResourceNames) && resourceNames.size() == expectedResourceNames.size());
	}

	//Top-level <<Resources>> in <<API>>
	@Test
	void generateOneTopResourcesInApi() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		List<String> expectedResourceNames = Arrays.asList(new String[] {"AlphaResource", "DeltaResource"});
		List<String> resourceNames = model.getToplevelResources().stream().map(RamlResource::getName).collect(Collectors.toList());
		assertTrue(resourceNames.containsAll(expectedResourceNames) && resourceNames.size() == expectedResourceNames.size());
	}

	//Derived resource path (with <<ResourcePath>> name and attribute)
	@Test
	void generateOneDerivedPath() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		RamlResource gammaResource = model.getAllResources().stream().filter(resource -> resource.getName().equals("GammaResource")).findFirst().get();
		assertEquals("/alpha/beta/gamma", gammaResource.getFullPath());
	}
	
	//ApiModel referenced from method return
	@Test
	void generateOneModelFromReturn() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		assertTrue(model.getTypeSet().stream().filter(type -> type.getName().equals("Foo")).findFirst().isPresent());
	}
	
	//Return parameter of type Api Model with multiplicity 0..* becomes new type when arraysAsTypes is set to true
	@Test
	void generateOneArrayInParameter() throws IOException {
		Generate generate = new Generate();
		generate.setArraysAsTypes(true);
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		RamlMethod oneMethod = model.getToplevelResources().stream().filter(resource -> resource.getName().equals("AlphaResource")).
				flatMap(resource -> resource.getMethods().stream()).filter(method -> "one".equals(method.getDisplayName())).findFirst().get();
		RamlTypeDeclaration typeDeclaration = oneMethod.getResponseType(200, "");
		RamlSimpleType ramlType = model.getTypeSet().stream().filter(type -> type.getName().equals(typeDeclaration.getName())).map(RamlSimpleType.class::cast).findAny().get();
		assertTrue(ramlType.getTypeDeclaration().isArray());
	}

	//Return parameter of type Api Model with multiplicity 0..* becomes an array
	@Test
	void generateOneArrayInParameterWithArrayAsTypes() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		RamlMethod oneMethod = model.getToplevelResources().stream().filter(resource -> resource.getName().equals("AlphaResource")).
				flatMap(resource -> resource.getMethods().stream()).filter(method -> "one".equals(method.getDisplayName())).findFirst().get();
		assertTrue(oneMethod.getResponseType(200, "").isArray());
	}
	
	//ApiModel referenced from method parameter
	@Test
	void generateOneModelFromParameter() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		assertTrue(model.getTypeSet().stream().filter(type -> type.getName().equals("Bar")).findFirst().isPresent());
	}

	//ApiModel referenced from other resource-referenced ApiModel
	@Test
	void generateOneModelFromModel() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		assertTrue(model.getTypeSet().stream().filter(type -> type.getName().equals("Baz")).findFirst().isPresent());
	}

	//Non ApiModel referenced from other resource-referenced ApiModel
	@Test
	void generateOneNonModelFromModel() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		assertFalse(model.getTypeSet().stream().filter(type -> type.getName().equals("Bam")).findFirst().isPresent());
	}

	//ApiModel referenced from ApiModel attribute
	@Test
	void generateOneModelFromModelAttribute() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		assertTrue(model.getTypeSet().stream().filter(type -> type.getName().equals("Bar")).findFirst().isPresent());
	}

	//ApiModel with optional attribute
	@Test
	void generateOneModelOptionalModelAttribute() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		RamlType bamType = model.getTypeSet().stream().filter(type -> type.getName().equals("Baz")).findFirst().get();
		assertTrue(((RamlObjectType)bamType).getProperty("bar").isOptional());
	}
	
	//ApiModel with array attribute
	@Test
	void generateOneModelArrayModelAttribute() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		RamlType bamType = model.getTypeSet().stream().filter(type -> type.getName().equals("Baz")).findFirst().get();
		assertTrue(((RamlObjectType)bamType).getProperty("baz").getTypeDeclaration().isArray());
	}

	//YAML: ApiModel with optional attribute
	@SuppressWarnings("rawtypes")
	@Test
	void generateFileOptionalModelAttribute() throws IOException {
		Generate generate = new Generate();
// File version:
//		File ramlFile = File.createTempFile("uml2ramltest", ".raml");
//		ramlFile.deleteOnExit();
//		generate.generateFile("src/test/resources/uml/RestTestAPI.uml", ramlFile.getAbsolutePath());
//		Yaml yaml = new Yaml();
//		Map map = yaml.load(new FileReader(ramlFile));
//		assertEquals("integer", yamlGet(map, "types/Baz/properties/bar?"));
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		StringWriter writer = new StringWriter();
		model.write(writer);
		Yaml yaml = new Yaml();
		Map map = yaml.load(new StringReader(writer.toString()));
		assertEquals("integer", yamlGet(map, "types/Baz/properties/bar?/type"));
	}

	//YAML: ApiModel with array attribute
	@SuppressWarnings("rawtypes")
	@Test
	void generateFileArrayModelAttribute() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		StringWriter writer = new StringWriter();
		model.write(writer);
		Yaml yaml = new Yaml();
		Map map = yaml.load(new StringReader(writer.toString()));
		assertEquals("array", yamlGet(map, "types/Baz/properties/baz/type"));
		assertEquals("integer", yamlGet(map, "types/Baz/properties/baz/items"));
	}

	//Non-stereotyped in parameters in HTTP methods become body type
	@Test
	void generateOneBodyFromParameter() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		RamlMethod doItMethod = model.getToplevelResources().stream().filter(resource -> resource.getName().equals("DeltaResource")).
				flatMap(resource -> resource.getMethods().stream()).filter(method -> "doIt".equals(method.getDisplayName())).findFirst().get();
		assertEquals("Waldo", doItMethod.getRequestBodyType().getName());
	}

	//QueryParameter-stereotyped parameters in HTTP methods become query parameters
	@Test
	void generateOneQSFromPostParameters() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		Map<String, RamlTypeDeclaration> queryParameters = model.getToplevelResources().stream().filter(resource -> resource.getName().equals("DeltaResource"))
				.flatMap(resource -> resource.getMethods().stream()).filter(method -> "doIt".equals(method.getDisplayName())).findFirst().get().getQueryParameterMap();
		assertEquals("Qux", queryParameters.get("qs1").getName());
		assertEquals("integer", queryParameters.get("qs2").getName());
	}
	
	//Non-stereotyped parameters in body-less methods become query parameters
	@Test
	void generateOneQSFromGetParameters() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		RamlMethod ramlMethod = model.getToplevelResources().stream().filter(resource -> resource.getName().equals("DeltaResource"))
				.flatMap(resource -> resource.getMethods().stream()).filter(method -> "doGet".equals(method.getDisplayName())).findFirst().get();
		Map<String, RamlTypeDeclaration> queryParameters = ramlMethod.getQueryParameterMap();
		assertEquals("string", queryParameters.get("qs1").getName());
		assertEquals("integer", queryParameters.get("qs2").getName());
	}
	
	//Multiple non-stereotyped in/in-out parameters in body-supporting methods become complex body type
	@Disabled
	@Test
	void generateOneComplexBodyFromParameters() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		RamlMethod ramlMethod = model.getToplevelResources().stream().filter(resource -> resource.getName().equals("DeltaResource"))
				.flatMap(resource -> resource.getMethods().stream()).filter(method -> "postMultiIn".equals(method.getDisplayName())).findFirst().get();
		assertEquals("[Qux, Waldo]", ramlMethod.getRequestBodyType());
	}

	//Multiple stereotyped return/out/in-out parameters methods become multiple responses
	@Test
	void generateOneMultipleResponsesFromParameters() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RestTestAPI.uml");
		RamlMethod ramlMethod = model.getToplevelResources().stream().filter(resource -> resource.getName().equals("DeltaResource"))
				.flatMap(resource -> resource.getMethods().stream()).filter(method -> "getMultiOut".equals(method.getDisplayName())).findFirst().get();
		RamlTypeDeclaration responseType200 = ramlMethod.getResponseType(200, "");
		RamlTypeDeclaration responseType303 = ramlMethod.getResponseType(303, "");
		RamlTypeDeclaration responseType404 = ramlMethod.getResponseType(404, "");
		assertEquals("Qux", responseType200.getName());
		assertEquals("string", responseType303.getName());
		assertEquals("Waldo", responseType404.getName());
	}
	
	//RAML: multiple stereotyped return/out/in-out parameters methods become multiple responses
	@Test
	void generateOneRamlMultipleResponsesFromParameters() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RamlTestAPI.uml", "TypeAPI");
		RamlMethod ramlMethod = model.getToplevelResources().stream().filter(resource -> resource.getName().equals("InvoicesResource"))
				.flatMap(resource -> resource.getMethods().stream()).filter(method -> "doIt".equals(method.getDisplayName())).findFirst().get();
		RamlTypeDeclaration response201XML = ramlMethod.getResponseType(201, "application/xml");
		RamlTypeDeclaration response201JSON = ramlMethod.getResponseType(201, "application/json");
		RamlTypeDeclaration response422 = ramlMethod.getResponseType(422, "");
		assertEquals("InvoiceXML", response201XML.getName());
		assertEquals("InvoiceJSON", response201JSON.getName());
		assertEquals("Error", response422.getName());
	}

	//RAML: format enum in <<Faceted Number>>
	@Test
	void generateOneRamlEnunFacetedNumber() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RamlTestAPI.uml", "TypeAPI");
		RamlType ramlType = model.getTypeSet().stream().filter(type -> type.getName().equals("Error")).findFirst().get();
		assertEquals("int64", ((RamlObjectType)ramlType).getProperty("errorID").getTypeDeclaration().getFacet("format"));
	}

	//RAML: JSON/XML types and their schemas
	@Test
	void generateOneRamlSchemaTypes() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RamlTestAPI.uml", "TypeAPI");
		RamlType ramlJSONType = model.getTypeSet().parallelStream().filter(type -> type.getName().equals("InvoiceJSON")).findAny().get();
		RamlType ramlXMLType = model.getTypeSet().parallelStream().filter(type -> type.getName().equals("InvoiceXML")).findAny().get();
		assertEquals("invoice.json", ((RamlObjectType)ramlJSONType).getSchema());
		assertEquals("invoice.xsd#Invoice", ((RamlObjectType)ramlXMLType).getSchema());
	}
	
	//RAML: RAML any type as method response
	@Test
	void generateOneRamlAnyType() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RamlTestAPI.uml", "TypeAPI");
		RamlMethod ramlMethod = model.getToplevelResources().stream().filter(resource -> resource.getName().equals("InvoicesResource"))
				.flatMap(resource -> resource.getMethods().stream()).filter(method -> "doThat".equals(method.getDisplayName())).findFirst().get();
		assertEquals("any", ramlMethod.getResponseType(200, "").getName());
	}

	//Multiple method response/out/in-out parameters without stereotype generate union type
	@Test
	@Disabled
	void generateOneMultipleResponseNoStereotype() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RamlTestAPI.uml", "TypeAPI");
		RamlMethod ramlMethod = model.getToplevelResources().stream().filter(resource -> resource.getName().equals("InvoicesResource"))
				.flatMap(resource -> resource.getMethods().stream()).filter(method -> "doAlso".equals(method.getDisplayName())).findFirst().get();
		assertEquals("[InvoiceJSON, InvoiceXML]", ramlMethod.getRequestBodyType());
	}

	//Multiple method response/out/in-out parameters with HTTPRequest stereotype with distinct media type generate multiple request bodies
	@Test
	void generateOneMultipleResponseWithStereotype() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RamlTestAPI.uml", "TypeAPI");
		RamlMethod ramlMethod = model.getToplevelResources().stream().filter(resource -> resource.getName().equals("InvoicesResource"))
				.flatMap(resource -> resource.getMethods().stream()).filter(method -> "doToo".equals(method.getDisplayName())).findFirst().get();
		assertEquals("InvoiceJSON", ramlMethod.getRequestBodyType("application/json").getName());
		assertEquals("InvoiceXML", ramlMethod.getRequestBodyType("application/xml").getName());
	}

	//<<FacetedScalar>> stereotypes DataTypes when referenced from a resource translate into RAML basic types
	@Test
	void generateOneDataTypes() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RamlTestAPI.uml", "TypeAPI");
		RamlType simpleStringType = model.getTypeSet().stream().filter(type -> type.getName().equals("ShortString")).findAny().get();
		assertTrue(simpleStringType instanceof RamlSimpleType);
	}
	
	//Method that returns no ApiModel type
	@Test
	void generateOneReturnNoApiModel() throws IOException {
		Generate generate = new Generate();
		assertThrows(IllegalArgumentException.class, () -> {
			generate.generateOne("src/test/resources/uml/RamlTestAPI.uml", "ErrorAPI");
		});
	}

	//RAML API, Resouces and methods with description property set to ! generate .md files
	@Test
	void generateDescriptionCreateFiles() throws IOException {
		boolean descriptableApiExists = false;
		boolean descriptableResourceExists = false;		
		boolean descriptableMethodExists = false;		
		Generate generate = new Generate();
		generate.setGenerateDescriptionFiles(true);
		Path tmpDirPath = Files.createTempDirectory(FileSystems.getDefault().getPath("src", "test", "resources", "tmp"), "test");
		Path ramlPath = Paths.get(tmpDirPath.toString(), "descriptable.raml");
		Path descriptableApiPath = Paths.get(tmpDirPath.toString(), "DescriptableAPI.md");
		Path descriptableResourcePath = Paths.get(tmpDirPath.toString(), "DescriptableAPI_DescriptableResource.md");
		Path descriptableMethodPath = Paths.get(tmpDirPath.toString(), "DescriptableAPI_DescriptableResource_descriptable.md");
		generate.setDescriptionPath(tmpDirPath.toString());
		generate.generateFile("src/test/resources/uml/RamlTestAPI.uml", ramlPath.toString(), "DescriptableAPI");
		descriptableApiExists = Files.exists(descriptableApiPath);
		descriptableResourceExists = Files.exists(descriptableResourcePath);
		descriptableMethodExists = Files.exists(descriptableMethodPath);
		//code from https://stackoverflow.com/a/27917071/3687501
		Files.walkFileTree(tmpDirPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
		assertTrue(descriptableApiExists);
		assertTrue(descriptableResourceExists);
		assertTrue(descriptableMethodExists);
	}
	
	//All props for RAML API stereotype
	@Test
	void generateOneRamlApiAllProps() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RamlTestAPI.uml", "AllPropsAPI");
		assertEquals("title", model.getTitle());
		assertEquals("description", model.getDescription());
		assertEquals("version", model.getVersion());
		assertEquals("baseUri", model.getBaseUri());
		assertEquals("protocols", model.getProtocols());
		assertEquals("mediaType", model.getMediaType());
		assertEquals("documentation", model.getDocumentation());
		assertEquals("securitySchemes", model.getSecuritySchemes());
		assertEquals("securedBy", model.getSecuredBy());
		assertEquals("baseUriParameters", model.getBaseUriParameters());
		//list properties
		List<String> types = model.getTypes();
		assertTrue(types.size() == 2 && types.containsAll(Arrays.asList(new String[] { "- type1", "- type2" })));
		List<String> traits = model.getTraits();
		assertTrue(traits.size() == 2 && traits.containsAll(Arrays.asList(new String[] { "- trait1", "- trait2" })));
		List<String> resourceTypes = model.getResourceTypes();
		assertTrue(resourceTypes.size() == 2 && resourceTypes.containsAll(Arrays.asList(new String[] { "- resourceType1", "- resourceType2" })));
		List<String> uses = model.getUses();
		assertTrue(uses.size() == 2 && uses.containsAll(Arrays.asList(new String[] { "- use1", "- use2" })));
	}
	
	//All props for RAML API stereotype in RAML file
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void generateOneRamlApiAllPropsYaml() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RamlTestAPI.uml", "AllPropsAPI");
		StringWriter writer = new StringWriter();
		model.write(writer);
		Yaml yaml = new Yaml();
		Map map = yaml.load(new StringReader(writer.toString()));
		assertEquals("title", map.get("title"));
		assertEquals("description", map.get("description"));
		assertEquals("version", map.get("version"));
		assertEquals("baseUri", map.get("baseUri"));
		assertEquals("protocols", map.get("protocols"));
		assertEquals("mediaType", map.get("mediaType"));
		assertEquals("documentation", map.get("documentation"));
		assertEquals("securitySchemes", map.get("securitySchemes"));
		assertEquals("securedBy", map.get("securedBy"));
		assertEquals("baseUriParameters", map.get("baseUriParameters"));
		List<String> types = (List)map.get("types");
		assertTrue(types.size() == 2 && types.containsAll(Arrays.asList(new String[] { "type1", "type2" })));
		List<String> traits = (List)map.get("traits");
		assertTrue(traits.size() == 2 && traits.containsAll(Arrays.asList(new String[] { "trait1", "trait2" })));
//		List<String> resourceTypes = (List)map.get("resourceTypes");
//		assertTrue(resourceTypes.size() == 2 && resourceTypes.containsAll(Arrays.asList(new String[] { "resourceType1", "resourceType2" })));
		List<String> uses = (List)map.get("uses");
		assertTrue(uses.size() == 2 && uses.containsAll(Arrays.asList(new String[] { "use1", "use2" })));
	}
	
	//All props for <<RAML Resource>>
	@Test
	void generateOneRamlResourceAllProps() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RamlTestAPI.uml", "AllPropsAPI");
		RamlResource ramlResource = model.getAllResources().stream().filter(resource -> resource.getName().equals("AllPropsResource")).findAny().get();
		assertEquals("description", ramlResource.getDescription());
		assertEquals("is", ramlResource.getIs());
		assertEquals("type", ramlResource.getType());
		assertEquals("securedBy", ramlResource.getSecuredBy());
	}
	
	//All props for <<HTTP Method>>
	@Test
	void generateOneRamlMethodAllProps() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RamlTestAPI.uml", "AllPropsAPI");
		RamlMethod ramlMethod = model.getToplevelResources().stream().filter(resource -> resource.getName().equals("AllPropsResource"))
				.flatMap(resource -> resource.getMethods().stream()).filter(method -> "allProps".equals(method.getDisplayName())).findFirst().get();
		assertEquals("description", ramlMethod.getDescription());
		assertEquals("is", ramlMethod.getIs());
		assertEquals("queryParameters", ramlMethod.getQueryParameters());
		assertEquals("protocols", ramlMethod.getProtocols());
	}
	
	//All props for <<Query Parameter>>
	@Test
	void generateOneRamlQueryParameterAllProps() throws IOException {
		Generate generate = new Generate();
		RamlModel model = generate.generateOne("src/test/resources/uml/RamlTestAPI.uml", "AllPropsAPI");
		RamlMethod ramlMethod = model.getToplevelResources().stream().filter(resource -> resource.getName().equals("AllPropsResource"))
				.flatMap(resource -> resource.getMethods().stream()).filter(method -> "allProps".equals(method.getDisplayName())).findFirst().get();
		RamlTypeDeclaration parameterType = ramlMethod.getQueryParameterMap().get("allQS");
		assertEquals("default", parameterType.getFacet("default"));
		assertEquals("example", parameterType.getFacet("example"));
	}

	//TODO
	//Properties of <<API Model>> (default, example, examples)
	//Properties of <<HTTP Method>> (displayName, description, queryParameters, is, protocols)
	//<<Query Parameter>> default & example
	//One single non-stereotyped in/in-out parameter in request body-supporting methods translates into request type
	//In/in-out non-stereotyped parameters for non request body-supporting methods translate into query parameters
	//Faceted <type> stereotype support for both properties and parameters		
	//Spaces in Resources and ApiModels
	//Resource with both path attribute and incoming path
	//HTTP method with optional parameter
	//HTTP method with array parameter
	//GET that returns no ApiModel type
	//GET with return and parameters
	//POST with return and parameters
	//POST with return and <<QueryParameter>> parameter
	//GET with return as array of ApiModel (URIs?)
	//GET with return as resource (URI?)
	//One method in / in-out parameter -> body
}
