package se.repos.cmis;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
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
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
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
import org.apache.chemistry.opencmis.commons.enums.ContentStreamAllowed;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AllowableActionsImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.DocumentTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FailedToDeleteDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FolderTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectParentDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryCapabilitiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeMutabilityImpl;

import com.google.inject.Inject;
import com.google.inject.name.Named;

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

public class ReposCmisRepository {
    private final String repositoryId;
    private final CmsRepository repository;
    private final CmsCommit commit;
    private final CmsItemLookup lookup;
    private final RepoRevision currentRevision;
    private final ArrayList<TypeDefinition> types;

    @Inject
    public ReposCmisRepository(@Named("repositoryId") String repositoryId, CmsRepository repository,
            CmsCommit commit, CmsItemLookup lookup, RepoRevision currentRevision) {
        if (repositoryId == null || repository == null || commit == null
                || lookup == null || currentRevision == null) {
            throw new NullPointerException();
        }
        this.repositoryId = repositoryId;
        this.repository = repository;
        this.commit = commit;
        this.lookup = lookup;
        this.currentRevision = currentRevision;

        // Type definitions.
        this.types = new ArrayList<TypeDefinition>();
        DocumentTypeDefinitionImpl docType = new DocumentTypeDefinitionImpl();
        docType.setBaseTypeId(BaseTypeId.CMIS_DOCUMENT);
        // TODO Add content stream support?
        docType.setContentStreamAllowed(ContentStreamAllowed.NOTALLOWED);
        docType.setDescription("Document");
        docType.setDisplayName("Document");
        docType.setExtensions(new ArrayList<CmisExtensionElement>());
        docType.setId(BaseTypeId.CMIS_DOCUMENT.toString());
        docType.setIsControllableAcl(false);
        docType.setIsControllablePolicy(false);
        docType.setIsCreatable(true);
        docType.setIsFileable(true);
        docType.setIsFulltextIndexed(false);
        docType.setIsIncludedInSupertypeQuery(false);
        docType.setIsQueryable(false);
        docType.setIsVersionable(false);
        docType.setLocalName("Document");
        docType.setLocalNamespace("");
        docType.setParentTypeId(BaseTypeId.CMIS_DOCUMENT.value());
        docType.setPropertyDefinitions(new HashMap<String, PropertyDefinition<?>>());
        docType.setQueryName("Document");
        TypeMutabilityImpl mutable = new TypeMutabilityImpl();
        mutable.setCanCreate(true);
        mutable.setCanDelete(true);
        mutable.setCanUpdate(true);
        docType.setTypeMutability(mutable);
        this.types.add(docType);

        FolderTypeDefinitionImpl folder = new FolderTypeDefinitionImpl();
        folder.setBaseTypeId(BaseTypeId.CMIS_FOLDER);
        folder.setDescription("Folder");
        folder.setDisplayName("Folder");
        folder.setExtensions(new ArrayList<CmisExtensionElement>());
        folder.setId(BaseTypeId.CMIS_FOLDER.value());
        folder.setIsControllableAcl(false);
        folder.setIsControllablePolicy(false);
        folder.setIsCreatable(true);
        folder.setIsFileable(true);
        folder.setIsFulltextIndexed(false);
        folder.setIsIncludedInSupertypeQuery(false);
        folder.setIsQueryable(false);
        folder.setLocalName("Folder");
        folder.setLocalNamespace("");
        folder.setParentTypeId(BaseTypeId.CMIS_FOLDER.toString());
        folder.setPropertyDefinitions(new HashMap<String, PropertyDefinition<?>>());
        folder.setQueryName("Folder");
        folder.setTypeMutability(mutable);
        this.types.add(folder);
    }

    public String getRepositoryId() {
        return this.repositoryId;
    }

    private CmsItemId getItemId(String objectId) {
        return new CmsItemIdUrl(this.repository, objectId);
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
        return new TypeDefinitionListImpl(this.types);
    }

