/*
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
 */
package org.komodo.modeshape.teiid.sql.v8;

import java.util.Arrays;
import org.junit.Test;
import org.komodo.modeshape.teiid.parser.TeiidNodeFactory.ASTNodes;
import org.komodo.modeshape.teiid.sql.AbstractTestCloning;
import org.komodo.modeshape.teiid.sql.lang.Criteria;
import org.komodo.modeshape.teiid.sql.lang.CriteriaOperator.Operator;
import org.komodo.modeshape.teiid.sql.lang.From;
import org.komodo.modeshape.teiid.sql.lang.MatchCriteria;
import org.komodo.modeshape.teiid.sql.lang.Query;
import org.komodo.modeshape.teiid.sql.lang.Select;
import org.komodo.modeshape.teiid.sql.proc.AssignmentStatement;
import org.komodo.modeshape.teiid.sql.proc.Block;
import org.komodo.modeshape.teiid.sql.proc.BranchingStatement.BranchingMode;
import org.komodo.modeshape.teiid.sql.proc.CommandStatement;
import org.komodo.modeshape.teiid.sql.proc.CreateProcedureCommand;
import org.komodo.modeshape.teiid.sql.proc.ExceptionExpression;
import org.komodo.modeshape.teiid.sql.proc.IfStatement;
import org.komodo.modeshape.teiid.sql.proc.LoopStatement;
import org.komodo.modeshape.teiid.sql.proc.RaiseStatement;
import org.komodo.modeshape.teiid.sql.proc.Statement;
import org.komodo.modeshape.teiid.sql.symbol.ElementSymbol;
import org.komodo.modeshape.teiid.sql.symbol.Expression;
import org.komodo.modeshape.teiid.sql.symbol.Function;
import org.komodo.modeshape.teiid.sql.symbol.GroupSymbol;
import org.komodo.modeshape.teiid.sql.symbol.JSONObject;
import org.komodo.modeshape.teiid.sql.symbol.XMLSerialize;
import org.komodo.spi.runtime.version.TeiidVersion;
import org.komodo.spi.runtime.version.DefaultTeiidVersion.Version;

/**
 * Unit testing for the SQLStringVisitor for teiid version 8
 */
@SuppressWarnings( {"nls", "javadoc"} )
public class Test8Cloning extends AbstractTestCloning {

    private Test8Factory factory;

    protected Test8Cloning(TeiidVersion teiidVersion) {
        super(teiidVersion);
    }

    public Test8Cloning() {
        this(Version.TEIID_8_0.get());
    }

    @Override
    protected Test8Factory getFactory() {
        if (factory == null)
            factory = new Test8Factory(parser);

        return factory;
    }

    @Test
    public void testSignedExpression() {
        GroupSymbol g = getFactory().newGroupSymbol("g");
        From from = getFactory().newFrom();
        from.addGroup(g);

        Function f = getFactory().newFunction("*", new Expression[] {getFactory().newConstant(-1), getFactory().newElementSymbol("x")});
        Select select = getFactory().newSelect();
        select.addSymbol(f);
        select.addSymbol(getFactory().newElementSymbol("x"));
        select.addSymbol(getFactory().newConstant(5));

        Query query = getFactory().newQuery(select, from);
        helpTest(
                 "SELECT (-1 * x), x, 5 FROM g",
                 query);
    }

    @Test
    public void testFloatWithE() {
        GroupSymbol g = getFactory().newGroupSymbol("a.g1");
        From from = getFactory().newFrom();
        from.addGroup(g);

        Select select = getFactory().newSelect();
        select.addSymbol(getFactory().newConstant(new Double(1.3e8)));
        select.addSymbol(getFactory().newConstant(new Double(-1.3e+8)));
        select.addSymbol(getFactory().newConstant(new Double(+1.3e-8)));

        Query query = getFactory().newQuery(select, from);

        helpTest(
                 "SELECT 1.3E8, -1.3E8, 1.3E-8 FROM a.g1",
                 query);
    }

    @Test
    public void testPgLike() {
        GroupSymbol g = getFactory().newGroupSymbol("db.g");
        From from = getFactory().newFrom();
        from.addGroup(g);

        Select select = getFactory().newSelect();
        ElementSymbol a = getFactory().newElementSymbol("a");
        select.addSymbol(a);

        Expression string1 = getFactory().newConstant("\\_aString");
        MatchCriteria crit = getFactory().newMatchCriteria(getFactory().newElementSymbol("b"), string1, '\\');

        Query query = getFactory().newQuery(select, from);
        query.setCriteria(crit);
        helpTest("SELECT a FROM db.g WHERE b LIKE '\\_aString' ESCAPE '\\'",
                 query);
    }

    @Test
    public void testErrorStatement() throws Exception {
        ExceptionExpression ee = getFactory().newExceptionExpression();
        ee.setMessage(getFactory().newConstant("Test only"));
        RaiseStatement errStmt = getFactory().newNode(ASTNodes.RAISE_STATEMENT);
        errStmt.setExpression(ee);

        helpTest("RAISE SQLEXCEPTION 'Test only';",
                     errStmt);
    }

