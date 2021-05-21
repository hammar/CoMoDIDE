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

/**
 * (TODO)
 * 
 * @author cogan
 *
 */

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
	 * When an undo event occurs, the wrapped invoke method is passed the source of the event, and the event that occurred.
	 * The invoke method will then pass <code>undoManager.undoableEditHappened</code> the property of the given event.
	 */
	protected mxIEventListener undoHandler = new mxIEventListener()
	{
		public void invoke(Object source, mxEventObject evt)
		{
			undoManager.undoableEditHappened((mxUndoableEdit) evt.getProperty("edit"));
		}
	};

	/**
	 * When an change tracker event occurs, the wrapped invoke method is passed the source of the event, and the event that 
	 * occurred. The invoke method will then pass the <code>setModified</code> method the value <code>true</code>.
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
	 * @param component sets the state of the graphCompontent.
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

	/**
	 * Instantiates a new <code>mxUndoManager</code>.
	 * 
	 * @return Returns a new undo manager. 
	 */
	protected mxUndoManager createUndoManager()
	{
		return new mxUndoManager();
	}

	/**	
	 * Sets this rubber band and keyboard handler to new instances. The constructors of the new instances are passed
	 * a reference to the graph component.
	 */
	protected void installHandlers()
	{
		rubberband = new mxRubberband(graphComponent);
		keyboardHandler = new EditorKeyboardHandler(graphComponent);
	}

	/**	TODO Finish this method!
	 * This method calls the <code>add</code> method to add a new <code>EditorToolBar</code> to the north boarder of the panel. 
	 * The constructor of the new tool bar is passed a reference to this class and <code>JToolBar.Horizontal</code>. 
	 */
	protected void installToolBar()
	{
//		add(new EditorToolBar(this, JToolBar.HORIZONTAL), BorderLayout.NORTH);
	}

	/**
	 * Initializes a new <code>JLabel</code> to display a ready message. The boarder of the status bar is set to be a 2 pixel
	 * by 4 pixel box.
	 * 
	 * @return Returns the new label. 
	 */
	protected JLabel createStatusBar()
	{
		JLabel statusBar = new JLabel(mxResources.get("ready"));
		statusBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		return statusBar;
	}

	/** 
	 * This method gets the graph from the graph component and adds an event listener. The <code>addListener</code> method, from
	 * the <code>mxGraph</code> class, is passed the repaint event and a new <code>mxIEventListener</code>.
	 * <p>
	 * The new <code>mxIEventListener</code> wraps an invoke method that takes a source and an event. The invoke method
	 * generates a string buffer and unpacks the region property from the event. If the unpacked region is null, then the
	 * <code>status</code> method is passed the string "Repaint All" plus the buffer. 
	 * If the unpacked region is not null, then a string containing its x and y coordinates, width, and
	 * height is passed to the <code>status</code> method.
	 * 
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
	 * This method instantiates a new <code>EditorPalette</code> and a new <code>JScrollPane</code> when the constructor is 
	 * passed the new palette, both objects are closed for modification. The scroll pane's vertical scroll bar policy is set to
	 * always, and the horizontal scroll bar policy is set to never. This <code>libraryPane.add</code> is passed the given title 
	 * and scroll pane.
	 * <p>
	 * <code>libraryPane.addComponentListener</code> is passed a new <code>ComonentAdapter</code>. The new 
	 * <code>ComonentAdapter</code> is a adapter wrapper for the <code>componentResized</code> method. The wrapped method sets
	 * the preferred width of the palette to the width of the scroll pane minus the width of the vertical scroll bar.
	 * 
	 * @param title is the title to be displayed on the library pane tab.
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
	 * When the mouse wheel is rotated, this method determines whether to zoom in or out from the graph from the graph component.
	 * Once the zoom has been made, the <code>status</code> method is passed the new scale of the graph as a string.
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
	 * This method stores a point constructed from the position of the mouse on the graph component as a local variable.
	 * This also constructs a new pop-up menu for three check boxes. The first check box magnifies the page, the second is for 
	 * displaying the labels, and the third is for buffering. A listener is added to each check box to determine when a box had 
	 * been checked or unchecked. After each listener is attached, the check boxes are added to the menu, and the menu is shown 
	 * at the xy position of the point variable.
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

	/**
	 * Passes the <code>status</code> method the current x and y positions of the cursor when it's position has changed.
	 */
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
	 * 
	 * @param file the new current file.
	 */
	public void setCurrentFile(File file)
	{
		File oldValue = currentFile;
		currentFile = file;

		firePropertyChange("currentFile", oldValue, file);
	}

	/** @return Returns a reference to the current file. */
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

	/** @return Returns true if this graph has been modified. */
	public boolean isModified()
	{
		return modified;
	}

	/** @return Returns a reference to this graph component. */
	public mxGraphComponent getGraphComponent()
	{
		return graphComponent;
	}

	/** @return Returns a reference to this graph outline. */
	public mxGraphOutline getGraphOutline()
	{
		return graphOutline;
	}

	/** @return Returns a reference to this library pane. */
	public JTabbedPane getLibraryPane()
	{
		return libraryPane;
	}

	/** @return Returns a reference to this undo manager. */
	public mxUndoManager getUndoManager()
	{
		return undoManager;
	}

	/**
	 * This bind method accepts two arguments, a new and an unmodifiable action. The arguments are passed to the bind method
	 * that accepts three arguments, with <code>null</code> being the third value.
	 * 
	 * @param name is the name being bound action
	 * @param action is an unmodifiable action
	 * @return a new Action bound to the given string name
	 */
	public Action bind(String name, final Action action)
	{
		return bind(name, action, null);
	}

	/** 
	 * A new abstract action's constructor is passed the given name and a new <code>ImageIcon</code>, if the given icon URL is 
	 * not null. The new action wraps the <code>actionPerformed</code> method, which takes in an action event. The 
	 * <code>actionPerformed</code> method invokes and passes <code>action.actionPerformed</code> a new Action event.
	 * 
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
	 * Updates the text of this status label to the given message.
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

	/** This method disposes of the current window. */
	public void exit()
	{
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(this);

		if (frame != null)
		{
			frame.dispose();
		}
	}

	/** 
	 * This method calls the UI manager to set the look and feel to the given string clazz. The frame component is updated, and
	 * <code>keyboardHandler</code> is set to a new instance.
	 * <p>
	 * The UI manager will throw an exeption if the given string clazz is not a valid class name. This method will catch and
	 * print the exception and it's backtrace..
	 * 
	 * @param clazz is the name of the class that implements the look and feel.
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
	 * This method creates a new frame where this class is added to the content pane, the default close operation is set to 
	 * exit on close, and the given menu bar is set to be the menu bar of the frame.
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
	 * The method, <code>createLayout</code> is passed the given string key and boolean animate. It's result is stored as an
	 * unmodifiable layout variable. If the layout variable is not null, then this method will return a new abstract action.
	 * <p>
	 * The new abstract action acts as a wrapper for the action performed method. When this method is called it creates two
	 * variables, an unmodifiable graph and an object to represent a selected cell. If the selected cell is null or the cell has
	 * no children, it is set as a default parent cell. The graph is updated, and then this method will try to update the layout
	 * for any children the selected cell has. Finally, a new morphing instance is created for the graph. An event listener is
	 * attached to the morphing, and the animation is started.
	 * <p>
	 * This method handles when there is no layout by displaying a no layout message.
	 * 
	 * @param key Key to be used for getting the label from mxResources and also to
	 *            create the layout instance for the commercial graph editor
	 *            example.
	 * @param animate is a boolean value passed to the create layout method.
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
	 * This method determines the layout specified by the given string ident, and sets the sets the local layout  variable to
	 * a new instance of that layout. If ident is equal to <code>verticalPartition</code>, <code>horizontalPartition</code>, 
	 * <code>verticalStack</code>, or <code>horizontalStack</code>, then the empty implementation to return the size of the graph 
	 * control will be overrode.
	 * <p>
	 * If the given identifier is not provided, then null is returned.
	 * 
	 * @param ident the identifier that will determine the layout
	 * @param animate (TODO)
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
