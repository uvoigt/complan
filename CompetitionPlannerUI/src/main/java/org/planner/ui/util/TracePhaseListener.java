package org.planner.ui.util;

import javax.faces.component.UIComponent;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.inject.Inject;

import org.planner.ui.beans.PageConfig;
import org.slf4j.MDC;

/**
 * Phase-Listener für das Tracing der Rendering-Phase. Dafür wird im Log4J-MDC das Attribut "phase" gesetzt.
 * 
 * @author Uwe Voigt - IBM
 */
/*
 * <lifecycle> <phase-listener>com.talanx.tadeas.konfig.util.TracePhaseListener</phase- listener> </lifecycle>
 */
public class TracePhaseListener implements PhaseListener {
	private static final long serialVersionUID = 1L;

	@Inject
	private PageConfig pageConfig;

	@Override
	public void afterPhase(PhaseEvent event) {
		MDC.remove("phase");
		MDC.remove("user");
	}

	@Override
	public void beforePhase(PhaseEvent event) {
		MDC.put("phase", event.getPhaseId().getName());
		String user = event.getFacesContext().getExternalContext().getRemoteUser();
		if (user != null)
			MDC.put("user", user);

		if (event.getPhaseId() == PhaseId.RENDER_RESPONSE) {
			UIComponent component = event.getFacesContext().getViewRoot().findComponent("body");
			if (component != null)
				component.setTransient(pageConfig.isTransient());
		}
	}

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.ANY_PHASE;
	}
}
