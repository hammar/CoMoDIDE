package com.comodide.editor.model;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.comodide.editor.SDConstants;
import com.mxgraph.model.mxGeometry;

/**
 * This class represents a property edge cell and extends the <code>Comodide</code> class.
 * 
 * @author cogan
 *
 */

public class PropertyEdgeCell extends ComodideCell {

	private static final long serialVersionUID = -8498156089004202454L;

	private static final String STYLE = SDConstants.standardEdgeStyle;
	private static IRI DEFAULT_IRI =  OWLRDFVocabulary.RDF_PROPERTY.getIRI();
	private static EntityType<OWLObjectProperty> DEFAULT_TYPE = EntityType.OBJECT_PROPERTY;
	
	/** The default constructor for this class. */
	public PropertyEdgeCell() {
		this(OWLManager.getOWLDataFactory().getOWLEntity(DEFAULT_TYPE, DEFAULT_IRI));
	}
	
	/**
	 * The constructor for this class.
	 * <p>
	 * This geometry is set to a new instance of <code>mxGeometry</code> and it's relative state is set to false. The style is 
	 * set the private style variable, and <code>this.setEdge</code> is passed true.
	 * 
	 * @param owlProperty is passed to the super class constructor.
	 */
	public PropertyEdgeCell(OWLProperty owlProperty) {
		super(owlProperty);
		
		this.geometry = new mxGeometry();
		this.geometry.setRelative(true);
		
		this.style = STYLE;
		this.setEdge(true);
	}

	/** 
	 * This overrides the <code>defaultIRI</code> method in the <code>Comodide</code> class.
	 * 
	 * @return Returns the default IRI.
	 */
	@Override
	public IRI defaultIRI() {
		return DEFAULT_IRI;
	}
}
