package se.repos.cmis;

import java.util.ArrayList;
import java.util.List;

import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.info.CmsItemLookup;

public class ReposObjectInFolderContainer implements ObjectInFolderContainer {
    private CmsItem cmsItem;
    private CmsItemLookup lookup;

    public ReposObjectInFolderContainer(CmsItem cmsItem, CmsItemLookup lookup) {
        this.cmsItem = cmsItem;
        this.lookup = lookup;
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
    public ObjectInFolderData getObject() {
        return new ReposObjectInFolderData(this.cmsItem);
    }

    @Override
    public List<ObjectInFolderContainer> getChildren() {
        ArrayList<ObjectInFolderContainer> children = new ArrayList<ObjectInFolderContainer>();
        for (CmsItemId id : this.lookup.getDescendants(this.cmsItem.getId())) {
            CmsItem descendant = this.lookup.getItem(id);
            children.add(new ReposObjectInFolderContainer(descendant, this.lookup));
        }
        return children;
    }

}
