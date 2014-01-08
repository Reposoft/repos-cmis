/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cmis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.enums.CapabilityContentStreamUpdates;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.CapabilityRenditions;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.DocumentTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FailedToDeleteDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FolderTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryCapabilitiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractCmisService;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.Holder;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.commit.CmsCommit;
import se.simonsoft.cms.item.commit.CmsPatchItem;
import se.simonsoft.cms.item.commit.CmsPatchset;
import se.simonsoft.cms.item.commit.FileAdd;
import se.simonsoft.cms.item.commit.FileDelete;
import se.simonsoft.cms.item.commit.FolderAdd;
import se.simonsoft.cms.item.commit.FolderDelete;
import se.simonsoft.cms.item.impl.CmsItemIdUrl;
import se.simonsoft.cms.item.info.CmsItemLookup;

/**
 * CMIS Service Implementation.
 */
public class ReposCmisService extends AbstractCmisService {

    private final CmsRepository repository;
    private final CmsItemLookup lookup;
    private final CmsCommit commit;

    private final ArrayList<TypeDefinition> types;

    private CallContext context;
    private RepoRevision currentRevision;
    private RandomString randomString;

    public ReposCmisService(CmsRepository repository, CmsCommit commit,
            CmsItemLookup lookup, RepoRevision currentRevision, CallContext context) {
        this.repository = repository;
        this.commit = commit;
        this.lookup = lookup;
        this.currentRevision = currentRevision;
        this.context = context;
        this.types = new ArrayList<TypeDefinition>();
        this.types.add(new DocumentTypeDefinitionImpl());
        this.types.add(new FolderTypeDefinitionImpl());
        this.randomString = new RandomString(15);
    }

    public void setCurrentRevision(RepoRevision currentRevision) {
        this.currentRevision = currentRevision;
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
        repositoryInfo.setRootFolder(this.repository.getPath());
        repositoryInfo.setThinClientUri("");
        repositoryInfo.setChangesIncomplete(false);

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
        repositoryInfo.setCapabilities(capabilities);

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
        CmsItemId itemID = this.getItemId(folderId);
        CmsItem folder = this.lookup.getItem(itemID);
        if (folder.getKind() != CmsItemKind.Folder) {
            return children;
        }
        ArrayList<ObjectInFolderData> objectData = new ArrayList<ObjectInFolderData>();
        for (CmsItem item : this.lookup.getImmediates(itemID)) {
            objectData.add(new ReposObjectInFolderData(item));
        }
        children.setObjects(objectData);
        return children;
    }

    @Override
    public List<ObjectParentData> getObjectParents(String repositoryId, String objectId,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includeRelativePathSegment, ExtensionsData extension) {
        CmsItemId objectCmsId = this.getItemId(objectId);
        CmsItem object = this.lookup.getItem(objectCmsId);
        ArrayList<ObjectParentData> parentData = new ArrayList<ObjectParentData>();
        for (CmsItemPath parentPath : object.getId().getRelPath().getAncestors()) {
            CmsItemId parentId = new CmsItemIdUrl(this.repository, parentPath);
            CmsItem parent = this.lookup.getItem(parentId);
            parentData.add(new ReposObjectParentData(parent));
        }
        return parentData;
    }

    @Override
    public ObjectData getObject(String repositoryId, String objectId, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        CmsItemId objectCmsId = this.getItemId(objectId);
        CmsItem object = this.lookup.getItem(objectCmsId);
        return new ReposObjectData(object);
    }

    @Override
    public List<ObjectInFolderContainer> getDescendants(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
        CmsItem folder = this.lookup.getItem(this.getItemId(folderId));
        ObjectInFolderContainer folderContainer = new ReposObjectInFolderContainer(
                folder, this.lookup);
        return folderContainer.getChildren();
    }

