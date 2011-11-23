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
package com.openshift.express.internal.client;

import java.text.MessageFormat;
import java.util.Date;

import com.openshift.express.client.ApplicationLogReader;
import com.openshift.express.client.IApplication;
import com.openshift.express.client.ICartridge;
import com.openshift.express.client.IDomain;
import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.IOpenShiftService;
import com.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim
 */
public class Application extends UserInfoAware implements IApplication {

	private static final String GIT_URI_PATTERN = "ssh://{0}@{1}-{2}.{3}/~/git/{1}.git/";
	private static final String APPLICATION_URL_PATTERN = "https://{0}-{1}.{2}/";

	private String name;
	private ICartridge cartridge;
	private IEmbeddableCartridge embeddedCartridge;
	private IOpenShiftService service;
	private ApplicationLogReader logReader;
	private ApplicationInfo applicationInfo;

	public Application(String name, ICartridge cartridge, InternalUser user, IOpenShiftService service) {
		this(name, cartridge, null, user, service);
	}

	public Application(String name, ICartridge cartridge, ApplicationInfo applicationInfo, InternalUser user,
			IOpenShiftService service) {
		super(user);
		this.name = name;
		this.cartridge = cartridge;
		this.applicationInfo = applicationInfo;
		this.service = service;
	}

	public String getName() {
		return name;
	}

	public String getUUID() throws OpenShiftException {
		return getApplicationInfo().getUuid();
	}

	public ICartridge getCartridge() {
		return cartridge;
	}

	public Date getCreationTime() throws OpenShiftException {
		return getApplicationInfo().getCreationTime();
	}

	public void destroy() throws OpenShiftException {
		service.destroyApplication(name, cartridge, getUser());
	}

	public void start() throws OpenShiftException {
		service.startApplication(name, cartridge, getUser());
	}

	public void restart() throws OpenShiftException {
		service.restartApplication(name, cartridge, getUser());
	}

	public void stop() throws OpenShiftException {
		service.stopApplication(name, cartridge, getUser());
	}

	public ApplicationLogReader getLogReader() throws OpenShiftException {
		if (logReader == null) {
			this.logReader = new ApplicationLogReader(this, getUser(), service);
		}
		return logReader;
	}

	public String getGitUri() throws OpenShiftException {
		IDomain domain = getUser().getDomain();
		if (domain == null) {
			return null;
		}
		return MessageFormat
				.format(GIT_URI_PATTERN, getUUID(), getName(), domain.getNamespace(), domain.getRhcDomain());
	}

	public String getApplicationUrl() throws OpenShiftException {
		IDomain domain = getUser().getDomain();
		if (domain == null) {
			return null;
		}
		return MessageFormat.format(APPLICATION_URL_PATTERN, name, domain.getNamespace(), domain.getRhcDomain());
	}

	public void setEmbbedCartridge(IEmbeddableCartridge embeddedCartridge) {
		this.embeddedCartridge = embeddedCartridge;
	}

	public IEmbeddableCartridge getEmbeddedCartridge() throws OpenShiftException {
		if (embeddedCartridge == null) {
			this.embeddedCartridge = getApplicationInfo().getEmbedded();
		}
		return embeddedCartridge;
	}

	protected IOpenShiftService getService() {
		return service;
	}

	protected ApplicationInfo getApplicationInfo() throws OpenShiftException {
		if (applicationInfo == null) {
			this.applicationInfo = getUserInfo().getApplicationInfoByName(getName());
			if (applicationInfo == null) {
				throw new OpenShiftException("Could not find info for application {0}", getName());
			}
		}
		return applicationInfo;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (object == null)
			return false;
		if (getClass() != object.getClass())
			return false;
		Application other = (Application) object;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
