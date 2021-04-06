package com.comodide.editor;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxEdgeLabelLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.mxOrganicLayout;
import com.mxgraph.layout.mxParallelEdgeLayout;
import com.mxgraph.layout.mxPartitionLayout;
import com.mxgraph.layout.mxStackLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.swing.util.mxMorphing;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxResources;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.util.mxUndoableEdit;
import com.mxgraph.util.mxUndoableEdit.mxUndoableChange;
import com.mxgraph.view.mxGraph;

public class BasicGraphEditor extends JPanel
{
	/** Bookkeeping */
	private static final long serialVersionUID = -6561623072112577140L;

	/** Adds required resources for i18n */
	static
	{
		try
		{
			mxResources.add("resources/editor");
		}
		catch (Exception e)
		{
			// ignore
			e.printStackTrace();
		}
	}

	protected mxGraphComponent  graphComponent;
	protected mxGraphOutline    graphOutline;
	protected JTabbedPane       libraryPane;
	protected mxUndoManager     undoManager;
	protected JLabel            statusBar;
	protected File              currentFile;
	/** Flag indicating whether the current graph has been modified */
	protected boolean           modified = false;
	protected mxRubberband      rubberband;
	protected mxKeyboardHandler keyboardHandler;

	/**
	 * Listens for when the user presses undo and implements an invoke method.
	 * Invoke calls the undo manager to handle the event.
	 */
	protected mxIEventListener undoHandler = new mxIEventListener()
	{
		public void invoke(Object source, mxEventObject evt)
		{
			undoManager.undoableEditHappened((mxUndoableEdit) evt.getProperty("edit"));
		}
	};

	/**
	 * Listens for when the user changes a tracker and implements an invoke method.
	 * Invoke passes the method, <code>setModified</code>, the value <code>true</code>.
	 */
	protected mxIEventListener changeTracker = new mxIEventListener()
	{
		public void invoke(Object source, mxEventObject evt)
		{
			setModified(true);
		}
	};

	/**
	 * The constructor for this class.
	 * 
	 * @param component sets the state of <code>graphCompontent</code>.
	 */
	public BasicGraphEditor(mxGraphComponent component)
	{
		// Stores a reference to the graph and creates the command history
		graphComponent = component;
		final mxGraph graph = graphComponent.getGraph();
		undoManager = createUndoManager();

		// Do not change the scale and translation after files have been loaded
		graph.setResetViewOnRootChange(false);

		// Updates the modified flag if the graph model changes
		graph.getModel().addListener(mxEvent.CHANGE, changeTracker);

		// Adds the command history to the model and view
		graph.getModel().addListener(mxEvent.UNDO, undoHandler);
		graph.getView().addListener(mxEvent.UNDO, undoHandler);

		// Keeps the selection in sync with the command history
		mxIEventListener undoHandler = new mxIEventListener()
		{
			public void invoke(Object source, mxEventObject evt)
			{
				List<mxUndoableChange> changes = ((mxUndoableEdit) evt.getProperty("edit")).getChanges();
				graph.setSelectionCells(graph.getSelectionCellsForChanges(changes));
			}
		};

		undoManager.addListener(mxEvent.UNDO, undoHandler);
		undoManager.addListener(mxEvent.REDO, undoHandler);

		// Creates the graph outline component
		graphOutline = new mxGraphOutline(graphComponent);

		// Creates the library pane that contains the tabs with the palettes
		libraryPane = new JTabbedPane();

		// Creates the inner split pane that contains the library with the
		// palettes and the graph outline on the left side of the window
		JSplitPane inner = new JSplitPane(JSplitPane.VERTICAL_SPLIT, libraryPane, graphOutline);
		inner.setDividerLocation(320);
		inner.setResizeWeight(1);
		inner.setDividerSize(6);
		inner.setBorder(null);

		// Creates the outer split pane that contains the inner split pane and
		// the graph component on the right side of the window
		JSplitPane outer = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inner, graphComponent);
		outer.setOneTouchExpandable(true);
		outer.setDividerLocation(230);
		outer.setDividerSize(6);
		outer.setBorder(null);

		// Creates the status bar
		statusBar = createStatusBar();

