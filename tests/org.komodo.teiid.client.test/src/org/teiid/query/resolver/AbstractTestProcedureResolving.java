/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.teiid.query.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Collection;
import org.junit.Test;
import org.teiid.api.exception.query.QueryResolverException;
import org.komodo.spi.query.metadata.QueryMetadataInterface;
import org.komodo.spi.runtime.version.TeiidVersion;
import org.teiid.metadata.Table;
import org.teiid.query.metadata.TempMetadataAdapter;
import org.teiid.query.sql.lang.CommandImpl;
import org.teiid.query.sql.lang.BaseLanguageObject;
import org.teiid.query.sql.lang.ProcedureContainer;
import org.teiid.query.sql.navigator.DeepPreOrderNavigator;
import org.teiid.runtime.client.TeiidClientException;

@SuppressWarnings( {"javadoc"} )
public abstract class AbstractTestProcedureResolving extends AbstractTest {

    /**
     * @param teiidVersion
     */
    public AbstractTestProcedureResolving(TeiidVersion teiidVersion) {
        super(teiidVersion);
    }

    protected CommandImpl resolveProcedure(String userUpdateStr, QueryMetadataInterface metadata) throws Exception {
        ProcedureContainer userCommand = (ProcedureContainer)getQueryParser().parseCommand(userUpdateStr);
        TCQueryResolver queryResolver = new TCQueryResolver(getTeiidVersion());
        queryResolver.resolveCommand(userCommand, metadata);
        metadata = new TempMetadataAdapter(metadata, userCommand.getTemporaryMetadata());
        return queryResolver.expandCommand(userCommand, metadata);
    }

    protected CommandImpl helpResolve(CommandImpl command, QueryMetadataInterface queryMetadataInterface) {       
        // resolve
        try { 
            TCQueryResolver resolver = new TCQueryResolver(getTeiidVersion());
            resolver.resolveCommand(command, queryMetadataInterface);
        } catch(Exception e) {
            throw new RuntimeException(e);
        } 

        CheckSymbolsAreResolvedVisitor vis = new CheckSymbolsAreResolvedVisitor(getTeiidVersion());
        DeepPreOrderNavigator.doVisit(command, vis);
        Collection<BaseLanguageObject> unresolvedSymbols = vis.getUnresolvedSymbols();
        assertTrue("Found unresolved symbols: " + unresolvedSymbols, unresolvedSymbols.isEmpty()); //$NON-NLS-1$
        return command; 
    }

    protected CommandImpl helpResolve(String sql, QueryMetadataInterface queryMetadata) throws Exception {
        CommandImpl command = getQueryParser().parseCommand(sql);
        return helpResolve(command, queryMetadata);
    }

    protected void helpResolveException(String userUpdateStr, QueryMetadataInterface metadata, String msg) throws Exception {
        try {
            resolveProcedure(userUpdateStr, metadata);
            fail();
        } catch (QueryResolverException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    protected void helpFailUpdateProcedure(String procedure, String userUpdateStr, Table.TriggerEvent procedureType) {
        helpFailUpdateProcedure(procedure, userUpdateStr, procedureType, null);
    }

    protected abstract CommandImpl helpResolveUpdateProcedure(String procedure, String userUpdateStr, Table.TriggerEvent procedureType)
        throws Exception;

    protected void helpFailUpdateProcedure(String procedure, String userUpdateStr, Table.TriggerEvent procedureType, String msg) {
        // resolve
        try {
            helpResolveUpdateProcedure(procedure, userUpdateStr, procedureType);
            fail("Expected a QueryResolverException but got none."); //$NON-NLS-1$
        } catch (TeiidClientException ex) {
            if (msg != null) {
                assertTrue(ex.getMessage().contains(msg));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testVirtualProcedure() throws Exception {
        resolveProcedure("EXEC pm1.vsp1()", getMetadataFactory().example1Cached()); //$NON-NLS-1$
    }

    @Test
    public void testVirtualProcedure2() throws Exception {
        resolveProcedure("EXEC pm1.vsp14()", getMetadataFactory().example1Cached()); //$NON-NLS-1$
    }

    @Test
    public void testVirtualProcedurePartialParameterReference() throws Exception {
        resolveProcedure("EXEC pm1.vsp58(5)", getMetadataFactory().example1Cached()); //$NON-NLS-1$
    }

    //cursor starts with "#" Defect14924
    @Test
    public void testVirtualProcedureInvalid1() throws Exception {
        helpResolveException("EXEC pm1.vsp32()", getMetadataFactory().example1Cached(), "TEIID30125 Cursor or exception group names cannot begin with \"#\" as that indicates the name of a temporary table: #mycursor."); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testVirtualProcedureWithOrderBy() throws Exception {
        resolveProcedure("EXEC pm1.vsp29()", getMetadataFactory().example1Cached()); //$NON-NLS-1$
    }

    @Test
    public void testVirtualProcedureWithTempTableAndOrderBy() throws Exception {
        resolveProcedure("EXEC pm1.vsp33()", getMetadataFactory().example1Cached()); //$NON-NLS-1$
    }

    @Test
    public void testVirtualProcedureWithConstAndOrderBy() throws Exception {
        resolveProcedure("EXEC pm1.vsp34()", getMetadataFactory().example1Cached()); //$NON-NLS-1$
    }

    @Test
    public void testVirtualProcedureWithNoFromAndOrderBy() throws Exception {
        resolveProcedure("EXEC pm1.vsp28()", getMetadataFactory().example1Cached()); //$NON-NLS-1$
    }

    @Test
    public void testInvalidVirtualProcedure2() throws Exception {
        helpResolveException("EXEC pm1.vsp12()", getMetadataFactory().example1Cached(), "TEIID31119 Symbol mycursor.e2 is specified with an unknown group context"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testVariableResolutionWithIntervening() throws Exception {
        StringBuffer proc = new StringBuffer("CREATE VIRTUAL PROCEDURE") //$NON-NLS-1$
        .append("\nBEGIN") //$NON-NLS-1$
        .append("\n  declare string x;") //$NON-NLS-1$
        .append("\n  x = '1';") //$NON-NLS-1$
        .append("\n  declare string y;") //$NON-NLS-1$
        .append("\n  y = '1';") //$NON-NLS-1$
        .append("\nEND"); //$NON-NLS-1$

        helpResolve(proc.toString(), getMetadataFactory().example1Cached());
    }

    @Test
    public void testVDBQualified() throws Exception {
        resolveProcedure("EXEC example1.pm1.vsp29()", getMetadataFactory().example1Cached()); //$NON-NLS-1$
    }
}
