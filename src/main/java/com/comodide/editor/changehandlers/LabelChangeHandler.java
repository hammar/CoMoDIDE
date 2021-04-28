package com.comodide.editor.changehandlers;

import java.util.List;
import java.util.Set;

import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.entity.EntityCreationPreferences;
import org.protege.editor.owl.model.find.OWLEntityFinder;
import org.semanticweb.owlapi.io.AnonymousIndividualProperties;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comodide.axiomatization.AxiomManager;
import com.comodide.editor.SchemaDiagram;
import com.comodide.editor.model.ClassCell;
import com.comodide.editor.model.ComodideCell;
import com.comodide.editor.model.DatatypeCell;
import com.comodide.exceptions.ComodideException;
import com.comodide.exceptions.NameClashException;
import com.comodide.rendering.PositioningOperations;
import com.mxgraph.model.mxCell;

/**
 * The purpose of this class to handle any label changes. When the handle method is called, it ensures that the cell passed to it
 * is an instance of a <code>ComodideCell</code> and that the new label passed to it is unique. If it can guarantee both are true,
 * then the given cell and label are passed to one of the two private handle methods depending on if the given cell is an edge,
 * and the label change is completed.
 * 
 * @author cogan
 *
 */

public class LabelChangeHandler
{
	/** Logging */
	private static final Logger log = LoggerFactory.getLogger(LabelChangeHandler.class);

	/** Singleton reference to AxiomManager. Handles OWL entity constructions */
	private AxiomManager axiomManager;

	/** Used for adding positional arguments to updatedCells */
	private OWLModelManager modelManager;
	
	/**
	 * The constructor for this class.
	 * 
	 * @param modelManager sets the reference to the model manager for this class.
	 * @param schemaDiagram sets the reference to the schema diagram
	 */
	public LabelChangeHandler(OWLModelManager modelManager, SchemaDiagram schemaDiagram)
	{
		this.axiomManager = AxiomManager.getInstance(modelManager); //, schemaDiagram);
		this.modelManager = modelManager;
	}

	/**
	 * Ensures that the IRI created by the new label is new. If it is not new, then an exception is thrown.
	 * <p>
	 * If the given cell is an edge, then the <code>handleEdgeLabelChange</code> method is called and passed the given cell 
	 * and new label, otherwise, the <code>handleNodeLabelChange</code> method is called and passed the given cell and new label.
	 * 
	 * @param cell is passed to the <code>handleEdgeLabelChange</code> method or the <code>handleNodeLabelChange</code> method 
	 * 			   to receive it's new label.
	 * @param newLabel will be the new label of the given cell if it is a unique label.
	 * @return An entity with the new label returned from one of the handle change methods.
	 * @exception ComodideException is thrown when the given cell is not an instance of a <code>ComodideCell</code>.
	 * @exception NameClashException is thrown when the IRI created from the new label already exists.
	 */
	public OWLEntity handle(mxCell cell, String newLabel) throws ComodideException
	{
		if (!(cell instanceof ComodideCell)) {
			throw new ComodideException(String.format("[CoModIDE:LabelChangeHandler] The non-CoModIDE cell '%s' was found on the schema diagram. This should never happen.", cell));
		}
		
		// Ensure that the IRI created by this new label is in fact new
		IRI activeOntologyIri = modelManager.getActiveOntology().getOntologyID().getOntologyIRI().or(IRI.generateDocumentIRI());
		String entitySeparator = EntityCreationPreferences.getDefaultSeparator();
		IRI newIRI = IRI.create(activeOntologyIri.toString() + entitySeparator + newLabel);
		OWLEntityFinder finder = modelManager.getOWLEntityFinder();
		Set<OWLEntity> existingEntitiesWithName = finder.getEntities(newIRI);
		if (existingEntitiesWithName.size() > 0) {
			throw new NameClashException(String.format("[CoModIDE:LabelChangeHandler] An OWL entity with the identifier '%s' already exists; unable to add another one.", newLabel));
		}
		
		if (cell.isEdge())
		{
			return handleEdgeLabelChange(cell, newLabel);
		}
		else
		{
			return handleNodeLabelChange(cell, newLabel);
		}
	}

