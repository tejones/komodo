/* Generated By:JJTree: Do not edit this line. QueryCommand.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.komodo.modeshape.teiid.sql.lang;

import java.util.List;
import org.komodo.modeshape.teiid.cnd.TeiidSqlLexicon;
import org.komodo.modeshape.teiid.parser.LanguageVisitor;
import org.komodo.modeshape.teiid.parser.TeiidParser;
import org.komodo.modeshape.teiid.sql.symbol.Expression;
import org.komodo.spi.query.sql.lang.IQueryCommand;

/**
 *
 */
public abstract class QueryCommand extends Command
    implements IQueryCommand<OrderBy, Query, Expression, LanguageVisitor> {

    /**
     * @param p
     * @param id
     */
    public QueryCommand(TeiidParser p, int id) {
        super(p, id);
    }

    /**
     * Get the order by clause for the query.
     * @return order by clause
     */
    @Override
    public OrderBy getOrderBy() {
        return getChildforIdentifierAndRefType(TeiidSqlLexicon.QueryCommand.ORDER_BY_REF_NAME, OrderBy.class);
    }
    
    /**
     * Set the order by clause for the query.
     * @param orderBy New order by clause
     */
    @Override
    public void setOrderBy(OrderBy orderBy) {
        setChild(TeiidSqlLexicon.QueryCommand.ORDER_BY_REF_NAME, orderBy);
    }

    public Limit getLimit() {
        return getChildforIdentifierAndRefType(TeiidSqlLexicon.QueryCommand.LIMIT_REF_NAME, Limit.class);
    }

    public void setLimit(Limit limit) {
        setChild(TeiidSqlLexicon.QueryCommand.LIMIT_REF_NAME, limit);
    }
    
    /**
     * @return with
     */
    public List<WithQueryCommand> getWith() {
        return getChildrenforIdentifierAndRefType(
                                                  TeiidSqlLexicon.QueryCommand.WITH_REF_NAME, WithQueryCommand.class);
    }
    
    /**
     * @param with
     */
    public void setWith(List<WithQueryCommand> with) {
        setChildren(TeiidSqlLexicon.QueryCommand.WITH_REF_NAME, with);
    }

    @Override
    public boolean returnsResultSet() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((this.getLimit() == null) ? 0 : this.getLimit().hashCode());
        result = prime * result + ((this.getOrderBy() == null) ? 0 : this.getOrderBy().hashCode());
        result = prime * result + ((this.getWith() == null) ? 0 : this.getWith().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        QueryCommand other = (QueryCommand)obj;
        if (this.getLimit() == null) {
            if (other.getLimit() != null) return false;
        } else if (!this.getLimit().equals(other.getLimit())) return false;
        if (this.getOrderBy() == null) {
            if (other.getOrderBy() != null) return false;
        } else if (!this.getOrderBy().equals(other.getOrderBy())) return false;
        if (this.getWith() == null) {
            if (other.getWith() != null) return false;
        } else if (!this.getWith().equals(other.getWith())) return false;
        return true;
    }

    @Override
    public abstract QueryCommand clone();

    @Override
    public void acceptVisitor(LanguageVisitor visitor) {
        visitor.visit(this);
    }

}
/* JavaCC - OriginalChecksum=e5518987c086d24b10346b06ef207602 (do not edit this line) */