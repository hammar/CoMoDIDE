package com.comodide.axiomatization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.entity.EntityCreationPreferences;
import org.protege.editor.owl.model.find.OWLEntityFinder;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comodide.ComodideConfiguration;
import com.comodide.ComodideConfiguration.EdgeCreationAxiom;
import com.comodide.editor.model.PropertyEdgeCell;
import com.comodide.exceptions.MultipleMatchesException;
import com.mxgraph.model.mxCell;

/**
 * The purpose of this class is to provide a single point of entry for the
 * management, creation, and addition of Axioms based on certain actions. For
 * example, add a new class to the ontology given a string, or rename a class,
 * etc.
 * 
 * @author cogan 
 *
 */
public class AxiomManager
{

	/**
	 * AxiomManager is a singleton class
	 */
	private static AxiomManager instance = null;

	/**
	 * Gets the static instance of this class.
	 * <p>
	 * If the instance field has not yet been initialized, then it is set to a new <code>AxiomManager</code>.
	 * 
	 * @param modelManager is passed to the private constructor if this <code>AxiomManager</code> needs to be instantiated.
	 * @return This returns the static instance of this <code>AxiomManager</code>.
	 */
	public static AxiomManager getInstance(OWLModelManager modelManager)
	{
		if (instance == null)
		{
			instance = new AxiomManager(modelManager); // , schemaDiagram);
		}
		return instance;
	}

	/** Bookkeeping */
	private final Logger log = LoggerFactory.getLogger(AxiomManager.class);
	private final String pf  = "[CoModIDE:AxiomManager] ";

	/** Used for adding axioms to the active ontology */
	private OWLModelManager                modelManager;
	private OWLOntology                    owlOntology;
	private OWLDataFactory                 owlDataFactory;
	private OWLEntityFinder                owlEntityFinder;
	private OWLEntityRenamer               owlEntityRenamer;
	/** Used for adding OWLAx Axioms to the active ontology */
	private OWLAxAxiomFactory              owlaxAxiomFactory;
	/** Used for current namespace */
	private IRI                            iri;
	/** Used for parsing axioms added to the ontology */
	private SimpleAxiomParser              simpleAxiomParser;
	/** Used for generating human readable labels */
	private static final ShortFormProvider shortFormProvider = new SimpleShortFormProvider();

	/**
	 * The private constructor for this singleton class. 
	 * <p>
	 * Once this model manager is set to the given model manager, it retrieves the active ontology. If there is an active
	 * ontology then the rest of the fields are instantiated, otherwise, the log sends a warning message.
	 * 
	 * @param modelManager initializes <code>this.modelManager</code> along with other fields in this class. It is also
	 * 					   responsible for retrieving the active ontology.
	 */
	private AxiomManager(OWLModelManager modelManager)
	{
		this.modelManager = modelManager;

		// Retrieve the active ontology
		this.owlOntology = this.modelManager.getActiveOntology();
		// Only continue with the update if there is an active ontology
		if (this.owlOntology != null)
		{
			// DataFactory is used to create axioms to add to the ontology
			this.owlDataFactory = this.modelManager.getOWLDataFactory();
			// The EntitiyFinder allows us to find existing entities within the
			// ontology
			this.owlEntityFinder = this.modelManager.getOWLEntityFinder();
			// The EntityRenamer does exactly what you think it does
			createEntityRenamer();
			// Get the namespace for the active ontology
			this.iri = this.owlOntology.getOntologyID().getOntologyIRI().orNull();
			// Create Parsers for axioms
			this.simpleAxiomParser = new SimpleAxiomParser();
			// Create the OWLAxAxiomFactory
			this.owlaxAxiomFactory = new OWLAxAxiomFactory(this.modelManager);
		}
		else
		{
			log.warn(pf + "active ontology is null.");
		}
	}

	/**
	 * This method contains the logic for initializing <code>this.owlEntityRenamer</code>. It creates a new 
	 * <code>OWLEntityRenamer</code> by getting the ontology manager and embedding the active ontology
	 * into a set. The ontology manager and set are then passed to the constructor of the new <code>OWLEntityRenamer</code>.
	 */
	private void createEntityRenamer()
	{
		// Get the Ontology Manager
		OWLOntologyManager ontologyManager = this.owlOntology.getOWLOntologyManager();
		// Embed the active ontology into a Set
		Set<OWLOntology> list = new HashSet<>();
		list.add(this.owlOntology);
		// Create the EntityRenamer
		this.owlEntityRenamer = new OWLEntityRenamer(ontologyManager, list);
	}

