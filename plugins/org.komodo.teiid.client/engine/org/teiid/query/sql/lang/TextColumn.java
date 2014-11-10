/* Generated By:JJTree: Do not edit this line. TextColumn.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=TeiidNodeFactory,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.teiid.query.sql.lang;

import org.komodo.spi.annotation.Since;
import org.komodo.spi.query.sql.lang.ITextColumn;
import org.komodo.spi.runtime.version.DefaultTeiidVersion.Version;
import org.teiid.query.parser.LanguageVisitor;
import org.teiid.query.parser.TeiidParser;

/**
 *
 */
public class TextColumn extends ProjectedColumn implements ITextColumn<LanguageVisitor> {

    private Integer width;

    private boolean noTrim;

    private String selector;

    private Integer position;

    @Since(Version.TEIID_8_7)
    private boolean ordinal;

    /**
     * @param p
     * @param id
     */
    public TextColumn(TeiidParser p, int id) {
        super(p, id);
    }

    /**
     * @return the width
     */
    public Integer getWidth() {
        return this.width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * @return the noTrim
     */
    public boolean isNoTrim() {
        return this.noTrim;
    }

    /**
     * @param noTrim the noTrim to set
     */
    public void setNoTrim(boolean noTrim) {
        this.noTrim = noTrim;
    }

    /**
     * @return the selector
     */
    public String getSelector() {
        return this.selector;
    }

    /**
     * @param selector the selector to set
     */
    public void setSelector(String selector) {
        this.selector = selector;
    }

    /**
     * @return the position
     */
    public Integer getPosition() {
        return this.position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(Integer position) {
        this.position = position;
    }

    /**
     * @return ordinal
     */
    @Since(Version.TEIID_8_7)
    public boolean isOrdinal() {
        if (isTeiid87OrGreater())
            return ordinal;

        return false;
    }

    /**
     * @param ordinal the ordinal to set
     */
    @Since(Version.TEIID_8_7)
    public void setOrdinal(boolean ordinal) {
        if (isTeiid87OrGreater())
            this.ordinal = ordinal;
    }
 
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (this.noTrim ? 1231 : 1237);
        result = prime * result + ((this.position == null) ? 0 : this.position.hashCode());
        result = prime * result + ((this.selector == null) ? 0 : this.selector.hashCode());
        result = prime * result + ((this.width == null) ? 0 : this.width.hashCode());
        result = prime * result + (this.ordinal ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        TextColumn other = (TextColumn)obj;
        if (this.noTrim != other.noTrim) return false;
        if (this.position == null) {
            if (other.position != null) return false;
        } else if (!this.position.equals(other.position)) return false;
        if (this.selector == null) {
            if (other.selector != null) return false;
        } else if (!this.selector.equals(other.selector)) return false;
        if (this.width == null) {
            if (other.width != null) return false;
        } else if (!this.width.equals(other.width)) return false;
        if (this.ordinal != other.ordinal)
            return false;
        return true;
    }

    /** Accept the visitor. **/
    @Override
    public void acceptVisitor(LanguageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public TextColumn clone() {
        TextColumn clone = new TextColumn(this.parser, this.id);

        if(getSelector() != null)
            clone.setSelector(getSelector());
        if(getWidth() != null)
            clone.setWidth(getWidth());
        clone.setNoTrim(isNoTrim());
        if(getPosition() != null)
            clone.setPosition(getPosition());
        if(getName() != null)
            clone.setName(getName());
        if(getType() != null)
            clone.setType(getType());
        clone.setOrdinal(isOrdinal());

        return clone;
    }

}
/* JavaCC - OriginalChecksum=b1ade91310a19786192f5e8514ff33a8 (do not edit this line) */