	/**
	 * This method converts the given cell into a <code>ComdideCell</code> and stores a reference to it's property.
	 * If the property of the cell is not null, is named, and does not have a default IRI, then the renaming operation will begin.
	 * <p>
	 * The renaming operation constructs a new IRI using the given label. The property and the new IRI are then passed to the 
	 * <code>changeIRI</code> method in the <code>OWLEntityRenamer</code> class to create a list of ontology changes. The changes
	 * are applied to the active ontology, and a new <code>OWLEntity</code> is constructed based on the new IRI. The new
	 * <code>OWLEntity</code> is returned.
	 * <p>
	 * If the conditions are not not met, then this method gets the source and target cell from the given cell. If the source cell
	 * is a datatype, then <code>log.warn</code> is called and null is returned. (The entity of the source cell will be used as 
	 * the domain, which cannot be a datatype.) The domain and range are set to <code>sourceCell.getEntity</code> and 
	 * <code>targetCell.getEntity</code> respectively. If the target cell is a datatype, then the property is set to the
	 * <code>handleDataProperty</code> method from the axiom manager class. If it is not a datatype, then the property is set to
	 * the <code>handleObjectProperty</code> method from the axiom manager class. Both handle methods are passed the new label, 
	 * domain and range. The property is returned.  
	 * 
	 * @param cell provides the property entity that will be renamed to the new label.
	 * @param newLabel creates the new IRI for the cell's property.
	 * @return Returns an <code>OWLEntity</code> constructed based on the given label.
	 */
	private OWLEntity handleEdgeLabelChange(mxCell cell, String newLabel)
	{
		// Unpack useful things
		ComodideCell edgeCell = (ComodideCell)cell;
		OWLEntity property = edgeCell.getEntity();

		if (property != null && property.isNamed() && !property.getIRI().equals(edgeCell.defaultIRI())) {
			// This is a renaming operation.
			// Construct new property IRI
			OWLOntology activeOntology = modelManager.getActiveOntology();
			String ontologyNamespace = activeOntology.getOntologyID().getOntologyIRI().or(IRI.generateDocumentIRI()).toString();
			String entitySeparator = EntityCreationPreferences.getDefaultSeparator();
			IRI newIRI = IRI.create(ontologyNamespace + entitySeparator + newLabel);
			
			// Create and run renamer
			OWLOntologyManager ontologyManager = activeOntology.getOWLOntologyManager();
			OWLEntityRenamer renamer = new OWLEntityRenamer(ontologyManager, modelManager.getOntologies());			
			// The below configuration, and corresponding reset to that configuration 
			// two lines down, is a workaround for an OWLAPI bug;
			// see https://github.com/owlcs/owlapi/issues/892
			AnonymousIndividualProperties.setRemapAllAnonymousIndividualsIds(false);
			List<OWLOntologyChange> changes = renamer.changeIRI(property, newIRI);
			this.modelManager.applyChanges(changes);
			AnonymousIndividualProperties.resetToDefault();
			
			// Construct the OWLEntity to return
			OWLDataFactory factory = ontologyManager.getOWLDataFactory();
			OWLEntity newEntity = factory.getOWLEntity(property.getEntityType(), newIRI);
			
			// Return a reference entity based on the new IRI
			return newEntity;
		}
		else {
			// This is a new property creation operation. 
			ComodideCell sourceCell = (ComodideCell)cell.getSource();
			ComodideCell targetCell = (ComodideCell)cell.getTarget();
			
			// Domain can not be a datatype
			if(sourceCell instanceof DatatypeCell)
			{
				log.warn("[CoModIDE:LabelChangeHandler] Cannot create axiom with datatype as domain.");
				return null;
			}
			
			OWLEntity domain = sourceCell.getEntity();
			OWLEntity range = targetCell.getEntity();
			
			// Create the property
			if(targetCell instanceof DatatypeCell)
			{
				property = this.axiomManager.handleDataProperty(newLabel, domain, range);
				// Update positioning annotations for target which are stored on the data property
				// (since the target datatype does not have own identity)
				for (OWLOntology ontology : modelManager.getOntologies())
				{
					if (ontology.containsEntityInSignature(property.getIRI()))
					{
						PositioningOperations.updateXYCoordinateAnnotations(property, ontology, targetCell.getGeometry().getX(), targetCell.getGeometry().getY());
					}
				}
				
			}
			else
			{
				property = this.axiomManager.handleObjectProperty(newLabel, domain, range);
			}
			return property;
		}
	}

	/**
	 * If the given cell is a class cell and is named, then this method stores <code>classCell.getEntity().asOWLClass</code> as
	 * the current class. A new IRI is constructed with the new label.  The current class and the new IRI are then passed to the 
	 * <code>changeIRI</code> method in the <code>OWLEntityRenamer</code> class to create a list of ontology changes. The changes
	 * are applied to the active ontology, and a new <code>OWLEntity</code> is constructed based on the new IRI. The new
	 * <code>OWLEntity</code> is returned.
	 * <p>
	 * If the given cell is a class cell but is not named, then the result of <code>this.axiomManager.addNewClass</code> when it
	 * is passed the new label, is returned.
	 * <p>
	 * If the given cell is not a class cell, then null is returned.
	 * 
	 * @param cell provides the class that will be renamed to the new label.
	 * @param newLabel creates the new IRI for the cell's property.
	 * @return This returns an entity constructed from the class and the new IRI.
	 */
	private OWLEntity handleNodeLabelChange(mxCell cell, String newLabel)
	{
		if (cell instanceof ClassCell)
		{
			ClassCell classCell = (ClassCell)cell;
			// Extract current class, if it is present
			OWLClass currentClass = null;
			if (classCell.isNamed()) {
				// This is a renaming operation
				currentClass = classCell.getEntity().asOWLClass();
				
				OWLOntology activeOntology = modelManager.getActiveOntology();
				String ontologyNamespace = activeOntology.getOntologyID().getOntologyIRI().or(IRI.generateDocumentIRI()).toString();
				String entitySeparator = EntityCreationPreferences.getDefaultSeparator();
				IRI newIRI = IRI.create(ontologyNamespace + entitySeparator + newLabel);
				
				// Create and run renamer
				OWLOntologyManager ontologyManager = activeOntology.getOWLOntologyManager();
				OWLEntityRenamer renamer = new OWLEntityRenamer(ontologyManager, modelManager.getOntologies());			
				// The below configuration, and corresponding reset to that configuration 
				// two lines down, is a workaround for an OWLAPI bug;
				// see https://github.com/owlcs/owlapi/issues/892
				AnonymousIndividualProperties.setRemapAllAnonymousIndividualsIds(false);
				List<OWLOntologyChange> changes = renamer.changeIRI(currentClass, newIRI);
				this.modelManager.applyChanges(changes);
				AnonymousIndividualProperties.resetToDefault();
				
				// Construct the OWLEntity to return
				OWLDataFactory factory = ontologyManager.getOWLDataFactory();
				OWLEntity newEntity = factory.getOWLEntity(currentClass.getEntityType(), newIRI);
				
				// Return a reference entity based on the new IRI
				return newEntity;
			}
			else {
				// This is a creation operation -- pass it off to the axiom manager thing
				return this.axiomManager.addNewClass(newLabel);
			}
		}
		return null;
	}
}
