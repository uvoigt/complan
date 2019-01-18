package org.planner.dao;

import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.BooleanType;
import org.hibernate.type.StringType;

public class MySQL5DialectExt extends MySQL5Dialect {

	public MySQL5DialectExt() {

		registerFunction("to_boolean", new SQLFunctionTemplate(BooleanType.INSTANCE, "cast(?1 as unsigned)"));
		registerFunction("group_concat",
				new SQLFunctionTemplate(StringType.INSTANCE, "group_concat(?1 order by ?1 separator ', ')"));
	}
}
