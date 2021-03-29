package com.comodide.editor.model;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;

public abstract class ComodideCell extends mxCell {
	
	private static final long serialVersionUID = 8089893998863898138L;
	private static final ShortFormProvider shortFormProvider = new SimpleShortFormProvider();
	private OWLEntity entity;

	/**
	 * The constructor for this class.
	 * 
	 * @param owlEntity is the entity of this class.
	 */
	public ComodideCell(OWLEntity owlEntity) {
		setEntity(owlEntity);
		this.geometry = new mxGeometry();
	}
	
	/** Sets this entity to the given entity and reconfigueres the short form of entity.*/
	public void setEntity(OWLEntity entity) {
		this.entity = entity;
		this.value = shortFormProvider.getShortForm(entity);
	}
	
	/** Returns this entity. */
	public OWLEntity getEntity() {
		return this.entity;
	}
	
	/** Returns true if this entity's IRI is not equal to the default IRI. */
	public boolean isNamed() {
		return !this.getEntity().getIRI().toString().equalsIgnoreCase(this.defaultIRI().toString());
	}
	
	/** Abstract method signature. */
	public abstract IRI defaultIRI();
}
