package sharpc2b.transform

import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.util.DefaultPrefixManager

import java.util.regex.Pattern

/**
 * User: rk
 * Date: 4/24/13
 *
 * Transform a SKOS A-Box ICD9 codes ontology into a T-Box ontology.
 *
 * Initial Groovy development version of SkosABoxToTBox.  These methods were eventually migrated over to
 * SkosABoxToTBox and turned into a Java class.
 */
//@RunWith(JUnit4.class)
public class Icd9SkosABoxToTBoxTest extends GroovyTestCase {

    static String sharpCodesOntsRelPath = TrConfig.codeOntologiesRelPath;

    /*
     * SKOS
     */
    static File skosFile = FileUtil.getFileInTestResourceDir( "/onts/in/skos-core.rdfxml" );

    /*
     * Published ICD9 Codes Ontology (A-Box, using SKOS Concept, broader, notation, prefLabel)
     */
    static IRI pubCodesIRI = new IRI( "http://" + sharpCodesOntsRelPath + "icd9-pub" );

    static File pubCodesFile = FileUtil.getFileInTestResourceDir( "/onts/in/icd9-pub.ofn" );

    /*
     * T-Box defined Sharp Ontology of ICD9 Code OWL Classes
     */
    static IRI sharpCodesIRI = new IRI( "http://" + sharpCodesOntsRelPath + "icd9-classes2" );
    static File sharpCodesFile = FileUtil.getFileInTestResourceDir( "onts/out/icd9-classes2.ofn" )

    OWLOntologyManager oom;
    OWLDataFactory odf;

    PrefixManager pm;

    OWLOntology skos;
    OWLOntology icd9pub;
    OWLOntology icd9cl;
    Set<OWLOntology> onts;

    OWLClass topCodeClass;
    OWLObjectProperty refinesProp;
    OWLObjectProperty skosBroaderTransitive;
    OWLAnnotationProperty prefLabelProp;
    OWLDataProperty icd9Prop;

    @BeforeClass
    static void setUpOnce () {

    }

    @AfterClass
    static void tearDownOnce () {

    }

    @Before
    void setUp () {
        oom = OWLManager.createOWLOntologyManager();
        odf = oom.getOWLDataFactory();

//        skos = oom.loadOntologyFromOntologyDocument( new File( skosRootPath + ".rdf" ) );
//        aboxModel = oom.loadOntologyFromOntologyDocument( new File(
//                ontologiesHttpFileRoot + pubCodesOntRelPath + ".ofn" ) );
//        println "SKOS Doc IRI = <${skosDocIRI}>";
//        println "SKOS Doc IRI = <${pubCodesDocIRI}>";

        assert skosFile.exists();
        assert pubCodesFile.exists();

        skos = oom.loadOntologyFromOntologyDocument( skosFile );
        icd9pub = oom.loadOntologyFromOntologyDocument( pubCodesFile );

        onts = new HashSet<OWLOntology>();
        onts.add( skos );
        onts.add( icd9pub );
//        onts = [skos, aboxModel];
    }

    @After
    void tearDown () {

    }

    @Test
    public void testIt () {

        createClassesOntology();
    }

    OWLOntology createClassesOntology () {

        setUp();

        initNamespaces();
        createNewOntology();
        addImports();
        initObjects();
        addCommonAxioms();
        addAxiomsForCodes();

        setUpOntologyFormat();
        serialize();

        return icd9cl;
    }

    def initNamespaces () {

        pm = new DefaultPrefixManager( sharpCodesIRI.toString() + "#" );
        pm.setPrefix( "icd9:", pubCodesIRI.toString() + "#" );
        pm.setPrefix( "skos:", IriUtil.skos + "#" );
    }

    def createNewOntology () {

        icd9cl = oom.createOntology( sharpCodesIRI );
    }

    def addImports () {

        OWLImportsDeclaration importsAxiom;
        AddImport imp;

        importsAxiom = odf.getOWLImportsDeclaration( icd9pub.getOntologyID().getOntologyIRI() );
        imp = new AddImport( icd9cl, importsAxiom );
        oom.applyChange( imp );

        importsAxiom = odf.getOWLImportsDeclaration( skos.getOntologyID().getOntologyIRI() );
        imp = new AddImport( icd9cl, importsAxiom );
        oom.applyChange( imp );
    }

