package cz.vutbr.web.domassign.decode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.CSSProperty.ValueType;
import cz.vutbr.web.css.Declaration;
import cz.vutbr.web.css.SupportedCSS;
import cz.vutbr.web.css.Term;
import cz.vutbr.web.css.TermIdent;
import cz.vutbr.web.css.TermList;
import cz.vutbr.web.css.TermPropertyValue;
import cz.vutbr.web.css.Term.Operator;

/**
 * Selects appropriate variant when parsing content of CSS declaration. Allows
 * easy parsing of CSS declaration multi-values such as
 * <code>border: blue 1px</code>
 * 
 * @author kapy
 * 
 */
public abstract class Variator extends Decoder {

	/**
	 * All variants flag
	 */
	protected final static int ALL_VARIANTS = -1;

	/**
	 * Total variants available
	 */
	protected int variants;

	/**
	 * Results of variants. Each variant is allowed to be passed only once in
	 * case of multi-value declaration, so this array is used to show that
	 * currently passed variant was already successfully passed in past
	 */
	protected boolean[] variantPassed;

	/**
	 * Property names according to each variant
	 */
	protected List<String> names;

	protected List<Class<? extends CSSProperty>> types;

	/**
	 * Terms over which variants are tested
	 */
	protected List<Term<?>> terms;

	/**
	 * Creates variator which contains <code>variants</code> variants to be
	 * tested
	 * 
	 * @param variants
	 */
	public Variator(int variants) {
		this.variants = variants;
		this.variantPassed = new boolean[variants];
		this.names = new ArrayList<String>(variants);
		this.types = new ArrayList<Class<? extends CSSProperty>>(variants);
		reset();
	}
	
	/**
	 * Resets the variator to its initial state.
	 */
	public void reset() {
        for (int i = 0; i < variants; i++)
            variantPassed[i] = false;
	}

	/**
	 * This function contains parsing block for variants
	 * 
	 * @param variant
	 *            Tested variant
	 * @param iteration
	 *            Number of iteration, that is term to be tested.
	 *            This number may be changed internally in function 
	 *            to inform that more than one term was used for variant
	 * @param properties
	 *            Properties map where to store properties types
	 * @param values
	 *            Values map where to store properties values
	 * @return <code>true</code> in case of success, <code>false</code>
	 *         otherwise
	 */
	protected abstract boolean variant(int variant, IntegerRef iteration,
			Map<String, CSSProperty> properties, Map<String, Term<?>> values);

	/**
	 * Solves variant which leads to <code>inherit</code> CSS Property value.
	 * This overrides all other possible variants and no other informations are
	 * allowed per CSS Declaration.
	 * 
	 * This method is called before check for variants or before variant itself
	 * is called in one shot way.
	 * 
	 * Example: <code>margin: inherit</code> is valid value and leads to setting
	 * of
	 * <ul>
	 * <li><code>margin-top: inherit</code></li>
	 * <li><code>margin-right: inherit</code></li>
	 * <li><code>margin-bottom: inherit</code></li>
	 * <li><code>margin-left: inherit</code></li>
	 * </ul>
	 * 
	 * <code>margin: 0px inherit</code> is invalid value.
	 * 
	 * @param variant
	 *            Number of variant or identifier of all variants
	 *            <code>VARIANT_ALL</code>
	 * @param properties
	 *            Properties map where to store properties types
	 * @param term
	 *            Term to be checked
	 * @return <code>true</code> in case of success, <code>false</code>
	 *         otherwise
	 */
	protected boolean checkInherit(int variant, Term<?> term,
			Map<String, CSSProperty> properties) {

		// check whether term equals inherit
		if (!(term instanceof TermIdent)
				|| !CSSProperty.INHERIT_KEYWORD
						.equalsIgnoreCase(((TermIdent) term).getValue())) {
			return false;
		}

		if (variant == ALL_VARIANTS) {
			for (int i = 0; i < variants; i++) {
				properties.put(names.get(i), createInherit(i));
			}
			return true;
		}

		properties.put(names.get(variant), createInherit(variant));

		return true;
	}

