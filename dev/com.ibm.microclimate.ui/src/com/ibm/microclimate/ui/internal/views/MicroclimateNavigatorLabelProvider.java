/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.views;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;

import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.BuildStatus;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Label provider for the Microclimate view.
 */
public class MicroclimateNavigatorLabelProvider extends LabelProvider implements IStyledLabelProvider {

	static final Styler BOLD_FONT_STYLER = new BoldFontStyler();
	
	public static final Styler ERROR_STYLER = StyledString.createColorRegistryStyler(
			JFacePreferences.ERROR_COLOR, null);
	
	@Override
	public String getText(Object element) {
		if (element instanceof MicroclimateConnection) {
			MicroclimateConnection connection = (MicroclimateConnection)element;
			String text = Messages.MicroclimateConnectionLabel + " " + connection.baseUrl;
			if (!connection.isConnected()) {
				String errorMsg = connection.getConnectionErrorMsg();
				if (errorMsg == null) {
					errorMsg = Messages.MicroclimateDisconnected;
				}
				text = text + " (" + errorMsg + ")";
			} else if (connection.getApps().size() == 0) {
				text = text + " (" + Messages.MicroclimateConnectionNoProjects + ")";
			}
			return text;
		} else if (element instanceof MicroclimateApplication) {
			MicroclimateApplication app = (MicroclimateApplication)element;
			StringBuilder builder = new StringBuilder(app.name);
			
			if (app.isEnabled()) {
				AppState appState = app.getAppState();
				String displayString = appState.getDisplayString(app.getStartMode());
				builder.append(" [" + displayString + "]");
				
				BuildStatus buildStatus = app.getBuildStatus();
				String buildDetails = app.getBuildDetails();
				if (buildDetails != null && !buildDetails.isEmpty()) {
					builder.append(" [" + buildStatus.getDisplayString() + ": " + buildDetails + "]");
				} else {
					builder.append(" [" + buildStatus.getDisplayString() + "]");
				}
			} else {
				builder.append(" [" + Messages.MicroclimateProjectDisabled + "]");
			}
			return builder.toString();
		}
		return super.getText(element);
	}

	@Override
	public StyledString getStyledText(Object element) {
		StyledString styledString;
		if (element instanceof MicroclimateConnection) {
			MicroclimateConnection connection = (MicroclimateConnection)element;
			styledString = new StyledString(Messages.MicroclimateConnectionLabel + " " );
			styledString.append(connection.baseUrl.toString(), StyledString.QUALIFIER_STYLER);
			if (!connection.isConnected()) {
				String errorMsg = connection.getConnectionErrorMsg();
				if (errorMsg == null) {
					errorMsg = Messages.MicroclimateDisconnected;
				}
				styledString.append(" (" + errorMsg + ")", ERROR_STYLER);
			} else if (connection.getApps().size() == 0) {
				styledString.append(" (" + Messages.MicroclimateConnectionNoProjects + ")", StyledString.DECORATIONS_STYLER);
			}
		} else if (element instanceof MicroclimateApplication) {
			MicroclimateApplication app = (MicroclimateApplication)element;
			styledString = new StyledString(app.name);
			
			if (app.isEnabled()) {
				AppState appState = app.getAppState();
				String displayString = appState.getDisplayString(app.getStartMode());
				styledString.append(" [" + displayString + "]", StyledString.DECORATIONS_STYLER);
				
				BuildStatus buildStatus = app.getBuildStatus();
				String buildDetails = app.getBuildDetails();
				if (buildDetails != null) {
					styledString.append(" [" + buildStatus.getDisplayString() + ": ", StyledString.DECORATIONS_STYLER);
					styledString.append(buildDetails, StyledString.QUALIFIER_STYLER);
					styledString.append("]", StyledString.DECORATIONS_STYLER);
				} else {
					styledString.append(" [" + buildStatus.getDisplayString() + "]", StyledString.DECORATIONS_STYLER);
				}
			} else {
				styledString.append(" [" + Messages.MicroclimateProjectDisabled + "]", StyledString.DECORATIONS_STYLER);
			}
		} else {
			styledString = new StyledString(getText(element));
		}
		return styledString;
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof MicroclimateConnection) {
			return MicroclimateUIPlugin.getImage(MicroclimateUIPlugin.MICROCLIMATE_ICON);
		} else if (element instanceof MicroclimateApplication) {
			ProjectType type = ((MicroclimateApplication)element).projectType;
			if (type.isLanguage(ProjectType.LANGUAGE_JAVA)) {
				return MicroclimateUIPlugin.getImage(MicroclimateUIPlugin.JAVA_ICON);
			}
			if (type.isLanguage(ProjectType.LANGUAGE_NODEJS)) {
				return MicroclimateUIPlugin.getImage(MicroclimateUIPlugin.NODE_ICON);
			}
			if (type.isLanguage(ProjectType.LANGUAGE_SWIFT)) {
				return MicroclimateUIPlugin.getImage(MicroclimateUIPlugin.SWIFT_ICON);
			}
			if (type.isLanguage(ProjectType.LANGUAGE_GO)) {
				return MicroclimateUIPlugin.getImage(MicroclimateUIPlugin.GO_ICON);
			}
			if (type.isLanguage(ProjectType.LANGUAGE_PYTHON)) {
				return MicroclimateUIPlugin.getImage(MicroclimateUIPlugin.PYTHON_ICON);
			}
		}
		return null;
	}

	static class BoldFontStyler extends Styler {
	    @Override
	    public void applyStyles(final TextStyle textStyle)
	    {
	        FontDescriptor boldDescriptor = FontDescriptor.createFrom(new FontData()).setStyle(SWT.BOLD);
	        Font boldFont = boldDescriptor.createFont(Display.getCurrent());
	        textStyle.font = boldFont;
	    }
	}

}
