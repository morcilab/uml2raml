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

import java.util.Collection;
import java.util.HashSet;

public abstract class RamlElementBase implements RamlElement {
	protected String name;
	protected RamlElement parent;
	protected Collection<RamlElement> children = new HashSet<>();
	protected RamlModel model;

	public RamlElementBase(String name, RamlElement parent) {
		this.name = name;
		this.parent = parent;
		if(parent != null) {
			this.model = parent.getModel();
		}
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void addChild(RamlElement element) {
		this.children.add(element);
	}
	
	@Override
	public Collection<RamlElement> getChildren() {
		return this.children;
	}
	
	@Override
	public RamlModel getModel() {
		return this.model;
	}
	
	@Override
	public Collection<RamlElement> getAllChildren() {
		Collection<RamlElement> allChildren = new HashSet<>(this.children);
		for(RamlElement child : this.children) {
			allChildren.addAll(child.getAllChildren());
		}
		return allChildren;
	}
}
