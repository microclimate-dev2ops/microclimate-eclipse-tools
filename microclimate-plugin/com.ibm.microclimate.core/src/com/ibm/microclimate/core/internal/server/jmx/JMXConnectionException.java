/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.microclimate.core.internal.server.jmx;

/**
 * Exception class that denotes a problem with JMX Connectivity
 */
public class JMXConnectionException extends Exception {

    private static final long serialVersionUID = -5632639632836543882L;

    private static final String ERROR_MSG = "JMX Connection Failure";

    public JMXConnectionException() {
        super(ERROR_MSG);
    }

    /**
     * @param message - the failure message
     * @param cause - the exception that caused the failure
     */
    public JMXConnectionException(Throwable cause) {
        super(ERROR_MSG, cause);
    }

}
