package org.planner.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.planner.dao.IOperation;
import org.planner.dao.PlannerDao;
import org.planner.eo.Role;
import org.planner.eo.Role_;

@Startup
@Singleton
public class Init {

	@Inject
	private PlannerDao dao;

	@PostConstruct
	public void initialize() {
		dao.executeOperation(new IOperation<Void>() {
			@Override
			public Void execute(EntityManager em) {
				checkRoles(em);
				return null;
			}
		});
	}

	private void checkRoles(EntityManager em) {
		insertRoleIfNotExists(em, "Admin", "Administrator");
		insertRoleIfNotExists(em, "Sportwart",
				"Sportwart (Kann Ausschreibungen erstellen, Melden und Trainer und Sportler im Verein anlegen und bearbeiten)");
		insertRoleIfNotExists(em, "Trainer", "Trainer (Kann Sportler melden aber keine zusätzlichen Trainer anlegen)");
		insertRoleIfNotExists(em, "Mastersportler", "Mastersportler (Kann sich selbst melden)");
		insertRoleIfNotExists(em, "Sportler", "Sportler (Kann nur Meldungen und Ausschreibungen ansehen)");

	}

	private void insertRoleIfNotExists(EntityManager em, String role, String description) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Role> query = builder.createQuery(Role.class);
		Root<Role> root = query.from(Role.class);
		query.where(builder.equal(root.get(Role_.role), builder.parameter(String.class, "role")));
		if (em.createQuery(query).setParameter("role", role).getResultList().isEmpty()) {
			Role entity = new Role();
			entity.setRole(role);
			entity.setDescription(description);
			dao.save(entity, "init");
		}
	}
}
