/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package com.openshift.express.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;

import com.openshift.express.internal.client.InternalUser;
import com.openshift.express.internal.client.UserInfo;
import com.openshift.express.internal.client.httpclient.HttpClientException;
import com.openshift.express.internal.client.httpclient.NotFoundException;
import com.openshift.express.internal.client.httpclient.UnauthorizedException;
import com.openshift.express.internal.client.httpclient.UrlConnectionHttpClient;
import com.openshift.express.internal.client.request.AbstractDomainRequest;
import com.openshift.express.internal.client.request.ApplicationAction;
import com.openshift.express.internal.client.request.ApplicationRequest;
import com.openshift.express.internal.client.request.ChangeDomainRequest;
import com.openshift.express.internal.client.request.CreateDomainRequest;
import com.openshift.express.internal.client.request.EmbedAction;
import com.openshift.express.internal.client.request.EmbedRequest;
import com.openshift.express.internal.client.request.ListCartridgesRequest;
import com.openshift.express.internal.client.request.OpenShiftEnvelopeFactory;
import com.openshift.express.internal.client.request.UserInfoRequest;
import com.openshift.express.internal.client.request.marshalling.ApplicationRequestJsonMarshaller;
import com.openshift.express.internal.client.request.marshalling.DomainRequestJsonMarshaller;
import com.openshift.express.internal.client.request.marshalling.EmbedRequestJsonMarshaller;
import com.openshift.express.internal.client.request.marshalling.ListCartridgesRequestJsonMarshaller;
import com.openshift.express.internal.client.request.marshalling.UserInfoRequestJsonMarshaller;
import com.openshift.express.internal.client.response.OpenShiftResponse;
import com.openshift.express.internal.client.response.unmarshalling.ApplicationResponseUnmarshaller;
import com.openshift.express.internal.client.response.unmarshalling.ApplicationStatusResponseUnmarshaller;
import com.openshift.express.internal.client.response.unmarshalling.DomainResponseUnmarshaller;
import com.openshift.express.internal.client.response.unmarshalling.EmbedResponseUnmarshaller;
import com.openshift.express.internal.client.response.unmarshalling.JsonSanitizer;
import com.openshift.express.internal.client.response.unmarshalling.ListCartridgesResponseUnmarshaller;
import com.openshift.express.internal.client.response.unmarshalling.ListEmbeddableCartridgesResponseUnmarshaller;
import com.openshift.express.internal.client.response.unmarshalling.UserInfoResponseUnmarshaller;

/**
 * @author André Dietisheim
 */
public class OpenShiftService implements IOpenShiftService {

	// TODO extract to properties file
	private static final String USERAGENT_FORMAT = "Java OpenShift/{0} ({1})";

	private String baseUrl;
	private String id;

	public OpenShiftService(String id, String baseUrl) {
		this.id = id;
		this.baseUrl = baseUrl;
	}

	@Override
	public String getServiceUrl() {
		return baseUrl + SERVICE_PATH;
	}

	@Override
	public String getPlatformUrl() {
		return baseUrl;
	}

	@Override
	public boolean isValid(InternalUser user) throws OpenShiftException {
		return getUserInfo(user) != null;
	}

	@Override
	public UserInfo getUserInfo(InternalUser user) throws OpenShiftException {
		UserInfoRequest request = new UserInfoRequest(user.getRhlogin(), true);
		String url = request.getUrlString(getServiceUrl());
		try {
			String requestString = new UserInfoRequestJsonMarshaller().marshall(request);
			String openShiftRequestString = new OpenShiftEnvelopeFactory(user.getPassword(), requestString)
					.createString();
			String responseString = createHttpClient(id, url).post(openShiftRequestString);
			responseString = JsonSanitizer.sanitize(responseString);
			OpenShiftResponse<UserInfo> response =
					new UserInfoResponseUnmarshaller().unmarshall(responseString);
			return response.getOpenShiftObject();
		} catch (MalformedURLException e) {
			throw new OpenShiftEndpointException(
					url, e, "Could not get user info for user \"{0}\" at \"{1}\"", user.getRhlogin(), url);
		} catch (UnauthorizedException e) {
			throw new InvalidCredentialsOpenShiftException(url, e);
		} catch (NotFoundException e) {
			throw new NotFoundOpenShiftException(url, e);
		} catch (HttpClientException e) {
			throw new OpenShiftEndpointException(
					url, e, "Could not get user info for user \"{0}\" at \"{1}\"", user.getRhlogin(), url);
		}
	}

