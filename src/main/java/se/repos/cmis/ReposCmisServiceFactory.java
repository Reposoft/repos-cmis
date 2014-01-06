/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cmis;

import java.math.BigInteger;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.CmisServiceWrapper;

// TODO Convert this class to an {@link AbstractModule}.
/**
 * CMIS Service Factory.
 */
public class ReposCmisServiceFactory extends AbstractServiceFactory {

    /** Default maxItems value for getTypeChildren()}. */
    private static final BigInteger DEFAULT_MAX_ITEMS_TYPES = BigInteger.valueOf(50);

    /** Default depth value for getTypeDescendants(). */
    private static final BigInteger DEFAULT_DEPTH_TYPES = BigInteger.valueOf(-1);

    /**
     * Default maxItems value for getChildren() and other methods returning
     * lists of objects.
     */
    private static final BigInteger DEFAULT_MAX_ITEMS_OBJECTS = BigInteger.valueOf(200);

    /** Default depth value for getDescendants(). */
    private static final BigInteger DEFAULT_DEPTH_OBJECTS = BigInteger.valueOf(10);

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
        // TODO Add authentication here.

        ReposCmisService service = new ReposCmisService();
        service.setCallContext(context);
        @SuppressWarnings("unused")
        CmisServiceWrapper<ReposCmisService> wrapperService = new CmisServiceWrapper<ReposCmisService>(
                service, DEFAULT_MAX_ITEMS_TYPES, DEFAULT_DEPTH_TYPES,
                DEFAULT_MAX_ITEMS_OBJECTS, DEFAULT_DEPTH_OBJECTS);
        // TODO Return wrapperService instead.
        return service;
    }
}
