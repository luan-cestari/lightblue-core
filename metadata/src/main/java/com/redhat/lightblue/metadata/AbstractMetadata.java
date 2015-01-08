/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.redhat.lightblue.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.lightblue.util.Path;

import java.util.*;

/**
 *
 * @author nmalik
 */
public abstract class AbstractMetadata implements Metadata {
    public static final String SEMVER_REGEX = "^\\d+\\.\\d+\\.\\d+(-.*)?$";

    protected Map<MetadataRoles, List<String>> roleMap;

    /**
     * Checks that the given version exists, raises an error if it does not.
     *
     * @param version
     * @return true if the version exists
     */
    protected abstract boolean checkVersionExists(String entityName, String version);

    protected abstract void checkDataStoreIsValid(EntityInfo md);

    /**
     * Checks that the default version on the EntityInfo exists. If no default
     * version is set then has no side effect. If the default version does not
     * exist an error is raised.
     *
     * @param ei
     */
    protected final void validateDefaultVersion(EntityInfo ei) {
        if (ei.getDefaultVersion() != null && !checkVersionExists(ei.getName(), ei.getDefaultVersion())) {
            throw com.redhat.lightblue.util.Error.get(MetadataConstants.ERR_INVALID_DEFAULT_VERSION, ei.getName() + ":" + ei.getDefaultVersion());
        }
    }

    protected final Version checkVersionIsValid(EntityMetadata md) {
        return checkVersionIsValid(md.getEntitySchema().getVersion());
    }

    protected final Version checkVersionIsValid(EntitySchema md) {
        return checkVersionIsValid(md.getVersion());
    }

    protected final Version checkVersionIsValid(Version ver) {
        if (ver == null || ver.getValue() == null || ver.getValue().length() == 0) {
            throw new IllegalArgumentException(MetadataConstants.ERR_INVALID_VERSION);
        }
        String value = ver.getValue();
        if (!value.matches(SEMVER_REGEX)) {
            throw com.redhat.lightblue.util.Error.get(MetadataConstants.ERR_INVALID_VERSION_NUMBER, ver.getValue());
        }
        return ver;
    }

    protected final void checkMetadataHasName(EntityMetadata md) {
        checkMetadataHasName(md.getEntityInfo());
    }

    protected final void checkMetadataHasName(EntityInfo md) {
        if (md.getName() == null || md.getName().length() == 0) {
            throw new IllegalArgumentException(MetadataConstants.ERR_EMPTY_METADATA_NAME);
        }
    }

    protected final void checkMetadataHasFields(EntityMetadata md) {
        checkMetadataHasFields(md.getEntitySchema());
    }

    protected final void checkDataStoreIsValid(EntityMetadata md) {
        checkDataStoreIsValid(md.getEntityInfo());
    }

    protected final void checkMetadataHasFields(EntitySchema md) {
        if (md.getFields().getNumChildren() <= 0) {
            throw new IllegalArgumentException(MetadataConstants.ERR_METADATA_WITH_NO_FIELDS);
        }
    }

    /**
     * Add roles and paths to accessMap where accessMap = <role, <operation,
     * List<path>>>
     *
     * @param roles
     * @param operation
     * @param path
     * @param accessMap
     */
    protected final void helperAddRoles(Collection<String> roles, String operation, String path, Map<String, Map<String, List<String>>> accessMap) {
        for (String role : roles) {
            if (!accessMap.containsKey(role)) {
                accessMap.put(role, new HashMap<String, List<String>>());
            }
            if (!accessMap.get(role).containsKey(operation)) {
                accessMap.get(role).put(operation, new ArrayList<String>());
            }
            accessMap.get(role).get(operation).add(path);
        }
    }

    @Override
    public Map<MetadataRoles, List<String>> getMappedRoles() {
        return roleMap;
    }

    public void setRoleMap(Map<MetadataRoles, List<String>> roleMap) {
        this.roleMap = roleMap;
    }

    @Override
    public JsonNode getJSONSchema(String entityName, String version) {
        ObjectNode jsonNode = new ObjectNode(JsonNodeFactory.instance);
        ObjectNode properties = new ObjectNode(JsonNodeFactory.instance);
        EntityMetadata entityMetadata = getEntityMetadata(entityName, version);
        FieldTreeNode fieldTreeRoot = entityMetadata.getEntitySchema().getFieldTreeRoot();

        jsonNode.set("$schema", TextNode.valueOf("http://json-schema.org/draft-04/schema#"));
        jsonNode.set("type", TextNode.valueOf("object"));
        jsonNode.set("description", TextNode.valueOf(String.format("JSON schema for entity '%s' version '%s'", entityName, version)));
        jsonNode.set("properties", properties);
        buildJsonNodeSchema(properties, fieldTreeRoot);
        ArrayNode value = new ArrayNode(JsonNodeFactory.instance);
        Field[] requiredFields = entityMetadata.getEntitySchema().getRequiredFields();
        for (Field requiredField : requiredFields) {
            TextNode.valueOf(requiredField.getFullPath().toString());
        }
        properties.set("required", value);

        return jsonNode;
    }

    private void buildJsonNodeSchema(ObjectNode jsonNode, FieldTreeNode fieldTreeRoot) {
        TreeMap<Path, Field> fieldMap = new TreeMap<>();
        Iterator<? extends FieldTreeNode> children = fieldTreeRoot.getChildren();
        Stack<Iterator<? extends FieldTreeNode>> fieldsPending = new Stack<>();
        do{
            FieldTreeNode fieldTreeChild = children.next();
            if (fieldTreeChild instanceof ObjectField) {
                ObjectField of = (ObjectField) fieldTreeChild;
                /////
                fieldsPending.push(children);
                children = of.getChildren();
            }else if (fieldTreeChild instanceof SimpleField) {
                SimpleField sf = (SimpleField) fieldTreeChild;
                ObjectNode json = new ObjectNode(JsonNodeFactory.instance);
                json.set("type",  TextNode.valueOf(sf.getType().getName()));
                //json.set("type",  TextNode.valueOf(sf.getProperties().getName()));


                for (FieldConstraint fc : sf.getConstraints()) {

                }
                jsonNode.set(sf.getName(),json);
            }
            do {
                if(!children.hasNext()){
                    if(!fieldsPending.empty()){
                        children = fieldsPending.pop();
                    } else {
                        break;
                    }
                }
            } while(!children.hasNext());

        } while (children.hasNext());

    }
}
