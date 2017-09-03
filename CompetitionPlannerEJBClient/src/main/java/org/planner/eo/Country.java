package org.planner.eo;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("1")
public class Country extends AbstractEnum {

	private static final long serialVersionUID = 1L;

}
