package org.planner.eo;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("2")
public class City extends AbstractEnum {

	private static final long serialVersionUID = 1L;

}
