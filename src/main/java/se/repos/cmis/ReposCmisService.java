/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cmis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
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
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.DocumentTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FolderTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryCapabilitiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractCmisService;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.Holder;

import se.repos.cms.backend.filehead.LocalRepoRevision;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.commit.CmsCommit;
import se.simonsoft.cms.item.commit.CmsPatchItem;
import se.simonsoft.cms.item.commit.CmsPatchset;
import se.simonsoft.cms.item.commit.FileDelete;
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
        CmsItem folder = this.lookup.getItem(itemID);
        if (folder.getKind() != CmsItemKind.Folder) {
            return children;
        }
        ArrayList<ObjectInFolderData> objectData = new ArrayList<ObjectInFolderData>();
        for (CmsItem item : this.lookup.getImmediates(itemID)) {
            objectData.add(new ReposCmsItemObjectInFolderData(item));
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
        CmsItem object = this.lookup.getItem(objectCmsId);
        ArrayList<ObjectParentData> parentData = new ArrayList<ObjectParentData>();
        for (CmsItemPath parentPath : object.getId().getRelPath().getAncestors()) {
            CmsItemId parentId = new CmsItemIdUrl(this.repository, parentPath);
            CmsItem parent = this.lookup.getItem(parentId);
            parentData.add(new ReposCmsItemObjectParentData(parent));
        }
        return parentData;
    }

    @Override
    public ObjectData getObject(String repositoryId, String objectId, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        CmsItemId objectCmsId = new CmsItemIdUrl(this.repository, objectId);
        CmsItem object = this.lookup.getItem(objectCmsId);
        return new ReposCmsItemObjectData(object);
    }

    @Override
    public List<ObjectInFolderContainer> getDescendants(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public List<ObjectInFolderContainer> getFolderTree(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public ObjectData getFolderParent(String repositoryId, String folderId,
            String filter, ExtensionsData extension) {
        CmsItemPath itemPath = new CmsItemPath(folderId);
        CmsItemPath parentPath = itemPath.getParent();
        CmsItem item = this.lookup.getItem(new CmsItemIdUrl(this.repository, parentPath));
        return new ReposCmsItemObjectData(item);
    }

    @Override
    public ObjectList getCheckedOutDocs(String repositoryId, String folderId,
            String filter, String orderBy, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public String createDocument(String repositoryId, Properties properties,
            String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public String createDocumentFromSource(String repositoryId, String sourceId,
            Properties properties, String folderId, VersioningState versioningState,
            List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public String createFolder(String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces, Acl removeAces,
            ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public ObjectData getObjectByPath(String repositoryId, String path, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public ContentStream getContentStream(String repositoryId, String objectId,
            String streamId, BigInteger offset, BigInteger length,
            ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public void moveObject(String repositoryId, Holder<String> objectId,
            String targetFolderId, String sourceFolderId, ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public void deleteObjectOrCancelCheckOut(String repositoryId, String objectId,
            Boolean allVersions, ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public FailedToDeleteData deleteTree(String repositoryId, String folderId,
            Boolean allVersions, UnfileObject unfileObjects, Boolean continueOnFailure,
            ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public void setContentStream(String repositoryId, Holder<String> objectId,
            Boolean overwriteFlag, Holder<String> changeToken,
            ContentStream contentStream, ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public void appendContentStream(String repositoryId, Holder<String> objectId,
            Holder<String> changeToken, ContentStream contentStream, boolean isLastChunk,
            ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public void deleteContentStream(String repositoryId, Holder<String> objectId,
            Holder<String> changeToken, ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public void checkOut(String repositoryId, Holder<String> objectId,
            ExtensionsData extension, Holder<Boolean> contentCopied) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public void cancelCheckOut(String repositoryId, String objectId,
            ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public void checkIn(String repositoryId, Holder<String> objectId, Boolean major,
            Properties properties, ContentStream contentStream, String checkinComment,
            List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public ObjectData getObjectOfLatestVersion(String repositoryId, String objectId,
            String versionSeriesId, Boolean major, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        return this.getObject(repositoryId, objectId, filter, includeAllowableActions,
                includeRelationships, renditionFilter, includePolicyIds, includeAcl,
                extension);
    }

    @Override
    public void addObjectToFolder(String repositoryId, String objectId, String folderId,
            Boolean allVersions, ExtensionsData extension) {
        // TODO Implement this method.
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public void removeObjectFromFolder(String repositoryId, String objectId,
            String folderId, ExtensionsData extension) {
        CmsItemPath deletePath = new CmsItemPath(folderId).append(objectId);
        CmsPatchItem patchItem = new FileDelete(deletePath);
        CmsPatchset change = new CmsPatchset(this.repository, new LocalRepoRevision());
        change.add(patchItem);
        this.commit.run(change);
    }
}
