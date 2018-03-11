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

public interface RamlExemplifiableElement {
	void setExample(String example);
	
	String getExample();

	default void writeExample(Writer writer) throws IOException {
		writeExample(writer, -1);
	}
	
	default void writeExample(Writer writer, int depth) throws IOException {
		writer.write("description: "+getExample()+"\n");
	}
}
