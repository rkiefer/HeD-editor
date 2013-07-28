package edu.asu.sharpc2b.transform;

/*
 * Copyright 2011 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.RuntimeDroolsException;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.impl.ClassPathResource;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.shapes.xsd.Jaxplorer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;


public class HeD2OwlDumper {


    private static KnowledgeBase kBase;
    private static final String HED = "org.hl7.v3.hed";


    public HeD2OwlDumper() {
        super();
        initKBase();
    }


    private void initKBase() {
        KnowledgeBuilder kBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kBuilder.add( new ClassPathResource( "edu/asu/sharpc2b/drl/hed2owl.drl" ), ResourceType.DRL );
        if ( kBuilder.hasErrors() ) {
            throw new RuntimeDroolsException( kBuilder.getErrors().toString() );
        }
        kBase = KnowledgeBaseFactory.newKnowledgeBase();
        kBase.addKnowledgePackages( kBuilder.getKnowledgePackages() );

    }

    public void compile( String inputFile, String targetFile ) throws FileNotFoundException {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( inputFile );

        Object hed = loadModel( HED, stream );

        VersionedIdentifier versionedIdentifier = hed.getMetadata().getIdentifiers().getIdentifiers().iterator().next();

        PrefixManager prefixManager = mapNamespaces( versionedIdentifier );

        OWLOntology result = transform( hed, versionedIdentifier, prefixManager );

        String path = targetFile.substring( 0, targetFile.lastIndexOf( File.separator ) );
        File dir = new File( path );
        if ( ! dir.exists() ) {
            dir.mkdirs();
        }


        PrefixOWLOntologyFormat format = new OWLFunctionalSyntaxOntologyFormat();
        format.copyPrefixesFrom( prefixManager );
        stream( result,
                new FileOutputStream( new File( targetFile ) ),
                format
        );

        PrefixOWLOntologyFormat format3 = new RDFXMLOntologyFormat();
        format3.copyPrefixesFrom( prefixManager );
        stream( result,
                new FileOutputStream( new File( targetFile.replace( ".owl", ".rdf" ) ) ),
                format3
        );


        PrefixOWLOntologyFormat format2 = new ManchesterOWLSyntaxOntologyFormat();
        format2.copyPrefixesFrom( prefixManager );
        stream( result,
                System.out,
                format2
        );
    }

    private PrefixManager mapNamespaces( VersionedIdentifier versionedIdentifier ) {
        DefaultPrefixManager prefixManager = new DefaultPrefixManager();
        prefixManager.setPrefix( "DUL:", "http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#" );
        prefixManager.setPrefix( "IOLite:", "http://www.ontologydesignpatterns.org/ont/dul/IOLite.owl#" );
        prefixManager.setPrefix( "act:", "http://asu.edu/sharpc2b/actions#" );
        prefixManager.setPrefix( "meta:", "http://asu.edu/sharpc2b/metadata#" );
        prefixManager.setPrefix( "ops:", "http://asu.edu/sharpc2b/ops#" );
        prefixManager.setPrefix( "prr:", "http://asu.edu/sharpc2b/prr#" );
        prefixManager.setPrefix( "prr-sharp:", "http://asu.edu/sharpc2b/prr-sharp#" );
        prefixManager.setPrefix( "skos:", "http://www.w3.org/2004/02/skos/core#" );
        prefixManager.setPrefix( "skos-ext:", "http://asu.edu/sharpc2b/skos-ext#" );
        prefixManager.setPrefix( "dcterms:", "http://purl.org/dc/terms/" );
        String root = versionedIdentifier.getRoot();
        if ( ! root.startsWith( "http://" ) ) {
            root = "http://" + root;
        }
        prefixManager.setPrefix( "tns:", root + "#" );
        return prefixManager;
    }


    public OWLOntology transform( Object doc, VersionedIdentifier versionedIdentifier, PrefixManager prefixManager ) {
        System.out.println( "Transforming...." );
        StatefulKnowledgeSession kSession = kBase.newStatefulKnowledgeSession();
        OWLOntology ontology = null;

        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLDataFactory factory = manager.getOWLDataFactory();

            ontology = manager.createOntology( new OWLOntologyID(
                    IRI.create( versionedIdentifier.getRoot() ),
                    IRI.create( versionedIdentifier.getRoot() + "/" + versionedIdentifier.getVersion() ) ) );

//            kSession.setGlobal( "ontology", ontology );
//            kSession.setGlobal( "manager", manager );
//            kSession.setGlobal( "factory", factory );
//            kSession.setGlobal( "prefixManager", prefixManager );
            kSession.setGlobal( "helper", new HeD2OwlHelper( ontology, manager, factory, prefixManager ) );

            kSession.setGlobal( "tns", versionedIdentifier.getRoot() + "#" );

            visit( doc, kSession, manager );

        } catch ( Exception e ) {
            e.printStackTrace();
            try {
                return OWLManager.createOWLOntologyManager().createOntology();
            } catch (OWLOntologyCreationException e1) {
                e1.printStackTrace();
                return null;
            }
        } finally {
            kSession.dispose();
        }

        System.out.println( "DONE!" );

        return ontology;
    }



    private void visit( Object doc, StatefulKnowledgeSession kSession, OWLOntologyManager manager ) throws MalformedURLException {

        System.out.println( "Visiting HeD Document.... " );

        new Jaxplorer( doc ).deepInsert( kSession );

        kSession.fireAllRules();

        System.out.println( "Done with HeD Document.... " );

    }



    public boolean stream( OWLOntology onto, OutputStream stream, OWLOntologyFormat format ) {
        try {
            onto.getOWLOntologyManager().saveOntology( onto, format, stream );
            return true;
        } catch (OWLOntologyStorageException e) {
            return false;
        }
    }


    public Object loadModel( String model, InputStream source ) {
        try {
            JAXBContext jc = JAXBContext.newInstance( model );
            Unmarshaller unmarshaller = jc.createUnmarshaller();

            return unmarshaller.unmarshal( source );
        } catch ( JAXBException e ) {
            e.printStackTrace();
            return null;
        }

    }

    public static void dumpModel( Object model, OutputStream target ) {
        try {
            JAXBContext jc = JAXBContext.newInstance( HED );
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );

            marshaller.marshal( model, target );
        } catch ( JAXBException e ) {
            e.printStackTrace();
        }

    }





    public void test() {
        OWLDataFactory f;

    }


}