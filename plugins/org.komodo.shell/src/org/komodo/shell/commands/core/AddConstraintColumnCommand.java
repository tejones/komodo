/*************************************************************************************
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership. Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 ************************************************************************************/
package org.komodo.shell.commands.core;

import static org.komodo.shell.CompletionConstants.MESSAGE_INDENT;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.komodo.relational.model.Column;
import org.komodo.relational.model.TableConstraint;
import org.komodo.shell.BuiltInShellCommand;
import org.komodo.shell.Messages;
import org.komodo.shell.api.WorkspaceContext;
import org.komodo.shell.api.WorkspaceStatus;
import org.komodo.shell.util.ContextUtils;
import org.komodo.spi.repository.KomodoObject;
import org.komodo.spi.repository.KomodoType;
import org.komodo.spi.repository.Repository;
import org.komodo.utils.StringUtils;

/**
 * Adds a {@link Column column reference} to a {@link TableConstraint table constraint}.
 */
public final class AddConstraintColumnCommand extends BuiltInShellCommand {

    /**
     * The command name.
     */
    public static final String NAME = "add-column"; //$NON-NLS-1$

    /**
     * @param status
     *        the workspace status (cannot be <code>null</code>)
     */
    public AddConstraintColumnCommand( final WorkspaceStatus status ) {
        super( status, NAME );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.api.ShellCommand#execute()
     */
    @Override
    public boolean execute() throws Exception {
        try {
            final String columnPathArg = requiredArgument( 0,
                                                           Messages.getString( Messages.AddConstraintColumnCommand.missingColumnPathArg ) );

            // get reference of the column at the specified path
            final WorkspaceContext columnContext = ContextUtils.getContextForPath( getWorkspaceStatus(), columnPathArg );

            if ( columnContext == null ) {
                print( MESSAGE_INDENT, Messages.getString( Messages.AddConstraintColumnCommand.columnPathNotFound, columnPathArg ) );
                return false;
            }

            final KomodoObject column = columnContext.getKomodoObj();

            if ( column instanceof Column ) {
                // initValidWsContextTypes() method assures execute is called only if current context is a TableConstraint
                final KomodoObject kobject = getContext().getKomodoObj();
                assert ( kobject instanceof TableConstraint );
                final TableConstraint constraint = ( TableConstraint )kobject;

                // must be a column in the parent of the table constraint
                final Repository.UnitOfWork transaction = getWorkspaceStatus().getTransaction();
                final KomodoObject parentTable = constraint.getParent( transaction );

                if ( parentTable.equals( column.getParent( transaction ) ) ) {
                    constraint.addColumn( transaction, ( Column )column );
                } else {
                    throw new Exception( Messages.getString( Messages.AddConstraintColumnCommand.invalidColumn,
                                                             ContextUtils.convertPathToDisplayPath( column.getAbsolutePath() ),
                                                             constraint.getName( transaction ) ) );
                }

                // Commit transaction
                if ( isAutoCommit() ) {
                    getWorkspaceStatus().commit( AddConstraintColumnCommand.class.getSimpleName() );
                }

                print( MESSAGE_INDENT,
                       Messages.getString( Messages.AddConstraintColumnCommand.columnRefAdded, columnPathArg, getContext().getFullName() ) );

                return true;
            } else {
                print( MESSAGE_INDENT, Messages.getString( Messages.AddConstraintColumnCommand.invalidColumnPath, columnPathArg ) );
                return false;
            }
        } catch ( final Exception e ) {
            print( MESSAGE_INDENT, Messages.getString( Messages.AddConstraintColumnCommand.error, e.getLocalizedMessage() ) );
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.api.AbstractShellCommand#initValidWsContextTypes()
     */
    @Override
    public void initValidWsContextTypes() {
        final List< String > temp = new ArrayList<>();
        temp.add( KomodoType.PRIMARY_KEY.getType() );
        temp.add( KomodoType.UNIQUE_CONSTRAINT.getType() );
        temp.add( KomodoType.ACCESS_PATTERN.getType() );
        temp.add( KomodoType.FOREIGN_KEY.getType() );
        temp.add( KomodoType.INDEX.getType() );

        this.validWsContextTypes = Collections.unmodifiableList( temp );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.api.AbstractShellCommand#tabCompletion(java.lang.String, java.util.List)
     */
    @Override
    public int tabCompletion( final String lastArgument,
                              final List< CharSequence > candidates ) throws Exception {
        if ( getArguments().isEmpty() ) {

            // find columns
            final KomodoObject parent = getContext().getParent().getKomodoObj();
            final String[] columnPaths = FindCommand.query( getWorkspaceStatus(), KomodoType.COLUMN, parent.getAbsolutePath(), null );

            if ( columnPaths.length == 0 ) {
                return -1;
            }

            if ( StringUtils.isBlank( lastArgument ) ) {
                candidates.addAll( Arrays.asList( columnPaths ) );
            } else {
                for ( final String item : Arrays.asList( columnPaths ) ) {
                    if ( item.startsWith( lastArgument ) ) {
                        candidates.add( item );
                    }
                }
            }

            return ( candidates.isEmpty() ? -1 : ( toString().length() + 1 ) );
        }

        // no completions if more than one arg
        return -1;
    }

}
