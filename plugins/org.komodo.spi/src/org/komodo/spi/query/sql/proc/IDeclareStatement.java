/*************************************************************************************
 * Copyright (c) 2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.komodo.spi.query.sql.proc;

import org.komodo.spi.query.sql.ILanguageVisitor;
import org.komodo.spi.query.sql.lang.IExpression;



/**
 *
 */
public interface IDeclareStatement<E extends IExpression, LV extends ILanguageVisitor> 
    extends IAssignmentStatement<E, LV> {

    /**
     * Get the type of this variable declared in this statement.
     * 
     * @return A string giving the variable type
     */
    String getVariableType();

}