package net.morcilab.uml2raml.raml;

public abstract class RamlType extends RamlElementBase {
	private boolean processed;
	
	public RamlType(String name, RamlElement parent) {
		super(name, parent);
	}
	
	public boolean isProcessed() {
		return processed;
	}
	
	public void setProcessed(boolean processed) {
		this.processed = processed;
	}
}
