/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * Microclimate explorer view.  Shows connections to Microclimate instances and
 * the applications for each instance.
 */
public class MicroclimateExplorerView extends CommonNavigator {
    public static final String VIEW_ID = "com.ibm.microclimate.ui.explorerView";

    @Override
    protected CommonViewer createCommonViewerObject(Composite parent) {
        CommonViewer viewer = super.createCommonViewerObject(parent);
        viewer.setAutoExpandLevel(2);
        return viewer;
    }
}
