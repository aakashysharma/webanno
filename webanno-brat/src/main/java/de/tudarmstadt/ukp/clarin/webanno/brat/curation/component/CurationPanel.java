/*******************************************************************************
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.brat.curation.component;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectSentenceAt;
import static org.apache.uima.fit.util.JCasUtil.selectFollowing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.SourceListView;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.CuratorUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Main Panel for the curation page. It displays a box with the complete text on the left side and a
 * box for a selected sentence on the right side.
 *
 * @author Andreas Straninger
 * @author Seid Muhie Yimam
 */
public class CurationPanel
    extends Panel
{
    private static final long serialVersionUID = -5128648754044819314L;

    private static final Log LOG = LogFactory.getLog(CurationPanel.class);

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    public final static String CURATION_USER = "CURATION_USER";

    public SuggestionViewPanel suggestionViewPanel;
    private BratAnnotator mergeVisualizer;
    private AnnotationDetailEditorPanel annotationDetailEditorPanel;

    private final WebMarkupContainer sentencesListView;
    private final WebMarkupContainer corssSentAnnoView;

    private BratAnnotatorModel bModel;

    private int fSn = 0;
    private int lSn = 0;
    private boolean firstLoad = true;
    private boolean annotate = false;
    /**
     * Map for tracking curated spans. Key contains the address of the span, the value contains the
     * username from which the span has been selected
     */
    private Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress = new HashMap<String, Map<Integer, AnnotationSelection>>();

    public SourceListView curationView;

    ListView<SourceListView> sentenceList;
    ListView<String> crossSentAnnoList;
    List<SourceListView> sourceListModel;

    // CurationContainer curationContainer;

    /**
     * Class for combining an on click ajax call and a label
     */
    class AjaxLabel
        extends Label
    {

        private static final long serialVersionUID = -4528869530409522295L;
        private AbstractAjaxBehavior click;

        public AjaxLabel(String id, String label, AbstractAjaxBehavior click)
        {
            super(id, label);
            this.click = click;
        }

        @Override
        public void onComponentTag(ComponentTag tag)
        {
            // add onclick handler to the browser
            // if clicked in the browser, the function
            // click.response(AjaxRequestTarget target) is called on the server side
            tag.put("ondblclick", "Wicket.Ajax.get({'u':'" + click.getCallbackUrl() + "'})");
            tag.put("onclick", "Wicket.Ajax.get({'u':'" + click.getCallbackUrl() + "'})");
        }

    }

    public void setModel(IModel<CurationContainer> aModel)
    {
        setDefaultModel(aModel);
    }

    public void setModelObject(CurationContainer aModel)
    {
        setDefaultModelObject(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<CurationContainer> getModel()
    {
        return (IModel<CurationContainer>) getDefaultModel();
    }

    public CurationContainer getModelObject()
    {
        return (CurationContainer) getDefaultModelObject();
    }

    public CurationPanel(String id, final IModel<CurationContainer> cCModel)
    {
        super(id, cCModel);
        // add container for list of sentences panel
        sentencesListView = new WebMarkupContainer("sentencesListView");
        sentencesListView.setOutputMarkupId(true);
        add(sentencesListView);

        // add container for the list of sentences where annotations exists crossing multiple
        // sentences
        // outside of the current page
        corssSentAnnoView = new WebMarkupContainer("corssSentAnnoView");
        corssSentAnnoView.setOutputMarkupId(true);
        add(corssSentAnnoView);

        bModel = getModelObject().getBratAnnotatorModel();

        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();
        CurationUserSegmentForAnnotationDocument curationUserSegmentForAnnotationDocument = new CurationUserSegmentForAnnotationDocument();
        if (bModel != null) {
            curationUserSegmentForAnnotationDocument
                    .setAnnotationSelectionByUsernameAndAddress(annotationSelectionByUsernameAndAddress);
            curationUserSegmentForAnnotationDocument.setBratAnnotatorModel(bModel);
            sentences.add(curationUserSegmentForAnnotationDocument);
        }
        // update source list model only first time.
        sourceListModel = sourceListModel == null ? getModelObject().getCurationViews()
                : sourceListModel;

        suggestionViewPanel = new SuggestionViewPanel("suggestionViewPanel",
                new Model<LinkedList<CurationUserSegmentForAnnotationDocument>>(sentences))
        {
            private static final long serialVersionUID = 2583509126979792202L;
            CurationContainer curationContainer = cCModel.getObject();

            @Override
            public void onChange(AjaxRequestTarget aTarget)
            {
                try {
                    // update begin/end of the curationsegment based on bratAnnotatorModel changes
                    // (like sentence change in auto-scroll mode,....
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    updatePanel(aTarget, curationContainer);
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException e) {
                    error(e.getMessage());
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                catch (BratAnnotationException e) {
                    error(e.getMessage());
                }
            }
        };

        suggestionViewPanel.setOutputMarkupId(true);
        add(suggestionViewPanel);

        annotationDetailEditorPanel = new AnnotationDetailEditorPanel(
                "annotationDetailEditorPanel", new Model<BratAnnotatorModel>(bModel))
        {
            private static final long serialVersionUID = 2857345299480098279L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
            {
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                annotate = true;

                mergeVisualizer.onChange(aTarget, aBModel);
            }
        };

        annotationDetailEditorPanel.setOutputMarkupId(true);
        add(annotationDetailEditorPanel);

        mergeVisualizer = new BratAnnotator("mergeView", new Model<BratAnnotatorModel>(bModel),
                annotationDetailEditorPanel)
        {

            private static final long serialVersionUID = 7279648231521710155L;

            @Override
            public void onChange(AjaxRequestTarget aTarget, BratAnnotatorModel bratAnnotatorModel)
            {
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                try {
                    updatePanel(aTarget, cCModel.getObject());
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException e) {
                    error(e.getMessage());
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                catch (BratAnnotationException e) {
                    error(e.getMessage());
                }
            }
        };
        // reset sentenceAddress and lastSentenceAddress to the orginal once

        mergeVisualizer.setOutputMarkupId(true);
        add(mergeVisualizer);

        LoadableDetachableModel sentenceDiffModel = new LoadableDetachableModel()
        {

            @Override
            protected Object load()
            {
                int fSN = bModel.getFSN();
                int lSN = bModel.getLSN();

                List<String> crossSentAnnos = new ArrayList<>();
                if (SuggestionBuilder.crossSentenceLists != null) {
                    for (int sn : SuggestionBuilder.crossSentenceLists.keySet()) {
                        if (sn >= fSN && sn <= lSN) {
                            List<Integer> cr = new ArrayList<>();
                            for(int c:SuggestionBuilder.crossSentenceLists.get(sn)){
                                if (c<fSN || c>lSN){
                                    cr.add(c);
                                }
                            }
                            if(!cr.isEmpty()) {
                                crossSentAnnos.add(sn + "-->"+cr);
                            }
                        }
                    }
                }

                return crossSentAnnos;
            }
        };

       crossSentAnnoList = new ListView<String>("crossSentAnnoList",
                sentenceDiffModel)
        {
            private static final long serialVersionUID = 8539162089561432091L;

            @Override
            protected void populateItem(ListItem<String> item)
            {
                String crossSentAnno = item.getModelObject();

                // ajax call when clicking on a sentence on the left side
                final AbstractDefaultAjaxBehavior click = new AbstractDefaultAjaxBehavior()
                {
                    private static final long serialVersionUID = 5803814168152098822L;

                    @Override
                    protected void respond(AjaxRequestTarget aTarget)
                    {
                        // Expand curation view
                    }

                };

                // add subcomponents to the component
                item.add(click);
                Label crossSentAnnoItem = new AjaxLabel("crossAnnoSent", crossSentAnno, click);
                item.add(crossSentAnnoItem);
            }

        };
        crossSentAnnoList.setOutputMarkupId(true);
        corssSentAnnoView.add(crossSentAnnoList);

        LoadableDetachableModel sentencesListModel = new LoadableDetachableModel()
        {

            @Override
            protected Object load()
            {

                return getModelObject().getCurationViews();
            }
        };

        sentenceList = new ListView<SourceListView>("sentencesList", sentencesListModel)
        {
            private static final long serialVersionUID = 8539162089561432091L;

            @Override
            protected void populateItem(ListItem<SourceListView> item)
            {
                final SourceListView curationViewItem = item.getModelObject();

                // ajax call when clicking on a sentence on the left side
                final AbstractDefaultAjaxBehavior click = new AbstractDefaultAjaxBehavior()
                {
                    private static final long serialVersionUID = 5803814168152098822L;

                    @Override
                    protected void respond(AjaxRequestTarget aTarget)
                    {
                        curationView = curationViewItem;
                        fSn = 0;
                        try {
                            JCas jCas = repository.readCurationCas(bModel.getDocument());
                            updateCurationView(cCModel.getObject(), curationViewItem, aTarget, jCas);
                            updatePanel(aTarget, cCModel.getObject());
                            bModel.setSentenceNumber(curationViewItem.getSentenceNumber());

                        }
                        catch (UIMAException e) {
                            error(ExceptionUtils.getRootCause(e));
                        }
                        catch (ClassNotFoundException e) {
                            error(e.getMessage());
                        }
                        catch (IOException e) {
                            error(e.getMessage());
                        }
                        catch (BratAnnotationException e) {
                            error(e.getMessage());
                        }
                    }

                };

                // add subcomponents to the component
                item.add(click);

                String cC = curationViewItem.getSentenceState().getValue();
                // mark current sentence in yellow
                if (curationViewItem.getSentenceNumber() == bModel.getSentenceNumber()) {
                    if (cC != null) {
                        item.add(AttributeModifier.append("class", "current-disagree"));
                    }
                }
                else if (cC != null) {
                    item.add(AttributeModifier.append("class", "disagree"));
                }

                try {
                    getBColor(item, curationViewItem, fSn, lSn, cC);
                }
                catch (UIMAException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                Label sentenceNumber = new AjaxLabel("sentenceNumber", curationViewItem
                        .getSentenceNumber().toString(), click);
                item.add(sentenceNumber);
            }
        };
        // add subcomponents to the component
        sentenceList.setOutputMarkupId(true);
        sentencesListView.add(sentenceList);
    }

    private void getBColor(ListItem<SourceListView> aItem, SourceListView aCurationViewItem,
            int aFSn, int aLSn, String aCC)
        throws UIMAException, ClassNotFoundException, IOException
    {
        if (aCurationViewItem.getSentenceNumber() >= aFSn
                && aCurationViewItem.getSentenceNumber() <= aLSn) {
            aItem.add(AttributeModifier.append("class", "range"));
        }
    }

    private void updateCurationView(final CurationContainer curationContainer,
            final SourceListView curationViewItem, AjaxRequestTarget aTarget, JCas jCas)
    {
        int currentSentAddress = BratAjaxCasUtil.getCurrentSentence(jCas,
                curationViewItem.getBegin(), curationViewItem.getEnd()).getAddress();
        bModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas, currentSentAddress,
                curationViewItem.getBegin(), bModel.getProject(), bModel.getDocument(), bModel
                        .getPreferences().getWindowSize()));

        Sentence sentence = selectByAddr(jCas, Sentence.class, bModel.getSentenceAddress());
        bModel.setSentenceBeginOffset(sentence.getBegin());
        bModel.setSentenceEndOffset(sentence.getEnd());

        Sentence firstSentence = selectSentenceAt(jCas, bModel.getSentenceBeginOffset(),
                bModel.getSentenceEndOffset());
        int lastAddressInPage = getLastSentenceAddressInDisplayWindow(jCas, getAddr(firstSentence),
                bModel.getPreferences().getWindowSize());
        // the last sentence address in the display window
        Sentence lastSentenceInPage = (Sentence) selectByAddr(jCas, FeatureStructure.class,
                lastAddressInPage);
        bModel.setFSN(BratAjaxCasUtil.getSentenceNumber(jCas, firstSentence.getBegin()));
        bModel.setLSN(BratAjaxCasUtil.getSentenceNumber(jCas, lastSentenceInPage.getBegin()));

        curationContainer.setBratAnnotatorModel(bModel);
        onChange(aTarget);
    }

    protected void onChange(AjaxRequestTarget aTarget)
    {

    }

    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead(response);

        if (firstLoad) {
            firstLoad = false;
        }
        else if (bModel.getProject() != null) {
            // mergeVisualizer.setModelObject(bratAnnotatorModel);
            mergeVisualizer.setCollection("#" + bModel.getProject().getName() + "/");
            mergeVisualizer.bratInitRenderLater(response);
        }
    }

    public void updatePanel(AjaxRequestTarget aTarget, CurationContainer aCC)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        JCas jCas = repository.readCurationCas(bModel.getDocument());

        final int sentenceAddress = getAddr(selectSentenceAt(jCas, bModel.getSentenceBeginOffset(),
                bModel.getSentenceEndOffset()));
        bModel.setSentenceAddress(sentenceAddress);

        final Sentence sentence = selectByAddr(jCas, Sentence.class, sentenceAddress);
        List<Sentence> followingSentences = selectFollowing(jCas, Sentence.class, sentence, bModel
                .getPreferences().getWindowSize());
        // Check also, when getting the last sentence address in the display window, if this is the
        // last sentence or the ONLY sentence in the document
        Sentence lastSentenceAddressInDisplayWindow = followingSentences.size() == 0 ? sentence
                : followingSentences.get(followingSentences.size() - 1);
        if (curationView == null) {
            curationView = new SourceListView();
        }
        curationView.setCurationBegin(sentence.getBegin());
        curationView.setCurationEnd(lastSentenceAddressInDisplayWindow.getEnd());

        int ws = bModel.getPreferences().getWindowSize();
        Sentence fs = BratAjaxCasUtil.selectSentenceAt(jCas, bModel.getSentenceBeginOffset(),
                bModel.getSentenceEndOffset());
        int l = BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(jCas, getAddr(fs), ws);
        Sentence ls = (Sentence) selectByAddr(jCas, FeatureStructure.class, l);
        fSn = BratAjaxCasUtil.getSentenceNumber(jCas, fs.getBegin());
        lSn = BratAjaxCasUtil.getSentenceNumber(jCas, ls.getBegin());

        sentencesListView.addOrReplace(sentenceList);
        aTarget.add(sentencesListView);

/*        corssSentAnnoView.addOrReplace(crossSentAnnoList);
        aTarget.add(corssSentAnnoView);
*/
        aTarget.add(suggestionViewPanel);
        if (annotate) {
            mergeVisualizer.bratRender(aTarget, annotationDetailEditorPanel.getCas(bModel));
            mergeVisualizer.bratRenderHighlight(aTarget, bModel.getSelection().getAnnotation());

        }
        else {
            mergeVisualizer.bratRenderLater(aTarget);
        }
        annotate = false;
        CuratorUtil.updatePanel(aTarget, suggestionViewPanel, aCC, mergeVisualizer, repository,
                annotationSelectionByUsernameAndAddress, curationView, annotationService,
                userRepository);
    }

}
