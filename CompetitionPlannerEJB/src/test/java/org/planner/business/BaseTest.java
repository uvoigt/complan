package org.planner.business;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.ejb.EJBContext;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.planner.dao.EntityManagerFactory;
import org.planner.ejb.ResourceFactory;
import org.planner.eo.User;
import org.planner.util.CurrentTime;
import org.planner.util.Messages;

@RunWith(Arquillian.class)
public abstract class BaseTest {

	@Inject
	protected CommonImpl common;

	@Inject
	protected Messages messages;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Deployment
	public static JavaArchive getDeployment() {
		return ShrinkWrap.create(JavaArchive.class, "test.jar").addPackages(true, "org.planner")
				.deleteClasses(EntityManagerFactory.class, ResourceFactory.class, CurrentTime.class)
				.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	@AfterClass
	public static void close() {
		TestEntityManagerFactory.shutdown();
	}

	private String testUser = "Tester";

	private List<String> testRoles = new ArrayList<>();

	private Date now;

	@Inject
	private EJBContext context;

	@Inject
	private CurrentTime currentTime;

	@Inject
	private EntityManager em;

	protected EntityManager getEm() {
		return em;
	}

	protected void clear() {
		em.clear();
	}

	protected void flushAndClear() {
		em.flush();
		em.clear();
	}

	protected void setupVariables() throws Exception {
		Principal principal = Mockito.spy(Principal.class);
		Mockito.when(principal.getName()).then(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				return testUser;
			}
		});
		Mockito.when(context.getCallerPrincipal()).thenReturn(principal);
		Mockito.when(context.isCallerInRole(Mockito.anyString())).then(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				for (Object a : invocation.getArguments()) {
					if (testRoles.contains(a))
						return true;
				}
				return false;
			}
		});
		Mockito.when(currentTime.now()).then(new Answer<Date>() {
			@Override
			public Date answer(InvocationOnMock invocation) throws Throwable {
				return now;
			}
		});
	}

	protected void createTestUser() {
		TestUtils.getUserByUserId(em, testUser, testUser + "-club");
	}

	protected User getCallingUser() {
		return common.getCallingUser();
	}

	protected void beginTransaction() {
		em.getTransaction().begin();
	}

	protected void commitTransaction() {
		em.getTransaction().commit();
	}

	@Before
	public void before() throws Exception {
		setupVariables();
		createTestUser();

		beginTransaction();
	}

	@After
	public void after() {
		commitTransaction();
		em.clear();
	}

	public void setTestUser(String testUser) {
		this.testUser = testUser;
	}

	public void setTestRoles(String... testRoles) {
		this.testRoles.addAll(Arrays.asList(testRoles));
	}

	public void setNow(Date now) {
		this.now = now;
	}
}
