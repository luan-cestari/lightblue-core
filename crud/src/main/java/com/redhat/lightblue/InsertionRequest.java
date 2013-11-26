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
package com.redhat.lightblue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.redhat.lightblue.query.Projection;

public class InsertionRequest extends Request {

    private JsonNode entityData;
    private Projection returnFields;

    public JsonNode getEntityData() {
        return entityData;
    }

    public void setEntityData(JsonNode data) {
        this.entityData=data;
    }

    public Projection getReturnFields() {
        return returnFields;
    }

    public void setReturnFields(Projection p) {
        returnFields=p;
    }

    public JsonNode toJson() {
        ObjectNode node=(ObjectNode)super.toJson();
        if(entityData!=null)
            node.set("data",entityData);
        if(returnFields!=null)
            node.set("returning",returnFields.toJson());
        return node;
    }

    public static InsertionRequest fromJson(ObjectNode node) {
        InsertionRequest req=new InsertionRequest();
        req.parse(node);
        req.entityData=node.get("data");
        JsonNode x=node.get("returning");
        if(x!=null)
            req.returnFields=Projection.fromJson(x);
        return req;
    }
}