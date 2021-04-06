package com.comodide.axiomatization;

/**
 * 
 * 
 * @author cogan
 *
 */

public enum OWLAxAxiomType
{
	//@formatter:off
	GLOBAL_DOMAIN("Global domain"),
	SCOPED_DOMAIN("Scoped domain"),
	GLOBAL_RANGE("Global Range"),
	SCOPED_RANGE("Scoped Range"),
	EXISTENTIAL("Existential"),
	INVERSE_EXISTENTIAL("Inverse Existential"),
	FUNCTIONAL_ROLE("Functional Role"),
	QUALIFIED_FUNCTIONAL_ROLE("Qualified Functional Role"),
	SCOPED_FUNCTIONAL_ROLE("Scoped Functional Role"),
	QUALIFIED_SCOPED_FUNCTIONAL_ROLE("Qualified Scoped Functional Role"),
	INVERSE_FUNCTIONAL_ROLE("Inverse Functional Role"),
	INVERSE_QUALIFIED_FUNCTIONAL_ROLE("Inverse Qualified Functional Role"),
	INVERSE_SCOPED_FUNCTIONAL_ROLE("Inverse Scoped Functional Role"),
	INVERSE_QUALIFIED_SCOPED_FUNCTIONAL_ROLE("Inverse Qualified Scoped Functional Role"),
	STRUCTURAL_TAUTOLOGY("Structural Tautology");
	//@formatter:on
	
	private String axiomType;

	/** Private constructor for this enum. Sets <code>this.axiomType</code>. */
	OWLAxAxiomType(String axiomType)
	{
		this.axiomType = axiomType;
	}

	/** Returns the current value of <code>this.axiomType</code>. */
	public String getAxiomType()
	{
		return this.axiomType;
	}

	/**
	 * This finds the axiom type matching the given string. If the type does not exist, then
	 * an <code>IllegalArgumentException</code> is thrown.
	 * 
	 * @param owlAxAxiomType is the name of an axiom type.
	 * @return Returns a <code>OwlAxAxiomType</code> object that matches the given string.
	 * @exception <code>IllegalArgumentException</code> is thrown when the given string is not a valid axiom type.
	 */
	public static OWLAxAxiomType fromString(String owlAxAxiomType)
	{
		for (OWLAxAxiomType oaat : OWLAxAxiomType.values())
		{
			if (oaat.getAxiomType().equalsIgnoreCase(owlAxAxiomType))
				return oaat;
		}

		throw new IllegalArgumentException("The OWLAxAxiomType was not found: " + owlAxAxiomType);
	}
}
