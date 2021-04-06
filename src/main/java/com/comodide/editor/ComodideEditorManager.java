package com.comodide.editor;

import java.util.List;

import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import com.comodide.editor.changehandlers.UpdateFromOntologyHandler;

public class ComodideEditorManager implements OWLOntologyChangeListener, OWLModelManagerListener
{
	private OWLOntology presentlyRenderedOntology;
	private SchemaDiagram  schemaDiagram;
	private final UpdateFromOntologyHandler updateFromOntologyHandler;
	private final OWLModelManager modelManager;

	/**
	 * This method gets the reference to this schema diagram.
	 */
	public SchemaDiagram getSchemaDiagram() {
		return schemaDiagram;
	}

	/**
	 * The constructor for this class.
	 * 
	 * @param modelManager is the reference to this modelManager
	 */
	public ComodideEditorManager(OWLModelManager modelManager)
	{
		// Used to keep track of changes and clearing/re-rendering editor
		this.modelManager = modelManager;
		this.modelManager.addListener(this);
		this.presentlyRenderedOntology = modelManager.getActiveOntology();
		
		// Create a new schema diagram to work with
		this.schemaDiagram = new SchemaDiagram(modelManager);
		
		// Assign a handler that renders updates from the underlying ontology onto the schema diagram 
		this.updateFromOntologyHandler = new UpdateFromOntologyHandler(schemaDiagram, modelManager);
		
		// Register as listener to detect changes in the ontology that trigger the above updates
		this.modelManager.addOntologyChangeListener(this);
		
		// Parse and render the active ontology initially
		this.RenderActiveOntology();
	}

	/**
	 * Render the currently active ontology. Naïve implementation that simply
	 * pipes all ontology axioms through the UpdateFromOntologyHandler.
	 */
	private void RenderActiveOntology() {
		OWLOntology ontology = modelManager.getActiveOntology();
		ontology.getAxioms().forEach(axiom -> {
			this.updateFromOntologyHandler.handleAddAxiom(axiom, ontology);
		});
	}
	
	/**
	 * Clear the schema diagram and redraw using the currently active ontology
	 */
	private void ClearAndRedraw() {
		this.schemaDiagram.Clear();
		this.RenderActiveOntology();
		this.presentlyRenderedOntology = modelManager.getActiveOntology();
	}
	
	@Override
	public void ontologiesChanged(List<? extends OWLOntologyChange> changes) throws OWLException {
		changes.forEach(change -> {
			if (change.isAxiomChange()) {
				this.updateFromOntologyHandler.handle(change);
			}
		});
	}

	@Override
	public void handleChange(OWLModelManagerChangeEvent event) {
		if (event.isType(EventType.ACTIVE_ONTOLOGY_CHANGED))
		{
			if (!modelManager.getActiveOntology().equals(presentlyRenderedOntology)) {
				this.ClearAndRedraw();
			}
		}
	}
}
