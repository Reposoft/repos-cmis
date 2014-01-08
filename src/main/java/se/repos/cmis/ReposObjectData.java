package se.repos.cmis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ChangeEventInfo;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.PolicyIdList;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AllowableActionsImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ChangeEventInfoDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PolicyIdListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;

import se.simonsoft.cms.item.CmsItem;

class ReposObjectData implements ObjectData {
    private final CmsItem item;
    private final Set<Action> actionSet;

    public ReposObjectData(CmsItem item) {
        this.item = item;
        this.actionSet = new HashSet<Action>();

        // TODO Should Repos support working copies?
        // this.actionSet.add(Action.CAN_CHECK_OUT);
        // this.actionSet.add(Action.CAN_CANCEL_CHECK_OUT);
        // this.actionSet.add(Action.CAN_CHECK_IN);

        // TODO Should Repos support content streams?
        // this.actionSet.add(Action.CAN_GET_CONTENT_STREAM);
        // this.actionSet.add(Action.CAN_SET_CONTENT_STREAM);
        // this.actionSet.add(Action.CAN_DELETE_CONTENT_STREAM);

        this.actionSet.add(Action.CAN_ADD_OBJECT_TO_FOLDER);
        this.actionSet.add(Action.CAN_REMOVE_OBJECT_FROM_FOLDER);
        this.actionSet.add(Action.CAN_DELETE_OBJECT);
        this.actionSet.add(Action.CAN_GET_FOLDER_TREE);
        this.actionSet.add(Action.CAN_GET_OBJECT_PARENTS);
        this.actionSet.add(Action.CAN_GET_FOLDER_PARENT);
        this.actionSet.add(Action.CAN_GET_DESCENDANTS);
        this.actionSet.add(Action.CAN_MOVE_OBJECT);
        this.actionSet.add(Action.CAN_GET_CHILDREN);
        this.actionSet.add(Action.CAN_CREATE_DOCUMENT);
        this.actionSet.add(Action.CAN_CREATE_FOLDER);
        this.actionSet.add(Action.CAN_CREATE_ITEM);
        this.actionSet.add(Action.CAN_DELETE_TREE);
    }

    @Override
    public List<CmisExtensionElement> getExtensions() {
        return new ArrayList<CmisExtensionElement>();
    }

    @Override
    public void setExtensions(List<CmisExtensionElement> extensions) {
        return;
    }

    @Override
    public String getId() {
        return this.item.getId().getLogicalId();
    }

    @Override
    public BaseTypeId getBaseTypeId() {
        switch (this.item.getKind()) {
        case File:
            return BaseTypeId.CMIS_DOCUMENT;
        case Folder:
            return BaseTypeId.CMIS_FOLDER;
        case Repository:
        case Symlink:
        default:
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Properties getProperties() {
        PropertiesImpl properties = new PropertiesImpl();
        properties.addProperty(new PropertyStringImpl(PropertyIds.NAME, this.item.getId()
                .getLogicalId()));
        properties.addProperty(new PropertyStringImpl(PropertyIds.OBJECT_ID, this.item.getId()
                .getLogicalIdFull()));
        // TODO Set properties.
        return properties;
    }

    @Override
    public AllowableActions getAllowableActions() {
        AllowableActionsImpl actions = new AllowableActionsImpl();
        actions.setAllowableActions(this.actionSet);
        return actions;
    }

    @Override
    public List<ObjectData> getRelationships() {
        return new ArrayList<ObjectData>();
    }

    @Override
    public ChangeEventInfo getChangeEventInfo() {
        return new ChangeEventInfoDataImpl();
    }

    @Override
    public Acl getAcl() {
        return new AccessControlListImpl();
    }

    @Override
    public Boolean isExactAcl() {
        return Boolean.TRUE;
    }

    @Override
    public PolicyIdList getPolicyIds() {
        return new PolicyIdListImpl();
    }

    @Override
    public List<RenditionData> getRenditions() {
        return new ArrayList<RenditionData>();
    }
}