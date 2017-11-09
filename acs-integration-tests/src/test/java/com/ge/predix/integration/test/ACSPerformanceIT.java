/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ge.predix.integration.test;

import static com.ge.predix.test.utils.PrivilegeHelper.DEFAULT_RESOURCE_IDENTIFIER;
import static com.ge.predix.test.utils.PrivilegeHelper.DEFAULT_SUBJECT_ID;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.test.utils.ACSITSetUpFactory;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.PrivilegeHelper;

@SuppressWarnings({ "nls" })
@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
public class ACSPerformanceIT extends AbstractTestNGSpringContextTests {

    @Autowired
    private ACSITSetUpFactory acsitSetUpFactory;

    @Autowired
    private PolicyHelper policyHelper;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    private String testPolicyName;
    private String acsUrl;
    private HttpHeaders zone1Headers;
    private OAuth2RestTemplate acsRestTemplate;
    private BaseSubject defaultSubject;
    private static final String NOT_MATCHING_ACTION = "HEAD";

    @BeforeClass
    public void setup() throws JsonParseException, JsonMappingException, IOException {

        this.acsitSetUpFactory.setUp();
        this.acsUrl = this.acsitSetUpFactory.getAcsUrl();
        this.zone1Headers = this.acsitSetUpFactory.getZone1Headers();
        this.acsRestTemplate = this.acsitSetUpFactory.getAcsZoneAdminRestTemplate();
        this.defaultSubject = new BaseSubject(DEFAULT_SUBJECT_ID);
        this.privilegeHelper.putSubject(this.acsRestTemplate, this.defaultSubject, this.acsUrl, this.zone1Headers,
                this.privilegeHelper.getDefaultAttribute());

        String policyFile = "src/test/resources/policies/large-policy-set.json";
        this.testPolicyName = this.policyHelper.setTestPolicy(this.acsRestTemplate, this.zone1Headers, this.acsUrl,
                policyFile);
    }

    @AfterClass
    public void tearDown() throws Exception {
        this.policyHelper.deletePolicySet(this.acsRestTemplate, this.acsUrl, this.testPolicyName, this.zone1Headers);
        this.privilegeHelper.deleteSubject(this.acsRestTemplate, this.acsUrl,
                this.defaultSubject.getSubjectIdentifier(), this.zone1Headers);
        this.acsitSetUpFactory.destroy();
    }

    @AfterMethod
    public void logExecutionTime(final ITestResult tr) {
        long time = tr.getEndMillis() - tr.getStartMillis();
        System.out.println("Execution time: " + Long.toString(time) + " ms");
    }

    @Test(invocationCount = 2)
    public void testPolicyEvalPerformanceWithLargePolicySet() throws Exception {

        PolicyEvaluationRequestV1 request = this.policyHelper
                .createEvalRequest(NOT_MATCHING_ACTION, this.defaultSubject.getSubjectIdentifier(),
                        (new BaseResource(DEFAULT_RESOURCE_IDENTIFIER)).getResourceIdentifier(), null);

        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsRestTemplate.postForEntity(
                this.acsUrl + PolicyHelper.ACS_POLICY_EVAL_API_PATH, new HttpEntity<>(request, this.zone1Headers),
                PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.DENY);

    }
}
