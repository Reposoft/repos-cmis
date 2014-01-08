package se.repos.cmis;

import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;

import se.repos.authproxy.AuthFailedException;
import se.repos.authproxy.AuthRequiredException;
import se.repos.authproxy.ReposCurrentUser;
import se.repos.cms.backend.filehead.LocalCmsCommit;
import se.repos.cms.backend.filehead.LocalCmsItemLookup;
import se.repos.cms.backend.filehead.LocalRepoRevision;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
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

        CmsRepository repo = new CmsRepository("/tmp", "repos");
        this.bind(CmsRepository.class).toInstance(repo);
        ReposCurrentUser currentUser = new ReposCurrentUser() {

            @Override
            public void setFailed(String realm) throws AuthFailedException {
                return;
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public String getUsernameRequired(String realm) throws AuthRequiredException {
                return this.getUsername();
            }

            @Override
            public String getUsername() {
                return "user";
            }

            @Override
            public String getPassword() {
                return "secret";
            }
        };
        this.bind(ReposCurrentUser.class).toInstance(currentUser);
        this.bind(RepoRevision.class).toInstance(new LocalRepoRevision());
    }
}
