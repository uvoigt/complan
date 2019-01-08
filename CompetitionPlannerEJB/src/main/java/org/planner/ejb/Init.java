package org.planner.ejb;

import java.util.Arrays;
import java.util.List;

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
		Role create_announcements = insertRoleIfNotExists(em, "create_announcements", null, true);
		Role read_announcements = insertRoleIfNotExists(em, "read_announcements", null, true);
		Role update_announcements = insertRoleIfNotExists(em, "update_announcements", null, true);
		Role delete_announcements = insertRoleIfNotExists(em, "delete_announcements", null, true);

		Role create_registrations = insertRoleIfNotExists(em, "create_registrations", null, true);
		Role read_registrations = insertRoleIfNotExists(em, "read_registrations", null, true);
		Role update_registrations = insertRoleIfNotExists(em, "update_registrations", null, true);
		Role delete_registrations = insertRoleIfNotExists(em, "delete_registrations", null, true);

		Role create_programs = insertRoleIfNotExists(em, "create_programs", null, true);
		Role read_programs = insertRoleIfNotExists(em, "read_programs", null, true);
		Role update_programs = insertRoleIfNotExists(em, "update_programs", null, true);
		Role delete_programs = insertRoleIfNotExists(em, "delete_programs", null, true);

		Role create_results = insertRoleIfNotExists(em, "create_results", null, true);
		Role read_results = insertRoleIfNotExists(em, "read_results", null, true);
		Role update_results = insertRoleIfNotExists(em, "update_results", null, true);
		Role delete_results = insertRoleIfNotExists(em, "delete_results", null, true);

		Role create_users = insertRoleIfNotExists(em, "create_users", null, true);
		Role read_users = insertRoleIfNotExists(em, "read_users", null, true);
		Role update_users = insertRoleIfNotExists(em, "update_users", null, true);
		Role delete_users = insertRoleIfNotExists(em, "delete_users", null, true);

		Role create_clubs = insertRoleIfNotExists(em, "create_clubs", null, true);
		Role read_clubs = insertRoleIfNotExists(em, "read_clubs", null, true);
		Role update_clubs = insertRoleIfNotExists(em, "update_clubs", null, true);
		Role delete_clubs = insertRoleIfNotExists(em, "delete_clubs", null, true);

		Role create_roles = insertRoleIfNotExists(em, "create_roles", null, true);
		Role read_roles = insertRoleIfNotExists(em, "read_roles", null, true);
		Role update_roles = insertRoleIfNotExists(em, "update_roles", null, true);
		Role delete_roles = insertRoleIfNotExists(em, "delete_roles", null, true);

		insertRoleIfNotExists(em, "Admin", "Administrator", false, create_announcements, read_announcements,
				update_announcements, delete_announcements, create_registrations, read_registrations,
				update_registrations, delete_registrations, create_programs, read_programs, update_programs,
				delete_programs, create_results, read_results, update_results, delete_results, create_users, read_users,
				update_users, delete_users, create_clubs, read_clubs, update_clubs, delete_clubs, create_roles,
				read_roles, update_roles, delete_roles);
		// "update_clubes" für Anzeige des Save-Buttons auf "Mein Verein"
		insertRoleIfNotExists(em, "Sportwart",
				"Sportwart (Kann Ausschreibungen erstellen, Melden und Trainer und Sportler im Verein anlegen und bearbeiten)",
				false, create_announcements, read_announcements, update_announcements, delete_announcements,
				create_registrations, read_registrations, update_registrations, delete_registrations, create_programs,
				read_programs, update_programs, delete_programs, create_results, read_results, update_results,
				delete_results, create_users, read_users, update_users, delete_users, update_clubs);
		insertRoleIfNotExists(em, "Trainer", "Trainer (Kann Sportler melden aber keine zusätzlichen Trainer anlegen)",
				false, read_announcements, create_registrations, read_registrations, update_registrations,
				delete_registrations, read_programs, read_results);
		insertRoleIfNotExists(em, "Mastersportler", "Mastersportler (Kann sich selbst melden)", false,
				read_announcements, read_registrations, update_registrations, read_programs, read_results);
		insertRoleIfNotExists(em, "Sportler", "Sportler (Kann nur Meldungen und Ausschreibungen ansehen)", false,
				read_announcements, read_registrations, read_programs, read_results);
		insertRoleIfNotExists(em, "Tester",
				"Tester (Kann den Status von Ausschreibungen, Meldungen und Programmen zurücksetzen)", false);
		createRoleView(em);
		createResultView(em);
	}

	private Role insertRoleIfNotExists(EntityManager em, String role, String description, boolean internal,
			Role... roles) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Role> query = builder.createQuery(Role.class);
		Root<Role> root = query.from(Role.class);
		query.where(builder.equal(root.get(Role_.role), builder.parameter(String.class, "role")));
		List<Role> result = em.createQuery(query).setParameter("role", role).getResultList();
		Role entity = !result.isEmpty() ? result.get(0) : null;
		if (entity != null) {
			boolean modified = false;
			for (Role r : roles) {
				if (!entity.getRoles().contains(r)) {
					entity.getRoles().add(r);
					modified = true;
				}
			}
			if (modified)
				dao.save(entity, "init");
		} else {
			entity = new Role();
			entity.setRole(role);
			entity.setDescription(description);
			entity.getRoles().addAll(Arrays.asList(roles));
			entity.setInternal(internal);
			dao.save(entity, "init");
		}
		return entity;
	}

	private void createRoleView(EntityManager em) {
		em.createNativeQuery("create or replace view vrole as " //
				+ "select userid, r.role " //
				+ "from User u " //
				+ "left join User_Role ur on ur.user_id=u.id " //
				+ "left join Role r on ur.role_id=r.id " //
				+ "union all " //
				+ "select userid, r2.role " //
				+ "from User u " //
				+ "left join User_Role ur on ur.user_id=u.id " //
				+ "left join Role r on ur.role_id=r.id " //
				+ "inner join Role_Role rr on rr.role_id=r.id " //
				+ "left join Role r2 on r2.id=rr.roles_id ").executeUpdate();
	}

	private void createResultView(EntityManager em) {
		em.createNativeQuery("create or replace view vresult as " //
				+ "select distinct p.id, a.name aName, c.name cName, a.startDate, a.endDate, p.status " //
				+ "from Program p " //
				+ "left join Announcement a on p.announcement_id=a.id " //
				+ "left join Club c on a.club_id=c.id " //
				+ "where exists(" //
				+ "select pl.position from Placement pl left join ProgramRace  pr on pl.programrace_id=pr.id " //
				+ "where pr.program_id=p.id)").executeUpdate();
	}
}
