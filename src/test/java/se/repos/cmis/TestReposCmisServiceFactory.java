package se.repos.cmis;

import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TestReposCmisServiceFactory extends AbstractServiceFactory {
    private ReposCmisServiceFactory fact;

    public TestReposCmisServiceFactory() {
        Injector inject = Guice.createInjector(new TestModule());
        this.fact = inject.getInstance(ReposCmisServiceFactory.class);
    }

    @Override
    public CmisService getService(CallContext context) {
        return this.fact.getService(context);
    }
}
