package com.comodide.editor;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.util.mxGraphActions;

/**
 * @author Administrator
 * 
 */
public class EditorKeyboardHandler extends mxKeyboardHandler
{

	/**
	 * The constructor for this class.
	 * 
	 * @param graphComponent is the super class of this class.
	 */
	public EditorKeyboardHandler(mxGraphComponent graphComponent)
	{
		super(graphComponent);
	}

	/**
	 * Return minimal input map binding delete and backspace keys to ActionMapKey "delete".
	 */
	protected InputMap getInputMap(int condition)
	{
		InputMap map = null;

		if (condition == JComponent.WHEN_FOCUSED)
		{
			map = new InputMap();
			
			map.put(KeyStroke.getKeyStroke("DELETE"), "delete");
			map.put(KeyStroke.getKeyStroke("BACK_SPACE"), "delete");
		}

		return map;
	}
	
	
	/**
	 * Return minimal mapping between ActionMapKeys and actions.
	 */
	protected ActionMap createActionMap()
	{
		ActionMap map = (ActionMap) UIManager.get("ScrollPane.actionMap");

		map.put("delete", mxGraphActions.getDeleteAction());

		return map;
	}

}