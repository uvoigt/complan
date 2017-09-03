package org.planner.business;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.planner.dao.PlannerDao;
import org.planner.eo.AbstractEntity;
import org.planner.eo.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Geschäftsmethoden der Adminkonsole.
 * 
 * @author Uwe Voigt - IBM
 */
@Named
public class AdminKonfigurationGFO {

	private static final Logger LOG = LoggerFactory.getLogger(AdminKonfigurationGFO.class);

	@Inject
	private PlannerDao adminDZO;

	public <T extends AbstractEntity> T getById(Class<T> typ, Long id) {
		return adminDZO.find(typ, id);
	}

	public <T extends AbstractEntity> T getByIdForCopy(Class<T> typ, Long id) {
		T object = adminDZO.find(typ, id);
		if (object != null) {
			object.setId(null);
			try {
				for (PropertyDescriptor pd : Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors()) {
					Object propertyValue = pd.getReadMethod().invoke(object);
					if (propertyValue instanceof AbstractEntity) {
						((AbstractEntity) propertyValue).setId(null);
					} else if (propertyValue instanceof Collection) {
						for (Object o : ((Collection<?>) propertyValue)) {
							if (o instanceof AbstractEntity)
								((AbstractEntity) o).setId(null);
						}
					}
				}
			} catch (Exception e) {
				LOG.error("Fehler beim Lesen der Properties", e);
			}
		}
		return object;
	}

	public Map<String, Properties> leseBenutzerEinstellungen(String angemeldeterBenutzter) {
		List<Properties> userProps = adminDZO.leseBenutzerEinstellungen(angemeldeterBenutzter);
		List<Properties> defaultProps = adminDZO.leseBenutzerEinstellungen(null);
		Map<String, Properties> result = new HashMap<String, Properties>(
				Math.max(userProps.size(), defaultProps.size()));
		for (Properties p : defaultProps) {
			result.put(p.getName(), p);
		}
		// benutzerspezifische Properties überschreiben die Defaults
		for (Properties p : userProps) {
			result.put(p.getName(), p);
		}
		return result;
	}

	public Map<String, Properties> speichernBenutzerEinstellungen(List<Properties> properties,
			String angemeldeterBenutzter) {
		adminDZO.speichernBenutzerEinstellungen(properties, angemeldeterBenutzter);
		return leseBenutzerEinstellungen(angemeldeterBenutzter);
	}

	// public int speichernMitFilter(DatenSuchkriterien kriterien, Map<String,
	// String> werte, String angemeldeterBenutzter) {
	// return 0;//adminDZO.updateXml(kriterien, werte, angemeldeterBenutzter);
	// }
	//
	// public int loeschenMitFilter(Class<? extends AbstractEO> entityType,
	// DatenSuchkriterien kriterien) {
	// return 0;//adminDZO.loeschenMitFilter(entityType, kriterien);
	// }
	//
	// public int kopierenMitFilter(Class<AbstractEO> entityType,
	// DatenSuchkriterien kriterien, String angemeldeterBenutzter) {
	// return 0;//adminDZO.kopierenMitFilter(entityType, kriterien,
	// angemeldeterBenutzter);
	// }
}