	/**
	 * Constructs a new axiom by passing the given type and edge cell to <code>this.owlaxAxiomFactory.createAxiomFromEdge</code>.
	 * 
	 * @param axiomType is passed to the <code>createAxiomFromEdge</code> method in the axiom factory class.
	 * @param edgeCell is passed to the <code>createAxiomFromEdge</code> method in the axiom factory class.
	 * @return Returns the new axiom.
	 */
	public OWLAxiom createOWLAxAxiom(OWLAxAxiomType axiomType, PropertyEdgeCell edgeCell)
	{
		OWLAxiom axiom = this.owlaxAxiomFactory.createAxiomFromEdge(axiomType, edgeCell);

		return axiom;
	}

	/**
	 * Constructs a new axiom by passing the given type and edge cell to the <code>createOWLAxAxiom</code> method.
	 * An <code>AddAxiom</code> object is created and applied to the active ontology.
	 * 
	 * @param axiomType is passed as an argument to the new axiom.
	 * @param edgeCell is passed as an argument to the new axiom.
	 */
	public void addOWLAxAxiom(OWLAxAxiomType axiomType, PropertyEdgeCell edgeCell)
	{
		OWLAxiom axiom = createOWLAxAxiom(axiomType, edgeCell);

		AddAxiom add = new AddAxiom(owlOntology, axiom);
		this.modelManager.applyChange(add);
	}

	/**
	 * Constructs a new axiom and initializes it to <code>this.owlDataFacotry.getOWLSubClassOfAxiom</code>.
	 * An <code>AddAxiom</code> object is created and applied to the active ontology.
	 * 
	 * @param source is converted to an <code>OWLClassExpression</code> and is passed as the source argument to
	 * 				 the <code>getOWLSubClassOfAxiom</code> method call.
	 * @param target is converted to an <code>OWLClassExpression</code> and is passed as the target argument to
	 * 				 the <code>getOWLSubClassOfAxiom</code> method call.
	 */
	public void addOWLAxAxiomtoBFO(OWLEntity source, OWLEntity target)
	{
		OWLClassExpression sourceExpression = source.asOWLClass();
		OWLClassExpression targetExpression = target.asOWLClass();
		// To be returned
		OWLAxiom owlaxAxiom = this.owlDataFactory.getOWLSubClassOfAxiom(sourceExpression, targetExpression);

		AddAxiom add = new AddAxiom(owlOntology, owlaxAxiom);
		this.modelManager.applyChange(add);
		// return owlaxAxiom;
	}

	/**
	 * Constructs a new axiom and initializes it to <code>this.owlDataFacotry.getOWLSubObjectPropertyOfAxiom</code>.
	 * An <code>AddAxiom</code> object is created and applied to the active ontology.
	 * 
	 * @param source is converted to an <code>OWLObjectPropertyExpression</code> and is passed as the source argument to
	 * 				 the <code>getOWLSubObjectPropertyOfAxiom</code> method call.
	 * @param target is converted to an <code>OWLObjectPropertyExpression</code> and is passed as the target argument to
	 * 				 the <code>getOWLSubObjectPropertyOfAxiom</code> method call.
	 */
	public void addPropertyOWLAxAxiom(OWLEntity source, OWLEntity target)
	{
		OWLObjectPropertyExpression sourcePropertyExpression = source.asOWLObjectProperty();
		OWLObjectPropertyExpression targetPropertyExpression = target.asOWLObjectProperty();
		OWLAxiom                    owlaxAxiom               = this.owlDataFactory
				.getOWLSubObjectPropertyOfAxiom(sourcePropertyExpression, targetPropertyExpression);

		AddAxiom add = new AddAxiom(owlOntology, owlaxAxiom);
		this.modelManager.applyChange(add);
	}

	/**
	 * Constructs a new axiom and initializes it to <code>this.owlDataFacotry.getOWLSubObjectPropertyOfAxiom</code>.
	 * A <code>RemoveAxiom</code> object is created and applied to the active ontology.
	 * 
	 * @param source is converted to an <code>OWLObjectPropertyExpression</code> and is passed as the source argument to
	 * 				 the <code>getOWLSubClassOfAxiom</code> method call.
	 * @param target is converted to an <code>OWLObjectPropertyExpression</code> and is passed as the target argument to
	 * 				 the <code>getOWLSubClassOfAxiom</code> method call.
	 */
	public void removePropertyOWLAxAxiom(OWLEntity source, OWLEntity target)
	{
		OWLObjectPropertyExpression sourcePropertyExpression = source.asOWLObjectProperty();
		OWLObjectPropertyExpression targetPropertyExpression = target.asOWLObjectProperty();
		OWLAxiom                    owlaxAxiom               = this.owlDataFactory
				.getOWLSubObjectPropertyOfAxiom(sourcePropertyExpression, targetPropertyExpression);
		// OWLAxiom axiom = this.owlaxAxiomFactory.createAxiomOfProperty(axiomType,
		// source, target);

		RemoveAxiom add = new RemoveAxiom(owlOntology, owlaxAxiom);
		this.modelManager.applyChange(add);

	}

