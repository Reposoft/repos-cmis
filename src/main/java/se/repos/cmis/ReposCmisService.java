/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cmis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractCmisService;
import org.apache.chemistry.opencmis.commons.server.CallContext;

/**
 * CMIS Service Implementation.
 */
public class ReposCmisService extends AbstractCmisService {
    private CallContext context;
    private ReposRepositoryManager manager;

    public ReposCmisService(ReposRepositoryManager manager, CallContext context) {
        this.manager = manager;
        this.context = context;
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
        ArrayList<RepositoryInfo> infos = new ArrayList<RepositoryInfo>();
        for (ReposCmisRepository repo : this.manager.getRepositories()) {
            infos.add(repo.getRepositoryInfo());
        }
        return infos;
    }

    private ReposCmisRepository getRepository(String repositoryId) {
        return this.manager.getRepository(repositoryId);
    }

    @Override
    public TypeDefinitionList getTypeChildren(String repositoryId, String typeId,
            Boolean includePropertyDefinitions, BigInteger maxItems,
            BigInteger skipCount, ExtensionsData extension) {
        return this.getRepository(repositoryId).getTypes();
    }

    @Override
    public TypeDefinition getTypeDefinition(String repositoryId, String typeId,
            ExtensionsData extension) {
        return this.getRepository(repositoryId).getType(typeId);
    }

    @Override
    public ObjectInFolderList getChildren(String repositoryId, String folderId,
            String filter, String orderBy, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount,
            ExtensionsData extension) {
        return this.getRepository(repositoryId).getChildren(folderId);
    }

    @Override
    public List<ObjectParentData> getObjectParents(String repositoryId, String objectId,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includeRelativePathSegment, ExtensionsData extension) {
        return this.getRepository(repositoryId).getObjectParents(objectId);
    }

    @Override
    public ObjectData getObject(String repositoryId, String objectId, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        return this.getRepository(repositoryId).getObject(objectId);
    }

    @Override
    public List<ObjectInFolderContainer> getDescendants(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
        return this.getRepository(repositoryId).getDescendants(folderId, depth, false);
    }

    @Override
    public List<ObjectInFolderContainer> getFolderTree(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
        return this.getRepository(repositoryId).getDescendants(folderId, depth, true);
    }

    @Override
    public ObjectData getFolderParent(String repositoryId, String folderId,
            String filter, ExtensionsData extension) {
        return this.getRepository(repositoryId).getFolderParent(folderId);
    }

    @Override
    public String createDocument(String repositoryId, Properties properties,
            String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        return this.getRepository(repositoryId).createDocument(folderId, properties,
                contentStream.getStream());
    }

    @Override
    public String createFolder(String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces, Acl removeAces,
            ExtensionsData extension) {
        return this.getRepository(repositoryId).createFolder(folderId, properties);
    }

    @Override
    public ObjectData getObjectByPath(String repositoryId, String path, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        return this.getRepository(repositoryId).getObjectByPath(path);
    }

    @Override
    public void deleteObjectOrCancelCheckOut(String repositoryId, String objectId,
            Boolean allVersions, ExtensionsData extension) {
        this.getRepository(repositoryId).deleteObject(objectId);
    }

    @Override
    public FailedToDeleteData deleteTree(String repositoryId, String folderId,
            Boolean allVersions, UnfileObject unfileObjects, Boolean continueOnFailure,
            ExtensionsData extension) {
        return this.getRepository(repositoryId).deleteTree(folderId);
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
        this.getRepository(repositoryId).moveObject(objectId, folderId);
    }

    @Override
    public void removeObjectFromFolder(String repositoryId, String objectId,
            String folderId, ExtensionsData extension) {
        this.getRepository(repositoryId).deleteObject(objectId);
    }
}
