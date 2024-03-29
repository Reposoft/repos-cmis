/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cmis;

import java.util.HashMap;

import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import se.simonsoft.cms.item.CmsItemId;

import com.google.inject.Inject;

/**
 * Assigns each item an ID based on an integer. So the first ID assigned will be
 * "0", the second "1" and so on.. The IDs are stored in memory in a table.
 */
public class IncreasingCmisIdService implements CmisIdService {
    private HashMap<String, CmsItemId> idToItem;
    private HashMap<CmsItemId, String> itemToId;
    private long label = 0L;

    @Inject
    public IncreasingCmisIdService() {
        this.idToItem = new HashMap<String, CmsItemId>();
        this.itemToId = new HashMap<CmsItemId, String>();
    }

    @Override
    public CmsItemId getItemId(String cmisID) {
        if (cmisID == null) {
            throw new NullPointerException();
        }
        CmsItemId item = this.idToItem.get(cmisID);
        if (item == null) {
            throw new CmisObjectNotFoundException("Unknown ID: " + cmisID);
        }
        return item;
    }

    @Override
    public String getCmisId(CmsItemId item) {
        if (item == null) {
            throw new NullPointerException();
        }

        if (this.itemToId.containsKey(item)) {
            return this.itemToId.get(item);
        }

        String newCmisId = Long.toString(this.label++);
        this.putItem(item, newCmisId);
        return newCmisId;
    }

    @Override
    public void putItem(CmsItemId item, String cmisId) {
        this.idToItem.put(cmisId, item);
        this.itemToId.put(item, cmisId);
    }

    @Override
    public void deleteItem(CmsItemId item, String cmisId) {
        this.itemToId.remove(item);
        this.idToItem.remove(cmisId);
        return;
    }
}
