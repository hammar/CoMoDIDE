package com.comodide.editor.model;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.comodide.editor.SDConstants;
import com.mxgraph.model.mxGeometry;

/**
 * This class represents a data type cell and extends the <code>Comodide</code> class.
 * 
 * @author cogan
 *
 */

public class DatatypeCell extends ComodideCell {

	private static final long serialVersionUID = 2351081035519057130L;

	private static final int WIDTH = 75;
	private static final int HEIGHT = 30;
	private static final String STYLE = SDConstants.datatypeStyle;
	private static IRI DEFAULT_IRI =  OWLRDFVocabulary.OWL_DATATYPE.getIRI();
	private static EntityType<OWLDatatype> DEFAULT_TYPE = EntityType.DATATYPE;
	
	/** The default constructor for this class. */
	public DatatypeCell() {
		this(OWLManager.getOWLDataFactory().getOWLEntity(DEFAULT_TYPE, DEFAULT_IRI),
				0.0, 0.0);
	}
	
	/**
	 * The constructor for this class.
	 * <p>
	 * This geometry is set to a new instance of <code>mxGeometry</code> when the constructor is passed the given x and y positions,
	 * and private width and height variables. This geometry's relative state is set to false. The style is set the private style
	 * variable, and this vertex and this connectable are both set to true.
	 * 
	 * @param owlEntity is passed to the super class constructor.
	 * @param positionX is passed to the constructor of the new geometry.
	 * @param positionY is passed to the constructor of the new geometry.
	 */
	public DatatypeCell(OWLEntity owlEntity, double positionX, double positionY) {
		super(owlEntity);
		
		this.geometry = new mxGeometry(positionX, positionY, WIDTH, HEIGHT);
		this.geometry.setRelative(false);
		
		this.style = STYLE;
		
		this.vertex = true;
		this.connectable = true;
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
