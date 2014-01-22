package se.repos.cmis;

import se.simonsoft.cms.item.CmsItemId;

/**
 * Interface for going between CMIS ID Strings and CMSItems. Created for mocking
 * out functionality that will probably have to be implemented in CmsItem for a
 * production implementation.
 */
public interface CmisIdService {

    /**
     * Given a CMIS ID String, returns the associated item.
     */
    public CmsItemId getItemId(String cmisID);

    /**
     * Gets the CMIS ID of the provided CmsItem.
     */
    public String getCmisId(CmsItemId item);

    public void putItem(CmsItemId item, String cmisId);

    void deleteItem(CmsItemId item, String cmisId);
}
