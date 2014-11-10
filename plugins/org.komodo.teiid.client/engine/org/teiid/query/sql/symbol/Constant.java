/* Generated By:JJTree: Do not edit this line. Constant.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.teiid.query.sql.symbol;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.komodo.spi.query.sql.symbol.IConstant;
import org.komodo.spi.runtime.version.TeiidVersion;
import org.teiid.core.types.ClobType;
import org.teiid.core.types.DataTypeManagerService;
import org.teiid.core.types.DataTypeManagerService.DefaultDataTypes;
import org.teiid.core.util.PropertiesUtils;
import org.teiid.query.function.FunctionMethods;
import org.teiid.query.parser.LanguageVisitor;
import org.teiid.query.parser.TeiidNodeFactory.ASTNodes;
import org.teiid.query.parser.TeiidParser;
import org.teiid.query.sql.lang.SimpleNode;
import org.teiid.runtime.client.Messages;

/**
 *
 */
public class Constant extends SimpleNode implements Expression, IConstant<LanguageVisitor> {

    private Class<?> type = DataTypeManagerService.DefaultDataTypes.NULL.getTypeClass();

    private boolean multiValued;

    private static Map<TeiidVersion, Constant> nullCache = new HashMap<TeiidVersion, Constant>();
    
    /**
     * @param teiidParser
     * @return a null constant for the given teiid parser if one has not already been created
     */
    public static Constant getNullConstant(TeiidParser teiidParser) {
        Constant constant = nullCache.get(teiidParser.getVersion());
        if (constant == null) {
            constant = teiidParser.createASTNode(ASTNodes.CONSTANT);
            nullCache.put(teiidParser.getVersion(), constant);
        }

        return constant;
    }

    /**
     * locale for comparison of constant values
     */
    public static final String COLLATION_LOCALE = System.getProperties().getProperty("org.teiid.collationLocale"); //$NON-NLS-1$

    /**
     * System property indicating whether to compare padded constant values
     */
    public static final boolean PAD_SPACE = PropertiesUtils.getBooleanProperty(System.getProperties(), "org.teiid.padSpace", false); //$NON-NLS-1$

    /**
     * Comparator for comparing constants
     */
    public static final Comparator<Object> COMPARATOR = getComparator(COLLATION_LOCALE, PAD_SPACE);
    
