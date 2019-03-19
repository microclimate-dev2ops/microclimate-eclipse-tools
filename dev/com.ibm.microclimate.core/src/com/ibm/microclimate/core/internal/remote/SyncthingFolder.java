/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal.remote;

import java.util.List;

public class SyncthingFolder {
	
	public final String folderId;
	private List<String> errors = null;
	private FolderState state = FolderState.IDLE;
	
	public enum FolderState {

		IDLE("idle"),
		SCANNING("scanning"),
		SYNCING("syncing"),
		ERROR("error");
		
		public final String folderState;

		/**
		 * @param buildStatus - Internal build status used by Microclimate
		 */
		private FolderState(String folderState) {
			this.folderState = folderState;
		}

		public boolean equals(String s) {
			return this.name().equals(s);
		}
		
		public static FolderState get(String folderState) {
			for (FolderState state : FolderState.values()) {
				if (state.folderState.equals(folderState)) {
					return state;
				}
			}
			return null;
		}
	};
	
	public SyncthingFolder(String folderId) {
		this.folderId = folderId;
	}
	
	public void setErrors(List<String> errors) {
		this.errors = errors;
	}
	
	public List<String> getErrors() {
		return errors;
	}
	
	public void setState(String state) {
		this.state = FolderState.get(state);
	}
	
	public FolderState getState() {
		return state;
	}

}
