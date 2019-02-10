package org.planner.business;

import javax.ejb.EJBContext;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.mockito.Mockito;
import org.planner.util.CurrentTime;

public class TestResourceFactory {

	@Produces
	@ApplicationScoped
	private EJBContext context;

	@Produces
	@ApplicationScoped
	private CurrentTime currentTime;

	public TestResourceFactory() throws Exception {
		context = Mockito.spy(EJBContext.class);
		currentTime = Mockito.spy(CurrentTime.class);
	}
}