    static Comparator<Object> getComparator(String localeString, final boolean padSpace) {
        if (localeString == null) {
            return getComparator(padSpace);
        }
        String[] parts = localeString.split("_"); //$NON-NLS-1$
        Locale locale = null;
        if (parts.length == 1) {
            locale = new Locale(parts[0]);
        } else if (parts.length == 2) {
            locale = new Locale(parts[0], parts[1]);
        } else if (parts.length == 3) {
            locale = new Locale(parts[0], parts[1], parts[2]);
        } else {
            return getComparator(padSpace);
        }
        final Collator c = Collator.getInstance(locale);
        return new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                Class<?> clazz = o1.getClass();
                if (clazz == String.class) {
                    String s1 = (String)o1;
                    String s2 = (String)o2;
                    if (padSpace) {
                        s1 = FunctionMethods.rightTrim(s1, ' ', false);
                        s2 = FunctionMethods.rightTrim(s2, ' ', false);
                    }
                    return c.compare(s1, s2);
                }
                return ((Comparable<Object>)o1).compareTo(o2);
            }
        };
    }
    
    static Comparator<Object> getComparator(boolean padSpace) {
        if (!padSpace) {
            return new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    return ((Comparable<Object>)o1).compareTo(o2);
                }
            };
        }
        return new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                Class<?> clazz = o1.getClass();
                if (clazz == String.class) {
                    CharSequence s1 = (CharSequence)o1;
                    CharSequence s2 = (CharSequence)o2;
                    return comparePadded(s1, s2);
                } else if (clazz == ClobType.class) {
                    CharSequence s1 = ((ClobType)o1).getCharSequence();
                    CharSequence s2 = ((ClobType)o2).getCharSequence();
                    return comparePadded(s1, s2);
                }
                return ((Comparable<Object>)o1).compareTo(o2);
            }
        };
    }

    /**
     * @param p
     * @param id
     */
    public Constant(TeiidParser p, int id) {
        super(p, id);
    }

    /**
     * Get type of constant, if known
     * @return Java class name of type
     */
    @Override
    public Class<?> getType() {
        return this.type;
    }
    
    /**
     * @param type
     */
    public void setType(Class<?> type) {
        // Check that type is valid, then set it
        if(type == null) {
            throw new IllegalArgumentException(Messages.getString(Messages.ERR.ERR_015_010_0014));
        }
        
        this.type = type;
    }

    /**
     * Get value of constant
     * @return Constant value
     */
    @Override
    public Object getValue() {
        return this.value;
    }

    /**
     * @param value
     */
    public void setValue(Object value) {
        jjtSetValue(value);
        if (this.value == null) {
            this.type = DataTypeManagerService.DefaultDataTypes.NULL.getClass();
        } else { 
            this.type = this.value.getClass();

            Class<?> originalType = type;
            while (type.isArray() && !type.getComponentType().isPrimitive()) {
                type = type.getComponentType();
            }

            DefaultDataTypes dataType = getTeiidParser().getDataTypeService().getDataType(type);
            if (dataType != null) {
                //array of a runtime-type
                this.type = originalType;
            } else if (originalType.isArray() && !originalType.getComponentType().isPrimitive()) {
                this.type = DataTypeManagerService.DefaultDataTypes.OBJECT.getTypeArrayClass();
            } else {
                this.type = DataTypeManagerService.DefaultDataTypes.OBJECT.getClass();
            }
        }
    }

    @Override
    public boolean isMultiValued() {
        return multiValued;
    }

    final static int comparePadded(CharSequence s1, CharSequence s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int n = Math.min(len1, len2);
        int i = 0;
        int result = 0;
        for (; i < n; i++) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        result = len1 - len2;
        for (int j = i; j < len1; j++) {
            if (s1.charAt(j) != ' ') {
                return result;
            }
        }
        for (int j = i; j < len2; j++) {
            if (s2.charAt(j) != ' ') {
                return result;
            }
        }
        return 0;
    }

    /**
     * @param value
     */
    public void setMultiValued(List<?> value) {
        this.multiValued = true;
        this.value = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();

        if(this.value != null && !isMultiValued()) {
            if (this.value instanceof BigDecimal) {
                BigDecimal bd = (BigDecimal)this.value;
                int xsign = bd.signum();
                if (xsign == 0)
                    return 0;
                bd = bd.stripTrailingZeros();
                return prime * result + bd.hashCode();
            }

            result = prime * result + this.value.hashCode();
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        Constant other = (Constant)obj;

        if (this.value == null && other.value == null) {
            // Only consider type information if values are not null
            return true;
        }

        if (this.value instanceof BigDecimal) {
            if (this.value == other.value) {
                return true;
            }
            if (!(other.value instanceof BigDecimal)) {
                return false;
            }
            return ((BigDecimal)this.value).compareTo((BigDecimal)other.value) == 0;
        }

        if (this.type == null) {
            if (other.type != null) return false;
        } else if (!this.type.equals(other.type)) return false;

        return multiValued == other.multiValued && other.getValue().equals(this.getValue());
    }

    /** Accept the visitor. **/
    @Override
    public void acceptVisitor(LanguageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Constant clone() {
        Constant clone = new Constant(this.parser, this.id);

        if(getType() != null)
            clone.setType(getType());
        clone.multiValued = multiValued;
        if(getValue() != null)
            clone.setValue(getValue());

        return clone;
    }

}
/* JavaCC - OriginalChecksum=6271d54b51de261eb4775571a208cc1b (do not edit this line) */
