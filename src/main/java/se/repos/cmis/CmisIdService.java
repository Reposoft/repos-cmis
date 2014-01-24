/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cmis;

import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;

/**
 * Interface for going between CMIS ID Strings and CMSItems. In OpenCMIS every
 * item should have a permanent string ID which doesn't change when the file is
 * moved. This means you can't use the current {@link CmsItem} ID as that will
 * change when the item is moved. This interface was created for mocking out
 * that functionality, but for a real deployment some types in Simonsoft CMS
 * would probably need changing.
 */
public interface CmisIdService {

    /**
     * Given a CMIS ID String, returns the associated item.
     * 
     * @throws CmisObjectNotFoundException
     */
    public CmsItemId getItemId(String cmisID);

    /**
     * Gets the CMIS ID of the provided CmsItem. If the item doesn't have an ID
     * yet one will be assigned.
     */
    public String getCmisId(CmsItemId item);

    /**
     * Directly assigns the item to that ID.
     */
    public void putItem(CmsItemId item, String cmisId);

    /**
     * Deletes the given ID and item from the service.
     */
    void deleteItem(CmsItemId item, String cmisId);
}