	/**
	 * Constructs a new axiom and initializes it to <code>createOWLAxAxiom</code>.
	 * A <code>RemoveAxiom</code> object is created and applied to the active ontology.
	 * 
	 * @param axiomType is passed to <code>createOWLAxAxiom</code>.
	 * @param edgeCell is passed to <code>createOWLAxAxiom</code>.
	 */
	public void removeOWLAxAxiom(OWLAxAxiomType axiomType, PropertyEdgeCell edgeCell)
	{
		OWLAxiom axiom = createOWLAxAxiom(axiomType, edgeCell);

		RemoveAxiom add = new RemoveAxiom(owlOntology, axiom);
		this.modelManager.applyChange(add);
	}

	/**
	 * Constructs a new axiom and initializes it to <code>this.owlDataFacotry.getOWLSubObjectPropertyOfAxiom</code>.
	 * A <code>RemoveAxiom</code> object is created and applied to the active ontology.
	 * 
	 * @param source is converted to an <code>OWLClassExpression</code> and is passed as the source argument to
	 * 				 the <code>getOWLSubClassOfAxiom</code> method call.
	 * @param target is converted to an <code>OWLClassExpression</code> and is passed as the target argument to
	 * 				 the <code>getOWLSubClassOfAxiom</code> method call.
	 */
	public void removeOWLAxAxiomtoBFO(OWLEntity source, OWLEntity target)
	{
		OWLClassExpression sourceExpression = source.asOWLClass();
		OWLClassExpression targetExpression = target.asOWLClass();
		// To be returned
		OWLAxiom owlaxAxiom = this.owlDataFactory.getOWLSubClassOfAxiom(sourceExpression, targetExpression);
		// OWLAxiom axiom = this.owlaxAxiomFactory.createAxiom(axiomType, source,
		// property, target);

		RemoveAxiom add = new RemoveAxiom(owlOntology, owlaxAxiom);
		this.modelManager.applyChange(add);
	}

	/**
	 * This method  attempts to find a Class with the existing targetName
	 * If it does not find it, it will create a Class with target name in the active
	 * ontology.
	 * <p>
	 * If the currentClass is null, then it will return the result from above.
	 * <p>
	 * If the currentClass is not null and targetName exists, it will return
	 * targetName
	 * <p>
	 * If the currentClass is not null and targetName did not exist, it will rename
	 * currentClass to targetName
	 *
	 * @param currentClass will be renamed to the target name if a class with the 
	 * 					   target name does not already exist.
	 * @param targetName is the name of the target class.
	 * @return This returns a class with the target name.
	 */
	public OWLEntity handleClassChange(OWLClass currentClass, String targetName)
	{
		OWLClass targetClass = findClass(targetName);

		if (targetClass != null)
		{
			return targetClass;
		}

		if (currentClass == null)
		{
			return addNewClass(targetName);
		}
		else
		{
			return renameClass(currentClass, targetName);
		}
	}

	/**
	 * Determines if there is a class with the given sting className. If there is, the <code>renameClass</code> method is called
	 * and passed the given name.
	 * <p>
	 * If there are no classes with the given name, then the <code>addNewClass</code> method is called and passed the
	 * given name. 
	 * 
	 * @param className is the name of a class that this method will rename or add.
	 * @return This returns a class with the given name as an <code>OWLEntity</code>.
	 */
	public OWLEntity handleClass(String className)
	{
		// Determine if the new class that we are naming the node exists
		OWLClass cls = findClass(className);

		// Null indicates that there was no class with that name, so create a new one
		if (cls == null)
		{
			return addNewClass(className);
		}
		else
		{
			return renameClass(cls, className);
		}

	}

	/**
	 * Determines if a class with the given class name exists. If there is not a class with the given name, then 
	 * the <code>addNewClass</code> method is called and passed the given name.
	 * 
	 * @param className is the name of the class this method will find or add.
	 * @return This returns a class with the given class name.
	 */
	public OWLEntity findOrAddClass(String className)
	{
		OWLClass owlClass = findClass(className);

		if (owlClass == null)
		{
			owlClass = addNewClass(className);
		}

		return owlClass;
	}

