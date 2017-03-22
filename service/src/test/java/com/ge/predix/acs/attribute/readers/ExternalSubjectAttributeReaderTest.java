package com.ge.predix.acs.attribute.readers;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.model.Attribute;

public class ExternalSubjectAttributeReaderTest {

    @Spy
    private AttributeCache attributeCache;

    @InjectMocks
    @Spy
    private ExternalSubjectAttributeReader externalSubjectAttributeReader;

    private static final String IDENTIFIER = "part/03f95db1-4255-4265-a509-f7bca3e1fee4";

    private Set<Attribute> expectedAdapterAttributes;

    private static String generateRandomString() {
        return RandomStringUtils.randomAlphanumeric(20);
    }

    @BeforeClass
    void beforeClass() throws Exception {
        this.expectedAdapterAttributes = Collections
                .singleton(new Attribute(generateRandomString(), generateRandomString(), generateRandomString()));
    }

    @BeforeMethod
    void beforeMethod() {
        this.attributeCache = Mockito.mock(AttributeCache.class);
        this.externalSubjectAttributeReader = new ExternalSubjectAttributeReader(null, this.attributeCache, 3000);

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAttributesWithCacheMiss() throws Exception {
        Mockito.when(this.attributeCache.getAttributes(IDENTIFIER)).thenReturn(Collections.emptySet());

        Mockito.doReturn(this.expectedAdapterAttributes).when(this.externalSubjectAttributeReader)
                .getAttributesFromAdapters(IDENTIFIER);

        Set<Attribute> actualAdapterAttributes = this.externalSubjectAttributeReader.getAttributes(IDENTIFIER);

        Mockito.verify(this.attributeCache).setAttributes(IDENTIFIER, this.expectedAdapterAttributes);

        Assert.assertEquals(this.expectedAdapterAttributes, actualAdapterAttributes);
    }

    @Test
    public void testGetAttributesWithCacheHit() throws Exception {
        Mockito.when(this.attributeCache.getAttributes(IDENTIFIER)).thenReturn(this.expectedAdapterAttributes);

        Set<Attribute> actualAdapterAttributes = this.externalSubjectAttributeReader.getAttributes(IDENTIFIER);

        Mockito.verify(this.externalSubjectAttributeReader, Mockito.times(0)).getAttributesFromAdapters(IDENTIFIER);
        Mockito.verify(this.attributeCache, Mockito.times(0)).setAttributes(IDENTIFIER, this.expectedAdapterAttributes);

        Assert.assertEquals(this.expectedAdapterAttributes, actualAdapterAttributes);
    }

    @Test
    public void testGetAttributesByScope() throws Exception {
        Mockito.doReturn(this.expectedAdapterAttributes).when(this.externalSubjectAttributeReader)
                .getAttributesFromAdapters(IDENTIFIER);

        Set<Attribute> actualAttributes = this.externalSubjectAttributeReader.getAttributes(IDENTIFIER);

        Assert.assertEquals(this.expectedAdapterAttributes, actualAttributes);

        actualAttributes = this.externalSubjectAttributeReader.getAttributesByScope(IDENTIFIER,
                Collections.singleton(new Attribute("test-issuer", "test-scope", "test-value")));

        Assert.assertEquals(this.expectedAdapterAttributes, actualAttributes);
    }
}
