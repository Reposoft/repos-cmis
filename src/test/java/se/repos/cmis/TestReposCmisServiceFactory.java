package se.repos.cmis;

import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;

import com.google.inject.Guice;
import com.google.inject.Injector;

// TODO Find out how to run this class as a stand alone server.
/**
 * Demo implementation of {@link ReposCmisServiceFactory} that uses the classes
 * in se.repos.cms.backend.filehead.* instead of the real CMS as a back end.
 * 
 * To run this class:
 * 
 * To test this class: Start any of the tests in
 * org.apache.chemistry.opencmis.tck.tests.*, and set the following VM
 * parameters:
 *  -Dorg.apache.chemistry.opencmis.binding.spi.type=local
 *  -Dorg.apache.chemistry.opencmis.binding.local.classname=se.repos.cmis.TestReposCmisServiceFactory
 *  -Dorg.apache.chemistry.opencmis.binding.repositoryRoot=$YOUR_TEST_FOLDER
 */
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