	@Override
	public List<IEmbeddableCartridge> getEmbeddableCartridges(InternalUser user) throws OpenShiftException {
		ListCartridgesRequest listCartridgesRequest = new ListCartridgesRequest(
				ListCartridgesRequest.CartridgeType.EMBEDDED, user.getRhlogin(), true);
		String url = listCartridgesRequest.getUrlString(getServiceUrl());
		try {
			String listCartridgesRequestString =
					new ListCartridgesRequestJsonMarshaller().marshall(listCartridgesRequest);
			String request =
					new OpenShiftEnvelopeFactory(user.getPassword(), listCartridgesRequestString).createString();
			String listCatridgesReponse = createHttpClient(id, url).post(request);
			listCatridgesReponse = JsonSanitizer.sanitize(listCatridgesReponse);
			OpenShiftResponse<List<IEmbeddableCartridge>> response =
					new ListEmbeddableCartridgesResponseUnmarshaller().unmarshall(listCatridgesReponse);
			return response.getOpenShiftObject();
			/**
			 * always allowed to list cartridges, even with invalid credentials
			 */
		} catch (MalformedURLException e) {
			throw new OpenShiftEndpointException(url, e, "Could not list available embeddable cartridges at \"{0}\"",
					url);
		} catch (NotFoundException e) {
			throw new NotFoundOpenShiftException(url, e);
		} catch (HttpClientException e) {
			throw new OpenShiftEndpointException(url, e, "Could not list available embeddable cartridges at \"{0}\"",
					url);
		}
	}

	@Override
	public List<ICartridge> getCartridges(InternalUser user) throws OpenShiftException {
		ListCartridgesRequest listCartridgesRequest = new ListCartridgesRequest(
				ListCartridgesRequest.CartridgeType.STANDALONE, user.getRhlogin(), true);
		String url = listCartridgesRequest.getUrlString(getServiceUrl());
		try {
			String listCartridgesRequestString =
					new ListCartridgesRequestJsonMarshaller().marshall(listCartridgesRequest);
			String request =
					new OpenShiftEnvelopeFactory(user.getPassword(), listCartridgesRequestString).createString();
			String listCatridgesReponse = createHttpClient(id, url).post(request);
			listCatridgesReponse = JsonSanitizer.sanitize(listCatridgesReponse);
			OpenShiftResponse<List<ICartridge>> response =
					new ListCartridgesResponseUnmarshaller().unmarshall(listCatridgesReponse);
			return response.getOpenShiftObject();
			/**
			 * always allowed to list cartridges, even with invalid credentials
			 */
		} catch (MalformedURLException e) {
			throw new OpenShiftEndpointException(url, e, "Could not list available cartridges at \"{0}\"", url);
		} catch (NotFoundException e) {
			throw new NotFoundOpenShiftException(url, e);
		} catch (HttpClientException e) {
			throw new OpenShiftEndpointException(url, e, "Could not list available cartridges at \"{0}\"", url);
		}
	}

	@Override
	public IDomain createDomain(String name, ISSHPublicKey sshKey, InternalUser user) throws OpenShiftException {
		return requestDomainAction(new CreateDomainRequest(name, sshKey, user.getRhlogin(), true), user);
	}