	/**
	 * This generates a set of entities by calling <code>this.owlEntityFinder.getMatchingOWLEntities</code> and passing 
	 * the given string entity to the <code>getMatchingOWLEntities</code> method.
	 * <p>
	 * If there is more than one element in the set, an exception is thrown.
	 * <p>
	 * If there are no elements in the set, then null is returned.
	 * 
	 * @param entity is the string representation of the entity this method attempts to find.
	 * @return This returns an entity matching the given string entity.
	 * @exception MultipleMatchesException is thrown if <code>this.owlEntityFinder.getMatchingOWLEntities</code> finds
	 * 									   multiple entities that match the given string entity.
	 */
	public OWLEntity findEntity(String entity) throws MultipleMatchesException
	{
		Set<OWLEntity> foundEntities = this.owlEntityFinder.getMatchingOWLEntities(entity);
		if (foundEntities.size() > 1)
		{
			throw new MultipleMatchesException(String.format(
					"[CoModIDE:AxiomManager] There are multiple OWL entities matching the identifier %s.", entity));
		}
		else if (foundEntities.isEmpty())
		{
			return null;
		}
		else
		{
			return foundEntities.iterator().next();
		}
	}

	/**
	 * This generates a set of classes by calling <code>this.owlEntityFinder.getMatchingOWLClasses</code> and passing the
	 * <code>getMatchingOWLClasses</code> method the given string clazz.
	 * <p>
	 * If there is more than one element in the set, the <code>MultipleMatchesException</code> is thrown and 
	 * <code>log.warn</code> is called. This method includes a catch block for the <code>MultipleMatchesException</code>.
	 * It generates an array of classes, and returns the first class in the array.
	 * <p>
	 * If there are no elements in the set, then null is returned.
	 *  
	 * @param clazz is the string representation of the class this method attempts to find.
	 * @return Returns a class matching the given string clazz.
	 * @exception MultipleMatchesException is thrown if <code>this.owlEntityFinder.getMatchingOWLClasses</code> finds
	 * 									   multiple classes that match the given string clazz.
	 */
	public OWLClass findClass(String clazz)
	{
		// Get the list of matches
		// Based on how it is used here, we should never see more than one match.
		// TODO sanitize for wildcard
		Set<OWLClass> classes = this.owlEntityFinder.getMatchingOWLClasses(clazz);

		// Do some basic handling of potential errors
		try
		{
			if (classes.size() > 1)
			{
				log.warn("[CoModIDE:AxiomManager] The returned class is not guaranteed to be the correct match.");
				throw new MultipleMatchesException("The returned class is not guaranteed to be the correct match.");
			}
			else if (classes.isEmpty())
			{
				// No class of that name was found.
				return null;
			}
			else
			{
				// Return the matched class
				OWLClass cls = (new ArrayList<OWLClass>(classes)).get(0);
				return cls;
			}
		}
		catch (MultipleMatchesException e)
		{
			// Return something? or is it better to return nothing?
			OWLClass cls = (new ArrayList<OWLClass>(classes)).get(0);
			return cls;
		}
	}
	
	/**
	 * This method starts by constructing a class IRI with the active namespace and the given string str. The class
	 * IRI is then passed to <code>this.owlDataFactory.getOWLClass</code> to create the new class. Before an 
	 * <code>AddAxiom</code> object can be created, this method gets the declaration axiom from the new class. The 
	 * declaration axiom is passed to a new <code>AddAxiom</code>, and the change is applied to the active ontology.
	 * <p>
	 * This method is called when previousLabel.equals("")
	 * 
	 * @param str is the name of the new class and is used to construct the class IRI.
	 * @return Returns the new class.
	 */
	public OWLClass addNewClass(String str)
	{
		/* Construct the Declaration Axiom */
		// Create the IRI for the class using the active namespace
		String entitySeparator = EntityCreationPreferences.getDefaultSeparator();
		IRI    classIRI        = IRI.create(iri + entitySeparator + str);
		// Create the OWLAPI construct for the class
		OWLClass owlClass = this.owlDataFactory.getOWLClass(classIRI);
		// Create the Declaration Axiom
		OWLDeclarationAxiom oda = this.owlDataFactory.getOWLDeclarationAxiom(owlClass);
		// Create the Axiom Change
		AddAxiom addAxiom = new AddAxiom(this.owlOntology, oda);
		// Apply the change to the active ontology!
		this.modelManager.applyChange(addAxiom);
		// Return a reference to the class that was added
		return owlClass;
	}
	
