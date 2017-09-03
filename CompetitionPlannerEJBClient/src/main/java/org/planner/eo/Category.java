package org.planner.eo;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("6")
public class Category extends AbstractEnum {

	private static final long serialVersionUID = 1L;

}