	/**
	 * Creates INHERIT value of given class
	 * 
	 * @param i
	 *            Ordinal in list of types
	 * @return Created CSSProperty with value inherit
	 * @throws UnsupportedOperationException
	 *             If class does not provide INHERIT or is not implementation of
	 *             CSSProperty
	 */
	private CSSProperty createInherit(int i) {

		try {
			Class<? extends CSSProperty> clazz = types.get(i);
			CSSProperty property = CSSProperty.Translator.createInherit(clazz);
			if (property != null)
				return property;

			throw new IllegalAccessException("No inherit value for: "
					+ clazz.getName());
		} catch (Exception e) {
			throw new UnsupportedOperationException(
					"Unable to create inherit value", e);
		}

	}

/**
	 * Check if variant, which was passed is able to be located in place where it was 
	 * found.
	 * 
	 * Example:
	 * We have declaration:
	 * <code>font: 12px/14px sans-serif</code>
	 * Then according to grammar:
	 * <pre>
	 * 	[ 
	 * 		[ &lt;'font-style'&gt; || &lt;'font-variant'&gt; || &lt;'font-weight'&gt; ]? 
	 * 		&lt;'font-size'&gt; 
	 * 		[ / &lt;'line-height'&gt; ]? 
	 * 		&lt;'font-family'&gt; 
	 *  ] 
	 *  | caption | icon | menu | message-box | 
	 *  small-caption | status-bar | inherit
	 * </pre> 
	 * <ol>
	 * <li><code>12px</code> is assigned to <i>font-size</i></li>
	 * <li><code>14px</code> is checked to have SLASH operator before 
	 * and check to whether <i>font-size</i> was defined before it</li>
	 * <li><code>sans-serif</code> is tested to have at least 
	 * definition of <i>font-size</i> before itself</li>
	 * <li>declaration passes</li>
	 * </ol>
	 *   	  
	 * @param variant Identification of current variant which passed test 
	 * @param term Position in term list of terms which passed test, for multiple
	 * value term allow to change it
	 * @return <code>true</code> in case of success, <code>false</code> elsewhere
	 * @see Term.Operator
	 */
	protected boolean variantCondition(int variant, IntegerRef term) {
		return true;
	}

	/**
	 * Test all terms
	 * 
	 */
	public boolean vary(Map<String, CSSProperty> properties,
			Map<String, Term<?>> values) {

		// try inherit variant
		if (terms.size() == 1
				&& checkInherit(ALL_VARIANTS, terms.get(0), properties))
			return true;

		// for all terms
		for (IntegerRef i = new IntegerRef(0); i.get() < terms.size(); i.inc()) {

			boolean passed = false;

			// check all variants
			for (int v = 0; v < variants; v++) {
				// check and if variant was already found
				// signalize error by discarding complete declaration
				// have to check variant condition firstly to avoid false
				// positive
				// variantPassed
				if (!variantCondition(v, i))
					continue;
				//if this variant already passed, do not try again
				//TODO: check if we shouldn't try better combination of terms
				if (variantPassed[v])
				    continue;
				//check if this term corresponds to this variant
				passed = variant(v, i, properties, values);
				if (passed) {
					// mark occurrence of variant
					variantPassed[v] = true;
					// we have found, skip evaluation
					break;
				}
			}
			// no variant could be assigned
			if (!passed)
				return false;
		}
		// all terms passed
		return true;
	}

	/**
	 * Uses variator functionality to test selected variant on term
	 * 
	 * @param variant
	 *            Which variant will be tested
	 * @param d
	 *            The declaration on which variant will be tested
	 * @param properties
	 *            Properties map where to store property type
	 * @param values
	 *            Values map where to store property value
	 * @return <code>true</code> in case of success, <code>false</code>
	 *         otherwise
	 */
	public boolean tryOneTermVariant(int variant, Declaration d,
			Map<String, CSSProperty> properties, Map<String, Term<?>> values) {

		// only one term is allowed
		if (d.size() != 1)
			return false;

		// try inherit variant
		if (checkInherit(variant, d.get(0), properties))
			return true;

		this.terms = new ArrayList<Term<?>>();
		this.terms.add(d.get(0));

		return variant(variant, new IntegerRef(0), properties, values);
	}

	/**
	 * Uses variator functionality to test selected variant on more terms. This
	 * is used when variant is represented by more terms. Since usually only one
	 * term per variant is used, only one multiple variant is allowed per
	 * variator and should be placed as the last one
	 * 
	 * @param variant
	 *            Number of variant (last variant in variator)
	 * @param properties
	 *            Properties map where to store property type
	 * @param values
	 *            Values map where to store property value
	 * @param terms
	 *            Array of terms used for variant
	 * @return <code>true</code> in case of success, <code>false</code>
	 *         otherwise
	 */
	public boolean tryMultiTermVariant(int variant,
			Map<String, CSSProperty> properties, Map<String, Term<?>> values,
			Term<?>... terms) {

		this.terms = Arrays.asList(terms);

		// try inherit variant
		if (this.terms.size() == 1
				&& checkInherit(variant, this.terms.get(0), properties))
			return true;

		return variant(variant, new IntegerRef(0), properties, values);
	}

