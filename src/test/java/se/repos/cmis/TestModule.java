package se.repos.cmis;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;

import se.repos.authproxy.AuthFailedException;
import se.repos.authproxy.AuthRequiredException;
import se.repos.authproxy.ReposCurrentUser;
import se.repos.cms.backend.filehead.LocalCmsCommit;
import se.repos.cms.backend.filehead.LocalCmsItemLookup;
import se.repos.cms.backend.filehead.LocalRepoRevision;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.commit.CmsCommit;
import se.simonsoft.cms.item.info.CmsItemLookup;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class TestModule extends AbstractModule {

    @Override
    protected void configure() {
        this.bind(AbstractServiceFactory.class).to(ReposCmisServiceFactory.class);
        this.bind(ReposCmisServiceFactory.class);
        this.bind(CmsCommit.class).to(LocalCmsCommit.class);
        this.bind(CmsItemLookup.class).to(LocalCmsItemLookup.class);
        this.bind(RepoRevision.class).toInstance(new LocalRepoRevision());

        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        String repositoryRoot = runtimeMxBean.getSystemProperties().get(
                "org.apache.chemistry.opencmis.binding.repositoryRoot");
        new File(repositoryRoot).mkdir();
        CmsItemPath repoPath = new CmsItemPath(repositoryRoot);
        CmsRepository repo = new CmsRepository("http://localHost", repoPath.getParent()
                .getPath(), repoPath.getName());
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
        this.bind(String.class).annotatedWith(Names.named("repositoryId"))
                .toInstance("repo");
        this.bind(ReposCmisRepository.class);

        ReposRepositoryManager manager = new ReposRepositoryManager();
        this.bind(ReposRepositoryManager.class).toInstance(manager);
    }
}
