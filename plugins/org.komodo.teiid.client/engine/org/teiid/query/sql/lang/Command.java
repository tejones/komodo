/* Generated By:JJTree: Do not edit this line. Command.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.teiid.query.sql.lang;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.komodo.spi.query.sql.lang.ICommand;
import org.komodo.spi.runtime.version.DefaultTeiidVersion.Version;
import org.teiid.core.types.DataTypeManagerService;
import org.teiid.query.metadata.TempMetadataStore;
import org.teiid.query.parser.LanguageVisitor;
import org.teiid.query.parser.TeiidNodeFactory.ASTNodes;
import org.teiid.query.parser.TeiidParser;
import org.teiid.query.sql.symbol.ElementSymbol;
import org.teiid.query.sql.symbol.Expression;
import org.teiid.query.sql.symbol.GroupSymbol;
import org.teiid.query.sql.util.SymbolMap;
import org.teiid.query.sql.visitor.CommandCollectorVisitor;

/**
 *
 */
public abstract class Command extends SimpleNode implements ICommand<Expression, LanguageVisitor>{

    private static List<Expression> updateCommandSymbol;

    /** The option clause */
    private Option option;

    private SourceHint sourceHint;

    private boolean isResolved;

    private transient GroupContext externalGroups;

    private SymbolMap correlatedReferences;

    /**
     * All temporary group IDs discovered while resolving this 
     * command.  The key is a TempMetadataID and the value is an 
     * ordered List of TempMetadataID representing the elements.
     */
    protected TempMetadataStore tempGroupIDs;

    /**
     * @param p
     * @param id
     */
    public Command(TeiidParser p, int id) {
        super(p, id);
    }

    /**
     * Get the option clause for the query.
     * @return option clause
     */
    @Override
    public Option getOption() {
        return option;
    }
    
    /**
     * Set the option clause for the query.
     * @param option New option clause
     */
    public void setOption(Option option) {
        this.option = option;
    }

    /**
     * @return the sourceHint
     */
    public SourceHint getSourceHint() {
        return sourceHint;
    }

    /**
     * @return whether this object returns a result set
     */
    public boolean returnsResultSet() {
        return false;
    }

    /**
     * @return null if unknown, empty if results are not returned, or the resultset columns
     */
    @Override
    public List<? extends Expression> getResultSetColumns() {
        if (returnsResultSet() || parser.getVersion().isLessThan(Version.TEIID_8_0.get())) {
            return getProjectedSymbols();
        }
        return Collections.emptyList();
    }

    /**
     * @param sourceHint the sourceHint to set
     */
    public void setSourceHint(SourceHint sourceHint) {
        this.sourceHint = sourceHint;
    }

    /**
     * Indicates whether this command has been resolved or not - 
     * attempting to resolve a command that has already been resolved
     * has undefined results.  Also, caution should be taken in modifying
     * a command which has already been resolved, as it could result in
     * adding unresolved components to a supposedly resolved command.
     * @return whether this command is resolved or not.
     */
    @Override
    public boolean isResolved() {
        return this.isResolved;
    }

    /**
     * This command is intended to only be used by the QueryResolver.
     * @param isResolved whether this command is resolved or not
     */
    public void setIsResolved(boolean isResolved) {
        this.isResolved = isResolved;
    }

    /**
     * @return singleton update symbol which is lazily created
     */
    public List<Expression> getUpdateCommandSymbol() {
        if (updateCommandSymbol == null ) {
            ElementSymbol symbol = parser.createASTNode(ASTNodes.ELEMENT_SYMBOL);
            symbol.setName("Count"); //$NON-NLS-1$
            symbol.setType(DataTypeManagerService.DefaultDataTypes.INTEGER.getTypeClass());
            updateCommandSymbol = Arrays.asList((Expression)symbol);
        }
        return updateCommandSymbol;
    }

