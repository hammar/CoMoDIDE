/**
 * This code is adapted from the Graph Editor example provided by JGraph Ltd
 * https://github.com/jgraph/jgraphx
 * Copyright (c) 2006-2012, JGraph Ltd 
 * */
package com.comodide.editor;

import java.awt.Color;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import com.comodide.editor.model.ClassCell;
import com.comodide.editor.model.DatatypeCell;
import com.comodide.editor.model.PropertyEdgeCell;
import com.comodide.editor.model.SubClassEdgeCell;
import com.mxgraph.swing.util.mxGraphTransferable;
import com.mxgraph.swing.util.mxSwingConstants;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraph;

public class ComodideEditor extends BasicGraphEditor
{
    private static final long serialVersionUID = -4601740824088314699L;
    
    /**
     * The constructor for this class.
     * 
     * @param component is the super class of this class.
     */
    public ComodideEditor(SchemaDiagramComponent component)
    {
        super(component);
        final mxGraph graph = graphComponent.getGraph();

        // Creates the shapes palette
        EditorPalette coreConstructsPalette = insertPalette("Core constructs");
        EditorPalette xsdTypesPalette = insertPalette("XSD datatypes");

        // Sets the edge template to be used for creating new edges if an edge
        // is clicked in the shape palette
        coreConstructsPalette.addListener(mxEvent.SELECT, new mxIEventListener()
        {
            public void invoke(Object sender, mxEventObject evt)
            {
                Object tmp = evt.getProperty("transferable");

                if (tmp instanceof mxGraphTransferable)
                {
                    mxGraphTransferable t = (mxGraphTransferable) tmp;
                    Object cell = t.getCells()[0];

                    if (graph.getModel().isEdge(cell))
                    {
                        ((SchemaDiagram) graph).setEdgeTemplate(cell);
                    }
                }
            }

        });

        /*
         * Add some template cells for dropping into the graph. 
         * rounded rectangle: class
         * ellipse: datatype 
         * arrow: relation
         */
        /*shapesPalette // This shape could probably in the future be used for allocating modules/patterns
                .addTemplate("Container", new ImageIcon(GraphEditor.class.getResource("/images/swimlane.png")),
                        "swimlane", 280, 280, "Container");*/
        coreConstructsPalette // class
                .addTemplate("Class", new ImageIcon(ComodideEditor.class.getResource("/images/rounded.png")),
                        new ClassCell());
        coreConstructsPalette // relation
                .addTemplate("Property", new ImageIcon(ComodideEditor.class.getResource("/images/straight.png")),
                        new PropertyEdgeCell());
        coreConstructsPalette // subclass
                .addTemplate("Subclass", new ImageIcon(ComodideEditor.class.getResource("/images/arrow.png")), 
                		new SubClassEdgeCell());
        
        // Factory for creating templates
        OWLDataFactory df = OWLManager.getOWLDataFactory();
        
        // xsd:string
        OWLDatatype xsdString = df.getOWLDatatype(XSDVocabulary.STRING.getIRI()); 
        xsdTypesPalette
        		.addTemplate("string", new ImageIcon(ComodideEditor.class.getResource("/images/ellipse.png")), 
        			new DatatypeCell(xsdString, 0.0, 0.0));
        
        // xsd:int
        OWLDatatype xsdInt = df.getOWLDatatype(XSDVocabulary.INT.getIRI());
        xsdTypesPalette
				.addTemplate("int", new ImageIcon(ComodideEditor.class.getResource("/images/ellipse.png")),
					new DatatypeCell(xsdInt, 0.0, 0.0));
        
        // xsd:float
        OWLDatatype xsdFloat = df.getOWLDatatype(XSDVocabulary.FLOAT.getIRI());
        xsdTypesPalette
			.addTemplate("float", new ImageIcon(ComodideEditor.class.getResource("/images/ellipse.png")),
					new DatatypeCell(xsdFloat, 0.0, 0.0));
        
        // xsd:float
        OWLDatatype xsdBoolean = df.getOWLDatatype(XSDVocabulary.BOOLEAN.getIRI());
        xsdTypesPalette
			.addTemplate("boolean", new ImageIcon(ComodideEditor.class.getResource("/images/ellipse.png")), 
					new DatatypeCell(xsdBoolean, 0.0, 0.0));
        
        // xsd:dateTime
        OWLDatatype xsdDateTime = df.getOWLDatatype(XSDVocabulary.DATE_TIME.getIRI());
        xsdTypesPalette
			.addTemplate("dateTime", new ImageIcon(ComodideEditor.class.getResource("/images/ellipse.png")), 
					new DatatypeCell(xsdDateTime, 0.0, 0.0));
    }

	/** For debugging purposes */
    public static void main(String[] args)
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e1)
        {
            e1.printStackTrace();
        }

        mxSwingConstants.SHADOW_COLOR = Color.LIGHT_GRAY;
        mxConstants.W3C_SHADOWCOLOR = "#D3D3D3";

        ComodideEditor editor = new ComodideEditor(new SchemaDiagramComponent(null, null));
        editor.createFrame(null).setVisible(true);
    }
}
