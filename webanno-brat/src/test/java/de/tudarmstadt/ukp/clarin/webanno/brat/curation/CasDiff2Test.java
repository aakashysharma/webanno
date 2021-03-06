/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.curation;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.brat.curation.AgreementUtils.AgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.ArcDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.SpanDiffAdapter;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2006Reader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class CasDiff2Test
{
    @Test
    public void noDataTest()
        throws Exception
    {
        List<String> entryTypes = new ArrayList<>();
        
        List<DiffAdapter> diffAdapters = new ArrayList<>();

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void singleEmptyCasTest()
        throws Exception
    {
        String text = "";
        
        JCas user1Cas = JCasFactory.createJCas();
        user1Cas.setDocumentText(text);
        
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1Cas));

        List<String> entryTypes = asList(Token.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(new SpanDiffAdapter(Token.class.getName()));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
    }

    @Test
    public void twoEmptyCasTest()
        throws Exception
    {
        String text = "";
        
        JCas user1Cas = JCasFactory.createJCas();
        user1Cas.setDocumentText(text);

        JCas user2Cas = JCasFactory.createJCas();
        user2Cas.setDocumentText(text);

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1Cas));
        casByUser.put("user2", asList(user2Cas));

        List<String> entryTypes = asList(Token.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(new SpanDiffAdapter(Token.class.getName()));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(Double.NaN, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void multipleEmptyCasWithMissingOnesTest()
        throws Exception
    {
        String text = "";
        
        JCas user1Cas1 = null;

        JCas user1Cas2 = null;

        JCas user1Cas3 = JCasFactory.createJCas();
        user1Cas3.setDocumentText(text);

        JCas user1Cas4 = JCasFactory.createJCas();
        user1Cas4.setDocumentText(text);

        JCas user2Cas1 = JCasFactory.createJCas();
        user2Cas1.setDocumentText(text);

        JCas user2Cas2 = null;

        JCas user2Cas3 = null;

        JCas user2Cas4 = JCasFactory.createJCas();
        user2Cas4.setDocumentText(text);
        
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1Cas1, user1Cas2, user1Cas3, user1Cas4));
        casByUser.put("user2", asList(user2Cas1, user2Cas2, user2Cas3, user2Cas4));

        List<String> entryTypes = asList(Token.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(new SpanDiffAdapter(Token.class.getName()));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(Double.NaN, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }
    @Test
    public void noDifferencesPosTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/noDifferences/data.conll",
                "casdiff/noDifferences/data.conll");

        List<String> entryTypes = asList(POS.class.getName());
        
        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void noDifferencesDependencyTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/noDifferences/data.conll",
                "casdiff/noDifferences/data.conll");

        List<String> entryTypes = asList(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(ArcDiffAdapter.DEPENDENCY);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "DependencyType", casByUser);
        assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void noDifferencesPosDependencyTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/noDifferences/data.conll",
                "casdiff/noDifferences/data.conll");

        List<String> entryTypes = asList(POS.class.getName(), Dependency.class.getName());
        
        List<? extends DiffAdapter> diffAdapters = asList(
                SpanDiffAdapter.POS, 
                ArcDiffAdapter.DEPENDENCY);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(52, result.size());
        assertEquals(26, result.size(POS.class.getName()));
        assertEquals(26, result.size(Dependency.class.getName()));
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void singleDifferencesTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/singleSpanDifference/user1.conll",
                "casdiff/singleSpanDifference/user2.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(1, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(0.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void someDifferencesTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/someDifferences/user1.conll",
                "casdiff/someDifferences/user2.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(4, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(0.836477987d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void singleNoDifferencesTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/singleSpanNoDifference/data.conll",
                "casdiff/singleSpanNoDifference/data.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new SpanDiffAdapter(POS.class.getName(),
                "PosValue"));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(1, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(Double.NaN, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void relationDistanceTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/relationDistance/user1.conll",
                "casdiff/relationDistance/user2.conll");

        List<String> entryTypes = asList(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new ArcDiffAdapter(
                Dependency.class.getName(), "Dependent", "Governor", "DependencyType"));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(27, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(2, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "DependencyType", casByUser);
        assertEquals(1.0, agreement.getAgreement(), 0.000001d);
        assertEquals(2, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void spanLabelLabelTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/spanLabel/user1.conll",
                "casdiff/spanLabel/user2.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new SpanDiffAdapter(POS.class.getName(),
                "PosValue"));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(0.958730d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void relationLabelTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/relationLabel/user1.conll",
                "casdiff/relationLabel/user2.conll");

        List<String> entryTypes = asList(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new ArcDiffAdapter(
                Dependency.class.getName(), "Dependent", "Governor", "DependencyType"));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "DependencyType", casByUser);
        assertEquals(0.958199d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    private static final String HOST_TYPE = "LinkHost";
    private static final String LINK_TYPE = "LinkType";
    
    private static TypeSystemDescription createMultiLinkWithRoleTestTypeSytem()
        throws Exception
    {
        List<TypeSystemDescription> typeSystems = new ArrayList<>();
        
        TypeSystemDescription tsd = new TypeSystemDescription_impl();
        
        // Link type
        TypeDescription linkTD = tsd.addType(LINK_TYPE, "", CAS.TYPE_NAME_TOP);
        linkTD.addFeature("role", "", CAS.TYPE_NAME_STRING);
        linkTD.addFeature("target", "", Token.class.getName());
        
        // Link host
        TypeDescription hostTD = tsd.addType(HOST_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        hostTD.addFeature("links", "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(), false);
        
        typeSystems.add(tsd);
        typeSystems.add(TypeSystemDescriptionFactory.createTypeSystemDescription());
        TypeSystemDescription allTypes = CasCreationUtils.mergeTypeSystems(typeSystems);

        return allTypes;
    }
    
    private static void makeLinkHostFS(JCas aJCas, int aBegin, int aEnd, FeatureStructure... aLinks)
    {
        Type hostType = aJCas.getTypeSystem().getType(HOST_TYPE);
        AnnotationFS hostA1 = aJCas.getCas().createAnnotation(hostType, aBegin, aEnd);
        hostA1.setFeatureValue(hostType.getFeatureByBaseName("links"), 
                FSCollectionFactory.createFSArray(aJCas, asList(aLinks)));
        aJCas.getCas().addFsToIndexes(hostA1);
    }

    private static FeatureStructure makeLinkFS(JCas aJCas, String aSlotLabel, int aTargetBegin,
            int aTargetEnd)
    {
        Token token1 = new Token(aJCas, aTargetBegin, aTargetEnd);
        token1.addToIndexes();
        
        Type linkType = aJCas.getTypeSystem().getType(LINK_TYPE);
        FeatureStructure linkA1 = aJCas.getCas().createFS(linkType);
        linkA1.setStringValue(linkType.getFeatureByBaseName("role"), aSlotLabel);
        linkA1.setFeatureValue(linkType.getFeatureByBaseName("target"), token1);
        aJCas.getCas().addFsToIndexes(linkA1);
        
        return linkA1;
    }
    
    @Test
    public void multiLinkWithRoleNoDifferenceTest()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));        
        makeLinkHostFS(jcasA, 10, 10, makeLinkFS(jcasA, "slot1", 10, 10));        

        JCas jcasB = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 0, 0));
        makeLinkHostFS(jcasB, 10, 10, makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA));
        casByUser.put("user2", asList(jcasB));

        List<String> entryTypes = asList(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        diff.print(System.out);
        
        assertEquals(4, diff.size());
        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
        
        // Check against new impl
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE, "links",
                casByUser);

        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        AgreementUtils.dumpAgreementStudy(System.out, agreement);
        
        assertEquals(1.0d, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleLabelDifferenceTest()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));     

        JCas jcasB = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA));
        casByUser.put("user2", asList(jcasB));

        List<String> entryTypes = asList(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        diff.print(System.out);
        
        assertEquals(3, diff.size());
        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(2, diff.getIncompleteConfigurationSets().size());
        
        // Check against new impl
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE, "links",
                casByUser);

        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        AgreementUtils.dumpAgreementStudy(System.out, agreement);
        
        assertEquals(Double.NaN, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleTargetDifferenceTest()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));      

        JCas jcasB = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA));
        casByUser.put("user2", asList(jcasB));

        List<String> entryTypes = asList(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        diff.print(System.out);
        
        assertEquals(2, diff.size());
        assertEquals(1, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());

        // Check against new impl
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE, "links",
                casByUser);

        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        AgreementUtils.dumpAgreementStudy(System.out, agreement);
        
        assertEquals(0.0, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleMultiTargetDifferenceTest()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasA, 0, 0, 
                makeLinkFS(jcasA, "slot1", 0, 0),
                makeLinkFS(jcasA, "slot1", 10, 10));      

        JCas jcasB = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasB, 0, 0, 
                makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA));
        casByUser.put("user2", asList(jcasB));

        List<String> entryTypes = asList(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        diff.print(System.out);
        
        assertEquals(2, diff.size());
        assertEquals(1, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
        
//        // Check against new impl
//        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE, "links",
//                casByUser);
//
//        // Asserts
//        System.out.printf("Agreement: %s%n", agreement.toString());
//        AgreementUtils.dumpAgreementStudy(System.out, agreement);
//        
//        assertEquals(0.0, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleMultiTargetDifferenceTest2()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasA, 0, 0, 
                makeLinkFS(jcasA, "slot1", 0, 0),
                makeLinkFS(jcasA, "slot1", 10, 10));      

        JCas jcasB = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasB, 0, 0, 
                makeLinkFS(jcasB, "slot2", 10, 10));

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA));
        casByUser.put("user2", asList(jcasB));

        List<String> entryTypes = asList(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        diff.print(System.out);
        
        assertEquals(3, diff.size());
        assertEquals(1, diff.getDifferingConfigurationSets().size());
        assertEquals(2, diff.getIncompleteConfigurationSets().size());

//        // Check against new impl
//        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE, "links",
//                casByUser);
//
//        // Asserts
//        System.out.printf("Agreement: %s%n", agreement.toString());
//        AgreementUtils.dumpAgreementStudy(System.out, agreement);
//        
//        assertEquals(0.0, agreement.getAgreement(), 0.00001d);
    }

    private static Map<String, List<JCas>> load(String... aPaths)
        throws UIMAException, IOException
    {
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        int n = 1;
        for (String path : aPaths) {
            JCas cas = read(path);
            casByUser.put("user"+n, asList(cas));
            n++;
        }
        return casByUser;
    }
    
    private static JCas read(String aPath)
        throws UIMAException, IOException
    {
        CollectionReader reader = createReader(Conll2006Reader.class,
                Conll2006Reader.PARAM_SOURCE_LOCATION, "src/test/resources/" + aPath);
        
        JCas jcas = JCasFactory.createJCas();
        
        reader.getNext(jcas.getCas());
        
        return jcas;
    }
    
    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
