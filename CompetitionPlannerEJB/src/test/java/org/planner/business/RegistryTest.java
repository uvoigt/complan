package org.planner.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.jboss.arquillian.junit.InSequence;
import org.junit.Test;
import org.planner.eo.User;

public class RegistryTest extends BaseTest {

	@Inject
	private RegistryImpl registry;

	private static String token;

	@Test
	@InSequence(1)
	public void rememberMe() {
		token = registry.rememberMe(token);

		getEm().flush();
		getEm().clear();
		User user = getCallingUser();
		assertEquals(1, user.getTokens().size());
	}

	@Test
	@InSequence(2)
	public void authenticate() {
		User user = registry.authenticate(token, false);
		assertNotNull(user);
	}
}