    @Test
    public void testRaiseErrorStatement() throws Exception {
        ExceptionExpression ee = getFactory().newExceptionExpression();
        ee.setMessage(getFactory().newConstant("Test only"));
        ee.setSqlState(getFactory().newConstant("100"));
        ee.setParent(getFactory().newElementSymbol("e"));
        RaiseStatement errStmt = getFactory().newNode(ASTNodes.RAISE_STATEMENT);
        errStmt.setExpression(ee);
        errStmt.setWarning(true);

        helpTest("RAISE SQLWARNING SQLEXCEPTION 'Test only' SQLSTATE '100' CHAIN e;",
                     errStmt);
    }

    @Test
    public void testXmlSerialize2() throws Exception {
        XMLSerialize f = getFactory().newXMLSerialize();
        f.setExpression(getFactory().newElementSymbol("x"));
        f.setTypeString("BLOB");
        f.setDeclaration(Boolean.TRUE);
        f.setVersion("1.0");
        f.setEncoding("UTF-8");
        helpTest("XMLSERIALIZE(x AS BLOB ENCODING \"UTF-8\" VERSION '1.0' INCLUDING XMLDECLARATION)",
                           f);
    }

    @Test
    public void testBlockExceptionHandling() throws Exception {
        Select select = getFactory().newSelectWithMultileElementSymbol();
        From from = getFactory().newFrom();
        from.setClauses(Arrays.asList(getFactory().newUnaryFromClause("x")));
        Query query = getFactory().newQuery(select, from);
        CommandStatement cmdStmt = getFactory().newCommandStatement(query);
        AssignmentStatement assigStmt = getFactory().newAssignmentStatement(getFactory().newElementSymbol("a"), getFactory().newConstant(new Integer(1)));
        RaiseStatement errStmt = getFactory().newNode(ASTNodes.RAISE_STATEMENT);
        ExceptionExpression ee = getFactory().newExceptionExpression();
        ee.setMessage(getFactory().newConstant("My Error"));
        errStmt.setExpression(ee);
        Block b = getFactory().newBlock();
        b.setExceptionGroup("e");
        b.addStatement(cmdStmt);
        b.addStatement(assigStmt);
        b.addStatement(errStmt, true);
        helpTest("BEGIN\nSELECT * FROM x;\na = 1;\nEXCEPTION e\nRAISE SQLEXCEPTION 'My Error';\nEND", b);
    }

    @Test
    public void testJSONObject() throws Exception {
        JSONObject f = getFactory().newJSONObject(Arrays.asList(getFactory().newDerivedColumn("table", getFactory().newElementSymbol("a"))));
        helpTest("JSONOBJECT(a AS \"table\")", f);
    }

    @Test public void testVirtualProcedure(){        
        ElementSymbol x = getFactory().newElementSymbol("x");
        String intType = new String("integer");
        Statement dStmt = getFactory().newDeclareStatement(x, intType);
        
        GroupSymbol g = getFactory().newGroupSymbol("m.g");
        From from = getFactory().newFrom();
        from.addGroup(g);
        
        Select select = getFactory().newSelect();
        ElementSymbol c1 = getFactory().newElementSymbol("c1");
        select.addSymbol(c1);
        select.addSymbol(getFactory().newElementSymbol("c2"));

        Query query = getFactory().newQuery(select, from);

        x = getFactory().newElementSymbol("x");
        c1 = getFactory().newElementSymbol("mycursor.c1");
        Statement assignmentStmt = getFactory().newAssignmentStatement(x, c1);
        Block block = getFactory().newBlock(); 
        block.addStatement(assignmentStmt);
        
        Block ifBlock = getFactory().newBlock();
        Statement continueStmt = getFactory().newBranchingStatement(BranchingMode.CONTINUE);
        ifBlock.addStatement(continueStmt);
        Criteria crit = getFactory().newCompareCriteria(x.clone(), Operator.GT,  getFactory().newConstant(new Integer(5)));
        IfStatement ifStmt = getFactory().newIfStatement(crit, ifBlock);
        block.addStatement(ifStmt); 
        
        String cursor = "mycursor";
        LoopStatement loopStmt = getFactory().newLoopStatement(block, query, cursor);
        
        block = getFactory().newBlock();        
        block.addStatement(dStmt);
        block.addStatement(loopStmt);
        CommandStatement cmdStmt = getFactory().newCommandStatement(query);
        block.addStatement(cmdStmt);
        
        CreateProcedureCommand virtualProcedureCommand = getFactory().newCreateProcedureCommand();
        virtualProcedureCommand.setBlock(block);
        
        helpTest("CREATE VIRTUAL PROCEDURE\nBEGIN\nDECLARE integer x;\n"
        + "LOOP ON (SELECT c1, c2 FROM m.g) AS mycursor\nBEGIN\n"
        + "x = mycursor.c1;\nIF(x > 5)\nBEGIN\nCONTINUE;\nEND\nEND\n"
        + "SELECT c1, c2 FROM m.g;\nEND", virtualProcedureCommand);

    }
}