	/**
	 * Constructs a new class IRI using the active namespace and the given string newName. A list of 
	 * <code>OWLOntologyChange</code> objects is generated using <code>this.owlEntityRenamer.ChangeIRI</code>.
	 * The given old class and the new IRI are passed to the <code>changeIRI</code> method. Finally, the list of changes
	 * is applied to the active ontology.
	 * <p>
	 * This method should be called when !previousLabel.equals("")
	 * 
	 * @param oldClass is passed to the <code>changeIRI</code> method.
	 * @param newName is used to create the new IRI for the given old class.
	 * @return Returns an <code>owlClass</code> with the new IRI.
	 */
	public OWLClass renameClass(OWLClass oldClass, String newName)
	{
		// Construct new class IRI
		String                  entitySeparator = EntityCreationPreferences.getDefaultSeparator();
		IRI                     newIRI          = IRI.create(iri + entitySeparator + newName);
		List<OWLOntologyChange> changes         = this.owlEntityRenamer.changeIRI(oldClass, newIRI);
		// Apply the changes
		this.modelManager.applyChanges(changes);
		// Construct the OWLClass to return
		OWLClass owlClass = this.owlDataFactory.getOWLClass(newIRI);
		// Return a reference of the class based on the new IRI
		return owlClass;
	}

	/**
	* This method should be called when newValue == null
	*/
	public void removeClass()
	{
		// TODO Why is this empty???!!! (3/15/20)
	}

	/**
	 * This method determines if the given string datatype is an existing <code>OWLDatatype</code>.
	 * 
	 * @param datatype is the name of the target <code>OWLDatatype</code>.
	 * @return Returns a list of <code>OWLDatatype</code> objects that match the given string.
	 */
	public List<OWLDatatype> addDatatype(String datatype)
	{
		// Determine if datatype property exists?
		Set<OWLDatatype> datatypes = this.owlEntityFinder.getMatchingOWLDatatypes(datatype);
		// return the matches
		return new ArrayList<>(datatypes);
	}

	/**
	 * Generates a set of <code>OWLDatatype</code> that match the given string datatype.
	 * <p>
	 * If there is more than one element in the set, a <code>MultipleMatchesException</code> is thrown. This method 
	 * includes a catch block for the <code>MultipleMatchesException</code>. It generates an array of <code>OWLDatatype</code>, 
	 * and returns the first data type in the array.
	 * <p>
	 * If there are no elements in the set, then null is returned. 
	 * 
	 * @param datatype is the name of the target <code>OWLDatatype</code>.
	 * @return Returns the <code>OWLDatatype</code> that matches the given string.
	 * @exception MultipleMatchesException is thrown when there are multiple data types that match the given string.
	 */
	public OWLDatatype findDatatype(String datatype)
	{
		// Determine if datatype property exists?
		Set<OWLDatatype> datatypes = this.owlEntityFinder.getMatchingOWLDatatypes(datatype);

		// Do some basic handling of potential errors
		try
		{
			if (datatypes.size() > 1)
			{
				throw new MultipleMatchesException("The returned class is not guaranteed to be the correct match.");
			}
			else if (datatypes.isEmpty())
			{
				// I hate this
				return null;
			}
			else
			{
				// Return the matched class
				OWLDatatype matchedDatatype = (new ArrayList<OWLDatatype>(datatypes)).get(0);
				return matchedDatatype;
			}
		}
		catch (MultipleMatchesException e)
		{
			// Return something? or is it better to return nothing?
			OWLDatatype cls = (new ArrayList<OWLDatatype>(datatypes)).get(0);
			return cls;
		}

	}

