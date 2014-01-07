package se.repos.cmis;

import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;

import se.repos.authproxy.ReposCurrentUser;
import se.repos.cms.backend.filehead.LocalCmsCommit;
import se.repos.cms.backend.filehead.LocalCmsItemLookup;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.commit.CmsCommit;
import se.simonsoft.cms.item.info.CmsItemLookup;

import com.google.inject.AbstractModule;

public class TestModule extends AbstractModule {

    @Override
    protected void configure() {
        this.bind(ReposCmisServiceFactory.class);
        this.bind(AbstractServiceFactory.class).to(ReposCmisServiceFactory.class);
        this.bind(CmsCommit.class).to(LocalCmsCommit.class);
        this.bind(CmsItemLookup.class).to(LocalCmsItemLookup.class);

        // TODO Initialize instance objects.
        CmsRepository repo = null;
        this.bind(CmsRepository.class).toInstance(repo);
        ReposCurrentUser currentUser = null;
        this.bind(ReposCurrentUser.class).toInstance(currentUser);
    }
}
