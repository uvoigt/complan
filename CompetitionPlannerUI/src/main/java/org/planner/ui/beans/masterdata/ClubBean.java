package org.planner.ui.beans.masterdata;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.planner.eo.Address;
import org.planner.eo.Address_;
import org.planner.eo.Club;
import org.planner.eo.Club_;
import org.planner.model.FetchInfo;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.util.JsfUtil;

@Named
@RequestScoped
public class ClubBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private Club club;

	@Override
	@PostConstruct
	public void init() {
		super.init();

		Long id = getIdFromRequestParameters();
		if (id == null)
			id = (Long) JsfUtil.getViewVariable("id");
		if (id != null && !isCancelPressed()) {
			club = service.getObject(Club.class, id, getFetchInfo());
			JsfUtil.setViewVariable("id", club.getId());
		} else {
			club = new Club();
		}
		if (club.getAddress() == null)
			club.setAddress(new Address());
	}

	@Override
	public void setItem(Object item) {
		this.club = (Club) item;
	}

	@Override
	public FetchInfo[] getFetchInfo() {
		return new FetchInfo[] { new FetchInfo(Club_.address, true).add(new FetchInfo(Address_.country, true),
				new FetchInfo(Address_.city, true)) };
	}

	public boolean canDelete(@SuppressWarnings("unused") Map<String, String> item) {
		return true;
	}

	public Club getClub() {
		return club;
	}

	@Override
	protected void doSave() {
		service.saveClub(club);
	}
}