	/**
	 * This calls the <code>findObjectProperty</code> method to find the property with the given property name. If 
	 * there is not a property with the given name, then a new one is added and a set of <code>EdgeCreationAxiom</code>
	 * is created to determine which axioms to create.
	 * <p>
	 * If the set contains <code>EdgeCreationAxiom.RDFS_DOMAIN_RANGE</code> then the property, domain, and range are used
	 * to create <code>OWLObjectPropertyDomainAxiom</code> and <code>OWLObjectPropertyRangeAxiom</code> objects. The objects
	 * are then packaged into <code>AddAxiom</code> objects and applied to the active ontology.
	 * <p>
	 * If the set contains <code>EdgeCreationAxiom.SCOPED_DOMAIN</code> or <code>EdgeCreationAxiom.SCOPED_RANGE</code>, then
	 * the property and range will be used to construct <code>OWLObjectSomeValuesFrom</code> and 
	 * <code>OWLObjectSomeValuesFrom</code> objects respectively. The domain is used to generate the sub class axiom, which
	 * is passed to the <code>AddAxiom</code> object and applied to the active ontology.
	 * 
	 * @param propertyName is the name of the target property.
	 * @param domain is used to create a domain axiom or a sub class axiom depending on which axioms are in the set.
	 * @param range is used to create a range axiom, an <code>OWLObjectSomeValuesFrom</code> object, or an 
	 * 				<code>OWLObjectAllValuesFrom</code> object depending on which axioms are in the set.
	 * @return This returns the property with the given name.
	 */
	public OWLObjectProperty handleObjectProperty(String propertyName, OWLEntity domain, OWLEntity range)
	{
		// Perhaps the ObjectProperty already exists?
		OWLObjectProperty property = findObjectProperty(propertyName);

		// Create the ObjectProperty and add the requisite axioms
		if (property == null)
		{
			property = addNewObjectProperty(propertyName);

			// Get which axioms to create
			Set<EdgeCreationAxiom> axiomGenerationConfiguration = ComodideConfiguration.getSelectedEdgeCreationAxioms();

			/*
			 * These if statements will add an axiom for each of the EdgeCreationAxioms as
			 * selected in the Pattern Configuration view
			 */
			if (axiomGenerationConfiguration.contains(EdgeCreationAxiom.RDFS_DOMAIN_RANGE))
			{
				// Create the OWLAPI construct for the property domain/range restriction
				OWLObjectPropertyDomainAxiom opda = this.owlDataFactory.getOWLObjectPropertyDomainAxiom(property,
						domain.asOWLClass());
				OWLObjectPropertyRangeAxiom  opra = this.owlDataFactory.getOWLObjectPropertyRangeAxiom(property,
						range.asOWLClass());
				// Package into AddAxioms
				AddAxiom addDomainAxiom = new AddAxiom(this.owlOntology, opda);
				AddAxiom addRangeAxiom  = new AddAxiom(this.owlOntology, opra);
				// Make the change to the ontology
				ArrayList<AddAxiom> changes = new ArrayList<AddAxiom>(Arrays.asList(addDomainAxiom, addRangeAxiom));
				this.modelManager.applyChanges(changes);
			}

			if (axiomGenerationConfiguration.contains(EdgeCreationAxiom.SCOPED_DOMAIN))
			{
				OWLObjectSomeValuesFrom someValuesFrom = this.owlDataFactory.getOWLObjectSomeValuesFrom(property,
						range.asOWLClass());
				OWLSubClassOfAxiom      subClassAxiom  = this.owlDataFactory.getOWLSubClassOfAxiom(someValuesFrom,
						domain.asOWLClass());
				AddAxiom                addAxiomChange = new AddAxiom(this.owlOntology, subClassAxiom);
				this.modelManager.applyChange(addAxiomChange);
			}

			if (axiomGenerationConfiguration.contains(EdgeCreationAxiom.SCOPED_RANGE))
			{
				OWLObjectAllValuesFrom allValuesFrom  = this.owlDataFactory.getOWLObjectAllValuesFrom(property,
						range.asOWLClass());
				OWLSubClassOfAxiom     subClassAxiom  = this.owlDataFactory.getOWLSubClassOfAxiom(domain.asOWLClass(),
						allValuesFrom);
				AddAxiom               addAxiomChange = new AddAxiom(this.owlOntology, subClassAxiom);
				this.modelManager.applyChange(addAxiomChange);
			}
		}
		return property;
	}
	
	/**
	 * This calls the <code>getMatchingOWLObjectProperties</code> method from this <code>owlEntityFinder</code> to generate
	 * a set of properties that match the given property name.
	 * <p>
	 * If there are no elements in the set, then null is returned.
	 * 
	 * @param propertyName is the name of the property this method will search for.
	 * @return Returns the first property in the set whose short form matches the given property name.
	 */
	public OWLObjectProperty findObjectProperty(String propertyName)
	{
		Set<OWLObjectProperty> properties = this.owlEntityFinder.getMatchingOWLObjectProperties(propertyName);

		if (properties.isEmpty())
		{
			return null;
		}
		else
		{
			for (OWLObjectProperty op : properties)
			{
				if (shortFormProvider.getShortForm(op).contentEquals(propertyName))
				{
					return op;
				}
			}
		}

		return null;
	}

	/**
	 * This creates an <code>IRI</code> for the new <code>OWLObjectProperty</code> from the active namespace and the given property name.
	 * The new <code>IRI</code> is passed to <code>this.owlDataFactory.getOWLObjectProperty</code>, which will initialize a new
	 * <code>OWLObjectProperty</code>. This method finishes by creating an <code>AddAxiom</code> object and applying it to the
	 * active ontology.
	 * 
	 * @param propteryName is used to create an <code>IRI</code> for the new <code>OWLObjectProperty</code>.
	 * @return This returns the new object property.
	 */
	public OWLObjectProperty addNewObjectProperty(String propertyName)
	{
		// Create the IRI for the class using the active namespace
		String entitySeparator = EntityCreationPreferences.getDefaultSeparator();
		IRI    propertyIRI     = IRI.create(iri + entitySeparator + propertyName);
		// Create the OWLAPI construct for the class
		OWLObjectProperty objectProperty = this.owlDataFactory.getOWLObjectProperty(propertyIRI);
		// Create the Declaration Axiom
		OWLDeclarationAxiom oda = this.owlDataFactory.getOWLDeclarationAxiom(objectProperty);
		// Create the Axiom Change
		AddAxiom addAxiom = new AddAxiom(this.owlOntology, oda);
		// Apply the change to the active ontology!
		this.modelManager.applyChange(addAxiom);
		// Return a reference to the class that was added
		return objectProperty;
	}