    public TypeDefinition getType(String typeId) {
        for (TypeDefinition ty : this.types) {
            if (ty.getId().equals(typeId)) {
                return ty;
            }
        }
        throw new CmisObjectNotFoundException("Unknown type: " + typeId);
    }

    public ObjectInFolderList getChildren(String folderId) {
        ObjectInFolderListImpl children = new ObjectInFolderListImpl();
        CmsItemId itemID = this.getItemId(folderId);
        CmsItem folder = this.lookup.getItem(itemID);
        if (folder.getKind() != CmsItemKind.Folder) {
            return children;
        }
        ArrayList<ObjectInFolderData> objectData = new ArrayList<ObjectInFolderData>();
        for (CmsItem item : this.lookup.getImmediates(itemID)) {
            objectData.add(this.compileObjectData(item));
        }
        children.setObjects(objectData);
        return children;
    }

    public List<ObjectInFolderContainer> getDescendants(String folderId,
            BigInteger depth, boolean foldersOnly) {
        ArrayList<ObjectInFolderContainer> objects = new ArrayList<ObjectInFolderContainer>();
        for (CmsItemId item : this.lookup.getDescendants(this.getItemId(folderId))) {
            objects.add(this.compileObjectContainer(this.lookup.getItem(item)));
            // TODO Recurse on depth.
            // TODO Check foldersOnly.
        }
        return objects;
    }

    public List<ObjectParentData> getObjectParents(String objectId) {
        CmsItemId objectCmsId = this.getItemId(objectId);
        CmsItem object = this.lookup.getItem(objectCmsId);
        ArrayList<ObjectParentData> parentData = new ArrayList<ObjectParentData>();
        for (CmsItemPath parentPath : object.getId().getRelPath().getAncestors()) {
            CmsItemId parentId = new CmsItemIdUrl(this.repository, parentPath);
            CmsItem parent = this.lookup.getItem(parentId);
            parentData.add(this.compileParentData(parent));
        }
        return parentData;
    }

    public ObjectData getObject(String objectId) {
        CmsItemId objectCmsId = this.getItemId(objectId);
        CmsItem object = this.lookup.getItem(objectCmsId);
        return this.compileObject(object);
    }

    public ObjectData getFolderParent(String folderId) {
        CmsItemPath parentPath = this.getItemId(folderId).getRelPath().getParent();
        CmsItem item = this.lookup.getItem(new CmsItemIdUrl(this.repository, parentPath));
        return this.compileObject(item);
    }

    public String createDocument(String folderId, Properties properties,
            InputStream inputStream) {
        CmsItemId folder = this.getItemId(folderId);
        String newItemName = this.getStringProperty(properties, PropertyIds.NAME);
        CmsItemPath newItemPath = folder.getRelPath().append(newItemName);
        CmsPatchItem fileAdd = new FileAdd(newItemPath, inputStream);
        CmsPatchset changes = new CmsPatchset(this.repository, this.currentRevision);
        changes.add(fileAdd);
        this.commit.run(changes);
        return newItemPath.getPath();
    }

    public String createFolder(String folderId, Properties properties) {
        CmsItemId parentId = this.getItemId(folderId);
        String newFolderName = this.getStringProperty(properties, PropertyIds.NAME);
        CmsItemPath newFolderPath = parentId.getRelPath().append(newFolderName);
        CmsPatchItem folderAdd = new FolderAdd(newFolderPath);
        CmsPatchset changes = new CmsPatchset(this.repository, this.currentRevision);
        changes.add(folderAdd);
        this.commit.run(changes);
        return newFolderPath.getPath();
    }

    public ObjectData getObjectByPath(String path) {
        CmsItem item = this.lookup.getItem(this.getItemId(path));
        return this.compileObject(item);
    }

