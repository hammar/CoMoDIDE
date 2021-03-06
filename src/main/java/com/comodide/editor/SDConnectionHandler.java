package com.comodide.editor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mxgraph.swing.handler.mxConnectPreview;
import com.mxgraph.swing.handler.mxConnectionHandler;

public class SDConnectionHandler extends mxConnectionHandler
{
	/** Bookkeeping */
	private final Logger log = LoggerFactory.getLogger(SDConnectionHandler.class);
	private final String pf  = "[CoModIDE:SDConnectionHandler] ";
	
	public SDConnectionHandler(SchemaDiagramComponent sdComponent)
	{
		super(sdComponent);
		log.info(pf + "SDConnectionHandler Initialized.");
	}
	
	public mxConnectPreview createConnectPreview()
	{
		return new SDConnectPreview(graphComponent);
	}
}