	/**
	 * This calls the <code>findDataProperty</code> method to find a data property with the given property name. If it does not
	 * find one, then <code>addNewDataProperty</code> is called to add a property with the given name and a set of
	 * <code>EdgeCreationAxiom</code> is created to determine which axioms to create.
	 * <p>
	 * If the set contains <code>EdgeCreationAxiom.RDFS_DOMAIN_RANGE</code>, then the property, domain, and range are used
	 * to create <code>OWLObjectPropertyDomainAxiom</code> and <code>OWLObjectPropertyRangeAxiom</code> objects. The objects
	 * are then packaged into <code>AddAxiom</code> objects and applied to the active ontology.
	 * 
	 * @param propertyName is the name of the property this method will either find or create.
	 * @param domain is used create a domain axiom, which is passed as a parameter to the <code>ArrayList</code> of 
	 * 				 <code>AddAxiom</code> objects.
	 * @param range is used create a range axiom, which is passed as a parameter to the <code>ArrayList</code> of 
	 * 				 <code>AddAxiom</code> objects.
	 * @return This returns the data property that has the given property name.
	 */
	public OWLDataProperty handleDataProperty(String propertyName, OWLEntity domain, OWLEntity range)
	{
		OWLDataProperty dataProperty = findDataProperty(propertyName);

		if (dataProperty == null)
		{
			dataProperty = addNewDataProperty(propertyName);

			// Get which axioms to create
			Set<EdgeCreationAxiom> axioms = ComodideConfiguration.getSelectedEdgeCreationAxioms();

			/*
			 * These if statements will add an axiom for each of the EdgeCreationAxioms as
			 * selected in the Pattern Configuration view
			 */
			if (axioms.contains(EdgeCreationAxiom.RDFS_DOMAIN_RANGE))
			{
				// Create the OWLAPI construct for the property domain/range restriction
				OWLDataPropertyDomainAxiom opda = this.owlDataFactory.getOWLDataPropertyDomainAxiom(dataProperty,
						domain.asOWLClass());
				OWLDataPropertyRangeAxiom  opra = this.owlDataFactory.getOWLDataPropertyRangeAxiom(dataProperty,
						range.asOWLDatatype());
				// Package into AddAxioms
				AddAxiom addDomainAxiom = new AddAxiom(this.owlOntology, opda);
				AddAxiom addRangeAxiom  = new AddAxiom(this.owlOntology, opra);
				// Make the change to the ontology
				ArrayList<AddAxiom> changes = new ArrayList<AddAxiom>(Arrays.asList(addDomainAxiom, addRangeAxiom));
				this.modelManager.applyChanges(changes);
			}
		}
		return dataProperty;
	}
	
	/**
	 * This calls the <code>getMatchingOWLDataProperties</code> method from this <code>owlEntityFinder</code> to generate
	 * a set of data properties that match the given property name.
	 * <p>
	 * If there are no elements in the set, then null is returned.
	 * 
	 * @param propertyName is the name of the property this method will search for.
	 * @return Returns the first data property in the set whose short form matches the given property name.
	 */
	public OWLDataProperty findDataProperty(String propertyName)
	{
		Set<OWLDataProperty> properties = this.owlEntityFinder.getMatchingOWLDataProperties(propertyName);

		if (properties.isEmpty())
		{
			return null;
		}
		else
		{
			for (OWLDataProperty dp : properties)
			{
				if (shortFormProvider.getShortForm(dp).contentEquals(propertyName))
				{
					return dp;
				}
			}
		}

		return null;
	}

