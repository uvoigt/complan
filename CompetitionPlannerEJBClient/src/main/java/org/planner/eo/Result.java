package org.planner.eo;

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Subselect;
import org.planner.eo.Program.ProgramStatus;
import org.planner.util.NLSBundle;
import org.planner.util.Visible;

@Entity
@Subselect("select * from vresult")
@Access(AccessType.FIELD)
@NLSBundle("results")
public class Result extends HasId implements CanDelete {

	private static final long serialVersionUID = 1L;

	@Visible
	private String aName;

	@Visible
	private String cName;

	@Visible
	@Temporal(TemporalType.DATE)
	private Date startDate;

	@Visible(initial = false)
	@Temporal(TemporalType.DATE)
	private Date endDate;

	@Visible
	private ProgramStatus status;

	@Override
	public void delete(EntityManager em) {
		em.createQuery(
				"delete from Placement where id.programRaceId in (select id from ProgramRace where programId=:programId)")
				.setParameter("programId", getProgramId()).executeUpdate();
	}

	public Long getProgramId() {
		return id;
	}
}
