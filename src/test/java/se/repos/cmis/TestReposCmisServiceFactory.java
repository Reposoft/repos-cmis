package se.repos.cmis;

import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Demo implementation of {@link ReposCmisServiceFactory} that uses the classes
 * in se.repos.cms.backend.filehead.* instead of the real CMS as a back end.
 * 
 * To run this class: Open a shell in the project directory and run the command
 * "mvn jetty:run-war $PORT_NUMBER", where $PORT_NUMBER is the port you want to
 * run the server on.
 * 
 * To test this class: Start any of the tests in
 * org.apache.chemistry.opencmis.tck.tests.*, (available in -test-tck jar)
 * and set the following VM parameters:
 *  -Dorg.apache.chemistry.opencmis.binding.spi.type=local
 *  -Dorg.apache.chemistry.opencmis.binding.local.classname=se.repos.cmis.TestReposCmisServiceFactory
 *  -Dorg.apache.chemistry.opencmis.binding.repositoryRoot=$YOUR_TEST_FOLDER
 *  
 * We pass the .basic and .crud test packages.
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
