/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of the Alfresco Web Quick Start module.
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
 */
package org.alfresco.module.org_alfresco_module_wcmquickstart.util;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_wcmquickstart.model.WebSiteModel;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Site helper class.
 * 
 * @author Brian
 */
public class SiteHelper implements WebSiteModel
{
    private final static Log log = LogFactory.getLog(SiteHelper.class);

    /** Template for web asset URL */
    private static final String URL_WEBASSET = "http://{0}:{1}/{2}/asset/{3}/{4}";
    private static final QName TYPE_DATA_LIST = QName.createQName(NamespaceService.DATALIST_MODEL_1_0_URI, "dataList");
    private static final QName PROP_DATA_LIST_ITEM_TYPE = QName.createQName(NamespaceService.DATALIST_MODEL_1_0_URI, "dataListItemType");
    private static final String DATA_LISTS_FOLDER_NAME = "dataLists";
    private static final String FEEDBACK_DATA_LIST_NAME_FORMAT = "Visitor Feedback ({0})";

    private SiteService siteService;
    private NodeService nodeService;
    private DictionaryService dictionaryService;
    private SearchService searchService;
    private NamespaceService namespaceService;

    private Map<NodeRef,List<Locale>> websiteLocales = new HashMap<NodeRef, List<Locale>>();
    private List<Locale> defaultWebsiteLocales = new ArrayList<Locale>();
    
    public void setDefaultWebsiteLocales(List<Locale> defaultWebsiteLocales)
    {
        this.defaultWebsiteLocales = defaultWebsiteLocales;
    }

    /**
     * Given a webasset, return the full URL calculated from the containing web
     * site.
     * 
     * @param nodeRef
     *            node reference
     * @return String URL
     */
    public String getWebAssetURL(NodeRef nodeRef)
    {
        String result = null;
        if (nodeService.hasAspect(nodeRef, ASPECT_WEBASSET) == true)
        {
            NodeRef webSite = getRelevantWebSite(nodeRef);
            if (webSite != null)
            {
                // Get the parts of the URL
                Map<QName, Serializable> webSiteProps = nodeService.getProperties(webSite);
                String hostName = (String) webSiteProps.get(PROP_HOST_NAME);
                String hostPort = ((Integer) webSiteProps.get(PROP_HOST_PORT)).toString();
                String webappName = (String) webSiteProps.get(PROP_WEB_APP_CONTEXT);
                String id = nodeRef.getId();
                String fileName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);

                // Construct the URL
                result = MessageFormat.format(URL_WEBASSET, hostName, hostPort, webappName, id, fileName);
            }
        }