    protected void copyMetadataState(Command copy) {
        if(this.getExternalGroupContexts() != null) {
            copy.externalGroups = (GroupContext)this.externalGroups.clone();
        }
        if(this.tempGroupIDs != null) {
            copy.setTemporaryMetadata(this.tempGroupIDs.clone());
        }
        
        copy.setIsResolved(this.isResolved());

        if (this.correlatedReferences != null) {
            copy.correlatedReferences = this.correlatedReferences.clone();
        }
        if(this.getOption() != null) { 
            copy.setOption(this.getOption().clone());
        }

        copy.sourceHint = this.sourceHint;
    }

    /**
     * @return temporary group ids
     */
    public TempMetadataStore getTemporaryMetadata() {
        return this.tempGroupIDs;
    }

    /**
     * @param metadata
     */
    public void setTemporaryMetadata(TempMetadataStore metadata) {
        this.tempGroupIDs = metadata;
    }

    /**
     * @param group
     */
    public void addExternalGroupToContext(GroupSymbol group) {
        getExternalGroupContexts().addGroup(group);
    }

    /**
     * @param groups
     */
    public void addExternalGroupsToContext(Collection<GroupSymbol> groups) {
        getExternalGroupContexts().getGroups().addAll(groups);
    }

    /**
     * @param root
     */
    public void setExternalGroupContexts(GroupContext root) {
        if (root == null) {
            this.externalGroups = null;
        } else {
            this.externalGroups = (GroupContext)root.clone();
        }
    }
    
    /**
     * @param groups
     */
    public void pushNewResolvingContext(Collection<GroupSymbol> groups) {
        externalGroups = new GroupContext(externalGroups, new LinkedList<GroupSymbol>(groups));
    }

    /**
     * @return external groups
     */
    public GroupContext getExternalGroupContexts() {
        if (externalGroups == null) {
            this.externalGroups = new GroupContext();
        }
        return this.externalGroups;
    }
    
    /**
     * @return external groups
     */
    public List<GroupSymbol> getAllExternalGroups() {
        if (externalGroups == null) {
            return Collections.emptyList();
        }
        
        return externalGroups.getAllGroups();
    }

    /**
     * Helper method to print command tree at given tab level
     * @param str String buffer to add command sub tree to
     * @param tabLevel Number of tabs to print this command at
     */
    protected void printCommandTree(StringBuffer str, int tabLevel) {
        // Add tabs
        for(int i=0; i<tabLevel; i++) {
            str.append("\t"); //$NON-NLS-1$
        }
        
        // Add this command
        str.append(toString());
        str.append("\n"); //$NON-NLS-1$

        // Add children recursively
        tabLevel++;
        for (Command subCommand : CommandCollectorVisitor.getCommands(this)) {
            subCommand.printCommandTree(str, tabLevel);
        }
    }

    /**
     * Print the full tree of commands with indentation - useful for debugging
     * @return String String representation of command tree
     */
    public String printCommandTree() {
        StringBuffer str = new StringBuffer();
        printCommandTree(str, 0);
        return str.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (this.isResolved ? 1231 : 1237);
        result = prime * result + ((this.option == null) ? 0 : this.option.hashCode());
        result = prime * result + ((this.sourceHint == null) ? 0 : this.sourceHint.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Command other = (Command)obj;
        if (this.isResolved != other.isResolved)
            return false;
        if (this.option == null) {
            if (other.option != null)
                return false;
        } else if (!this.option.equals(other.option))
            return false;
        if (this.sourceHint == null) {
            if (other.sourceHint != null)
                return false;
        } else if (!this.sourceHint.equals(other.sourceHint))
            return false;
        return true;
    }

    /** Accept the visitor. **/
    @Override
    public void acceptVisitor(LanguageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public abstract Command clone();
}
/* JavaCC - OriginalChecksum=328e6e6dec01c1dc65d33fce3077b4f3 (do not edit this line) */
