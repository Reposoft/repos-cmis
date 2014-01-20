/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cmis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.chemistry.opencmis.commons.PropertyIds;
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
import org.apache.chemistry.opencmis.commons.spi.Holder;

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

    private ReposCmisRepository getRepository() {
        return this.manager.getRepository(this.getCallContext().getRepositoryId());
    }

    @Override
    public TypeDefinitionList getTypeChildren(String repositoryId, String typeId,
            Boolean includePropertyDefinitions, BigInteger maxItems,
            BigInteger skipCount, ExtensionsData extension) {
        return this.getRepository().getTypeChildren(this.context, typeId,
                includePropertyDefinitions, maxItems, skipCount);
    }

    @Override
    public TypeDefinition getTypeDefinition(String repositoryId, String typeId,
            ExtensionsData extension) {
        return this.getRepository().getType(typeId);
    }

    @Override
    public ObjectInFolderList getChildren(String repositoryId, String folderId,
            String filter, String orderBy, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount,
            ExtensionsData extension) {
        return this.getRepository().getChildren(this.context, folderId,
                this.splitFilter(filter), maxItems, skipCount, this);
    }

    @Override
    public List<ObjectParentData> getObjectParents(String repositoryId, String objectId,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includeRelativePathSegment, ExtensionsData extension) {
        return this.getRepository().getObjectParents(this.context, objectId,
                this.splitFilter(filter), this);
    }

    @Override
    public ObjectData getObject(String repositoryId, String objectId, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        return this.getRepository().getObject(this.context, objectId,
                this.splitFilter(filter), this);
    }

    @Override
    public List<ObjectInFolderContainer> getDescendants(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
        return this.getRepository().getDescendants(this.context, folderId, depth, false,
                this.splitFilter(filter), this);
    }

    @Override
    public List<ObjectInFolderContainer> getFolderTree(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
        return this.getRepository().getDescendants(this.context, folderId, depth, true,
                this.splitFilter(filter), this);
    }

    @Override
    public ObjectData getFolderParent(String repositoryId, String folderId,
            String filter, ExtensionsData extension) {
        return this.getRepository().getFolderParent(this.context, folderId,
                this.splitFilter(filter), this);
    }

    @Override
    public String createDocument(String repositoryId, Properties properties,
            String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        return this.getRepository().createDocument(folderId, properties, contentStream);
    }

    @Override
    public String createFolder(String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces, Acl removeAces,
            ExtensionsData extension) {
        return this.getRepository().createFolder(folderId, properties);
    }

    @Override
    public ObjectData getObjectByPath(String repositoryId, String path, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        return this.getRepository().getObjectByPath(this.context, path,
                this.splitFilter(filter), this);
    }

    @Override
    public void deleteObjectOrCancelCheckOut(String repositoryId, String objectId,
            Boolean allVersions, ExtensionsData extension) {
        this.getRepository().deleteObject(objectId);
    }

    @Override
    public FailedToDeleteData deleteTree(String repositoryId, String folderId,
            Boolean allVersions, UnfileObject unfileObjects, Boolean continueOnFailure,
            ExtensionsData extension) {
        return this.getRepository().deleteTree(folderId);
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
    public void removeObjectFromFolder(String repositoryId, String objectId,
            String folderId, ExtensionsData extension) {
        this.getRepository().deleteObject(objectId);
    }

    @Override
    public ContentStream getContentStream(String repositoryId, String objectId,
            String streamId, BigInteger offset, BigInteger length,
            ExtensionsData extension) {
        return this.getRepository().getContentStream(objectId, streamId, offset, length);
    }

    @Override
    public void setContentStream(String repositoryId, Holder<String> objectId,
            Boolean overwriteFlag, Holder<String> changeToken,
            ContentStream contentStream, ExtensionsData extension) {
        this.getRepository().setContentStream(objectId, overwriteFlag, contentStream);
    }

    @Override
    public void appendContentStream(String repositoryId, Holder<String> objectId,
            Holder<String> changeToken, ContentStream contentStream, boolean isLastChunk,
            ExtensionsData extension) {
        this.getRepository().appendContentStream(objectId, contentStream, isLastChunk);
    }

    @Override
    public void deleteContentStream(String repositoryId, Holder<String> objectId,
            Holder<String> changeToken, ExtensionsData extension) {
        this.getRepository().deleteContentStream(objectId);
    }

    @Override
    public void updateProperties(String repositoryId, Holder<String> objectId,
            Holder<String> changeToken, Properties properties, ExtensionsData extension) {
        this.getRepository().updateProperties(this.getCallContext(), objectId,
                properties, this);
    }

    @Override
    public void moveObject(String repositoryId, Holder<String> objectId,
            String targetFolderId, String sourceFolderId, ExtensionsData extension) {
        this.getRepository().moveObject(this.getCallContext(), objectId, targetFolderId,
                this);
    }

    private Set<String> splitFilter(String filter) {
        if (filter == null) {
            return null;
        }

        if (filter.trim().length() == 0) {
            return null;
        }

        Set<String> result = new HashSet<String>();
        for (String s : filter.split(",")) {
            s = s.trim();
            if (s.equals("*")) {
                return null;
            } else if (s.length() > 0) {
                result.add(s);
            }
        }

        // set a few base properties
        // query name == id (for base type properties)
        result.add(PropertyIds.OBJECT_ID);
        result.add(PropertyIds.OBJECT_TYPE_ID);
        result.add(PropertyIds.BASE_TYPE_ID);

        return result;
    }
}
