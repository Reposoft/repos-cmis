package se.repos.cmis;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
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
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.PropertyString;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.enums.CapabilityContentStreamUpdates;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.CapabilityRenditions;
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
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
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
    private final RepoRevision currentRevision;
    private ReposTypeManager typeManager;
    private final CmsItemPath repoPath;

    @Inject
    public ReposCmisRepository(@Named("repositoryId") String repositoryId,
            CmsRepository repository, CmsCommit commit, CmsItemLookup lookup,
            RepoRevision currentRevision) {
        if (repositoryId == null || repository == null || commit == null
                || lookup == null || currentRevision == null) {
            throw new NullPointerException();
        }
        this.repositoryId = repositoryId;
        this.repository = repository;
        this.commit = commit;
        this.lookup = lookup;
        this.currentRevision = currentRevision;
        this.typeManager = new ReposTypeManager();
        this.repoPath = new CmsItemPath(this.repository.getPath());
    }

    public String getRepositoryId() {
        return this.repositoryId;
    }

    private CmsItemId getItemId(String objectId) {
        return new CmsItemIdUrl(this.repository, objectId);
    }

    private CmsItemPath getRepoRelativePath(CmsItem item) {
        CmsItemPath itemPath = item.getId().getRelPath();
        if (itemPath.equals(this.repoPath)) {
            return null;
        }
        if (!this.repoPath.isAncestorOf(itemPath)) {
            return itemPath;
        }
        int segmentsToTrim = this.repoPath.getPathSegmentsCount();
        List<String> pathSegments = itemPath.getPathSegments();
        StringBuilder newPathBuilder = new StringBuilder();
        for (String pathSegment : pathSegments.subList(segmentsToTrim,
                pathSegments.size())) {
            newPathBuilder.append("/");
            newPathBuilder.append(pathSegment);
        }
        String newPathString = newPathBuilder.toString();
        return new CmsItemPath(newPathString);
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

        return repositoryInfo;
    }

    public TypeDefinitionList getTypes() {
        List<TypeDefinition> types = this.typeManager.getInternalTypeDefinitions();
        TypeDefinitionListImpl typeDefs = new TypeDefinitionListImpl();
        typeDefs.setList(types);
        return typeDefs;
    }

    public TypeDefinition getType(String typeId) {
        TypeDefinition ty = this.typeManager.getInternalTypeDefinition(typeId);
        if (ty == null) {
            throw new CmisObjectNotFoundException("Type " + typeId + " is not defined!");
        }
        return ty;
    }

    public ObjectInFolderList getChildren(CallContext context, String folderId,
            Set<String> orgFilter, ObjectInfoHandler objectInfos) {
        ObjectInFolderListImpl children = new ObjectInFolderListImpl();
        CmsItemId itemID = this.getItemId(folderId);
        CmsItem folder = this.lookup.getItem(itemID);
        if (folder.getKind() != CmsItemKind.Folder) {
            return children;
        }
        ArrayList<ObjectInFolderData> objectData = new ArrayList<ObjectInFolderData>();
        for (CmsItem item : this.lookup.getImmediates(itemID)) {
            objectData.add(this.compileObjectData(context, item, orgFilter, objectInfos));
        }
        children.setObjects(objectData);
        children.setNumItems(BigInteger.valueOf((objectData.size())));
        return children;
    }

    public List<ObjectInFolderContainer> getDescendants(CallContext context,
            String folderId, BigInteger depth, boolean foldersOnly,
            Set<String> orgFilter, ObjectInfoHandler objectInfos) {
        ArrayList<ObjectInFolderContainer> objects = new ArrayList<ObjectInFolderContainer>();
        for (CmsItemId item : this.lookup.getDescendants(this.getItemId(folderId))) {
            objects.add(this.compileObjectContainer(context, this.lookup.getItem(item),
                    orgFilter, objectInfos));
            // TODO Recurse on depth.
            // TODO Check foldersOnly.
        }
        return objects;
    }

    public List<ObjectParentData> getObjectParents(CallContext context, String objectId,
            Set<String> orgFilter, ObjectInfoHandler objectInfos) {
        // TODO Include repository root in parents list.
        // TODO If the requested object IS the root, return an empty list.
        CmsItem item = this.lookup.getItem(this.getItemId(objectId));
        CmsItemPath relativePath = this.getRepoRelativePath(item);
        ArrayList<ObjectParentData> parentData = new ArrayList<ObjectParentData>();
        for (CmsItemPath parentPath : relativePath.getAncestors()) {
            CmsItemId parentId = new CmsItemIdUrl(this.repository,
                    this.repoPath.append(parentPath.getPathSegments()));
            CmsItem parent = this.lookup.getItem(parentId);
            parentData.add(this
                    .compileParentData(context, parent, orgFilter, objectInfos));
        }
        return parentData;
    }

    public ObjectData getObject(CallContext context, String objectId,
            Set<String> orgFilter, ObjectInfoHandler objectInfos) {
        CmsItemId objectCmsId = this.getItemId(objectId);
        CmsItem item = this.lookup.getItem(objectCmsId);
        return this.compileObject(context, item, orgFilter, objectInfos);
    }

    public ObjectData getFolderParent(CallContext context, String folderId,
            Set<String> orgFilter, ObjectInfoHandler objectInfos) {
        CmsItemPath parentPath = this.getItemId(folderId).getRelPath().getParent();
        CmsItem item = this.lookup.getItem(new CmsItemIdUrl(this.repository, parentPath));
        return this.compileObject(context, item, orgFilter, objectInfos);
    }

    public String createDocument(String folderId, Properties properties,
            InputStream inputStream) {
        CmsItemId folder = this.getItemId(folderId);
        String newItemName = this.getStringProperty(properties, PropertyIds.NAME);
        CmsItemPath newItemPath = folder.getRelPath().append(newItemName);
        this.runChange(new FileAdd(newItemPath, inputStream));
        return newItemPath.getPath();
    }

    public String createFolder(String folderId, Properties properties) {
        CmsItemId parentId = this.getItemId(folderId);
        String newFolderName = this.getStringProperty(properties, PropertyIds.NAME);
        CmsItemPath newFolderPath = parentId.getRelPath().append(newFolderName);
        this.runChange(new FolderAdd(newFolderPath));
        return newFolderPath.getPath();
    }

    public ObjectData getObjectByPath(CallContext context, String path,
            Set<String> orgFilter, ObjectInfoHandler objectInfos) {
        CmsItemId itemId;
        if (path == "/") {
            // return the root folder
            itemId = this.repository.getItemId();
        } else {
            CmsItemPath itemPath = this.repoPath.append(new CmsItemPath(path)
                    .getPathSegments());
            itemId = new CmsItemIdUrl(this.repository, itemPath);
        }
        CmsItem item = this.lookup.getItem(itemId);
        return this.compileObject(context, item, orgFilter, objectInfos);
    }

    private ObjectParentData compileParentData(CallContext context, CmsItem item,
            Set<String> orgFilter, ObjectInfoHandler objectInfos) {
        ObjectParentDataImpl parentData = new ObjectParentDataImpl();
        parentData.setObject(this.compileObject(context, item, orgFilter, objectInfos));
        return parentData;
    }

    private ObjectInFolderContainer compileObjectContainer(CallContext context,
            CmsItem item, Set<String> orgFilter, ObjectInfoHandler objectInfos) {
        ObjectInFolderData objectData = this.compileObjectData(context, item, orgFilter,
                objectInfos);
        ObjectInFolderContainerImpl container = new ObjectInFolderContainerImpl();
        container.setObject(objectData);
        return container;
    }

    private ObjectInFolderData compileObjectData(CallContext context, CmsItem item,
            Set<String> orgFilter, ObjectInfoHandler objectInfos) {
        ObjectData object = this.compileObject(context, item, orgFilter, objectInfos);
        ObjectInFolderDataImpl objectData = new ObjectInFolderDataImpl();
        objectData.setObject(object);
        return objectData;
    }

    private ObjectData compileObject(CallContext context, CmsItem item,
            Set<String> orgFilter, ObjectInfoHandler objectInfos) {
        ObjectDataImpl object = new ObjectDataImpl();
        ObjectInfoImpl objectInfo = new ObjectInfoImpl();
        object.setProperties(this.compileProperties(context, item, orgFilter, objectInfo));
        object.setAllowableActions(this.compileAllowableActions(item));
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
            objectInfo.setHasContent(true);
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
        String id = itemPath.getPath();
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
            CmsItemPath repoRelativePath = this.getRepoRelativePath(item);
            String pathString = repoRelativePath == null ? "/" : repoRelativePath
                    .getPath();
            this.addPropertyString(result, typeId, filter, PropertyIds.PATH, pathString);

            // folder properties
            if (!this.repoPath.equals(itemPath.getPath())) {
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

                objectInfo.setHasContent(true);
                objectInfo.setContentType(MimeTypes.getMIMEType(itemPath.getName()));
                objectInfo.setFileName(itemPath.getName());
            }

            this.addPropertyId(result, typeId, filter, PropertyIds.CONTENT_STREAM_ID,
                    null);
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
        this.runChange(new FileDelete(this.getItemId(objectId).getRelPath()));
    }

    public FailedToDeleteData deleteTree(String folderId) {
        this.runChange(new FolderDelete(this.getItemId(folderId).getRelPath()));
        return new FailedToDeleteDataImpl();
    }

    public Object moveObject(String objectId, String folderId) {
        // TODO Auto-generated method stub
        return null;
    }

    public ContentStream getContentStream(String objectId, String streamId,
            BigInteger offset, BigInteger length) {
        CmsItemId itemId = this.getItemId(objectId);
        final CmsItem item = this.lookup.getItem(itemId);

        ContentStreamImpl result;
        if ((offset != null && offset.longValue() > 0) || length != null) {
            result = new PartialContentStreamImpl();
        } else {
            result = new ContentStreamImpl();
        }

        String fileName = item.getId().getRelPath().getName();
        result.setFileName(fileName);
        result.setLength(BigInteger.valueOf(item.getFilesize()));
        result.setMimeType(MimeTypes.getMIMEType(fileName));
        result.setStream(this.getInputStream(item));
        return result;
    }

    public void setContentStream(Holder<String> objectId, Boolean overwriteFlag,
            ContentStream contentStream) {
        CmsItemId itemId = this.getItemId(objectId.getValue());
        CmsItem item = this.lookup.getItem(itemId);
        this.runChange(new FileModification(item.getId().getRelPath(), this
                .getInputStream(item), contentStream.getStream()));
    }

    public void appendContentStream(Holder<String> objectId, ContentStream contentStream,
            boolean isLastChunk) {
        CmsItem item = this.lookup.getItem(this.getItemId(objectId.getValue()));
        // Creates a new input stream that returns first the existing content,
        // then the new.
        InputStream existingContent = this.getInputStream(item);
        InputStream stream = this.appendInputStreams(existingContent,
                contentStream.getStream());
        // Writes that stream to the item.
        this.runChange(new FileModification(item.getId().getRelPath(), this
                .getInputStream(item), stream));
    }

    public void deleteContentStream(Holder<String> objectId) {
        // Overwrites item contents with an empty input stream.
        CmsItem item = this.lookup.getItem(this.getItemId(objectId.getValue()));
        this.runChange(new FileModification(item.getId().getRelPath(), this
                .getInputStream(item), new ByteArrayInputStream(new byte[1])));
    }

    private InputStream getInputStream(final CmsItem item) {
        final PipedInputStream inputStream = new PipedInputStream();
        new Thread(new Runnable() {
            @Override
            public void run() {
                PipedOutputStream outputStream = null;
                try {
                    outputStream = new PipedOutputStream(inputStream);
                    item.getContents(outputStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                    IOUtils.closeQuietly(outputStream);
                }
            }
        }).start();
        return inputStream;
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
