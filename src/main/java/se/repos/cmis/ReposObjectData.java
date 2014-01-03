package se.repos.cmis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AllowableActionsImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.DocumentTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FolderTypeDefinitionImpl;

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
        return null;
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
            return new DocumentTypeDefinitionImpl().getBaseTypeId();
        case Folder:
            return new FolderTypeDefinitionImpl().getBaseTypeId();
        case Repository:
        case Symlink:
        default:
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Properties getProperties() {
        return null;
    }

    @Override
    public AllowableActions getAllowableActions() {
        AllowableActionsImpl actions = new AllowableActionsImpl();
        actions.setAllowableActions(this.actionSet);
        return actions;
    }

    @Override
    public List<ObjectData> getRelationships() {
        // TODO What is a relationship and does ReposWeb have them?
        return null;
    }

    @Override
    public ChangeEventInfo getChangeEventInfo() {
        return null;
    }

    @Override
    public Acl getAcl() {
        return null;
    }

    @Override
    public Boolean isExactAcl() {
        return null;
    }

    @Override
    public PolicyIdList getPolicyIds() {
        return null;
    }

    @Override
    public List<RenditionData> getRenditions() {
        return null;
    }
}