/* Generated By:JJTree: Do not edit this line. ElementSymbol.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.teiid.query.sql.symbol;

import org.komodo.spi.query.sql.symbol.IElementSymbol;
import org.komodo.spi.runtime.version.DefaultTeiidVersion.Version;
import org.teiid.query.parser.LanguageVisitor;
import org.teiid.query.parser.TeiidNodeFactory.ASTNodes;
import org.teiid.query.parser.TeiidParser;
import org.teiid.query.sql.lang.SingleElementSymbol;

/**
 *
 */
@SuppressWarnings( "unused" )
public class ElementSymbol extends Symbol implements SingleElementSymbol, Expression, IElementSymbol<GroupSymbol, LanguageVisitor> {

    private Class type;
    
    private Object metadataID;

    private GroupSymbol groupSymbol;

    private DisplayMode displayMode = DisplayMode.OUTPUT_NAME;

    private boolean isExternalReference;

    private boolean isAggregate;

    /**
     * @param p
     * @param id
     */
    public ElementSymbol(TeiidParser p, int id) {
        super(p, id);
    }

    /**
     * Get the type of the symbol
     * @return Type of the symbol, may be null before resolution
     */
    @Override
    public Class getType() {
        return this.type;
    }   
    
    /**
     * Set the type of the symbol
     * @param type New type
     */
    @Override
    public void setType(Class type) {
        this.type = type;
    }

    /**
     * Get the metadata ID reference
     * @return Metadata ID reference, may be null before resolution
     */
    @Override
    public Object getMetadataID() {
        return this.metadataID;
    }

    /**
     * Set the metadata ID reference for this element
     * @param metadataID Metadata ID reference
     */
    @Override
    public void setMetadataID(Object metadataID) {
        this.metadataID = metadataID;
    }

    /**
     * Get the group symbol referred to by this element symbol, may be null before resolution
     * @return Group symbol referred to by this element, may be null
     */
    @Override
    public GroupSymbol getGroupSymbol() {
        return this.groupSymbol;
    }

    /**
     * Set the group symbol referred to by this element symbol
     * @param symbol the group symbol to set
     */
    @Override
    public void setGroupSymbol(GroupSymbol symbol) {
        this.groupSymbol = symbol;
    }

    @Override
    public String getName() {
        if (this.groupSymbol != null) {
            return this.groupSymbol.getName() + Symbol.SEPARATOR + this.getShortName();
        }
        return super.getName();
    }

    @Override
    public void setName(String name) {
        int index = name.lastIndexOf('.');
        if (index > 0) {
            if (this.groupSymbol != null) {
                throw new AssertionError("Attempt to set an invalid name"); //$NON-NLS-1$
            }
            GroupSymbol gs = parser.createASTNode(ASTNodes.GROUP_SYMBOL);
            gs.setName(new String(name.substring(0, index)));
            this.setGroupSymbol(gs);
            name = new String(name.substring(index + 1));
        } else {
            this.groupSymbol = null;
        }
        super.setShortName(name);
    }

    @Override
    public void setDisplayMode(DisplayMode displayMode) {
        if (displayMode == null) {
            this.displayMode = DisplayMode.OUTPUT_NAME;
        }
        this.displayMode = displayMode;
    }

    @Override
    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    /**
     * Set whether this element will be displayed as fully qualified
     * @param displayFullyQualified True if should display fully qualified
     */
    @Override
    public void setDisplayFullyQualified(boolean displayFullyQualified) { 
        this.displayMode = displayFullyQualified?DisplayMode.FULLY_QUALIFIED:DisplayMode.SHORT_OUTPUT_NAME; 
    }
    
    /**
     * Get whether this element will be displayed as fully qualified
     * @return True if should display fully qualified
     */
    public boolean getDisplayFullyQualified() { 
        return this.displayMode.equals(DisplayMode.FULLY_QUALIFIED);    
    }

    /**
     * Set whether this element is an external reference.  An external 
     * reference is an element that comes from a group outside the current
     * command context.  Typical uses would be variables and correlated 
     * references in subqueries.
     * @param isExternalReference True if element is an external reference
     */
    public void setIsExternalReference(boolean isExternalReference) { 
        this.isExternalReference = isExternalReference; 
    }

    /**
     * Get whether this element is an external reference to a group
     * outside the command context.
     * @return True if element is an external reference
     */
    @Override
    public boolean isExternalReference() { 
        return this.isExternalReference;  
    }

    /**
     * @return is aggregate flag
     */
    public boolean isAggregate() {
        return isAggregate;
    }
    
    /**
     * @param isAggregate
     */
    public void setAggregate(boolean isAggregate) {
        this.isAggregate = isAggregate;
    }

    @Override
    public int hashCode() {
        super.hashCode();

        if (this.groupSymbol != null) {
            final int prime = 31;
            int result = 1;

            result = prime * result + this.groupSymbol.hashCode();
            if (getTeiidVersion().isLessThan(Version.TEIID_8_0.get()))
                result = prime * result + (this.getShortCanonicalName() == null ? 0 : this.getShortCanonicalName().hashCode());
            else
                result = prime * result + (this.getShortName() == null ? 0 : this.getShortName().hashCode());

            return result;
        }

        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ElementSymbol other = (ElementSymbol)obj;

        if (this.groupSymbol == null) {
            return super.equals(obj);
        }

        if (this.type == null) {
            if (other.type != null)
                return false;
        } else if (!this.type.equals(other.type))
            return false;

        return this.groupSymbol.equals(other.groupSymbol) && this.getShortName().equals(other.getShortName());
    }

    /** Accept the visitor. **/
    @Override
    public void acceptVisitor(LanguageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ElementSymbol clone() {
        ElementSymbol clone = new ElementSymbol(this.parser, this.id);

        if (getGroupSymbol() != null)
            clone.setGroupSymbol(getGroupSymbol().clone());
        if(getType() != null)
            clone.setType(getType());
        if(getMetadataID() != null)
            clone.setMetadataID(getMetadataID());
        if(getDisplayMode() != null)
            clone.setDisplayMode(getDisplayMode());
        if(getShortCanonicalName() != null)
            clone.setShortCanonicalName(getShortCanonicalName());
        if(getShortName() != null)
            clone.setShortName(getShortName());
        if(getOutputName() != null)
            clone.setOutputName(getOutputName());

        clone.isExternalReference = isExternalReference;
        clone.isAggregate = isAggregate;

        return clone;
    }

}
/* JavaCC - OriginalChecksum=77f5180826dcbc69682360fac6b1d467 (do not edit this line) */
