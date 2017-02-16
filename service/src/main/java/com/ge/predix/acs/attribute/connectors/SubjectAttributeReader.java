package com.ge.predix.acs.attribute.connectors;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;

public interface SubjectAttributeReader extends AttributeReader {
    Set<Attribute> getAttributesByScope(String identifier, Set<Attribute> scopes);
}
