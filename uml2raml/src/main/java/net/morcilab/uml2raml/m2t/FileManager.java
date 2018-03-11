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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.morcilab.uml2raml.raml.RamlDescriptableElement;
import net.morcilab.uml2raml.raml.RamlElement;
import net.morcilab.uml2raml.raml.RamlModel;

/*
 * Utility class used to generate and initialize the files included in the
 * RAML document
 */
public class FileManager {
	public static void generateDescriptionFiles(RamlModel model, String descriptionDirectory, String ramlFilename) throws IOException {
		if(ramlFilename == null || ramlFilename.equals("-")) {
			return;
		}
		Path ramlPath = new File(ramlFilename).toPath().toAbsolutePath().getParent();
		Path relPath = ramlPath.relativize(new File(descriptionDirectory).toPath().toAbsolutePath());

		final String relDirectory = relPath.toString().equals("") ? "" : relPath.toString()+"/";

		List<RamlElement> allElements = new ArrayList<>(model.getAllChildren());
		allElements.add(model);
		for(RamlElement element : allElements) {
			if(element instanceof RamlDescriptableElement) {
				String description = ((RamlDescriptableElement)element).getDescription();
				if(description != null && description.trim().equals("!")) {
					String filename = element.getFQName()+".md";
					((RamlDescriptableElement)element).setDescription("!include "+relDirectory+filename);
					createDescriptionFile(descriptionDirectory, filename , element.getName());
				}
			}
		}
	}

	public static void createDescriptionFile(String path, String filename, String elementName) throws IOException {
		File file = new File(path+filename);
		if(!file.exists()) {
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write("This is the description for **"+elementName+"**\n");
			fileWriter.close();
		}
	}
}