    @Override
    public List<ObjectInFolderContainer> getFolderTree(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
        CmsItem folder = this.lookup.getItem(this.getItemId(folderId));
        ObjectInFolderContainer folderContainer = new ReposObjectInFolderContainer(
                folder, this.lookup);
        ArrayList<ObjectInFolderContainer> subFolders = new ArrayList<ObjectInFolderContainer>();
        for (ObjectInFolderContainer obj : folderContainer.getChildren()) {
            if (obj.getObject().getObject().getBaseTypeId() == BaseTypeId.CMIS_FOLDER) {
                subFolders.add(obj);
            }
        }
        return subFolders;
    }

    @Override
    public ObjectData getFolderParent(String repositoryId, String folderId,
            String filter, ExtensionsData extension) {
        CmsItemPath parentPath = this.getItemId(folderId).getRelPath().getParent();
        CmsItem item = this.lookup.getItem(new CmsItemIdUrl(this.repository, parentPath));
        return new ReposObjectData(item);
    }

    @Override
    public String createDocument(String repositoryId, Properties properties,
            String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        CmsItemId folder = this.getItemId(folderId);
        // TODO Isn't there a new item name provided?
        String newItemName = this.randomString.nextString();
        CmsItemPath newItemPath = folder.getRelPath().append(newItemName);
        CmsPatchItem fileAdd = new FileAdd(newItemPath, contentStream.getStream());
        CmsPatchset changes = new CmsPatchset(this.repository, this.currentRevision);
        changes.add(fileAdd);
        this.commit.run(changes);
        // TODO Is this the right return value?
        return newItemName;
    }

    @Override
    public String createFolder(String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces, Acl removeAces,
            ExtensionsData extension) {
        CmsItemId parentId = this.getItemId(folderId);
        // TODO Isn't there a new item name provided?
        String newFolderName = this.randomString.nextString();
        CmsItemPath newFolderPath = parentId.getRelPath().append(newFolderName);
        CmsPatchItem folderAdd = new FolderAdd(newFolderPath);
        CmsPatchset changes = new CmsPatchset(this.repository, this.currentRevision);
        changes.add(folderAdd);
        this.commit.run(changes);
        return newFolderName;
    }

    @Override
    public ObjectData getObjectByPath(String repositoryId, String path, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        return new ReposObjectData(this.lookup.getItem(this.getItemId(path)));
    }

    @Override
    public void deleteObjectOrCancelCheckOut(String repositoryId, String objectId,
            Boolean allVersions, ExtensionsData extension) {
        CmsPatchItem delete = new FileDelete(this.getItemId(objectId).getRelPath());
        CmsPatchset changes = new CmsPatchset(this.repository, this.currentRevision);
        changes.add(delete);
        this.commit.run(changes);
    }

    @Override
    public FailedToDeleteData deleteTree(String repositoryId, String folderId,
            Boolean allVersions, UnfileObject unfileObjects, Boolean continueOnFailure,
            ExtensionsData extension) {
        CmsPatchItem delete = new FolderDelete(this.getItemId(folderId).getRelPath());
        CmsPatchset changes = new CmsPatchset(this.repository, this.currentRevision);
        changes.add(delete);
        this.commit.run(changes);
        return new FailedToDeleteDataImpl();
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
        CmsItemPath objectPath = this.getItemId(objectId).getRelPath();
        this.moveObject(repositoryId, new Holder<String>(objectId), folderId, objectPath
                .getParent().getPath(), extension);
    }

    @Override
    public void removeObjectFromFolder(String repositoryId, String objectId,
            String folderId, ExtensionsData extension) {
        CmsPatchItem patchItem = new FileDelete(this.getItemId(objectId).getRelPath());
        CmsPatchset change = new CmsPatchset(this.repository, this.currentRevision);
        change.add(patchItem);
        this.commit.run(change);
    }

    /**
     * Given an object Id String (which is an absolute path) convert it to a
     * path relative to the repository root.
     */
    private CmsItemId getItemId(String objectId) {
        String newPath;
        if (objectId.startsWith(this.repository.getPath())) {
            newPath = objectId.substring(this.repository.getPath().length());
        } else {
            newPath = objectId;
        }
        if (newPath.isEmpty()) {
            // Return the root folder.
            return this.repository.getItemId();
        }
        return new CmsItemIdUrl(this.repository, newPath);
    }
}