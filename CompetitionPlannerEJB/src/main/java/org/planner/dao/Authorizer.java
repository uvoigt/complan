package org.planner.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang.RandomStringUtils;
import org.planner.eo.Announcement.AnnouncementStatus;
import org.planner.eo.Announcement_;
import org.planner.eo.Club;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.eo.Registration_;
import org.planner.eo.User;
import org.planner.eo.User_;
import org.planner.model.Suchkriterien.Filter;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class Authorizer {
	public static class UserAuthorizer extends Authorizer {

		public UserAuthorizer(User caller) {
			super(caller);
		}

		@Override
		Predicate createPredicate(Root root, CriteriaBuilder builder) {
			return builder.equal(root.get(User_.club), builder.parameter(Club.class, nextParam(caller.getClub())));
		}
	}

	// entweder du bist Vereinsmitglied des Erstellers der Ausschreibung
	// oder du bist kein Vereinsmitglied (Verein egal) und
	// die Ausschreibung ist im Status "announced"
	public static class AnnouncementAuthorizer extends Authorizer {

		public AnnouncementAuthorizer(User caller) {
			super(caller);
		}

		@Override
		Predicate createPredicate(Root root, CriteriaBuilder builder) {
			Predicate club = builder.equal(root.get(Announcement_.club),
					builder.parameter(Club.class, nextParam(caller.getClub())));
			Predicate status = builder.equal(root.get(Announcement_.status),
					builder.parameter(AnnouncementStatus.class, nextParam(AnnouncementStatus.announced)));
			return builder.or(club, status);
		}
	}

	// entweder du bist Vereinsmitglied des Erstellers der Meldung
	// oder du bist Vereinsmitglied des Erstellers der Ausschreibung und
	// die Meldung ist im Status "submitted"
	public static class RegistrationAuthorizer extends Authorizer {

		public RegistrationAuthorizer(User caller) {
			super(caller);
		}

		@Override
		Predicate createPredicate(Root root, CriteriaBuilder builder) {
			Predicate registrationClub = builder.equal(root.get(Registration_.club),
					builder.parameter(Club.class, nextParam(caller.getClub())));
			Predicate announcementClub = builder.equal(root.get(Registration_.announcement).get(Announcement_.club),
					builder.parameter(Club.class, nextParam(caller.getClub())));
			Predicate registrationStatus = builder.equal(root.get(Registration_.status),
					builder.parameter(RegistrationStatus.class, nextParam(RegistrationStatus.submitted)));
			return builder.or(registrationClub, builder.and(announcementClub, registrationStatus));
		}
	}

	protected User caller;
	private List<Filter> params;

	protected Authorizer(User caller) {
		this.caller = caller;
	}

	protected String nextParam(Object value) {
		if (params == null)
			params = new ArrayList<>();
		String param = RandomStringUtils.randomAlphabetic(5);
		params.add(new Filter(param, value));
		return param;
	}

	abstract Predicate createPredicate(Root root, CriteriaBuilder builder);

	void setParameters(Query query) {
		if (params != null) {
			for (Filter param : params) {
				query.setParameter(param.getName(), param.getValue());
			}
			params.clear();
		}
	}
}