    private ObjectParentData compileParentData(CmsItem item) {
        ObjectParentDataImpl parentData = new ObjectParentDataImpl();
        parentData.setObject(this.compileObject(item));
        return parentData;
    }

    private ObjectInFolderContainer compileObjectContainer(CmsItem item) {
        ObjectInFolderData objectData = this.compileObjectData(item);
        ObjectInFolderContainerImpl container = new ObjectInFolderContainerImpl();
        container.setObject(objectData);
        return container;
    }

    private ObjectInFolderData compileObjectData(CmsItem item) {
        ObjectData object = this.compileObject(item);
        ObjectInFolderDataImpl objectData = new ObjectInFolderDataImpl();
        objectData.setObject(object);
        return objectData;
    }

    private ObjectData compileObject(CmsItem item) {
        ObjectDataImpl object = new ObjectDataImpl();
        object.setProperties(this.compileProperties(item));
        object.setAllowableActions(this.compileAllowableActions(item));
        return object;
    }

    private Properties compileProperties(CmsItem item) {
        PropertiesImpl properties = new PropertiesImpl();
        properties.addProperty(new PropertyStringImpl(PropertyIds.NAME, item.getId()
                .getRelPath().getName()));
        properties.addProperty(new PropertyStringImpl(PropertyIds.OBJECT_ID, item.getId()
                .getRelPath().getPath()));
        properties.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID, this
                .getBaseTypeId(item).value()));
        properties.addProperty(new PropertyIdImpl(PropertyIds.BASE_TYPE_ID, this
                .getBaseTypeId(item).value()));
        // TODO Return author of first version instead of latest version.
        // Needs support in CmsItem for getting original author.
        properties.addProperty(new PropertyStringImpl(PropertyIds.CREATED_BY, item
                .getRevisionChangedAuthor()));
        properties.addProperty(new PropertyIntegerImpl(PropertyIds.CREATION_DATE,
                BigInteger.valueOf(item.getRevisionChanged().getDate().getTime())));
        properties.addProperty(new PropertyStringImpl(PropertyIds.LAST_MODIFIED_BY, item
                .getRevisionChangedAuthor()));
        properties.addProperty(new PropertyIntegerImpl(
                PropertyIds.LAST_MODIFICATION_DATE, BigInteger.valueOf(item
                        .getRevisionChanged().getDate().getTime())));
        if (this.getBaseTypeId(item) == BaseTypeId.CMIS_DOCUMENT) {
            properties.addProperty(new PropertyBooleanImpl(PropertyIds.IS_IMMUTABLE,
                    false));
            properties.addProperty(new PropertyBooleanImpl(PropertyIds.IS_LATEST_VERSION,
                    true));
            properties.addProperty(new PropertyBooleanImpl(PropertyIds.IS_MAJOR_VERSION,
                    true));
            properties.addProperty(new PropertyBooleanImpl(
                    PropertyIds.IS_LATEST_MAJOR_VERSION, true));
            properties.addProperty(new PropertyStringImpl(PropertyIds.VERSION_LABEL, ""));
            properties.addProperty(new PropertyStringImpl(PropertyIds.VERSION_SERIES_ID,
                    ""));
            properties.addProperty(new PropertyBooleanImpl(
                    PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, false));
            // TODO Support content stream properties.
            // properties.addProperty(new PropertyStringImpl(
            // PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, ""));
            // properties.addProperty(new PropertyStringImpl(
            // PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, ""));
            // properties
            // .addProperty(new PropertyStringImpl(PropertyIds.CHECKIN_COMMENT,
            // ""));
            // properties.addProperty(new PropertyIntegerImpl(
            // PropertyIds.CONTENT_STREAM_LENGTH, BigInteger.valueOf(1234)));
            // properties.addProperty(new PropertyIntegerImpl(
            // PropertyIds.CONTENT_STREAM_LENGTH, BigInteger.valueOf(1234)));
            // properties.addProperty(new PropertyStringImpl(
            // PropertyIds.CONTENT_STREAM_MIME_TYPE, ""));
            // properties.addProperty(new PropertyStringImpl(
            // PropertyIds.CONTENT_STREAM_FILE_NAME, ""));
            // properties.addProperty(new
            // PropertyIdImpl(PropertyIds.CONTENT_STREAM_ID, ""));
        }
        if (this.getBaseTypeId(item) == BaseTypeId.CMIS_FOLDER) {
            CmsItemPath itemPath = item.getId().getRelPath();
            properties.addProperty(new PropertyIdImpl(PropertyIds.PARENT_ID, itemPath
                    .getParent().getPath()));
            ArrayList<String> allowedIds = new ArrayList<String>();
            allowedIds.add(BaseTypeId.CMIS_DOCUMENT.value());
            allowedIds.add(BaseTypeId.CMIS_FOLDER.value());
            properties.addProperty(new PropertyIdImpl(
                    PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS, allowedIds));
            properties.addProperty(new PropertyStringImpl(PropertyIds.PATH, itemPath
                    .getPath()));
        }
        return properties;
    }

    private AllowableActions compileAllowableActions(CmsItem item) {
        HashSet<Action> actionSet = new HashSet<Action>();
        // TODO Should Repos support working copies?
        // this.actionSet.add(Action.CAN_CHECK_OUT);
        // this.actionSet.add(Action.CAN_CANCEL_CHECK_OUT);
        // this.actionSet.add(Action.CAN_CHECK_IN);
        // TODO Should Repos support content streams?
        // this.actionSet.add(Action.CAN_GET_CONTENT_STREAM);
        // this.actionSet.add(Action.CAN_SET_CONTENT_STREAM);
        // this.actionSet.add(Action.CAN_DELETE_CONTENT_STREAM);
        actionSet.add(Action.CAN_ADD_OBJECT_TO_FOLDER);
        actionSet.add(Action.CAN_REMOVE_OBJECT_FROM_FOLDER);
        actionSet.add(Action.CAN_DELETE_OBJECT);
        actionSet.add(Action.CAN_GET_FOLDER_TREE);
        actionSet.add(Action.CAN_GET_OBJECT_PARENTS);
        actionSet.add(Action.CAN_GET_FOLDER_PARENT);
        actionSet.add(Action.CAN_GET_DESCENDANTS);
        actionSet.add(Action.CAN_MOVE_OBJECT);
        actionSet.add(Action.CAN_GET_CHILDREN);
        actionSet.add(Action.CAN_CREATE_DOCUMENT);
        actionSet.add(Action.CAN_CREATE_FOLDER);
        actionSet.add(Action.CAN_CREATE_ITEM);
        actionSet.add(Action.CAN_DELETE_TREE);

        AllowableActionsImpl actions = new AllowableActionsImpl();
        actions.setAllowableActions(actionSet);
        return actions;
    }

    public void deleteObject(String objectId) {
        CmsPatchItem delete = new FileDelete(this.getItemId(objectId).getRelPath());
        CmsPatchset changes = new CmsPatchset(this.repository, this.currentRevision);
        changes.add(delete);
        this.commit.run(changes);
    }

    public FailedToDeleteData deleteTree(String folderId) {
        CmsPatchItem delete = new FolderDelete(this.getItemId(folderId).getRelPath());
        CmsPatchset changes = new CmsPatchset(this.repository, this.currentRevision);
        changes.add(delete);
        this.commit.run(changes);
        return new FailedToDeleteDataImpl();
    }

    public Object moveObject(String objectId, String folderId) {
        // TODO Auto-generated method stub
        return null;
    }

    private String getStringProperty(Properties properties, String name) {
        PropertyData<?> property = properties.getProperties().get(name);
        if (!(property instanceof PropertyString)) {
            return null;
        }

        return ((PropertyString) property).getFirstValue();
    }
}
