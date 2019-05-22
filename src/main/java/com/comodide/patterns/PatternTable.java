package com.comodide.patterns;

import java.awt.Component;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specialization of JTable specifically intended to list ontology design
 * patterns in the CoModIDE pattern selector view.
 * 
 * @author Karl Hammar <karl@karlhammar.com>
 *
 */
public class PatternTable extends JTable {

	private static final long serialVersionUID = -6533182826250657204L;
	
	private static final Logger log = LoggerFactory.getLogger(PatternTable.class);

	public PatternTable(PatternTableModel patternTableModel) {
		super(patternTableModel);
		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(true);
		getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.setDragEnabled(true);
		this.setTransferHandler(new TransferHandler() {
			private static final long serialVersionUID = -4277997093361110983L;
			
			@Override
			public int getSourceActions(JComponent c) {
			    return COPY;
			}

			@Override
			public Transferable createTransferable(JComponent c) {
				Pattern selectedPattern = ((PatternTableModel) dataModel).getPatternAtRow(getSelectedRow());
				try {
					OWLOntology selectedPatternOntology = PatternLibrary.getInstance().getOwlRepresentation(selectedPattern);
					Set<OWLAxiom> instantiationAxioms = PatternInstantiator.getInstantiationAxioms(selectedPatternOntology);
					Set<OWLAxiom> modularizationAxioms = PatternInstantiator.getModuleAnnotationAxioms(selectedPatternOntology);
					return new PatternTransferable(selectedPattern, instantiationAxioms, modularizationAxioms);
				}
				catch (OWLOntologyCreationException ooce) {
					log.error("The pattern could not be loaded as an OWLAPI OWLOntology: " + ooce.getLocalizedMessage());
					return new StringSelection(String.format("%s: %s", selectedPattern.getLabel(), selectedPattern.getIri().toString()));
				}
			}
			
		});
		
		columnModel.getColumn(1).setCellRenderer(new ButtonRenderer());
		columnModel.getColumn(1).setCellEditor(new ButtonEditor(new JCheckBox()));
	}

	/**
	 * Inner class for rendering buttons in a pattern table. Design from
	 * http://www.java2s.com/Code/Java/Swing-Components/ButtonTableExample.htm
	 * 
	 * @author Karl Hammar <karl@karlhammar.com>
	 *
	 */
	class ButtonRenderer extends JButton implements TableCellRenderer {

		private static final long serialVersionUID = 6502525250976663915L;

		public ButtonRenderer() {
			setOpaque(true);
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (isSelected) {
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			} else {
				setForeground(table.getForeground());
				setBackground(UIManager.getColor("Button.background"));
			}
			setText((value == null) ? "" : value.toString());
			return this;
		}
	}

	/**
	 * Inner class that supports clicking on buttons in pattern tables. Design from
	 * http://www.java2s.com/Code/Java/Swing-Components/ButtonTableExample.htm
	 * 
	 * @author Karl Hammar <karl@karlhammar.com>
	 *
	 */
	class ButtonEditor extends DefaultCellEditor {

		private static final long serialVersionUID = -4417701226982861490L;

		protected JButton button;

		private String label;

		private boolean isPushed;

		public ButtonEditor(JCheckBox checkBox) {
			super(checkBox);
			button = new JButton();
			button.setOpaque(true);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					fireEditingStopped();
				}
			});
		}

		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			if (isSelected) {
				button.setForeground(table.getSelectionForeground());
				button.setBackground(table.getSelectionBackground());
			} else {
				button.setForeground(table.getForeground());
				button.setBackground(table.getBackground());
			}
			label = (value == null) ? "" : value.toString();
			button.setText(label);
			isPushed = true;
			return button;
		}

		public Object getCellEditorValue() {
			if (isPushed) {
				Pattern pattern = ((PatternTableModel) dataModel).getPatternAtRow(getSelectedRow());
				PatternDocumentationFrame docFrame = new PatternDocumentationFrame(pattern);
				docFrame.setVisible(true);
			}
			isPushed = false;
			return new String(label);
		}

		public boolean stopCellEditing() {
			isPushed = false;
			return super.stopCellEditing();
		}

		protected void fireEditingStopped() {
			super.fireEditingStopped();
		}
	}

}
