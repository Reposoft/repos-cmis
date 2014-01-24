/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cmis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.definitions.MutableDocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableFolderTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;

/**
 * Manages the type definitions for a {@link ReposCmisRepository}. Since this is
 * a simple repository, we have just two types: files and folders.
 */
public class ReposTypeManager {

    private static final String NAMESPACE = "https://labs.repos.se/svn/repos-cmis/trunk";

    private final TypeDefinitionFactory typeDefinitionFactory;
    private final Map<String, TypeDefinition> typeDefinitions;

    public ReposTypeManager() {
        this.typeDefinitionFactory = TypeDefinitionFactory.newInstance();
        this.typeDefinitionFactory.setDefaultNamespace(NAMESPACE);

        this.typeDefinitionFactory.setDefaultControllableAcl(false);
        this.typeDefinitionFactory.setDefaultControllablePolicy(false);
        this.typeDefinitionFactory.setDefaultQueryable(false);
        this.typeDefinitionFactory.setDefaultFulltextIndexed(false);
        this.typeDefinitionFactory.setDefaultTypeMutability(this.typeDefinitionFactory
                .createTypeMutability(false, false, false));

        this.typeDefinitions = new HashMap<String, TypeDefinition>();

        MutableFolderTypeDefinition folderType = this.typeDefinitionFactory
                .createBaseFolderTypeDefinition(CmisVersion.CMIS_1_0);
        removeQueryableAndOrderableFlags(folderType);
        this.typeDefinitions.put(folderType.getId(), folderType);

        MutableDocumentTypeDefinition documentType = this.typeDefinitionFactory
                .createBaseDocumentTypeDefinition(CmisVersion.CMIS_1_0);
        removeQueryableAndOrderableFlags(documentType);
        this.typeDefinitions.put(documentType.getId(), documentType);
    }

    /**
     * Adds a type definition.
     */
    public synchronized void addTypeDefinition(TypeDefinition type) {
        if (type == null) {
            throw new IllegalArgumentException("Type must be set!");
        }

        if (type.getId() == null || type.getId().trim().length() == 0) {
            throw new IllegalArgumentException("Type must have a valid id!");
        }

        if (type.getParentTypeId() == null || type.getParentTypeId().trim().length() == 0) {
            throw new IllegalArgumentException("Type must have a valid parent id!");
        }

        TypeDefinition parentType = this.typeDefinitions.get(type.getParentTypeId());
        if (parentType == null) {
            throw new IllegalArgumentException("Parent type doesn't exist!");
        }

        MutableTypeDefinition newType = this.typeDefinitionFactory.copy(type, true);

        // copy parent type property definitions and mark them as inherited
        for (PropertyDefinition<?> propDef : parentType.getPropertyDefinitions().values()) {
            MutablePropertyDefinition<?> basePropDef = this.typeDefinitionFactory
                    .copy(propDef);
            basePropDef.setIsInherited(true);
            newType.addPropertyDefinition(basePropDef);
        }

        this.typeDefinitions.put(newType.getId(), newType);
    }

    /**
     * Removes the queryable and orderable flags from the property definitions
     * of a type definition because this implementations does neither support
     * queries nor can order objects.
     */
    private static void removeQueryableAndOrderableFlags(MutableTypeDefinition type) {
        for (PropertyDefinition<?> propDef : type.getPropertyDefinitions().values()) {
            MutablePropertyDefinition<?> mutablePropDef = (MutablePropertyDefinition<?>) propDef;
            mutablePropDef.setIsQueryable(false);
            mutablePropDef.setIsOrderable(false);
        }
    }

    /**
     * Returns the internal type definition.
     */
    public synchronized TypeDefinition getInternalTypeDefinition(String typeId) {
        return this.typeDefinitions.get(typeId);
    }

    /**
     * Returns all internal type definitions.
     */
    public synchronized List<TypeDefinition> getInternalTypeDefinitions() {
        return new ArrayList<TypeDefinition>(this.typeDefinitions.values());
    }

    // --- service methods ---

    public TypeDefinition getTypeDefinition(CallContext context, String typeId) {
        TypeDefinition type = this.typeDefinitions.get(typeId);
        if (type == null) {
            throw new CmisObjectNotFoundException("Type '" + typeId + "' is unknown!");
        }

        return this.typeDefinitionFactory.copy(type, true, context.getCmisVersion());
    }

    public TypeDefinitionList getTypeChildren(CallContext context, String typeId,
            Boolean includePropertyDefinitions, BigInteger maxItems, BigInteger skipCount) {
        return this.typeDefinitionFactory.createTypeDefinitionList(this.typeDefinitions,
                typeId, includePropertyDefinitions, maxItems, skipCount);
    }

    public List<TypeDefinitionContainer> getTypeDescendants(CallContext context,
            String typeId, BigInteger depth, Boolean includePropertyDefinitions) {
        return this.typeDefinitionFactory.createTypeDescendants(this.typeDefinitions,
                typeId, depth, includePropertyDefinitions);
    }
}