    def initObjects () {

        topCodeClass = odf.getOWLClass( "icd9:ICD9_Concept", pm );
        refinesProp = odf.getOWLObjectProperty( ":refines", pm );
        icd9Prop = odf.getOWLDataProperty( "skos:notation", pm );
        skosBroaderTransitive = odf.getOWLObjectProperty( "skos:broaderTransitive", pm );
        prefLabelProp = odf.getOWLAnnotationProperty( "skos:prefLabel", pm );
    }

    def addCommonAxioms () {

        Set<OWLAxiom> axioms = new TreeSet();
        axioms.add( odf.getOWLSubObjectPropertyOfAxiom( skosBroaderTransitive, refinesProp ) );
        oom.addAxioms( icd9cl, axioms );
    }

    def addAxiomsForCodes () {

        Set<OWLIndividual> codeIndividuals = topCodeClass.getIndividuals( icd9pub );
        assert codeIndividuals.size() > 0;

        for (OWLIndividual ind : codeIndividuals) {
            addAxiomsForCode( (OWLNamedIndividual) ind );
        }
    }

    def addAxiomsForCode (OWLNamedIndividual codeInd) {

        Set<OWLAnnotationAssertionAxiom> annos = icd9pub.getAnnotationAssertionAxioms( codeInd.getIRI() );
        Set<OWLAnnotationAssertionAxiom> labelAnnos = annos.findAll {
            it.getProperty().equals( prefLabelProp )
        };
        assert 1 == labelAnnos.size();
        OWLAnnotationValue value = labelAnnos.iterator().next().getValue();
        assert value instanceof OWLLiteral;
        String label = ((OWLLiteral) value).getLiteral();
        String name = localName( label );

        OWLClass codeClass = odf.getOWLClass( ":" + name, pm );

        addDefinitionUsingIndividual( codeInd, codeClass )
        addDefinitionUsingCodeValue( codeInd, codeClass )
    }

    def addDefinitionUsingIndividual (OWLNamedIndividual codeInd, OWLClass codeClass) {

        OWLObjectHasValue hasCodeValue = odf.getOWLObjectHasValue( refinesProp, codeInd );
        OWLObjectIntersectionOf codeConceptAndValue = odf.getOWLObjectIntersectionOf( hasCodeValue,
                topCodeClass );
        OWLEquivalentClassesAxiom eqAxiom = odf.getOWLEquivalentClassesAxiom( codeClass,
                codeConceptAndValue );
        assert eqAxiom;
        oom.addAxiom( icd9cl, eqAxiom );
    }

    def addDefinitionUsingCodeValue (OWLNamedIndividual codeInd, OWLClass codeClass) {

        Set<OWLLiteral> codeValues = codeInd.getDataPropertyValues( icd9Prop, icd9pub );
//        println codeValues.size();
        if (codeValues.isEmpty()) {
            println getClass().getSimpleName().toString() + ": no icd9 code: " + codeInd;
            return;
        }
        OWLLiteral litValue = codeValues.iterator().next();
        assert litValue;
//        println "icd9 code = " + litValue;
//        OWLLiteral litValue = odf.getOWLLiteral( codeValue );
        OWLDataHasValue hasCodeValue = odf.getOWLDataHasValue( icd9Prop, litValue );
        OWLObjectSomeValuesFrom some = odf.getOWLObjectSomeValuesFrom( refinesProp, hasCodeValue );

        OWLObjectIntersectionOf codeConceptAndValue = odf.getOWLObjectIntersectionOf( some,
                topCodeClass );
        OWLSubClassOfAxiom eqAxiom = odf.getOWLSubClassOfAxiom( codeConceptAndValue, codeClass );
        assert eqAxiom;
        oom.addAxiom( icd9cl, eqAxiom );
    }

    String localName (String s) {

        Pattern pat
        pat = ~/[^a-zA-Z0-9_]/;
        s.replaceAll( pat ) { "_" }
    }

    def setUpOntologyFormat () {

        OWLOntologyFormat oFormat = new OWLFunctionalSyntaxOntologyFormat();
        oFormat.copyPrefixesFrom( pm );
        oom.setOntologyFormat( icd9cl, oFormat );
    }

    def serialize () {

        oom.saveOntology( icd9cl, IRI.create( sharpCodesFile ) );
    }

}
