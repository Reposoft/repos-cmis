package se.repos.cmis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.PropertyString;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.enums.CapabilityContentStreamUpdates;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CapabilityOrderBy;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.CapabilityRenditions;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNameConstraintViolationException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.chemistry.opencmis.commons.impl.MimeTypes;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AllowableActionsImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FailedToDeleteDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectParentDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PartialContentStreamImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryCapabilitiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.commons.impl.server.ObjectInfoImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;
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
import se.simonsoft.cms.item.commit.FileModification;
import se.simonsoft.cms.item.commit.FolderAdd;
import se.simonsoft.cms.item.commit.FolderDelete;
import se.simonsoft.cms.item.impl.CmsItemIdUrl;
import se.simonsoft.cms.item.info.CmsItemLookup;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ReposCmisRepository {
    private static final String USER_UNKNOWN = "<unknown>";
    private final String repositoryId;
    private final CmsRepository repository;
    private final CmsCommit commit;
    private final CmsItemLookup lookup;
    private final CmisIdService idService;
    private final RepoRevision currentRevision;
    private ReposTypeManager typeManager;
    private final CmsItemPath repoPath;

    @Inject
    public ReposCmisRepository(@Named("repositoryId") String repositoryId,
            CmsRepository repository, CmsCommit commit, CmsItemLookup lookup,
            CmisIdService cmisId, RepoRevision currentRevision) {
        if (repositoryId == null || repository == null || commit == null
                || lookup == null || cmisId == null || currentRevision == null) {
            throw new NullPointerException();
        }
        this.repositoryId = repositoryId;
        this.repository = repository;
        this.commit = commit;
        this.lookup = lookup;
        this.idService = cmisId;
        this.currentRevision = currentRevision;
        this.typeManager = new ReposTypeManager();
        this.repoPath = new CmsItemPath(this.repository.getPath());
        this.idService.getCmisId(new CmsItemIdUrl(repository, repository.getPath()));
    }

    public String getRepositoryId() {
        return this.repositoryId;
    }

    private CmsItemPath cmisPathToReposPath(String objectId) {
        if (objectId == "/") {
            return this.repoPath;
        }
        return this.repoPath.append(new CmsItemPath(objectId).getPathSegments());
    }

    private String reposPathToCmisPath(CmsItemPath path) {
        if (path.equals(this.repoPath)) {
            return "/";
        }
        if (!this.repoPath.isAncestorOf(path)) {
            return path.getPath();
        }
        int segmentsToTrim = this.repoPath.getPathSegmentsCount();
        List<String> pathSegments = path.getPathSegments();
        StringBuilder newPathBuilder = new StringBuilder();
        for (String pathSegment : pathSegments.subList(segmentsToTrim,
                pathSegments.size())) {
            newPathBuilder.append("/");
            newPathBuilder.append(pathSegment);
        }
        String newPathString = newPathBuilder.toString();
        return new CmsItemPath(newPathString).getPath();
    }

    private BaseTypeId getBaseTypeId(CmsItem item) {
        switch (item.getKind()) {
        case File:
            return BaseTypeId.CMIS_DOCUMENT;
        case Folder:
            return BaseTypeId.CMIS_FOLDER;
        case Repository:
        case Symlink:
        default:
            throw new UnsupportedOperationException();
        }
    }

    public RepositoryInfo getRepositoryInfo() {
        RepositoryInfoImpl repositoryInfo = new RepositoryInfoImpl();
        repositoryInfo.setId(this.repositoryId);
        repositoryInfo.setName("Repos CMIS Repository");
        repositoryInfo.setDescription("CMIS Repository for ReposWeb.");
        repositoryInfo.setCmisVersionSupported("1.0");
        repositoryInfo.setProductName("Repos CMIS Repository");
        repositoryInfo.setProductVersion("1.0");
        repositoryInfo.setVendorName("Repos Mjukvara AB");
        repositoryInfo.setRootFolder(this.idService.getCmisId(new CmsItemIdUrl(
                this.repository, this.repository.getPath())));
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
        capabilities.setOrderByCapability(CapabilityOrderBy.NONE);
        repositoryInfo.setCapabilities(capabilities);

        return repositoryInfo;
    }

    public TypeDefinitionList getTypeChildren(CallContext context, String typeId,
            Boolean includePropertyDefinitions, BigInteger maxItems, BigInteger skipCount) {
        return this.typeManager.getTypeChildren(context, typeId,
                includePropertyDefinitions, maxItems, skipCount);
    }

    public TypeDefinition getType(String typeId) {
        TypeDefinition ty = this.typeManager.getInternalTypeDefinition(typeId);
        if (ty == null) {
            throw new CmisObjectNotFoundException("Type " + typeId + " is not defined!");
        }
        return ty;
    }

    public ObjectInFolderList getChildren(CallContext context, String folderId,
            Set<String> orgFilter, BigInteger maxItems, BigInteger skipCount,
            Boolean includeAllowableActions, Boolean includePathSegment,
            ObjectInfoHandler objectInfos) {
        CmsItem folder = this.lookup.getItem(this.idService.getItemId(folderId));
        if (folder.getKind() != CmsItemKind.Folder) {
            throw new CmisNotSupportedException("Cannot get children of file: "
                    + folderId);
        }

        ObjectInFolderListImpl children = new ObjectInFolderListImpl();
        children.setObjects(new ArrayList<ObjectInFolderData>());
        children.setHasMoreItems(false);

        int skip = skipCount == null ? 0 : skipCount.intValue();
        if (skip < 0) {
            skip = 0;
        }

        int max = maxItems == null ? Integer.MAX_VALUE : maxItems.intValue();

        Set<CmsItem> items = this.lookup.getImmediates(folder.getId());
        children.setNumItems(BigInteger.valueOf((items.size())));
        for (CmsItem item : items) {
            if (skip > 0) {
                skip--;
            } else if (children.getObjects().size() >= max) {
                children.setHasMoreItems(true);
                break;
            } else {
                children.getObjects().add(
                        this.compileObjectData(context, item, orgFilter,
                                includeAllowableActions, objectInfos));
            }
        }
        return children;
    }

    public List<ObjectInFolderContainer> getDescendants(CallContext context,
            String folderId, BigInteger depth, boolean foldersOnly,
            Set<String> orgFilter, Boolean includeAllowableActions,
            Boolean includeRelativePathSegment, ObjectInfoHandler objectInfos) {
        ArrayList<ObjectInFolderContainer> objects = new ArrayList<ObjectInFolderContainer>();
        if (depth.compareTo(BigInteger.ZERO) <= 0) {
            return objects;
        }
        for (CmsItem item : this.lookup.getImmediates(this.idService.getItemId(folderId))) {
            if (!foldersOnly || item.getKind() == CmsItemKind.Folder) {
                objects.add(this.compileObjectContainer(context, item, orgFilter,
                        includeAllowableActions, objectInfos));
            }
            if (item.getKind() == CmsItemKind.Folder
                    && depth.compareTo(BigInteger.ONE) >= 0) {
                objects.addAll(this.getDescendants(context, item.getId().getRelPath()
                        .getPath(), depth.subtract(BigInteger.ONE), foldersOnly,
                        orgFilter, includeAllowableActions, includeRelativePathSegment,
                        objectInfos));
            }
        }
        return objects;
    }

    public List<ObjectParentData> getObjectParents(CallContext context, String objectId,
            Set<String> orgFilter, Boolean includeAllowableActions,
            Boolean includeRelativePathSegment, ObjectInfoHandler objectInfos) {
        CmsItem item = this.lookup.getItem(this.idService.getItemId(objectId));
        CmsItemPath itemPath = item.getId().getRelPath();
        if (itemPath.getPath().equals(this.repository.getPath())) {
            // The folder is the root, and has no parents.
            return Collections.emptyList();
        }

        CmsItem parent = this.lookup.getItem(new CmsItemIdUrl(this.repository, itemPath
                .getParent()));
        ObjectParentDataImpl parentData = new ObjectParentDataImpl();
        parentData.setObject(this.compileObject(context, parent, orgFilter,
                includeAllowableActions, objectInfos));
        if (includeRelativePathSegment != null && includeRelativePathSegment) {
            parentData.setRelativePathSegment(itemPath.getName());
        }
        return Collections.<ObjectParentData> singletonList(parentData);
    }

    public ObjectData getObject(CallContext context, String objectId,
            Set<String> orgFilter, Boolean includeAllowableActions,
            ObjectInfoHandler objectInfos) {
        CmsItem item = this.lookup.getItem(this.idService.getItemId(objectId));
        return this.compileObject(context, item, orgFilter, includeAllowableActions,
                objectInfos);
    }

    public ObjectData getFolderParent(CallContext context, String folderId,
            Set<String> orgFilter, ObjectInfoHandler objectInfos) {
        return this
                .getObjectParents(context, folderId, orgFilter, false, false, objectInfos)
                .get(0).getObject();
    }

    public String createDocument(String folderId, Properties properties,
            ContentStream contentStream) {
        InputStream content = null;
        try {
            if (contentStream == null) {
                content = this.emptyContentStream();
            } else {
                content = contentStream.getStream();
            }
            String newItemName = this.getStringProperty(properties, PropertyIds.NAME);
            CmsItemPath parentPath = this.idService.getItemId(folderId).getRelPath();
            CmsItemPath newItemPath = parentPath.append(newItemName);
            this.runChange(new FileAdd(newItemPath, content));
            return this.idService
                    .getCmisId(new CmsItemIdUrl(this.repository, newItemPath));
        } finally {
            IOUtils.closeQuietly(content);
        }
    }

    public String createFolder(String folderId, Properties properties) {
        String newFolderName = this.getStringProperty(properties, PropertyIds.NAME);
        CmsItemPath parentFolderPath = this.idService.getItemId(folderId).getRelPath();
        CmsItemPath newFolderPath = parentFolderPath.append(newFolderName);
        this.runChange(new FolderAdd(newFolderPath));
        return this.idService.getCmisId(new CmsItemIdUrl(this.repository, newFolderPath));
    }

    public ObjectData getObjectByPath(CallContext context, String path,
            Set<String> orgFilter, Boolean includeAllowableActions,
            ObjectInfoHandler objectInfos) {
        CmsItemPath itemPath = this.cmisPathToReposPath(path);
        CmsItemId itemId = new CmsItemIdUrl(this.repository, itemPath);
        CmsItem item = this.lookup.getItem(itemId);
        return this.compileObject(context, item, orgFilter, includeAllowableActions,
                objectInfos);
    }

    private ObjectInFolderContainer compileObjectContainer(CallContext context,
            CmsItem item, Set<String> orgFilter, Boolean includeAllowableActions,
            ObjectInfoHandler objectInfos) {
        ObjectInFolderData objectData = this.compileObjectData(context, item, orgFilter,
                includeAllowableActions, objectInfos);
        ObjectInFolderContainerImpl container = new ObjectInFolderContainerImpl();
        container.setObject(objectData);
        return container;
    }

    private ObjectInFolderData compileObjectData(CallContext context, CmsItem item,
            Set<String> orgFilter, Boolean includeAllowableActions,
            ObjectInfoHandler objectInfos) {
        ObjectData object = this.compileObject(context, item, orgFilter,
                includeAllowableActions, objectInfos);
        ObjectInFolderDataImpl objectData = new ObjectInFolderDataImpl();
        objectData.setObject(object);
        return objectData;
    }

    private ObjectData compileObject(CallContext context, CmsItem item,
            Set<String> orgFilter, Boolean includeAllowableActions,
            ObjectInfoHandler objectInfos) {
        ObjectDataImpl object = new ObjectDataImpl();
        ObjectInfoImpl objectInfo = new ObjectInfoImpl();
        object.setProperties(this.compileProperties(context, item, orgFilter, objectInfo));
        if (includeAllowableActions != null && includeAllowableActions) {
            object.setAllowableActions(this.compileAllowableActions(item));
        }
        objectInfo.setObject(object);
        objectInfos.addObjectInfo(objectInfo);
        return object;
    }

    /**
     * Gathers all base properties of a file or folder.
     */
    private Properties compileProperties(CallContext context, CmsItem item,
            Set<String> orgfilter, ObjectInfoImpl objectInfo) {
        if (item == null) {
            throw new NullPointerException("Item is null.");
        }

        Set<String> filter = (orgfilter == null ? null : new HashSet<String>(orgfilter));
        String typeId = this.getBaseTypeId(item).value();
        CmsItemPath itemPath = item.getId().getRelPath();

        if (item.getKind() == CmsItemKind.Folder) {
            typeId = BaseTypeId.CMIS_FOLDER.value();
            objectInfo.setBaseType(BaseTypeId.CMIS_FOLDER);
            objectInfo.setTypeId(typeId);
            objectInfo.setContentType(null);
            objectInfo.setFileName(null);
            objectInfo.setHasAcl(true);
            objectInfo.setHasContent(false);
            objectInfo.setVersionSeriesId(null);
            objectInfo.setIsCurrentVersion(true);
            objectInfo.setRelationshipSourceIds(null);
            objectInfo.setRelationshipTargetIds(null);
            objectInfo.setRenditionInfos(null);
            objectInfo.setSupportsDescendants(true);
            objectInfo.setSupportsFolderTree(true);
            objectInfo.setSupportsPolicies(false);
            objectInfo.setSupportsRelationships(false);
            objectInfo.setWorkingCopyId(null);
            objectInfo.setWorkingCopyOriginalId(null);
        } else {
            typeId = BaseTypeId.CMIS_DOCUMENT.value();
            objectInfo.setBaseType(BaseTypeId.CMIS_DOCUMENT);
            objectInfo.setTypeId(typeId);
            objectInfo.setHasAcl(true);
            objectInfo.setHasParent(true);
            objectInfo.setVersionSeriesId(null);
            objectInfo.setIsCurrentVersion(true);
            objectInfo.setRelationshipSourceIds(null);
            objectInfo.setRelationshipTargetIds(null);
            objectInfo.setRenditionInfos(null);
            objectInfo.setSupportsDescendants(false);
            objectInfo.setSupportsFolderTree(false);
            objectInfo.setSupportsPolicies(false);
            objectInfo.setSupportsRelationships(false);
            objectInfo.setWorkingCopyId(null);
            objectInfo.setWorkingCopyOriginalId(null);
        }

        PropertiesImpl result = new PropertiesImpl();

        // id
        String id = this.idService.getCmisId(item.getId());
        this.addPropertyId(result, typeId, filter, PropertyIds.OBJECT_ID, id);
        objectInfo.setId(id);

        // name
        String name = item.getId().getRelPath().getName();
        this.addPropertyString(result, typeId, filter, PropertyIds.NAME, name);
        objectInfo.setName(name);

        // created and modified by
        this.addPropertyString(result, typeId, filter, PropertyIds.CREATED_BY,
                USER_UNKNOWN);
        this.addPropertyString(result, typeId, filter, PropertyIds.LAST_MODIFIED_BY,
                USER_UNKNOWN);
        objectInfo.setCreatedBy(USER_UNKNOWN);

        // creation and modification date
        GregorianCalendar lastModified = ReposCmisRepository.millisToCalendar(item
                .getRevisionChanged().getDate().getTime());
        this.addPropertyDateTime(result, typeId, filter, PropertyIds.CREATION_DATE,
                lastModified);
        this.addPropertyDateTime(result, typeId, filter,
                PropertyIds.LAST_MODIFICATION_DATE, lastModified);
        objectInfo.setCreationDate(lastModified);
        objectInfo.setLastModificationDate(lastModified);

        // change token - always null
        this.addPropertyString(result, typeId, filter, PropertyIds.CHANGE_TOKEN, null);

        // directory or file
        if (item.getKind() == CmsItemKind.Folder) {
            // base type and type name
            this.addPropertyId(result, typeId, filter, PropertyIds.BASE_TYPE_ID,
                    BaseTypeId.CMIS_FOLDER.value());
            this.addPropertyId(result, typeId, filter, PropertyIds.OBJECT_TYPE_ID,
                    BaseTypeId.CMIS_FOLDER.value());

            // item path (relative to repository root)
            this.addPropertyString(result, typeId, filter, PropertyIds.PATH,
                    this.reposPathToCmisPath(itemPath));

            // folder properties
            if (!this.repoPath.getPath().equals(itemPath.getPath())) {
                this.addPropertyId(result, typeId, filter, PropertyIds.PARENT_ID,
                        itemPath.getParent().getPath());
                objectInfo.setHasParent(true);
            } else {
                this.addPropertyId(result, typeId, filter, PropertyIds.PARENT_ID, null);
                objectInfo.setHasParent(false);
            }

            this.addPropertyIdList(result, typeId, filter,
                    PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS, null);
        } else {
            // base type and type name
            this.addPropertyId(result, typeId, filter, PropertyIds.BASE_TYPE_ID,
                    BaseTypeId.CMIS_DOCUMENT.value());
            this.addPropertyId(result, typeId, filter, PropertyIds.OBJECT_TYPE_ID,
                    BaseTypeId.CMIS_DOCUMENT.value());

            // file properties
            this.addPropertyBoolean(result, typeId, filter, PropertyIds.IS_IMMUTABLE,
                    false);
            this.addPropertyBoolean(result, typeId, filter,
                    PropertyIds.IS_LATEST_VERSION, true);
            this.addPropertyBoolean(result, typeId, filter, PropertyIds.IS_MAJOR_VERSION,
                    true);
            this.addPropertyBoolean(result, typeId, filter,
                    PropertyIds.IS_LATEST_MAJOR_VERSION, true);
            this.addPropertyString(result, typeId, filter, PropertyIds.VERSION_LABEL,
                    itemPath.getPath());
            this.addPropertyId(result, typeId, filter, PropertyIds.VERSION_SERIES_ID,
                    itemPath.getPath());
            this.addPropertyBoolean(result, typeId, filter,
                    PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, false);
            this.addPropertyString(result, typeId, filter,
                    PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null);
            this.addPropertyString(result, typeId, filter,
                    PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null);
            this.addPropertyString(result, typeId, filter, PropertyIds.CHECKIN_COMMENT,
                    "");

            if (item.getFilesize() == 0) {
                this.addPropertyBigInteger(result, typeId, filter,
                        PropertyIds.CONTENT_STREAM_LENGTH, null);
                this.addPropertyString(result, typeId, filter,
                        PropertyIds.CONTENT_STREAM_MIME_TYPE, null);
                this.addPropertyString(result, typeId, filter,
                        PropertyIds.CONTENT_STREAM_FILE_NAME, null);
                this.addPropertyId(result, typeId, filter, PropertyIds.CONTENT_STREAM_ID,
                        null);

                objectInfo.setHasContent(false);
                objectInfo.setContentType(null);
                objectInfo.setFileName(null);
            } else {
                this.addPropertyInteger(result, typeId, filter,
                        PropertyIds.CONTENT_STREAM_LENGTH, item.getFilesize());
                this.addPropertyString(result, typeId, filter,
                        PropertyIds.CONTENT_STREAM_MIME_TYPE,
                        MimeTypes.getMIMEType(itemPath.getName()));
                this.addPropertyString(result, typeId, filter,
                        PropertyIds.CONTENT_STREAM_FILE_NAME, itemPath.getName());
                this.addPropertyId(result, typeId, filter, PropertyIds.CONTENT_STREAM_ID,
                        id);

                objectInfo.setHasContent(true);
                objectInfo.setContentType(MimeTypes.getMIMEType(itemPath.getName()));
                objectInfo.setFileName(itemPath.getName());
            }
        }

        return result;
    }

    private void addPropertyId(PropertiesImpl props, String typeId, Set<String> filter,
            String id, String value) {
        if (!this.checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyIdImpl(id, value));
    }

    private void addPropertyIdList(PropertiesImpl props, String typeId,
            Set<String> filter, String id, List<String> value) {
        if (!this.checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyIdImpl(id, value));
    }

    private void addPropertyString(PropertiesImpl props, String typeId,
            Set<String> filter, String id, String value) {
        if (!this.checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyStringImpl(id, value));
    }

    private void addPropertyInteger(PropertiesImpl props, String typeId,
            Set<String> filter, String id, long value) {
        this.addPropertyBigInteger(props, typeId, filter, id, BigInteger.valueOf(value));
    }

    private void addPropertyBigInteger(PropertiesImpl props, String typeId,
            Set<String> filter, String id, BigInteger value) {
        if (!this.checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyIntegerImpl(id, value));
    }

    private void addPropertyBoolean(PropertiesImpl props, String typeId,
            Set<String> filter, String id, boolean value) {
        if (!this.checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyBooleanImpl(id, value));
    }

    private void addPropertyDateTime(PropertiesImpl props, String typeId,
            Set<String> filter, String id, GregorianCalendar value) {
        if (!this.checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyDateTimeImpl(id, value));
    }

    private boolean checkAddProperty(Properties properties, String typeId,
            Set<String> filter, String id) {
        if ((properties == null) || (properties.getProperties() == null)) {
            throw new IllegalArgumentException("Properties must not be null!");
        }

        if (id == null) {
            throw new IllegalArgumentException("Id must not be null!");
        }

        TypeDefinition type = this.typeManager.getInternalTypeDefinition(typeId);
        if (type == null) {
            throw new IllegalArgumentException("Unknown type: " + typeId);
        }
        if (!type.getPropertyDefinitions().containsKey(id)) {
            throw new IllegalArgumentException("Unknown property: " + id);
        }

        String queryName = type.getPropertyDefinitions().get(id).getQueryName();

        if ((queryName != null) && (filter != null)) {
            if (!filter.contains(queryName)) {
                return false;
            }
            filter.remove(queryName);
        }

        return true;
    }

    private AllowableActions compileAllowableActions(CmsItem item) {
        if (item == null) {
            throw new NullPointerException();
        }

        boolean isReadOnly = false;
        boolean userReadOnly = false;
        boolean isFolder = item.getKind() == CmsItemKind.Folder;
        boolean isRoot = this.repository.getPath().equals(
                item.getId().getRelPath().getPath());

        Set<Action> aas = EnumSet.noneOf(Action.class);

        ReposCmisRepository.addAction(aas, Action.CAN_GET_OBJECT_PARENTS, !isRoot);
        ReposCmisRepository.addAction(aas, Action.CAN_GET_PROPERTIES, true);
        ReposCmisRepository.addAction(aas, Action.CAN_UPDATE_PROPERTIES, !userReadOnly
                && !isReadOnly);
        ReposCmisRepository.addAction(aas, Action.CAN_MOVE_OBJECT, !userReadOnly
                && !isRoot);
        ReposCmisRepository.addAction(aas, Action.CAN_DELETE_OBJECT, !userReadOnly
                && !isReadOnly && !isRoot);
        ReposCmisRepository.addAction(aas, Action.CAN_GET_ACL, true);

        if (isFolder) {
            ReposCmisRepository.addAction(aas, Action.CAN_GET_DESCENDANTS, true);
            ReposCmisRepository.addAction(aas, Action.CAN_GET_CHILDREN, true);
            ReposCmisRepository.addAction(aas, Action.CAN_GET_FOLDER_PARENT, !isRoot);
            ReposCmisRepository.addAction(aas, Action.CAN_GET_FOLDER_TREE, true);
            ReposCmisRepository.addAction(aas, Action.CAN_CREATE_DOCUMENT, !userReadOnly);
            ReposCmisRepository.addAction(aas, Action.CAN_CREATE_FOLDER, !userReadOnly);
            ReposCmisRepository.addAction(aas, Action.CAN_DELETE_TREE, !userReadOnly
                    && !isReadOnly);
        } else {
            ReposCmisRepository.addAction(aas, Action.CAN_GET_CONTENT_STREAM,
                    item.getFilesize() > 0);
            ReposCmisRepository.addAction(aas, Action.CAN_SET_CONTENT_STREAM,
                    !userReadOnly && !isReadOnly);
            ReposCmisRepository.addAction(aas, Action.CAN_DELETE_CONTENT_STREAM,
                    !userReadOnly && !isReadOnly);
            ReposCmisRepository.addAction(aas, Action.CAN_GET_ALL_VERSIONS, true);
        }

        AllowableActionsImpl result = new AllowableActionsImpl();
        result.setAllowableActions(aas);

        return result;
    }

    private static void addAction(Set<Action> aas, Action action, boolean condition) {
        if (condition) {
            aas.add(action);
        }
    }

    private static GregorianCalendar millisToCalendar(long millis) {
        GregorianCalendar result = new GregorianCalendar();
        result.setTimeZone(TimeZone.getTimeZone("GMT"));
        result.setTimeInMillis((long) (Math.ceil((double) millis / 1000) * 1000));

        return result;
    }

    public void deleteObject(String objectId) {
        CmsItemId item = this.idService.getItemId(objectId);
        this.runChange(new FileDelete(item.getRelPath()));
        this.idService.deleteItem(item, objectId);
    }

    public FailedToDeleteData deleteTree(String folderId) {
        CmsItemId folder = this.idService.getItemId(folderId);

        CmsPatchset changes = new CmsPatchset(this.repository, this.currentRevision);
        changes.add(new FolderDelete(folder.getRelPath()));
        this.idService.deleteItem(folder, folderId);

        for (CmsItemId itemId : this.lookup.getDescendants(folder)) {
            CmsItem item = this.lookup.getItem(itemId);
            if (item.getKind() == CmsItemKind.Folder) {
                changes.add(new FolderDelete(itemId.getRelPath()));
            } else {
                changes.add(new FileDelete(itemId.getRelPath()));
            }
            String cmisId = this.idService.getCmisId(itemId);
            this.idService.deleteItem(itemId, cmisId);
        }
        return new FailedToDeleteDataImpl();
    }

    public void moveObject(CallContext context, Holder<String> objectId,
            String targetFolderId, ObjectInfoHandler objectInfos) {
        CmsItem item = this.lookup.getItem(this.idService.getItemId(objectId.getValue()));
        String itemName = item.getId().getRelPath().getName();
        CmsItemPath folderPath = this.idService.getItemId(targetFolderId).getRelPath();
        CmsItemPath newPath = folderPath.append(itemName);
        this.moveItem(item, newPath, objectId);
        this.compileObject(context, item, null, false, objectInfos);
    }

    public ContentStream getContentStream(String objectId, String streamId,
            BigInteger offset, BigInteger length) {
        CmsItem item = this.lookup.getItem(this.idService.getItemId(objectId));

        long streamLength = item.getFilesize();
        if (streamLength == 0L) {
            throw new CmisConstraintException("Document has no content!");
        }

        ContentStreamImpl result;
        if ((offset != null && offset.longValue() > 0) || length != null) {
            result = new PartialContentStreamImpl();
        } else {
            result = new ContentStreamImpl();
        }

        result.setLength(BigInteger.valueOf(streamLength));
        String fileName = item.getId().getRelPath().getName();
        result.setFileName(fileName);
        result.setMimeType(MimeTypes.getMIMEType(fileName));
        result.setStream(new ContentRangeInputStream(this.getInputStream(item), offset,
                length));
        return result;
    }

    public void setContentStream(Holder<String> objectId, Boolean overwriteFlag,
            ContentStream contentStream) {
        InputStream currentContent = null;
        InputStream newContent = null;
        try {
            CmsItemId itemId = this.idService.getItemId(objectId.getValue());
            CmsItem item = this.lookup.getItem(itemId);
            currentContent = this.getInputStream(item);
            newContent = contentStream.getStream();
            this.runChange(new FileModification(item.getId().getRelPath(), this
                    .getInputStream(item), newContent));
        } finally {
            IOUtils.closeQuietly(currentContent);
            IOUtils.closeQuietly(newContent);
        }
    }

    public void appendContentStream(Holder<String> objectId, ContentStream contentStream,
            boolean isLastChunk) {
        // Creates a new input stream that returns first the existing content,
        // then the new.
        InputStream currentContent = null;
        InputStream newContent = null;
        InputStream combinedContent = null;
        try {
            CmsItem item = this.lookup.getItem(new CmsItemIdUrl(this.repository, objectId
                    .getValue()));
            currentContent = this.getInputStream(item);
            newContent = contentStream.getStream();
            combinedContent = this.appendInputStreams(currentContent, newContent);
            // Writes that stream to the item.
            this.runChange(new FileModification(item.getId().getRelPath(),
                    currentContent, combinedContent));
        } finally {
            IOUtils.closeQuietly(currentContent);
            IOUtils.closeQuietly(newContent);
            IOUtils.closeQuietly(combinedContent);
        }
    }

    public void deleteContentStream(Holder<String> objectId) {
        // Overwrites item contents with an empty input stream.
        InputStream currentContent = null;
        InputStream newContent = null;
        try {
            CmsItem item = this.lookup.getItem(this.idService.getItemId(objectId
                    .getValue()));
            currentContent = this.getInputStream(item);
            newContent = this.emptyContentStream();
            this.runChange(new FileModification(item.getId().getRelPath(),
                    currentContent, newContent));
        } finally {
            IOUtils.closeQuietly(currentContent);
            IOUtils.closeQuietly(newContent);
        }
    }

    public ObjectList getCheckedOutDocs(String folderId, String filter,
            Boolean includeAllowableActions, BigInteger maxItems, BigInteger skipCount,
            ObjectInfoHandler objectInfos) {
        // PWCs are not supported yet.
        return new ObjectListImpl();
    }

    public void updateProperties(CallContext context, Holder<String> objectId,
            Properties properties, ObjectInfoHandler objectInfos) {
        if (objectId == null || objectId.getValue() == null) {
            throw new CmisInvalidArgumentException("Id is not valid!");
        }
        CmsItem item = this.lookup.getItem(this.idService.getItemId(objectId.getValue()));

        // check the properties
        String typeId = (item.getKind() == CmsItemKind.Folder ? BaseTypeId.CMIS_FOLDER
                .value() : BaseTypeId.CMIS_DOCUMENT.value());
        this.checkUpdateProperties(properties, typeId);

        // get and check the new name
        String newName = this.getStringProperty(properties, PropertyIds.NAME);
        String oldName = item.getId().getRelPath().getName();
        boolean isRename = (newName != null) && (!oldName.equals(newName));
        if (isRename && !ReposCmisRepository.isValidName(newName)) {
            throw new CmisNameConstraintViolationException("Name is not valid!");
        }

        if (isRename) {
            this.renameItem(item, newName, objectId);
        }
    }

    private void renameItem(CmsItem item, String newName, Holder<String> objectId) {
        CmsItemPath oldPath = item.getId().getRelPath();
        List<String> newPathSegments = oldPath.subPath(0,
                oldPath.getPathSegmentsCount() - 1);
        StringBuilder sb = new StringBuilder();
        for (String pathSegment : newPathSegments) {
            sb.append('/');
            sb.append(pathSegment);
        }
        sb.append('/');
        sb.append(newName);
        CmsItemPath newPath = new CmsItemPath(sb.toString());
        this.moveItem(item, newPath, objectId);
    }

    private void moveItem(CmsItem item, CmsItemPath newPath, Holder<String> objectId) {
        // TODO Ensure that folders can be moved correctly.
        InputStream content = null;
        try {
            CmsPatchset changes = new CmsPatchset(this.repository, this.currentRevision);
            content = this.getInputStream(item);
            // Move the file by adding an identical one, then deleting the
            // original.
            changes.add(new FileAdd(newPath, content));
            changes.add(new FileDelete(item.getId().getRelPath()));
            this.commit.run(changes);
            CmsItemId newId = new CmsItemIdUrl(this.repository, newPath);
            this.idService.deleteItem(item.getId(),
                    this.idService.getCmisId(item.getId()));
            this.idService.putItem(newId, objectId.getValue());
            objectId.setValue(this.idService.getCmisId(newId));
        } finally {
            IOUtils.closeQuietly(content);
        }
    }

    /**
     * Checks a property set for an update.
     */
    private void checkUpdateProperties(Properties properties, String typeId) {
        // check properties
        if (properties == null || properties.getProperties() == null) {
            throw new CmisInvalidArgumentException("Properties must be set!");
        }

        // check the name
        String name = this.getStringProperty(properties, PropertyIds.NAME);
        if (name != null) {
            if (!ReposCmisRepository.isValidName(name)) {
                throw new CmisNameConstraintViolationException("Name is not valid!");
            }
        }

        // check type properties
        this.checkTypeProperties(properties, typeId, false);
    }

    /**
     * Checks if the property belong to the type and are settable.
     */
    private void checkTypeProperties(Properties properties, String typeId,
            boolean isCreate) {
        // check type
        TypeDefinition type = this.typeManager.getInternalTypeDefinition(typeId);
        if (type == null) {
            throw new CmisObjectNotFoundException("Type '" + typeId + "' is unknown!");
        }

        // check if all required properties are there
        for (PropertyData<?> prop : properties.getProperties().values()) {
            PropertyDefinition<?> propType = type.getPropertyDefinitions().get(
                    prop.getId());

            // do we know that property?
            if (propType == null) {
                throw new CmisConstraintException("Property '" + prop.getId()
                        + "' is unknown!");
            }

            // can it be set?
            if (propType.getUpdatability() == Updatability.READONLY) {
                throw new CmisConstraintException("Property '" + prop.getId()
                        + "' is readonly!");
            }

            if (!isCreate) {
                // can it be set?
                if (propType.getUpdatability() == Updatability.ONCREATE) {
                    throw new CmisConstraintException("Property '" + prop.getId()
                            + "' cannot be updated!");
                }
            }
        }
    }

    /**
     * Checks if the given name is valid for a file system.
     * 
     * @param name
     *            the name to check
     * 
     * @return <code>true</code> if the name is valid, <code>false</code>
     *         otherwise
     */
    private static boolean isValidName(String name) {
        if (name == null || name.length() == 0 || name.indexOf(File.separatorChar) != -1
                || name.indexOf(File.pathSeparatorChar) != -1) {
            return false;
        }

        return true;
    }

    private InputStream getInputStream(CmsItem item) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        item.getContents(out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    private InputStream appendInputStreams(final InputStream first,
            final InputStream second) {
        InputStream stream = new InputStream() {

            @Override
            public int read() throws IOException {
                if (first.available() > 0) {
                    return first.read();
                } else if (second.available() > 0) {
                    return second.read();
                } else {
                    return -1;
                }
            }
        };
        return stream;
    }

    private InputStream emptyContentStream() {
        return new ByteArrayInputStream(new byte[0]);
    }

    private void runChange(CmsPatchItem change) {
        CmsPatchset changes = new CmsPatchset(this.repository, this.currentRevision);
        changes.add(change);
        this.commit.run(changes);
    }

    private String getStringProperty(Properties properties, String name) {
        PropertyData<?> property = properties.getProperties().get(name);
        if (!(property instanceof PropertyString)) {
            return null;
        }

        return ((PropertyString) property).getFirstValue();
    }
}