	@Override
	public IDomain changeDomain(String newName, ISSHPublicKey sshKey, InternalUser user) throws OpenShiftException {
		return requestDomainAction(new ChangeDomainRequest(newName, sshKey, user.getRhlogin(), true), user);
	}

	protected IDomain requestDomainAction(AbstractDomainRequest request, InternalUser user) throws OpenShiftException {
		String url = request.getUrlString(getServiceUrl());
		try {
			String requestString =
					new OpenShiftEnvelopeFactory(
							user.getPassword(),
							new DomainRequestJsonMarshaller().marshall(request))
							.createString();
			String responseString = createHttpClient(id, url).post(requestString);
			responseString = JsonSanitizer.sanitize(responseString);
			OpenShiftResponse<IDomain> response =
					new DomainResponseUnmarshaller(request.getName(), user, this).unmarshall(responseString);
			return response.getOpenShiftObject();
		} catch (MalformedURLException e) {
			throw new OpenShiftEndpointException(url, e, "Could reach openshift platform at \"{0}\"", url);
		} catch (UnauthorizedException e) {
			throw new InvalidCredentialsOpenShiftException(url, e);
		} catch (NotFoundException e) {
			throw new NotFoundOpenShiftException(url, e);
		} catch (HttpClientException e) {
			throw new OpenShiftEndpointException(url, e, "Could not {0}", request.toHumanReadable());
		}
	}

	@Override
	public IApplication createApplication(String name, ICartridge cartridge, InternalUser user)
			throws OpenShiftException {
		IApplication application = requestApplicationAction(new ApplicationRequest(name, cartridge,
				ApplicationAction.CONFIGURE,
				user.getRhlogin(), true), user);
		return application;
	}

	@Override
	public void destroyApplication(String name, ICartridge cartridge, InternalUser user) throws OpenShiftException {
		IApplication application = requestApplicationAction(new ApplicationRequest(name, cartridge,
				ApplicationAction.DECONFIGURE,
				user.getRhlogin(), true), user);
		user.remove(application);
	}

	@Override
	public IApplication startApplication(String name, ICartridge cartridge, InternalUser user)
			throws OpenShiftException {
		return requestApplicationAction(new ApplicationRequest(name, cartridge, ApplicationAction.START,
				user.getRhlogin(), true), user);
	}

	@Override
	public IApplication restartApplication(String name, ICartridge cartridge, InternalUser user)
			throws OpenShiftException {
		return requestApplicationAction(new ApplicationRequest(name, cartridge, ApplicationAction.RESTART,
				user.getRhlogin(), true), user);
	}

	@Override
	public IApplication stopApplication(String name, ICartridge cartridge, InternalUser user) throws OpenShiftException {
		return requestApplicationAction(new ApplicationRequest(name, cartridge, ApplicationAction.STOP,
				user.getRhlogin(), true), user);
	}

	@Override
	public String getStatus(String applicationName, ICartridge cartridge, InternalUser user) throws OpenShiftException {
		ApplicationRequest applicationRequest =
				new ApplicationRequest(applicationName, cartridge, ApplicationAction.STATUS, user.getRhlogin(), true);
		String url = applicationRequest.getUrlString(getServiceUrl());
		try {
			String applicationRequestString =
					new ApplicationRequestJsonMarshaller().marshall(applicationRequest);
			String request = new OpenShiftEnvelopeFactory(user.getPassword(), applicationRequestString).createString();
			String response = createHttpClient(id, url).post(request);

			response = JsonSanitizer.sanitize(response);
			OpenShiftResponse<String> openshiftResponse =
					new ApplicationStatusResponseUnmarshaller().unmarshall(response);
			return openshiftResponse.getOpenShiftObject();
		} catch (MalformedURLException e) {
			throw new OpenShiftException(
					e, "Could not {0} application \"{1}\" at \"{2}\": Invalid url \"{2}\"",
					applicationRequest.getAction().getCommand(), applicationRequest.getName(), url);
		} catch (UnauthorizedException e) {
			throw new InvalidCredentialsOpenShiftException(url, e);
		} catch (NotFoundException e) {
			throw new NotFoundOpenShiftException(url, e);
		} catch (HttpClientException e) {
			throw new OpenShiftEndpointException(
					url, e, "Could not {0} application \"{1}\" at \"{2}\"",
					applicationRequest.getAction().getCommand(), applicationRequest.getName(), url);
		}
	}

