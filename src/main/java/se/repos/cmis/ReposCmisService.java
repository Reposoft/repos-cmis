/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cmis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.enums.CapabilityContentStreamUpdates;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.CapabilityRenditions;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.DocumentTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FolderTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryCapabilitiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractCmisService;
import org.apache.chemistry.opencmis.commons.server.CallContext;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.commit.CmsCommit;
import se.simonsoft.cms.item.impl.CmsItemIdUrl;
import se.simonsoft.cms.item.info.CmsItemLookup;

/**
 * CMIS Service Implementation.
 */
public class ReposCmisService extends AbstractCmisService {

    private final String repositoryRoot;
    private CallContext context;
    private CmsRepository repository;
    private CmsItemLookup lookup;
    private CmsCommit commit;
    private final ArrayList<TypeDefinition> types;

    public ReposCmisService(String repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
        this.types = new ArrayList<TypeDefinition>();
        this.types.add(new DocumentTypeDefinitionImpl());
        this.types.add(new FolderTypeDefinitionImpl());
    }

    @Inject
    public void setCmsRepository(CmsRepository repository) {
        this.repository = repository;
    }

    @Inject
    public void setCmsItemLookup(CmsItemLookup lookup) {
        this.lookup = lookup;
    }

    @Inject
    public void setCmsCommit(CmsCommit commit) {
        this.commit = commit;
    }

    /**
     * Sets the call context.
     * 
     * This method should only be called by the service factory.
     */
    public void setCallContext(CallContext context) {
        this.context = context;
    }

    /**
     * Gets the call context.
     */
    public CallContext getCallContext() {
        return this.context;
    }

    @Override
    public List<RepositoryInfo> getRepositoryInfos(ExtensionsData extension) {
        RepositoryInfoImpl repositoryInfo = new RepositoryInfoImpl();
        repositoryInfo.setId("repos-cmis");
        repositoryInfo.setName("Repos CMIS Repository");
        repositoryInfo.setDescription("CMIS Repository for ReposWeb.");
        repositoryInfo.setCmisVersionSupported("1.0");
        repositoryInfo.setProductName("Repos CMIS Repository");
        repositoryInfo.setProductVersion("1.0");
        repositoryInfo.setVendorName("Repos Mjukvara AB");
        repositoryInfo.setRootFolder(this.repositoryRoot);
        repositoryInfo.setThinClientUri("");

        RepositoryCapabilitiesImpl capabilities = new RepositoryCapabilitiesImpl();
        capabilities.setCapabilityAcl(CapabilityAcl.NONE);
        capabilities.setAllVersionsSearchable(false);
        capabilities.setCapabilityJoin(CapabilityJoin.NONE);
        capabilities.setSupportsMultifiling(false);
        capabilities.setSupportsUnfiling(false);
        capabilities.setSupportsVersionSpecificFiling(false);
        capabilities.setIsPwcSearchable(false);
        capabilities.setIsPwcUpdatable(false);
        capabilities.setCapabilityQuery(CapabilityQuery.NONE);
        // TODO Does ReposWeb have a change log?
        capabilities.setCapabilityChanges(CapabilityChanges.NONE);
        capabilities
                .setCapabilityContentStreamUpdates(CapabilityContentStreamUpdates.ANYTIME);
        capabilities.setSupportsGetDescendants(true);
        capabilities.setSupportsGetFolderTree(true);
        capabilities.setCapabilityRendition(CapabilityRenditions.NONE);

        return Collections.singletonList((RepositoryInfo) repositoryInfo);
    }

    @Override
    public TypeDefinitionList getTypeChildren(String repositoryId, String typeId,
            Boolean includePropertyDefinitions, BigInteger maxItems,
            BigInteger skipCount, ExtensionsData extension) {
        return new TypeDefinitionListImpl(this.types);
    }

    @Override
    public TypeDefinition getTypeDefinition(String repositoryId, String typeId,
            ExtensionsData extension) {
        for (TypeDefinition ty : this.types) {
            if (ty.getId().equals(typeId)) {
                return ty;
            }
        }
        return null;
    }

    @Override
    public ObjectInFolderList getChildren(String repositoryId, String folderId,
            String filter, String orderBy, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount,
            ExtensionsData extension) {
        ObjectInFolderListImpl children = new ObjectInFolderListImpl();
        CmsItemId itemID = new CmsItemIdUrl(this.repository, folderId);
        // TODO Catch {@link CmsItemNotFoundException}.
        CmsItem folder = this.lookup.getItem(itemID);
        if (folder.getKind() != CmsItemKind.Folder) {
            return children;
        }
        ArrayList<ObjectInFolderData> objectData = new ArrayList<ObjectInFolderData>();
        for (CmsItem item : this.lookup.getImmediates(itemID)) {
            objectData.add(toObjectInFolderData(item));
        }
        children.setObjects(objectData);
        return children;
    }

    @Override
    public List<ObjectParentData> getObjectParents(String repositoryId, String objectId,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includeRelativePathSegment, ExtensionsData extension) {
        CmsItemId objectCmsId = new CmsItemIdUrl(this.repository, objectId);
        // TODO Catch {@link CmsItemNotFoundException}.
        CmsItem object = this.lookup.getItem(objectCmsId);
        ArrayList<ObjectParentData> parentData = new ArrayList<ObjectParentData>();
        for (CmsItemPath parentPath : object.getId().getRelPath().getAncestors()) {
            CmsItemId parentId = new CmsItemIdUrl(this.repository, parentPath);
            CmsItem parent = this.lookup.getItem(parentId);
            parentData.add(toObjectParentData(parent));
        }
        return parentData;
    }

    @Override
    public ObjectData getObject(String repositoryId, String objectId, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        CmsItemId objectCmsId = new CmsItemIdUrl(this.repository, objectId);
        // TODO Catch {@link CmsItemNotFoundException}.
        CmsItem object = this.lookup.getItem(objectCmsId);
        return toObjectData(object);
    }

    private static ObjectData toObjectData(CmsItem item) {
        // TODO Auto-generated method stub
        return null;
    }

    private static ObjectInFolderData toObjectInFolderData(CmsItem item) {
        // TODO Auto-generated method stub
        return null;
    }

    private static ObjectParentData toObjectParentData(CmsItem item) {
        // TODO Auto-generated method stub
        return null;
    }
}
