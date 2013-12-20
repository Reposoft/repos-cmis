package se.repos.cmis;

import java.util.List;

import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;

import se.simonsoft.cms.item.CmsItem;

public class ReposCmsItemObjectParentData implements ObjectParentData {
    private final CmsItem item;
    private final ObjectData itemData;

    public ReposCmsItemObjectParentData(CmsItem item) {
        this.item = item;
        this.itemData = new ReposCmsItemObjectData(this.item);
    }

    @Override
    public List<CmisExtensionElement> getExtensions() {
        return null;
    }

    @Override
    public void setExtensions(List<CmisExtensionElement> extensions) {
        return;
    }

    @Override
    public ObjectData getObject() {
        return this.itemData;
    }

    @Override
    public String getRelativePathSegment() {
        return this.item.getId().getRelPath().getPath();
    }

}
