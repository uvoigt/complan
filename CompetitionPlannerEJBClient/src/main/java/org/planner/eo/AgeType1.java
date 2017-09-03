package org.planner.eo;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("3")
public class AgeType1 extends AbstractEnum {

	private static final long serialVersionUID = 1L;
}
