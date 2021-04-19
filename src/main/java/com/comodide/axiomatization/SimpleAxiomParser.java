package com.comodide.axiomatization;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.HasFiller;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comodide.editor.model.ClassCell;
import com.comodide.editor.model.PropertyEdgeCell;
import com.comodide.editor.model.SubClassEdgeCell;
import com.mxgraph.model.mxCell;

/**
 * (TODO)
 * 
 * @author cogan
 *
 */

public class SimpleAxiomParser
{
	/** Logging */
	private static final Logger log = LoggerFactory.getLogger(SimpleAxiomParser.class);

	/** Empty Constructor */
	public SimpleAxiomParser()
	{

	}

	// @formatter:off
	/**
	 * This gets the left and right class expressions from the given axiom's sub class and super class respectively. If the type
	 * for both the left and right expression is <code>OWL_CLASS</code>, then the edge to return is set to the result of the
	 * <code>atomicSubclass</code> method call. The <code>atomicSubclass</code> method is passed the given axiom and 
	 * the left and right expressions.
	 * <p>
	 * If the type for just the left is <code>OWL_CLASS</code>, then the edge to return is set to the result of the 
	 * <code>rightComplex</code> method call. The <code>rightComplex</code> method is passed the given axiom and the left and 
	 * right expressions.
	 * <p>
	 * If the type for just the right is <code>OWL_CLASS</code>, then the edge to return is set to the result of the 
	 * <code>leftComplex</code> method call. The <code>leftComplex</code> method is passed the given axiom and the left and right 
	 * expressions.
	 * <p>
	 * If the type for neither expression is <code>OWL_CLASS</code>, then the edge to return is set null and <code>log.warn</code>
	 * is called.
	 * <p>
	 * This method is only capable of parsing axioms of the following forms 
	 * 
	 * A \sqsubseteq B 
	 * A \sqsubseteq \forall R.B 
	 * A \sqsubseteq \exists R.B 
	 * \forall R.A \sqsubseteq B 
	 * \exists R.A \sqsubseteq B
	 * 
	 * @param axiom is the axiom that will be parsed to either an <code>atomicSubclass</code>, a <code>rightComplex</code>, 
	 * 				or a <code>leftComplex</code>.
	 * @return This returns the parsed axiom.
	 */
	//Using this on complex axioms will throw big errors :(
	// @formatter:on
	public mxCell parseSimpleAxiom(OWLSubClassOfAxiom axiom)
	{
		// Edge to return
		mxCell edge;

		OWLClassExpression left  = axiom.getSubClass();
		OWLClassExpression right = axiom.getSuperClass();

		// Atomic subclass relationship (
		if (left.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)
				&& right.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
		{
			edge = atomicSubclass(axiom, left, right);
		}
		else if (left.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)
				&& !right.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
		{
			edge = rightComplex(axiom, left, right);
		}
		else if (!left.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)
				&& right.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
		{
			edge = leftComplex(axiom, left, right);
		}
		else
		{
			log.warn("[CoModIDE:SimpleAxiomParser] Rendering of the axiom is not supported:"
					+ axiom.getAxiomWithoutAnnotations().toString());
			edge = null;
		}

		return edge;
	}

	/**
	 * This extracts and wraps the left and right classes from the given left and right expressions. It then makes 
	 * an edge, setting it's source to the sub class wrapper, and the target to the super class wrapper.
	 *  
	 * @param axiom 
	 * @param left sets the source of the new edge cell.
	 * @param right sets the target of the new edge cell.
	 * @return This returns a sub class edge.
	 */
	private SubClassEdgeCell atomicSubclass(OWLAxiom axiom, OWLClassExpression left, OWLClassExpression right)
	{
		// Extract classes
		OWLClass leftClass  = left.asOWLClass();
		OWLClass rightClass = right.asOWLClass();
		
		// Construct wrappers for left/right nodes
		ClassCell subClassCell = new ClassCell(leftClass, 0.0, 0.0);
		ClassCell superClassCell = new ClassCell(rightClass, 0.0, 0.0);
		
		// Make and return edge
		SubClassEdgeCell subclassEdgeCell = new SubClassEdgeCell();
		subclassEdgeCell.setSource(subClassCell);
		subclassEdgeCell.setTarget(superClassCell);
		return subclassEdgeCell;
	}

	/**
	 * This method extracts the property and class from the given left expression and the class from the given right expresion.
	 * The left and right classes are wrapped into target and source cells. The property is used to initialize an edege,
	 * and the edge's source and target are set the the wrappers.
	 * 
	 * @param axiom 
	 * @param left sets the source of the new edge cell.
	 * @param right sets the target of the new edge cell.
	 * @return The new edge cell initialized with the property of the left expression.
	 */
	private PropertyEdgeCell leftComplex(OWLAxiom axiom, OWLClassExpression left, OWLClassExpression right)
	{
		/* Parse Left */
		// Extract Property
		OWLProperty property = (OWLProperty) ((HasProperty<?>) left).getProperty();
		// Extract Class
		OWLEntity leftClass = (OWLEntity) ((HasFiller<?>) left).getFiller();
		// Extract Right Class
		OWLEntity rightClass = right.asOWLClass();

		// Construct wrappers for left/right nodes
		ClassCell targetClassCell = new ClassCell(leftClass, 0.0, 0.0);
		ClassCell sourceClassCell = new ClassCell(rightClass, 0.0, 0.0);
		
		// Construct and return edge
		PropertyEdgeCell relationEdge = new PropertyEdgeCell(property);
		relationEdge.setSource(sourceClassCell);
		relationEdge.setTarget(targetClassCell);
		return relationEdge;
	}

	/**
	 * This method extracts the property and class from the given right expression and the class from the given left expresion.
	 * The left and right classes are wrapped into target and source cells. The property is used to initialize an edege,
	 * and the edge's source and target are set the the wrappers.
	 * 
	 * @param axiom is unused in this method. (TODO)
	 * @param left sets the source of the new edge cell.
	 * @param right sets the target of the new edge cell.
	 * @return The new edge cell initialized with the property of the right expression.
	 */
	private PropertyEdgeCell rightComplex(OWLAxiom axiom, OWLClassExpression left, OWLClassExpression right)
	{
		// Extract left Class
		OWLEntity leftClass = left.asOWLClass();
		/* Parse Right */
		// Extract Property
		OWLProperty property = (OWLProperty) ((HasProperty<?>) right).getProperty();
		// Extract Class
		OWLEntity rightClass = (OWLEntity) ((HasFiller<?>) right).getFiller();

		// Construct wrappers for left/right nodes
		ClassCell sourceClassCell = new ClassCell(leftClass, 0.0, 0.0);
		ClassCell targetClassCell = new ClassCell(rightClass, 0.0, 0.0);
		
		//Construct and return edge
		PropertyEdgeCell relationEdge = new PropertyEdgeCell(property);
		relationEdge.setSource(sourceClassCell);
		relationEdge.setTarget(targetClassCell);
		return relationEdge;
	}
}
