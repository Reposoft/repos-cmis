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

public class ReposCmisServiceFactory extends AbstractServiceFactory {
    private static final BigInteger DEFAULT_MAX_ITEMS_TYPES = BigInteger.valueOf(50);
    private static final BigInteger DEFAULT_DEPTH_TYPES = BigInteger.valueOf(-1);
    private static final BigInteger DEFAULT_MAX_ITEMS_OBJECTS = BigInteger.valueOf(200);
    private static final BigInteger DEFAULT_DEPTH_OBJECTS = BigInteger.valueOf(10);

    private ReposRepositoryManager manager;

    @Inject
    public ReposCmisServiceFactory(ReposRepositoryManager manager) {
        if (manager == null) {
            throw new NullPointerException();
        }
        this.manager = manager;
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
        ReposCmisService service = new ReposCmisService(this.manager, context);
        CmisServiceWrapper<ReposCmisService> wrapperService = new CmisServiceWrapper<ReposCmisService>(
                service, DEFAULT_MAX_ITEMS_TYPES, DEFAULT_DEPTH_TYPES,
                DEFAULT_MAX_ITEMS_OBJECTS, DEFAULT_DEPTH_OBJECTS);
        return wrapperService;
    }
}