	/**
	 * Assigns property names for each variant
	 * 
	 * @param variantPropertyNames
	 *            List of property names
	 */
	public void assignVariantPropertyNames(String... variantPropertyNames) {
		this.names = Arrays.asList(variantPropertyNames);
	}

	/**
	 * Assigns terms to be checked by variator
	 * 
	 * @param terms
	 *            Terms to be assigned
	 */
	public void assignTerms(Term<?>... terms) {
		this.terms = Arrays.asList(terms);
	}

	/**
	 * Assigns terms from declaration
	 * 
	 * @param d
	 *            Declaration which contains terms
	 */
	public void assignTermsFromDeclaration(Declaration d) {
		this.terms = d.asList();
	}
	
	
	/**
	 * Assigns the default values to all the properties.
	 * @param properties
	 * @param values
	 */
	public void assignDefaults(Map<String, CSSProperty> properties, Map<String, Term<?>> values) {
	    SupportedCSS css = CSSFactory.getSupportedCSS();
	    for (String name : names) {
	        CSSProperty dp = css.getDefaultProperty(name);
	        if (dp != null)
	            properties.put(name, dp);
	        Term<?> dv = css.getDefaultValue(name);
	        if (dv != null)
	            values.put(name, dv);
	    }
	}

	
    //=========================================================================
    // Nested list creation
	
    /**
     * Tries a single variant that may consist of a term or a list of comma-separated terms.
     * 
     * @param variant the variant to be tried
     * @param d the declaration to be processed
     * @param properties target property map
     * @param values target value map
     * @param listValue the list_values value to be used for the property value
     * @return {@code true} when parsed successfully
     */
    public boolean tryListOfOneTermVariant(int variant, Declaration d,
            Map<String, CSSProperty> properties, Map<String, Term<?>> values,
            CSSProperty listValue) {

        // try inherit variant
        if (d.size() == 1 && checkInherit(variant, d.get(0), properties))
            return true;
        //scan the list
        TermList list = tf.createList();
        final Map<String, CSSProperty> p = new HashMap<>();
        final Map<String, Term<?>> v = new HashMap<>();
        boolean first = true;
        for (Term<?> t : d.asList()) {
            //terms must be separated by commas
            if ((first && t.getOperator() != null)
                    || (!first && t.getOperator() != Operator.COMMA))
                return false; 
            //process the variant for a single term
            p.clear();
            v.clear();
            this.terms = new ArrayList<Term<?>>();
            this.terms.add(t);
            if (!variant(variant, new IntegerRef(0), p, v))
                return false;
            //collect the resulting term
            final CSSProperty prop = p.values().iterator().next();
            final Term<?> val = (v.values().isEmpty()) ? null : v.values().iterator().next();
            final TermPropertyValue pval = tf.createPropertyValue(prop, val);
            if (!first)
                pval.setOperator(Operator.COMMA);
            list.add(pval);
            first = false;
        }
        //store the result
        properties.put(names.get(variant), listValue);
        values.put(names.get(variant), list);
        return true;
    }
    
    /**
     * Tries a single variant that may consist of space-separated terms or a comma-separated list
     * of space-separated lists.
     * 
     * @param variant the variant to be tried
     * @param d the declaration to be processed
     * @param properties target property map
     * @param values target value map
     * @param listValue the list_values value to be used for the property value
     * @return {@code true} when parsed successfully
     */
    public boolean tryListOfMultiTermVariant(int variant, Declaration d,
            Map<String, CSSProperty> properties, Map<String, Term<?>> values,
            CSSProperty listValue) {
        
        // try inherit variant
        if (d.size() == 1 && checkInherit(variant, d.get(0), properties))
            return true;
        // for all sub-declarations
        TermList list = tf.createList();
        final Map<String, CSSProperty> p = new HashMap<>();
        final Map<String, Term<?>> v = new HashMap<>();
        boolean first = true;
        List<Declaration> subs = splitDeclarations(d, Operator.COMMA);
        for (Declaration sub : subs) {
            p.clear();
            v.clear();
            assignTermsFromDeclaration(sub);
            if (!variant(variant, new IntegerRef(0), p, v))
                return false;
            final CSSProperty prop = p.values().iterator().next();
            final Term<?> val = (v.values().isEmpty()) ? null : v.values().iterator().next();
            final TermPropertyValue pval = tf.createPropertyValue(prop, val);
            if (!first)
                pval.setOperator(Operator.COMMA);
            list.add(pval);
            first = false;
        }
        //store the result
        properties.put(names.get(variant), listValue);
        values.put(names.get(variant), list);
        return true;
    }
    
