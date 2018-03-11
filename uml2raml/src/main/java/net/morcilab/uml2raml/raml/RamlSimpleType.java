package net.morcilab.uml2raml.raml;

import java.io.IOException;
import java.io.Writer;

public class RamlSimpleType extends RamlType implements RamlDescriptableElement {
	private String description;
	private RamlTypeDeclaration typeDeclaration;

	public RamlSimpleType(String name, RamlModel parent) {
		super(name, parent);
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
	public String getFQName() {
		return super.getName(); //this is usually the FQN, this.getName returns the short name
	}
	
	/*
	 * Names for Faceted DataTypes are set to their FQN
	 * When later asking for the name as a type we only
	 * return the short name without spaces
	 */
	@Override
	public String getName() {
		String name = this.name;
		int cutAfter = name.lastIndexOf("::");
		if(cutAfter != -1) {
			name = name.substring(cutAfter+2).replace(" ", "");
		}
		return name;
	}

	public void setTypeDeclaration(RamlTypeDeclaration typeDeclaration) {
		this.typeDeclaration = typeDeclaration;
	}
	
	public RamlTypeDeclaration getTypeDeclaration() {
		return typeDeclaration;
	}

	@Override
	public void write(Writer writer) throws IOException {
		String name = this.getName();
		writer.write("  "+name+":\n");
		if(getDescription() != null && !getDescription().trim().equals("")) {
			writer.write("    description: "+getDescription()+"\n");
		}
		typeDeclaration.write(writer, "    ");
	}
}
