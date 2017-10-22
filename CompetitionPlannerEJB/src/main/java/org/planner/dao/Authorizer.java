package org.planner.dao;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.planner.eo.Announcement.AnnouncementStatus;
import org.planner.eo.Announcement_;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.eo.Registration_;
import org.planner.eo.User;
import org.planner.eo.User_;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class Authorizer extends QueryModifier {
	public static class UserAuthorizer extends Authorizer {

		public UserAuthorizer(User caller) {
			super(caller);
		}

		@Override
		public Predicate createPredicate(Root root, CriteriaBuilder builder) {
			return builder.equal(root.get(User_.club), nextParam(builder, caller.getClub()));
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
		public Predicate createPredicate(Root root, CriteriaBuilder builder) {
			Predicate club = builder.equal(root.get(Announcement_.club), nextParam(builder, caller.getClub()));
			Predicate status = builder.equal(root.get(Announcement_.status),
					nextParam(builder, AnnouncementStatus.announced));
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
		public Predicate createPredicate(Root root, CriteriaBuilder builder) {
			Predicate registrationClub = builder.equal(root.get(Registration_.club),
					nextParam(builder, caller.getClub()));
			Predicate announcementClub = builder.equal(root.get(Registration_.announcement).get(Announcement_.club),
					nextParam(builder, caller.getClub()));
			Predicate registrationStatus = builder.equal(root.get(Registration_.status),
					nextParam(builder, RegistrationStatus.submitted));
			return builder.or(registrationClub, builder.and(announcementClub, registrationStatus));
		}
	}

	protected User caller;

	protected Authorizer(User caller) {
		this.caller = caller;
	}
}
