/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cmis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import com.google.inject.Inject;

/**
 * In CMIS, the repository to perform an operation on is identified by a string.
 * This type maps these string repository IDs to {@link ReposCmisRepository}. 
 */
public class ReposRepositoryManager {
    private Map<String, ReposCmisRepository> repositories;
    
    public ReposRepositoryManager() {
        this.repositories = new HashMap<String, ReposCmisRepository>();
    }
    
    @Inject
    public void addRepository(ReposCmisRepository repository) {
        if(repository == null) {
            throw new NullPointerException();
        }
        this.repositories.put(repository.getRepositoryId(), repository);
    }
    
    public ReposCmisRepository getRepository(String repositoryId) {
        ReposCmisRepository repository = this.repositories.get(repositoryId);
        if(repository == null) {
            throw new CmisObjectNotFoundException("Unknown repository '" + repositoryId
                    + "'!");
        }
        return repository;
    }
    
    public Collection<ReposCmisRepository> getRepositories() {
        return this.repositories.values();
    }
}
