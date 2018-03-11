package net.morcilab.uml2raml.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import net.morcilab.uml2raml.m2t.Generate;

@Mojo(name = "uml2raml", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class MavenPlugin extends AbstractMojo {
	@Parameter(property = "umlFile", defaultValue = "in.uml")
	private String umlFile;
	@Parameter(property = "ramlFile", defaultValue = "out.raml")
	private String ramlFile;
	@Parameter(property = "verbose", defaultValue = "false")
	private String verbose;
	@Parameter(property = "generateDescriptionFiles", defaultValue = "false")
	private String generateDescriptionFiles;
	@Parameter(property = "descriptionPath", defaultValue = "")
	private String descriptionPath;
	@Parameter(property = "arrayAsTypes", defaultValue = "false")
	private String arrayAsTypes;
	@Parameter(property = "apiName", defaultValue = "")
	private String apiName;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("uml2raml");
		getLog().info("umlFile: "+umlFile);
		getLog().info("ramlFile: "+ramlFile);
		try {
			Generate generate = new Generate();
			if(verbose != null && verbose.equalsIgnoreCase("true")) {
				generate.setVerbose(true);
			}
			if(generateDescriptionFiles != null && generateDescriptionFiles.equalsIgnoreCase("true")) {
				generate.setGenerateDescriptionFiles(true);
			}
			if(descriptionPath != null && !descriptionPath.equals("")) {
				generate.setDescriptionPath(descriptionPath);
			}
			if(arrayAsTypes != null && arrayAsTypes.equalsIgnoreCase("true")) {
				generate.setArraysAsTypes(true);
			}
			if(apiName != null && apiName.equals("")) {
				 apiName = null;
			}
			generate.generateFile(umlFile, ramlFile, apiName);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