	protected IApplication requestApplicationAction(ApplicationRequest applicationRequest, InternalUser user)
			throws OpenShiftException {
		String url = applicationRequest.getUrlString(getServiceUrl());
		try {
			String applicationRequestString =
					new ApplicationRequestJsonMarshaller().marshall(applicationRequest);
			String request = new OpenShiftEnvelopeFactory(user.getPassword(), applicationRequestString).createString();
			String response = createHttpClient(id, url).post(request);

			response = JsonSanitizer.sanitize(response);
			OpenShiftResponse<IApplication> openshiftResponse =
					new ApplicationResponseUnmarshaller(applicationRequest.getName(),
							applicationRequest.getCartridge(), user, this).unmarshall(response);
			return openshiftResponse.getOpenShiftObject();
		} catch (MalformedURLException e) {
			throw new OpenShiftException(
					e, "Could not {0} application \"{1}\" at \"{2}\": Invalid url \"{2}\"",
					applicationRequest.getAction().getCommand(), applicationRequest.getName(), url);
		} catch (UnauthorizedException e) {
			throw new InvalidCredentialsOpenShiftException(url, e);
		} catch (NotFoundException e) {
			throw new NotFoundOpenShiftException(url, e);
		} catch (HttpClientException e) {
			throw new OpenShiftEndpointException(
					url, e, "Could not {0} application \"{1}\" at \"{2}\"",
					applicationRequest.getAction().getCommand(), applicationRequest.getName(), url);
		}
	}

	@Override
	public IApplication addEmbeddableCartridge(IApplication application, IEmbeddableCartridge cartridge,
			InternalUser user)
			throws OpenShiftException {
		return requestEmbedAction(
				new EmbedRequest(application.getName(), cartridge, EmbedAction.ADD, user.getRhlogin(), true)
				, application
				, user);
	}

	protected IApplication requestEmbedAction(EmbedRequest embedRequest, IApplication application, InternalUser user)
			throws OpenShiftException {
		String url = embedRequest.getUrlString(getServiceUrl());
		try {
			String embedRequestString = new EmbedRequestJsonMarshaller().marshall(embedRequest);
			String request = new OpenShiftEnvelopeFactory(user.getPassword(), embedRequestString).createString();

			String response = createHttpClient(id, url).post(request);
			response = JsonSanitizer.sanitize(response);
			OpenShiftResponse<IApplication> openshiftResponse =
					new EmbedResponseUnmarshaller(embedRequest.getEmbeddableCartridge(), application)
							.unmarshall(response);
			return openshiftResponse.getOpenShiftObject();
		} catch (MalformedURLException e) {
			throw new OpenShiftException(
					e, "Could not {0} application \"{1}\" at \"{2}\": Invalid url \"{2}\"",
					embedRequest.getAction().getCommand(), embedRequest.getName(), url);
		} catch (UnauthorizedException e) {
			throw new InvalidCredentialsOpenShiftException(url, e);
		} catch (NotFoundException e) {
			throw new NotFoundOpenShiftException(url, e);
		} catch (HttpClientException e) {
			throw new OpenShiftEndpointException(
					url, e, "Could not {0} application \"{1}\" at \"{2}\"",
					embedRequest.getAction().getCommand(), embedRequest.getName(), url);
		}
	}

	private IHttpClient createHttpClient(String id, String url) throws MalformedURLException {
		String userAgent = MessageFormat.format(USERAGENT_FORMAT, VERSION, id);
		return new UrlConnectionHttpClient(userAgent, new URL(url));
	}
}
