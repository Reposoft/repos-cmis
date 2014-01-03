package se.repos.cmis;

import java.util.List;

import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemPath;

public class ReposObjectInFolderData implements ObjectInFolderData {
    private final CmsItem item;
    private final ObjectData itemData;

    public ReposObjectInFolderData(CmsItem item) {
        this.item = item;
        this.itemData = new ReposObjectData(item);
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
    public String getPathSegment() {
        CmsItemPath itemPath = this.item.getId().getRelPath();
        String lastSegment = itemPath.getPathSegments().get(
                itemPath.getPathSegmentsCount() - 1);
        return lastSegment;
    }

}
