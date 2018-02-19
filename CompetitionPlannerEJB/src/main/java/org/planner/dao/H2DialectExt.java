package org.planner.dao;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StringType;

public class H2DialectExt extends H2Dialect {

	public H2DialectExt() {

		registerFunction("group_concat",
				new SQLFunctionTemplate(StringType.INSTANCE, "group_concat(?1 order by ?1 separator ', ')"));
	}
}