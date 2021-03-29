package com.comodide.editor.model;

import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.comodide.editor.SDConstants;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;

public class SubClassEdgeCell extends mxCell {

	private static final long serialVersionUID = -967537018367040076L;
	private static final String STYLE = SDConstants.subclassEdgeStyle;
	
	/** The constructor for this class. */
	public SubClassEdgeCell() {
		this.id = OWLRDFVocabulary.RDFS_SUBCLASS_OF.getIRI().toString();
		this.value = "subClassOf";
		
		this.geometry = new mxGeometry();
		this.geometry.setRelative(true);
		
		this.style = STYLE;
		this.setEdge(true);
	}
}
