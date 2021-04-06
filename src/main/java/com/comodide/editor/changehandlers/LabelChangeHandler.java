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
	 * @param schemaDiagram sets the reference to the schema diagram (TODO)
	 */
	public LabelChangeHandler(OWLModelManager modelManager, SchemaDiagram schemaDiagram)
	{
		this.axiomManager = AxiomManager.getInstance(modelManager); //, schemaDiagram);
		this.modelManager = modelManager;
	}

	/**
	 * This ensures that the IRI created by the new label is new. If it is new and the cell is an edge, 
	 * then a handle edge method is called, otherwise, a handle node method is called.
	 * 
	 * @param cell is passed to the handle change method to recieve it's new label.
	 * @param newLabel is the active namespace that will be the new label of the given cell.
	 * @return An entity with the new label returned from one of the handle change methods.
	 * @exception ComodideException is thrown when the given cell is not an instance of a <code>ComodideCell</code>.
	 * @exception NameClashException is thrown when the IRI created from the new label already exsists.
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
	 * If the given edge has a named property and the property's IRI is not the default IRI, then a new property
	 * IRI is constructed and the old IRI is renamed to the new one.
	 * <p>
	 * If it doesn't meet the criteria, then a new property is constructed. 
	 * This can call <code>log.warn</code> if the source cell is a data type cell. 
	 * 
	 * @param cell provides the property entity that will be renamed to the new label.
	 * @param newLabel creates the new IRI for the cell's property.
	 * @return This returns an entity constructed from the property and the new IRI.
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
	 * If the given node cell is a named class, then a new IRI is constructed from the new label. If it is not named,
	 * then the axiom manager adds a new class with the new label.
	 * <p>
	 * This returns null if the given cell is not a class.
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
