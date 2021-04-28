package com.comodide.editor.model;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;

/**
 * This class represents a comodide cell and extends <code>mxCell</code>.
 * 
 * @author cogan
 *
 */

public abstract class ComodideCell extends mxCell {
	
	private static final long serialVersionUID = 8089893998863898138L;
	private static final ShortFormProvider shortFormProvider = new SimpleShortFormProvider();
	private OWLEntity entity;

	/**
	 * The constructor for this class.
	 * <p>
	 * The <code>setEntity</code> method is passed the given entity, and this geometry is set to a new instance of 
	 * <code>mxGeometry<code>.
	 * 
	 * @param owlEntity is passed to the public <code>setEntity<code> method.
	 */
	public ComodideCell(OWLEntity owlEntity) {
		setEntity(owlEntity);
		this.geometry = new mxGeometry();
	}
	
	/** 
	 * Sets this entity to the given entity and reconfigures the short form of entity.
	 * 
	 * @param entity will be the value of this entity.
	 */
	public void setEntity(OWLEntity entity) {
		this.entity = entity;
		this.value = shortFormProvider.getShortForm(entity);
	}
	
	/** 
	 * @return Returns this entity.
	 */
	public OWLEntity getEntity() {
		return this.entity;
	}
	
	/** 
	 * This determines if this entitiy's IRI as a string is equivalent to this default IRI as a string.
	 * 
	 * @return Returns true if this entity's IRI is not equal to the default IRI.
	 */
	public boolean isNamed() {
		return !this.getEntity().getIRI().toString().equalsIgnoreCase(this.defaultIRI().toString());
	}
	
	/** 
	 * Abstract class for getting the default IRI.
	 * @returns Returns the default IRI.
	 */
	public abstract IRI defaultIRI();
}
