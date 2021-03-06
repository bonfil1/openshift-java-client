/******************************************************************************* 
 * Copyright (c) 2007 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package com.openshift.express.internal.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.junit.Test;

import com.openshift.express.client.IDomain;
import com.openshift.express.client.IOpenShiftService;
import com.openshift.express.client.SSHKeyType;
import com.openshift.express.client.OpenShiftException;
import com.openshift.express.client.SSHKeyPair;
import com.openshift.express.internal.client.InternalUser;
import com.openshift.express.internal.client.request.ChangeDomainRequest;
import com.openshift.express.internal.client.request.CreateDomainRequest;
import com.openshift.express.internal.client.request.OpenShiftEnvelopeFactory;
import com.openshift.express.internal.client.request.marshalling.DomainRequestJsonMarshaller;
import com.openshift.express.internal.client.response.OpenShiftResponse;
import com.openshift.express.internal.client.response.unmarshalling.DomainResponseUnmarshaller;
import com.openshift.express.internal.client.response.unmarshalling.JsonSanitizer;
import com.openshift.express.internal.client.test.fakes.NoopOpenShiftServiceFake;
import com.openshift.express.internal.client.test.fakes.TestSSHKey;

/**
 * @author André Dietisheim
 */
public class DomainTest {

	private static final String RHLOGIN = "toolsjboss@gmail.com";
	private static final String PASSWORD = "1q2w3e";
	private static final String UUID = "0c82860dae904a4d87f8e5d87a5af840";

	@Test
	public void canMarshallDomainCreateRequest() throws IOException, OpenShiftException {
		SSHKeyPair sshKey = TestSSHKey.create();
		String expectedRequestString = createDomainRequestString(
				PASSWORD, RHLOGIN, true, "myDomain", false, sshKey.getPublicKey(), sshKey.getKeyType(), false);

		CreateDomainRequest request = new CreateDomainRequest("myDomain", sshKey, RHLOGIN, true);
		String requestString =
				new OpenShiftEnvelopeFactory(
						PASSWORD, null, null,
						new DomainRequestJsonMarshaller().marshall(request))
						.createString();
		assertEquals(expectedRequestString, requestString);
	}

	@Test
	public void canUnmarshallDomainCreateResponse() throws IOException, OpenShiftException {
		String domainName = "myDomain";
		String responseString = createDomainResponseString(RHLOGIN, UUID);

		responseString = JsonSanitizer.sanitize(responseString);
		IOpenShiftService service = new NoopOpenShiftServiceFake();
		InternalUser user = new InternalUser(RHLOGIN, PASSWORD, service);
		OpenShiftResponse<IDomain> response = new DomainResponseUnmarshaller(domainName, user, service)
				.unmarshall(responseString);

		assertNotNull(response);
		IDomain domain = response.getOpenShiftObject();
		assertEquals(domainName, domain.getNamespace());
	}

	@Test
	public void canMarshallDomainAlterRequest() throws IOException, OpenShiftException {
		SSHKeyPair sshKey = TestSSHKey.create();
		String expectedRequestString = createDomainRequestString(
				PASSWORD, RHLOGIN, true, "myDomain", true, sshKey.getPublicKey(), sshKey.getKeyType(), false);

		ChangeDomainRequest request = new ChangeDomainRequest("myDomain", sshKey, RHLOGIN, true);
		String requestString =
				new OpenShiftEnvelopeFactory(
						PASSWORD, null, null,
						new DomainRequestJsonMarshaller().marshall(request))
						.createString();
		assertEquals(expectedRequestString, requestString);
	}

	private String createDomainRequestString(String password, String username, boolean debug, String namespace,
			 boolean alter, String sshPublicKey, SSHKeyType sSHKeyType, boolean delete) throws UnsupportedEncodingException {
		return "password="
				+ password
				+ "&json_data=%7B"
				+ "%22rhlogin%22+%3A+"
				+ "%22"
				+ URLEncoder.encode(username, "UTF-8")
				+ "%22"
				+ "%2C+%22debug%22+%3A+%22" + String.valueOf(debug) + "%22"
				+ "%2C+%22namespace%22+%3A+%22" + URLEncoder.encode(namespace, "UTF-8") + "%22"
				+ "%2C+%22alter%22+%3A+%22" + String.valueOf(alter) + "%22"
				+ "%2C+%22ssh%22+%3A+%22"
				+ URLEncoder.encode(sshPublicKey, "UTF-8")
				+ "%22"
				+ "%2C+%22key_type%22+%3A+%22" + URLEncoder.encode(sSHKeyType.getTypeId(), "UTF-8") + "%22%2C"
				+ "+%22delete%22+%3A+%22" + delete + "%22"
				+ "%7D";
	}

	/**
	 * WARNING: the response this method returns matches the actual response
	 * from the openshift service (9-12-2011). It is not valid json since it
	 * quotes the nested json object
	 * <p>
	 * "data": "{\"rhlogin\": ...
	 */
	private String createDomainResponseString(String username, String uuid) {
		return "{\"messages\":\"\",\"debug\":\"\",\"data\":\""
				+ "{\\\"rhlogin\\\":\\\""
				+ username
				+ "\\\",\\\"uuid\\\":\\\""
				+ uuid
				+ "\\\"}"
				+ "\",\"api\":\"1.1.1\",\"api_c\":[\"placeholder\"],\"result\":null,\"broker\":\"1.1.1\",\"broker_c\":[\"namespace\",\"rhlogin\",\"ssh\",\"app_uuid\",\"debug\",\"alter\",\"cartridge\",\"cart_type\",\"action\",\"app_name\",\"api\"],\"exit_code\":0}";
	}
}
