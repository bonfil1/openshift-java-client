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
package com.openshift.express.internal.client.test.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.text.MessageFormat;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;

import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.OpenShiftException;
import com.openshift.express.client.utils.RFC822DateUtils;
import com.openshift.express.internal.client.ApplicationInfo;

/**
 * @author André Dietisheim
 */
public class ApplicationInfoAsserts {

	public static void assertThatContainsApplicationInfo(String applicationName, List<IEmbeddableCartridge> embedded,
			String applicationUUID,
			String cartridgeName, String creationTime, List<ApplicationInfo> applicationInfos)
			throws OpenShiftException {
		ApplicationInfo applicationInfo = getApplicationInfo(applicationName, applicationInfos);
		if (applicationInfo == null) {
			fail(MessageFormat.format("Could not find application with name \"{0}\"", applicationName));
		}
		assertApplicationInfo(embedded, applicationUUID, cartridgeName, creationTime, applicationInfo);
	}

	public static void assertThatContainsApplicationInfo(String applicationName, List<ApplicationInfo> applicationInfos) {
		assertNotNull(getApplicationInfo(applicationName, applicationInfos));
	}

	private static ApplicationInfo getApplicationInfo(String name, List<ApplicationInfo> applicationInfos) {
		ApplicationInfo matchingApplicationInfo = null;
		for (ApplicationInfo applicationInfo : applicationInfos) {
			if (name.equals(applicationInfo.getName())) {
				matchingApplicationInfo = applicationInfo;
				break;
			}
		}
		return matchingApplicationInfo;
	}

	private static void assertApplicationInfo(List<IEmbeddableCartridge> embedded, String uuid, String cartridgeName,
			String creationTime, ApplicationInfo applicationInfo) throws OpenShiftException {
		assertEquals(uuid, applicationInfo.getUuid());
		assertNotNull(applicationInfo.getCartridge());
		assertEquals(cartridgeName, applicationInfo.getCartridge().getName());
		assertEmbeddedCartridges(embedded, applicationInfo);
		try {
			assertEquals(RFC822DateUtils.getDate(creationTime), applicationInfo.getCreationTime());
		} catch (DatatypeConfigurationException e) {
			fail(e.getMessage());
		}
	}

	private static void assertEmbeddedCartridges(List<IEmbeddableCartridge> cartridges, ApplicationInfo applicationInfo) {
		if (cartridges == null) {
			// null in cartridges is equivalent to empty list
			if (applicationInfo.getEmbeddedCartridges() != null
					&& applicationInfo.getEmbeddedCartridges().size() > 0) {
				fail("expected: no embedded cartridges, actual: " + applicationInfo.getEmbeddedCartridges().size()
						+ " cartridges found");
			}
		} else {
			assertEquals(cartridges.size(), applicationInfo.getEmbeddedCartridges().size());
		}
	}
}