		// Display some useful information about repaint events
		installRepaintListener();

		// Puts everything together
		setLayout(new BorderLayout());
		add(outer, BorderLayout.CENTER);
		add(statusBar, BorderLayout.SOUTH);
		installToolBar();

		// Installs rubberband selection and handling for some special
		// keystrokes such as F2, Control-C, -V, X, A etc.
		installHandlers();
		installListeners();
	}

	/** Returns a new undo manager. */
	protected mxUndoManager createUndoManager()
	{
		return new mxUndoManager();
	}

	/**	Sets rubberband and keyboard handler to new instances. */
	protected void installHandlers()
	{
		rubberband = new mxRubberband(graphComponent);
		keyboardHandler = new EditorKeyboardHandler(graphComponent);
	}

	/**	TODO Finish this method!
	 * <p>
	 * Adds a new <code>EditorToolBar</code>
	 */
	protected void installToolBar()
	{
//		add(new EditorToolBar(this, JToolBar.HORIZONTAL), BorderLayout.NORTH);
	}

	/**	
	 * Initializes a new status bar to the ready state and sets it's boarder.
	 * 
	 * @return Returns the new status bar. 
	 */
	protected JLabel createStatusBar()
	{
		JLabel statusBar = new JLabel(mxResources.get("ready"));
		statusBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		return statusBar;
	}

	/**
	 * Constructs an event listener that will repaint a specified region in the graph compontent.
	 * This implements an Invoke method that finds the region to repaint, and repaints it.
	 * <p>
	 * If the region is unspecified then everything is repainted.
	 */
	protected void installRepaintListener()
	{
		graphComponent.getGraph().addListener(mxEvent.REPAINT, new mxIEventListener()
		{
			public void invoke(Object source, mxEventObject evt)
			{
				String      buffer = (graphComponent.getTripleBuffer() != null) ? "" : " (unbuffered)";
				mxRectangle dirty  = (mxRectangle) evt.getProperty("region");

				if (dirty == null)
				{
					status("Repaint all" + buffer);
				}
				else
				{
					status("Repaint: x=" + (int) (dirty.getX()) + " y=" + (int) (dirty.getY()) + " w="
							+ (int) (dirty.getWidth()) + " h=" + (int) (dirty.getHeight()) + buffer);
				}
			}
		});
	}

	/**
	 * This adds a new palette with the given title to the library pane. It also listens when for the library pane is resized,
	 * and implements a method, <code>componentResized</code>, to update the widths of all of the palettets in the pane.
	 * 
	 * @return Returns the new palette.
	 */
	public EditorPalette insertPalette(String title)
	{
		final EditorPalette palette    = new EditorPalette();
		final JScrollPane   scrollPane = new JScrollPane(palette);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		libraryPane.add(title, scrollPane);

		// Updates the widths of the palettes if the container size changes
		libraryPane.addComponentListener(new ComponentAdapter()
		{
			public void componentResized(ComponentEvent e)
			{
				int w = scrollPane.getWidth() - scrollPane.getVerticalScrollBar().getWidth();
				palette.setPreferredWidth(w);
			}

		});

		return palette;
	}

	/** 
	 * This method adjusts the scale of the graph when the mouse wheel is moved. The status is updated to show the new scale
	 * of the graph as a percentage.
	 */
	protected void mouseWheelMoved(MouseWheelEvent e)
	{
		if (e.getWheelRotation() < 0)
		{
			graphComponent.zoomIn();
		}
		else
		{
			graphComponent.zoomOut();
		}

		status(mxResources.get("scale") + ": " + (int) (100 * graphComponent.getGraph().getView().getScale()) + "%");
	}

	/**
	 * Constructs a new popup menu from three action listeners. The first listens for the fit of the page, the second
	 * listens for when to draw the labels, and the third listens for buffering. The three listeners are added to the new
	 * popup menu, and the menu is displayed.
	 */
	protected void showOutlinePopupMenu(MouseEvent e)
	{
		Point             pt   = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), graphComponent);
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(mxResources.get("magnifyPage"));
		item.setSelected(graphOutline.isFitPage());

		item.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				graphOutline.setFitPage(!graphOutline.isFitPage());
				graphOutline.repaint();
			}
		});

		JCheckBoxMenuItem item2 = new JCheckBoxMenuItem(mxResources.get("showLabels"));
		item2.setSelected(graphOutline.isDrawLabels());

		item2.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				graphOutline.setDrawLabels(!graphOutline.isDrawLabels());
				graphOutline.repaint();
			}
		});

		JCheckBoxMenuItem item3 = new JCheckBoxMenuItem(mxResources.get("buffering"));
		item3.setSelected(graphOutline.isTripleBuffered());

		item3.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				graphOutline.setTripleBuffered(!graphOutline.isTripleBuffered());
				graphOutline.repaint();
			}
		});

		JPopupMenu menu = new JPopupMenu();
		menu.add(item);
		menu.add(item2);
		menu.add(item3);
		menu.show(graphComponent, pt.x, pt.y);

		e.consume();
	}

	/**
	 * 
	 */
	/*
	 * protected void showGraphPopupMenu(MouseEvent e) { Point pt =
	 * SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), graphComponent);
	 * EditorPopupMenu menu = new EditorPopupMenu(BasicGraphEditor.this);
	 * menu.show(graphComponent, pt.x, pt.y);
	 * 
	 * e.consume(); }
	 */

	/** Updates status to reflect the current x and y positions of the cursor. */
	protected void mouseLocationChanged(MouseEvent e)
	{
		status(e.getX() + ", " + e.getY());
	}

	/**  
	 * This installs listeners for mouse related event such as wheel movement and clicking. Each listener is added
	 * to this graph outline and this graph component.
	 */
	protected void installListeners()
	{
		// Installs mouse wheel listener for zooming
		MouseWheelListener wheelTracker = new MouseWheelListener()
		{
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				if (e.getSource() instanceof mxGraphOutline || e.isControlDown())
				{
					BasicGraphEditor.this.mouseWheelMoved(e);
				}
			}

		};

		// Handles mouse wheel events in the outline and graph component
		graphOutline.addMouseWheelListener(wheelTracker);
		graphComponent.addMouseWheelListener(wheelTracker);

		// Installs the popup menu in the outline
		graphOutline.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				// Handles context menu on the Mac where the trigger is on mousepressed
				mouseReleased(e);
			}

			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showOutlinePopupMenu(e);
				}
			}

		});

		// Installs the popup menu in the graph component
		graphComponent.getGraphControl().addMouseListener(new MouseAdapter()
		{

			public void mousePressed(MouseEvent e)
			{
				// Handles context menu on the Mac where the trigger is on mousepressed
				mouseReleased(e);
			}

			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
//					showGraphPopupMenu(e);
				}
			}

		});

		// Installs a mouse motion listener to display the mouse location
		graphComponent.getGraphControl().addMouseMotionListener(new MouseMotionListener()
		{

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
			 */
			public void mouseDragged(MouseEvent e)
			{
				mouseLocationChanged(e);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
			 */
			public void mouseMoved(MouseEvent e)
			{
				mouseDragged(e);
			}

		});
	}

	/** 
	 * Updates the current and given files so that the current file is the old file and the given file is the current file.
	 * <code>firePropertyChange</code> is notified that a bound property has been changed.
	 * @param file the new current file.
	 */
	public void setCurrentFile(File file)
	{
		File oldValue = currentFile;
		currentFile = file;

		firePropertyChange("currentFile", oldValue, file);
	}

	/** Returns a refence to the current file. */
	public File getCurrentFile()
	{
		return currentFile;
	}

	/**
	 * Updates the current and given values of <code>this.modified</code> so that the current value of modified is the old value,
	 * and the given value of modified is the current value. <code>firePropertyChange</code> is notified that a bound 
	 * property has been changed.
	 * 
	 * @param modified the new value of <code>this.modified</code>.
	 */
	public void setModified(boolean modified)
	{
		boolean oldValue = this.modified;
		this.modified = modified;

		firePropertyChange("modified", oldValue, modified);
	}

	/** Returns true if this has been modified. */
	public boolean isModified()
	{
		return modified;
	}

	/** Returns a reference to the graph component. */
	public mxGraphComponent getGraphComponent()
	{
		return graphComponent;
	}

	/** Returns a reference to the graph outline. */
	public mxGraphOutline getGraphOutline()
	{
		return graphOutline;
	}

	/** Returns a reference to the library pane. */
	public JTabbedPane getLibraryPane()
	{
		return libraryPane;
	}

	/** Returns a reference to the undo manager. */
	public mxUndoManager getUndoManager()
	{
		return undoManager;
	}

	/**
	 * Constructs an instance of a binding given the name and the action.
	 * 
	 * @param name is the name of the action
	 * @param action is the action being bound to the given name
	 * @return a new Action bound to the specified string name
	 */
	public Action bind(String name, final Action action)
	{
		return bind(name, action, null);
	}

	/**
	 * Binds the action argument to the given name and icon. This is done by constructing a new action and 
	 * passing name and icon as arguments. A new method is implemented to call <code>action.actionPerformed(...)</code>.
	 * 
	 * @param name is the name of the action
	 * @param action calls action performed method and provides the short description for the new action.
	 * @param iconUrl is passed to the new action. If it is null, then the new action's icon will also be null.
	 * @return a new Action bound to the specified string name and icon
	 */
	@SuppressWarnings("serial")
	public Action bind(String name, final Action action, String iconUrl)
	{
		AbstractAction newAction = new AbstractAction(name,
				(iconUrl != null) ? new ImageIcon(BasicGraphEditor.class.getResource(iconUrl)) : null)
		{
			public void actionPerformed(ActionEvent e)
			{
				action.actionPerformed(new ActionEvent(getGraphComponent(), e.getID(), e.getActionCommand()));
			}
		};

		newAction.putValue(Action.SHORT_DESCRIPTION, action.getValue(Action.SHORT_DESCRIPTION));

		return newAction;
	}

	/**
	 * Updates the text of this status label to string msg.
	 * 
	 * @param msg is the new text for the status bar label
	 */
	public void status(String msg)
	{
		statusBar.setText(msg);
	}

	/*
	 * public void about() { JFrame frame = (JFrame)
	 * SwingUtilities.windowForComponent(this);
	 * 
	 * if (frame != null) { EditorAboutFrame about = new EditorAboutFrame(frame);
	 * about.setModal(true);
	 * 
	 * // Centers inside the application frame int x = frame.getX() +
	 * (frame.getWidth() - about.getWidth()) / 2; int y = frame.getY() +
	 * (frame.getHeight() - about.getHeight()) / 2; about.setLocation(x, y);
	 * 
	 * // Shows the modal dialog and waits about.setVisible(true); } }
	 */

	/** This method disposes of the current frame. */
	public void exit()
	{
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(this);

		if (frame != null)
		{
			frame.dispose();
		}
	}

	/**
	 * This calls the UI manager to set the look and feel to the given string clazz. The frame component is updated, and
	 * <code>keyboardHandler</code> is set to a new instance.
	 * <p>
	 * The UI manager will throw an exeption if the given string clazz is not a valid class name. This method will catch and
	 * print the exception and it's backtrace..
	 * 
	 * @param clazz is the name of a class that implements the look and feel.
	 */
	public void setLookAndFeel(String clazz)
	{
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(this);

		if (frame != null)
		{
			try
			{
				UIManager.setLookAndFeel(clazz);
				SwingUtilities.updateComponentTreeUI(frame);

				// Needs to assign the key bindings again
				keyboardHandler = new EditorKeyboardHandler(graphComponent);
			}
			catch (Exception e1)
			{
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Generates a frame with the given menu bar. 
	 * 
	 * @param menuBar is the menu bar for the new frame.
	 * @return Returns the new frame.
	 */
	public JFrame createFrame(JMenuBar menuBar)
	{
		JFrame frame = new JFrame();
		frame.getContentPane().add(this);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.setJMenuBar(menuBar);
		frame.setSize(870, 640);

		return frame;
	}

	/**
	 * Creates an action that executes the specified layout.
	 * 
	 * @param key Key to be used for getting the label from mxResources and also to
	 *            create the layout instance for the commercial graph editor
	 *            example.
	 * @return an action that executes the specified layout
	 */
	@SuppressWarnings("serial")
	public Action graphLayout(final String key, boolean animate)
	{
		final mxIGraphLayout layout = createLayout(key, animate);

		if (layout != null)
		{
			return new AbstractAction(mxResources.get(key))
			{
				public void actionPerformed(ActionEvent e)
				{
					final mxGraph graph = graphComponent.getGraph();
					Object        cell  = graph.getSelectionCell();

					if (cell == null || graph.getModel().getChildCount(cell) == 0)
					{
						cell = graph.getDefaultParent();
					}

					graph.getModel().beginUpdate();
					try
					{
						long t0 = System.currentTimeMillis();
						layout.execute(cell);
						status("Layout: " + (System.currentTimeMillis() - t0) + " ms");
					}
					finally
					{
						mxMorphing morph = new mxMorphing(graphComponent, 20, 1.2, 20);

						morph.addListener(mxEvent.DONE, new mxIEventListener()
						{

							public void invoke(Object sender, mxEventObject evt)
							{
								graph.getModel().endUpdate();
							}

						});

						morph.startAnimation();
					}

				}

			};
		}
		else
		{
			return new AbstractAction(mxResources.get(key))
			{

				public void actionPerformed(ActionEvent e)
				{
					JOptionPane.showMessageDialog(graphComponent, mxResources.get("noLayout"));
				}

			};
		}
	}

	/**
	 * Creates a layout instance for the given identifier.
	 * <p>
	 * If the given identifier is not provided, then null is returned.
	 * 
	 * @param ident the identifier that will determine the layout
	 * @parm animate TODO this param is unused in this function
	 */
	protected mxIGraphLayout createLayout(String ident, boolean animate)
	{
		mxIGraphLayout layout = null;

		if (ident != null)
		{
			mxGraph graph = graphComponent.getGraph();

			if (ident.equals("verticalHierarchical"))
			{
				layout = new mxHierarchicalLayout(graph);
			}
			else if (ident.equals("horizontalHierarchical"))
			{
				layout = new mxHierarchicalLayout(graph, JLabel.WEST);
			}
			else if (ident.equals("verticalTree"))
			{
				layout = new mxCompactTreeLayout(graph, false);
			}
			else if (ident.equals("horizontalTree"))
			{
				layout = new mxCompactTreeLayout(graph, true);
			}
			else if (ident.equals("parallelEdges"))
			{
				layout = new mxParallelEdgeLayout(graph);
			}
			else if (ident.equals("placeEdgeLabels"))
			{
				layout = new mxEdgeLabelLayout(graph);
			}
			else if (ident.equals("organicLayout"))
			{
				layout = new mxOrganicLayout(graph);
			}
			if (ident.equals("verticalPartition"))
			{
				layout = new mxPartitionLayout(graph, false)
				{
					/**
					 * Overrides the empty implementation to return the size of the graph control.
					 */
					public mxRectangle getContainerSize()
					{
						return graphComponent.getLayoutAreaSize();
					}
				};
			}
			else if (ident.equals("horizontalPartition"))
			{
				layout = new mxPartitionLayout(graph, true)
				{
					/**
					 * Overrides the empty implementation to return the size of the graph control.
					 */
					public mxRectangle getContainerSize()
					{
						return graphComponent.getLayoutAreaSize();
					}
				};
			}
			else if (ident.equals("verticalStack"))
			{
				layout = new mxStackLayout(graph, false)
				{
					/**
					 * Overrides the empty implementation to return the size of the graph control.
					 */
					public mxRectangle getContainerSize()
					{
						return graphComponent.getLayoutAreaSize();
					}
				};
			}
			else if (ident.equals("horizontalStack"))
			{
				layout = new mxStackLayout(graph, true)
				{
					/**
					 * Overrides the empty implementation to return the size of the graph control.
					 */
					public mxRectangle getContainerSize()
					{
						return graphComponent.getLayoutAreaSize();
					}
				};
			}
			else if (ident.equals("circleLayout"))
			{
				layout = new mxCircleLayout(graph);
			}
		}

		return layout;
	}
}
