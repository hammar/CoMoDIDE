package com.comodide.editor.model;

import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.comodide.editor.SDConstants;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;

/**
 * This class represents a subclass edge cell and extends <code>mxCell</code>.
 * 
 * @author cogan
 *
 */

public class SubClassEdgeCell extends mxCell {

	private static final long serialVersionUID = -967537018367040076L;
	private static final String STYLE = SDConstants.subclassEdgeStyle;
	
	/**
	 *  The constructor for this class. 
	 *  <p>
	 *  The id of this class is set to <code>OWLRDFVocabulary.RDFS_SUBCLASS_OF.getIRI().toString()</code>, and 
	 *  <code>this.value</code> is set to store the string subClassof. This geometry is set to a new instance of 
	 *  <code>mxGeometry</code> and it's relative state is set to false. The style is set the private style variable, and
	 *  <code>this.setEdge</code> is passed true.
	 */
	public SubClassEdgeCell() {
		this.id = OWLRDFVocabulary.RDFS_SUBCLASS_OF.getIRI().toString();
		this.value = "subClassOf";
		
		this.geometry = new mxGeometry();
		this.geometry.setRelative(true);
		
		this.style = STYLE;
		this.setEdge(true);
	}
}
