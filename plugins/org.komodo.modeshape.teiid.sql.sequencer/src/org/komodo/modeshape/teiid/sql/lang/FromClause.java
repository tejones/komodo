/* Generated By:JJTree: Do not edit this line. FromClause.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.komodo.modeshape.teiid.sql.lang;

import java.util.Collection;
import org.komodo.modeshape.teiid.cnd.TeiidSqlLexicon;
import org.komodo.modeshape.teiid.parser.LanguageVisitor;
import org.komodo.modeshape.teiid.parser.TeiidParser;
import org.komodo.modeshape.teiid.sql.symbol.GroupSymbol;
import org.komodo.spi.annotation.Since;
import org.komodo.spi.query.sql.lang.IFromClause;
import org.komodo.spi.runtime.version.TeiidVersion.Version;

/**
 * A FromClause is an interface for subparts held in a FROM clause.  One 
 * type of FromClause is {@link UnaryFromClause}, which is the more common 
 * use and represents a single group.  Another, less common type of FromClause
 * is the {@link JoinPredicate} which represents a join between two FromClauses
 * and may contain criteria.
 */
public abstract class FromClause extends ASTNode implements IFromClause<LanguageVisitor> {

    /**
     * 
     */
    public static final String MAKEIND = "MAKEIND"; //$NON-NLS-1$

    /**
     * 
     */
    @Since(Version.TEIID_8_0)
    public static final String PRESERVE = "PRESERVE"; //$NON-NLS-1$

    /**
     * @param p
     * @param id
     */
    public FromClause(TeiidParser p, int id) {
        super(p, id);
    }

    /**
     * @return whether any hints set
     */
    public boolean hasHint() {
        return isOptional() || isMakeInd() || isMakeNotDep() || isNoUnnest() || isPreserve() || 
            (getMax() == 0 && !isJoin()); // makeDep.isSimple() in teiid language
    }

    @Override
    public boolean isOptional() {
        Object property = getProperty(TeiidSqlLexicon.FromClause.OPTIONAL_PROP_NAME);
        return property == null ? false : Boolean.parseBoolean(property.toString());
    }
    
    @Override
    public void setOptional(boolean optional) {
        setProperty(TeiidSqlLexicon.FromClause.OPTIONAL_PROP_NAME, optional);
    }
    
    /**
     * @return make ind flag
     */
    public boolean isMakeInd() {
        Object property = getProperty(TeiidSqlLexicon.FromClause.MAKE_IND_PROP_NAME);
        return property == null ? false : Boolean.parseBoolean(property.toString());
    }
    
    /**
     * @param makeInd
     */
    public void setMakeInd(boolean makeInd) {
        setProperty(TeiidSqlLexicon.FromClause.MAKE_IND_PROP_NAME, makeInd);
    }

    /**
     * @return no unnest flag
     */
    public boolean isNoUnnest() {
        Object property = getProperty(TeiidSqlLexicon.FromClause.NO_UNNEST_PROP_NAME);
        return property == null ? false : Boolean.parseBoolean(property.toString());
    }

    /**
     * @param noUnnest
     */
    public void setNoUnnest(boolean noUnnest) {
        setProperty(TeiidSqlLexicon.FromClause.NO_UNNEST_PROP_NAME, noUnnest);
    }

    @Override
    public boolean isMakeDep() {
        Object property = getProperty(TeiidSqlLexicon.FromClause.MAKE_DEP_PROP_NAME);
        return property == null ? false : Boolean.parseBoolean(property.toString());
    }

    @Override
    public void setMakeDep(boolean makeDep) {
        setProperty(TeiidSqlLexicon.FromClause.MAKE_DEP_PROP_NAME, makeDep);
        if (makeDep) {
            setJoin(! makeDep);
            setMax(makeDep ? 0 : 1);
        }
    }

    /**
     * Both this and isMax() are components of the former MakeDep
     * class.
     *
     * @return join flag
     */
    public boolean isJoin() {
        Object property = getProperty(TeiidSqlLexicon.FromClause.JOIN_PROP_NAME);
        return property == null ? false : Boolean.parseBoolean(property.toString());
    }

    public void setJoin(boolean join) {
        setProperty(TeiidSqlLexicon.FromClause.JOIN_PROP_NAME, join);
    }

    /**
     * Both this and isJoin() are components of the former MakeDep
     * class.
     *
     * @return join flag
     */
    public int getMax() {
        Object property = getProperty(TeiidSqlLexicon.FromClause.MAX_PROP_NAME);
        return property == null ? 0 : Integer.parseInt(property.toString());
    }

    public void setMax(int max) {
        setProperty(TeiidSqlLexicon.FromClause.MAX_PROP_NAME, max);
    }

    @Override
    public boolean isMakeNotDep() {
        Object property = getProperty(TeiidSqlLexicon.FromClause.MAKE_NOT_DEP_PROP_NAME);
        return property == null ? false : Boolean.parseBoolean(property.toString());
    }

    @Override
    public void setMakeNotDep(boolean makeNotDep) {
        setProperty(TeiidSqlLexicon.FromClause.MAKE_NOT_DEP_PROP_NAME, makeNotDep);
    }
    
    /**
     * @return preserve flag
     */
    public boolean isPreserve() {
        Object property = getProperty(TeiidSqlLexicon.FromClause.PRESERVE_PROP_NAME);
        return property == null ? false : Boolean.parseBoolean(property.toString());
    }
    
    /**
     * @param preserve
     */
    public void setPreserve(boolean preserve) {
        setProperty(TeiidSqlLexicon.FromClause.PRESERVE_PROP_NAME, preserve);
    }

    /**
     * @param groups
     */
    public abstract void collectGroups(Collection<GroupSymbol> groups);

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (this.isJoin() ? 1231 : 1237);
        result = prime * result + this.getMax();
        result = prime * result + (this.isMakeInd() ? 1231 : 1237);
        result = prime * result + (this.isMakeNotDep() ? 1231 : 1237);
        result = prime * result + (this.isNoUnnest() ? 1231 : 1237);
        result = prime * result + (this.isOptional() ? 1231 : 1237);
        result = prime * result + (this.isPreserve() ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        FromClause other = (FromClause)obj;

        if (this.isJoin() != other.isJoin()) return false;
        if (this.getMax() != other.getMax()) return false;
        if (this.isMakeInd() != other.isMakeInd()) return false;
        if (this.isMakeNotDep() != other.isMakeNotDep()) return false;
        if (this.isNoUnnest() != other.isNoUnnest()) return false;
        if (this.isOptional() != other.isOptional()) return false;
        if (this.isPreserve() != other.isPreserve()) return false;
        return true;
    }

    /** Accept the visitor. **/
    @Override
    public void acceptVisitor(LanguageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public abstract FromClause clone();

}
/* JavaCC - OriginalChecksum=908130697ce6a37a6c778dfefda987bb (do not edit this line) */