    /**
     * Varies over a comma-separated list of layers where each layer defines all the variants.
     * 
     * @param d
     * @param properties
     * @param values
     * @return
     */
    public boolean varyList(Declaration d, Map<String, CSSProperty> properties,
            Map<String, Term<?>> values) {

        // try inherit variant
        if (d.size() == 1
                && checkInherit(ALL_VARIANTS, d.get(0), properties))
            return true;

        // temporary result of the whole list which will be used after final validation
        final Map<String, CSSProperty> destProps = new HashMap<>();
        final Map<String, Term<?>> destVals = new HashMap<>();
        
        // for all sub-declarations
        int listIndex = 0;
        List<Declaration> subs = splitDeclarations(d, Operator.COMMA);
        for (Declaration sub : subs) {
            reset();
            final Map<String, CSSProperty> props = new HashMap<>();
            final Map<String, Term<?>> vals = new HashMap<>();
            assignDefaults(props, vals);
            assignTermsFromDeclaration(sub);
            // for all terms
            for (IntegerRef i = new IntegerRef(0); i.get() < terms.size(); i.inc()) {
    
                boolean passed = false;
    
                // check all variants
                for (int v = 0; v < variants; v++) {
                    // check and if variant was already found
                    // signalize error by discarding complete declaration
                    // have to check variant condition firstly to avoid false
                    // positive
                    // variantPassed
                    if (!variantCondition(v, i))
                        continue;
                    //if this variant already passed, do not try again
                    //TODO: check if we shouldn't try better combination of terms
                    if (variantPassed[v])
                        continue;
                    //check if this term corresponds to this variant
                    passed = variant(v, i, props, vals);
                    if (passed) {
                        // mark occurrence of variant
                        variantPassed[v] = true;
                        // we have found, skip evaluation
                        break;
                    }
                }
                // no variant could be assigned
                if (!passed)
                    return false;
            }
            if (!validateListItem(listIndex, subs.size(), props, vals))
                return false; //validation failed
            // all terms passed
            for (String key : props.keySet()) {
                final CSSProperty p = props.get(key);
                final Term<?> v = vals.get(key);
                if (p.getValueType() == ValueType.LIST) {
                    addToMap(destVals, key, p, v, (listIndex == 0));
                    destProps.put(key, CSSProperty.Translator.createNestedListValue(p.getClass()));
                } else {
                    destProps.put(key, p);
                    destVals.put(key, v);
                }
            }
            listIndex++;
        }
        
        //validate and store the whole list
        if (!validateList(subs.size(), destProps, destVals))
            return false;
        properties.putAll(destProps);
        values.putAll(destVals);
        return true;
    }

    /**
     * Adds a property-value pair to a nested list in the destination value map. Creates a new
     * list when the corresponding value is not set or it is not a list.
     * @param dest the destination value map
     * @param key the key to use (property name)
     * @param property the property value to set
     * @param value an optional Term value to set
     * @param first {@code true} for the first value in the list (for generating separators properly)
     */
    private void addToMap(Map<String, Term<?>> dest, String key, CSSProperty property, Term<?> value, boolean first)
    {
        final Term<?> cur = dest.get(key);
        
        TermList list;
        if (cur instanceof TermList)
            list = (TermList) cur;
        else {
            list = tf.createList();
            dest.put(key, list);
        }
        
        //make a copy and remove the original operator from the value
        Term<?> vvalue = (value == null) ? null : value.shallowClone();
        if (vvalue != null)
            vvalue.setOperator(null);
        //add the pair
        final TermPropertyValue pval = tf.createPropertyValue(property, vvalue);
        if (!first)
            pval.setOperator(Operator.COMMA);
        list.add(pval);
    }
	
    protected boolean validateListItem(int listIndex, int listSize,
            Map<String, CSSProperty> properties, Map<String, Term<?>> values) {
        return true; //no validation is performed by default, this may be overriden in subclasses
    }
    
    protected boolean validateList(int listSize, Map<String, CSSProperty> properties,
            Map<String, Term<?>> values) {
        return true; //no validation is performed by default, this may be overriden in subclasses
    }
    
    //=========================================================================
	
	/**
	 * Reference to integer
	 * @author kapy
	 *
	 */
	protected static class IntegerRef {
		
		private int i;
		
		public IntegerRef(int i) {
			this.i = i;
		}

		/**
		 * @return the i
		 */
		public int get() {
			return i;
		}

		/**
		 * @param i the i to set
		 */
		public void set(int i) {
			this.i = i;
		}
		
		public void inc() {
			this.i++;
		}
		
	}
}