	/**
	 * This creates an <code>IRI</code> for the new <code>OWLDataProperty</code> from the active namespace and the given property
	 * name. The new <code>IRI</code> is passed to <code>this.owlDataFactory.getOWLDataProperty</code>, which will initialize a 
	 * new <code>OWLDataProperty</code>. This method finishes by creating an <code>AddAxiom</code> object and applying it to the
	 * active ontology.
	 * 
	 * @param propteryName is used to create an <code>IRI</code> for the new <code>OWLDataProperty</code>.
	 * @return This returns the new data property.
	 */
	public OWLDataProperty addNewDataProperty(String propertyName)
	{
		// Create the IRI for the class using the active namespace
		String entitySeparator = EntityCreationPreferences.getDefaultSeparator();
		IRI    propertyIRI     = IRI.create(iri + entitySeparator + propertyName);
		// Create the OWLAPI construct for the class
		OWLDataProperty dataProperty = this.owlDataFactory.getOWLDataProperty(propertyIRI);
		// Create the Declaration Axiom
		OWLDeclarationAxiom oda = this.owlDataFactory.getOWLDeclarationAxiom(dataProperty);
		// Create the Axiom Change
		AddAxiom addAxiom = new AddAxiom(this.owlOntology, oda);
		// Apply the change to the active ontology!
		this.modelManager.applyChange(addAxiom);
		// Return a reference to the class that was added
		return dataProperty;
	}

	/**
	 * This casts the given axiom as a subClass axiom and then returns the value of the 
	 * <code>this.simpleAxiomParser.parseSimpleAxiom</code> method call.
	 *  
	 * @param axiom is the axiom to be parsed.
	 * @return Returns the result of parsing the axiom.
	 */
	public mxCell parseSimpleAxiom(OWLAxiom axiom)
	{
		return this.simpleAxiomParser.parseSimpleAxiom((OWLSubClassOfAxiom) axiom);
	}

	/**
	 * Constructs a new axiom by passing the given type and edge cell to the <code>createAxiomFromEdge</code> method in this
	 * <code>owlaxAxiomFactory</code>. The active ontology is then searched for an axiom that matches the newly created axiom.
	 * 
	 * @param axiomType is used to construct the axiom that will be compared to all of the axioms in the active ontology.
	 * @param edgeCell is used to construct the axiom that will be compared to all of the axioms in the active ontology.
	 * @return Returns true if there is an axiom in the active ontology that matches the axiom created from the given type
	 * 		   and edge cell.
	 */
	public boolean matchOWLAxAxiomType(OWLAxAxiomType axiomType, PropertyEdgeCell edgeCell)
	{
		OWLAxiom owlaxAxiom = this.owlaxAxiomFactory.createAxiomFromEdge(axiomType, edgeCell);

		for (OWLAxiom axiom : this.owlOntology.getAxioms())
		{
			if (axiom.equalsIgnoreAnnotations(owlaxAxiom))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates a sub class axiom by passing the given source and target to <code>this.owlDataFactory.getOWLSubClassOfAxiom</code>.
	 * The active ontology is then searched for a sub class that matches the newly created sub class.
	 * 
	 * @param source is used to create the sub class axiom that will be compared to all of the sub class axioms in
	 * 		  the active ontology.
	 * @param target is used to create the sub class axiom that will be compared to all of the sub class axioms in
	 * 		  the active ontology.
	 * @return This returns true if there is a matching sub class.
	 */
	public boolean matchSubClassAxiom(OWLClass source, OWLClass target)
	{
		// Create the subclass axiom
		OWLAxiom subClassAxiom = this.owlDataFactory.getOWLSubClassOfAxiom(source, target);
		for (OWLAxiom axiom : this.owlOntology.getSubClassAxiomsForSubClass(source))
		{
			// If they're equal, finish
			if (axiom.equalsIgnoreAnnotations(subClassAxiom))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Converts the source argument into an <code>OWLObjectPropertyExpression</code>, and constructs a subClass axiom using
	 * the source expression and the given target expression. The active ontology is then searched for a axioms with matching
	 * source expressions and sub class axioms.
	 * 
	 * @param source is converted to a source property expression and used to search the active ontology for
	 * 		  axioms with matching source expressions.
	 * @param targetPropertyExpression is used to create a subClass axiom. If an axiom with a matching source expression is found,
	 * 		  then it is checked for a matching sub class.
	 * @return Returns true if there is a sub property axiom in the active ontology with the same source expression and sub class
	 * 		   axiom.
	 */
	public boolean matchSubPropertyAxiom(OWLEntity source, OWLObjectProperty targePropertyExpression)
	{
		OWLObjectPropertyExpression sourcePropertyExpression = source.asOWLObjectProperty();
		// Create the subclass axiom
		OWLAxiom subClassAxiom = this.owlDataFactory.getOWLSubObjectPropertyOfAxiom(sourcePropertyExpression,
				targePropertyExpression);
		for (OWLAxiom owlaxAxiom : this.owlOntology.getObjectSubPropertyAxiomsForSubProperty(sourcePropertyExpression))
		{
			// If they're equal, finish
			if (owlaxAxiom.equalsIgnoreAnnotations(subClassAxiom))
			{
				return true;
			}
		}
		return false;
	}
}
