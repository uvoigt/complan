package org.planner.eo;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("4")
public class BoatClass1 extends AbstractEnum {

	private static final long serialVersionUID = 1L;
}
