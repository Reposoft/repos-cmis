/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cmis;

import java.math.BigInteger;
import java.util.Map;

import javax.inject.Inject;

import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.CmisServiceWrapper;

import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.commit.CmsCommit;
import se.simonsoft.cms.item.info.CmsItemLookup;

public class ReposCmisServiceFactory extends AbstractServiceFactory {
    private static final BigInteger DEFAULT_MAX_ITEMS_TYPES = BigInteger.valueOf(50);
    private static final BigInteger DEFAULT_DEPTH_TYPES = BigInteger.valueOf(-1);
    private static final BigInteger DEFAULT_MAX_ITEMS_OBJECTS = BigInteger.valueOf(200);
    private static final BigInteger DEFAULT_DEPTH_OBJECTS = BigInteger.valueOf(10);

    private CmsRepository repository;
    private CmsCommit commit;
    private CmsItemLookup lookup;
    private RepoRevision currentRevision;

    @Inject
    public ReposCmisServiceFactory(CmsRepository repository, CmsCommit commit,
            CmsItemLookup lookup, RepoRevision currentRevision) {
        this.repository = repository;
        this.commit = commit;
        this.lookup = lookup;
        this.currentRevision = currentRevision;
    }

    public void setCurrentRevision(RepoRevision currentRevision) {
        this.currentRevision = currentRevision;
    }

    @Override
    public void init(Map<String, String> parameters) {
        return;
    }

    @Override
    public void destroy() {
        return;
    }

    @Override
    public CmisService getService(CallContext context) {
        ReposCmisService service = new ReposCmisService(this.repository, this.commit,
                this.lookup, this.currentRevision, context);
        @SuppressWarnings("unused")
        CmisServiceWrapper<ReposCmisService> wrapperService = new CmisServiceWrapper<ReposCmisService>(
                service, DEFAULT_MAX_ITEMS_TYPES, DEFAULT_DEPTH_TYPES,
                DEFAULT_MAX_ITEMS_OBJECTS, DEFAULT_DEPTH_OBJECTS);
        // TODO Return wrapperService instead.
        return service;
    }
}
