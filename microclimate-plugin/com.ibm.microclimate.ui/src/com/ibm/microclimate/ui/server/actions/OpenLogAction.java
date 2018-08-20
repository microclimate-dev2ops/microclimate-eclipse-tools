/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.microclimate.ui.server.actions;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.server.MicroclimateServer;

/**
 * From com.ibm.ws.st.ui.internal.actions.OpenLogAction
 *
 */
public class OpenLogAction extends Action {
    protected MicroclimateServer mcServer;
    protected final Shell shell;
    protected final IPath logFilePath;

    public OpenLogAction(String name, Shell shell, IPath logFile) {
        super(name);
        this.shell = shell;
        this.logFilePath = logFile;
    }

    @Override
    public void run() {
        openFile(logFilePath);
    }

    public static void openFile(final IPath filePath) {
        final IFileStore fileStore = EFS.getLocalFileSystem().getStore(filePath);
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                IWorkbenchPage page = null;
                if (!fileStore.fetchInfo().isDirectory() && fileStore.fetchInfo().exists()) {
                    IWorkbenchWindow window;
                    window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    page = window.getActivePage();
                }
                try {
                    if (page != null) {
                        IDE.openEditorOnFileStore(page, fileStore);
                    }

                } catch (PartInitException e) {
                    MCLogger.logError("Error Opening messages.log located at : " + filePath.toOSString(), e);
                }
            }
        });
    }
}