        return result;
    }

    /**
     * For the specified node, find the Share site that it is located in (if
     * any)
     * 
     * @param noderef
     * @return The {@link org.alfresco.service.cmr.site.SiteInfo} object
     *         representing the relevant Share site or null if the specified
     *         node is not within a Share site
     */
    public SiteInfo getRelevantShareSite(NodeRef noderef)
    {
        SiteInfo siteInfo = null;

        NodeRef parentNode = findNearestParent(noderef, SiteModel.TYPE_SITE);
        if (parentNode != null && nodeService.exists(parentNode) == true)
        {
            // If we get here then parentNode identifies a Share site.
            siteInfo = siteService.getSite(parentNode);
            if (log.isDebugEnabled())
            {
                log.debug("Found the corresponding Share site for the specified node: " + siteInfo.getShortName());
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Specified node is not within a Share site: " + noderef);
            }
        }
        return siteInfo;
    }

    /**
     * Gets the web site that a node resides within
     * 
     * @param noderef
     *            node reference
     * @return NodeRef web site node reference, null otherwise
     */
    public NodeRef getRelevantWebSite(NodeRef noderef)
    {
        return findNearestParent(noderef, WebSiteModel.TYPE_WEB_SITE);
    }

    /**
     * Gets the web root that the node resides within
     * 
     * @param nodeRef
     *            node reference
     * @return NodeRef web root node reference, null otherwise
     */
    public NodeRef getRelevantWebRoot(NodeRef nodeRef)
    {
        return findNearestParent(nodeRef, WebSiteModel.TYPE_WEB_ROOT);
    }

    /**
     * Gets the section that the node resides within
     * 
     * @param nodeRef
     *            node reference
     * @return NodeRef section node reference, null otherwise
     */
    public NodeRef getRelevantSection(NodeRef nodeRef)
    {
        return getRelevantSection(nodeRef, true);
    }

    /**
     * Gets the section that the node resides within
     * 
     * @param nodeRef
     *            node reference
     * @param allowSelf
     *            true if the supplied noderef is included in the
     *            "relevant section" test. That is to say that if this flag is
     *            true and the supplied node ref identifies a Section then this
     *            method will return the supplied node ref.
     * @return NodeRef section node reference, null otherwise
     */
    public NodeRef getRelevantSection(NodeRef nodeRef, boolean allowSelf)
    {
        return findNearestParent(nodeRef, WebSiteModel.TYPE_SECTION, allowSelf);
    }
    
    public List<Locale> getWebSiteLocales(NodeRef website)
    {
        List<Locale> locales = Collections.emptyList();
        if (website != null && nodeService.exists(website) && 
                dictionaryService.isSubClass(TYPE_WEB_SITE, nodeService.getType(website)))
        {
            locales = websiteLocales.get(website);
            if (locales == null)
            {
                locales = loadWebSiteLocales(website);
            }
        }
        return locales;
    }
    
    @SuppressWarnings("unchecked")
    private List<Locale> loadWebSiteLocales(NodeRef website)
    {
        List<Locale> results = defaultWebsiteLocales;
        List<String> languages = (List<String>) nodeService.getProperty(website, PROP_SITE_LOCALES);
        if (languages != null)
        {
            results = new ArrayList<Locale>(languages.size());
            for (String locale : languages)
            {
                results.add(new Locale(locale));
            }
        }
        //Protect against multiple concurrent writes
        synchronized(websiteLocales)
        {
            Map<NodeRef,List<Locale>> newLocaleMap = new HashMap<NodeRef, List<Locale>>(websiteLocales);
            newLocaleMap.put(website, results);
            //Atomic replacement of old map with new one.
            websiteLocales = newLocaleMap;
        }
        return results;
    }

    /**
     * Gets the named site container that a given node reference resides within
     * 
     * @param noderef
     *            node reference
     * @param containerName
     *            container name
     * @return NodeRef container node reference, null otherwise
     */
    public NodeRef getWebSiteContainer(NodeRef noderef, String containerName)
    {
        NodeRef container = null;
        SiteInfo siteInfo = getRelevantShareSite(noderef);
        NodeRef websiteId = getRelevantWebSite(noderef);
        NodeRef shareSiteId = siteInfo == null ? null : siteInfo.getNodeRef();
        if (siteInfo != null && nodeService.exists(shareSiteId) && !nodeService.getNodeStatus(shareSiteId).isDeleted())
        {
            if (websiteId == null)
            {
                websiteId = siteInfo.getNodeRef();
            }

            if (siteService.getSite(siteInfo.getShortName()) != null)
            {
                NodeRef containerParent = siteService.getContainer(siteInfo.getShortName(), websiteId.getId());
                if (containerParent == null)
                {
                    containerParent = siteService.createContainer(siteInfo.getShortName(), websiteId.getId(), null,
                            null);
                }
                container = nodeService.getChildByName(containerParent, ContentModel.ASSOC_CONTAINS, containerName);
                if (container == null)
                {
                    HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
                    props.put(ContentModel.PROP_NAME, containerName);
                    container = nodeService.createNode(containerParent, ContentModel.ASSOC_CONTAINS,
                            QName.createQName(WebSiteModel.NAMESPACE, containerName), ContentModel.TYPE_FOLDER, props)
                            .getChildRef();
                }
            }
        }
        return container;
    }

    public NodeRef getFeedbackList(String websiteName, String shareSiteName, boolean create)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Found the corresponding Share site for the specified website: " + shareSiteName);
        }
        NodeRef dataListsFolder = siteService.getContainer(shareSiteName, DATA_LISTS_FOLDER_NAME);
        if (dataListsFolder == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Failed to find data lists container for site " + shareSiteName
                        + ". Attempting to create it.");
            }
            dataListsFolder = siteService.createContainer(shareSiteName, DATA_LISTS_FOLDER_NAME, null, null);
        }
        String listTitle = MessageFormat.format(FEEDBACK_DATA_LIST_NAME_FORMAT, new Object[] { websiteName });

        NodeRef visitorFeedbackList = null;

        // Try to find a data list with the appropriate name...
        if (log.isDebugEnabled())
        {
            log.debug("Searching for data list with title: " + listTitle);
        }
        String query = "+PARENT:\"" + dataListsFolder + "\" +@cm\\:title:\"" + listTitle + "\"";
        if (log.isDebugEnabled())
        {
            log.debug("Running query: " + query);
        }
        ResultSet rs = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_LUCENE,
                query);
        //It is possible that the query will find datalists whose titles *contain* the searched for name rather than
        //matching it exactly. Look at each result and test explicitly for equality...
        for (ResultSetRow row : rs)
        {
            MLText title = (MLText)row.getValue(ContentModel.PROP_TITLE);
            if (listTitle.equals(title.getDefaultValue()))
            {
                visitorFeedbackList = row.getNodeRef();
                if (log.isDebugEnabled())
                {
                    log.debug("Found the appropriate data list: " + visitorFeedbackList);
                }
                break;
            }
        }
        
        if ((visitorFeedbackList == null) && create)
        {
            // We haven't been able to find the data list. Create it...
            if (log.isDebugEnabled())
            {
                log.debug("Failed to find required data list. Creating...");
            }
            HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
            String name = GUID.generate();
            props.put(ContentModel.PROP_NAME, name);
            props.put(ContentModel.PROP_TITLE, listTitle);
            props.put(PROP_DATA_LIST_ITEM_TYPE, TYPE_VISITOR_FEEDBACK.toPrefixString(namespaceService));
            visitorFeedbackList = nodeService.createNode(dataListsFolder, ContentModel.ASSOC_CONTAINS,
                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name), TYPE_DATA_LIST, props)
                    .getChildRef();
            if (log.isDebugEnabled())
            {
                log.debug("Created data list with properties " + props + " as node " + visitorFeedbackList);
            }
        }
        return visitorFeedbackList;
    }

    public void renameFeedbackList(NodeRef feedbackList, String newName)
    {
        if (nodeService.getType(feedbackList).equals(TYPE_DATA_LIST))
        {
            String listTitle = MessageFormat.format(FEEDBACK_DATA_LIST_NAME_FORMAT, new Object[] { newName });
            nodeService.setProperty(feedbackList, ContentModel.PROP_TITLE, listTitle);
        }
    }
    
    /**
     * When checking for translation parents for quickstart, should
     *  this node (and potentially those above it) be checked or not?
     *  
     * @param nodeRef The node to check for
     */
    public boolean isTranslationParentLimitReached(NodeRef nodeRef)
    {
        if(nodeRef == null)
        {
            return true;
        }
        
        // Stop when we reach the site
        QName nodeType = nodeService.getType(nodeRef);
        if(dictionaryService.isSubClass(nodeType, SiteModel.TYPE_SITE))
        {
            return true;
        }
        if(dictionaryService.isSubClass(nodeType, WebSiteModel.TYPE_WEB_SITE))
        {
            return true;
        }
        
        // Otherwise carry on
        return false;
    }

    /**
     * Set the site service
     * 
     * @param siteService
     *            site service
     */
    public void setSiteService(SiteService siteService)
    {
        this.siteService = siteService;
    }

    /**
     * Set the node service
     * 
     * @param nodeService
     *            node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    /**
     * Find the nearest parent in the primary child association hierarchy that
     * is of the specified content type (or a sub-type of that type).
     * 
     * @param noderef
     *            node reference
     * @param parentType
     *            parent node type
     * @return NodeRef nearest parent node reference, null otherwise
     */
    private NodeRef findNearestParent(NodeRef noderef, QName parentType)
    {
        return findNearestParent(noderef, parentType, true);
    }

    /**
     * Find the nearest parent in the primary child association hierarchy that
     * is of the specified content type (or a sub-type of that type).
     * 
     * @param noderef
     *            node reference
     * @param parentType
     *            parent node type
     * @return NodeRef nearest parent node reference, null otherwise
     */
    private NodeRef findNearestParent(NodeRef noderef, QName parentType, boolean allowSelf)
    {
        NodeRef parentNode;
        parentNode = allowSelf ? noderef : nodeService.getPrimaryParent(noderef).getParentRef();
        while (parentNode != null && nodeService.exists(parentNode) == true
                && dictionaryService.isSubClass(nodeService.getType(parentNode), parentType) == false)
        {
            parentNode = nodeService.getPrimaryParent(parentNode).getParentRef();
        }
        return parentNode;
    }
}
