@echo off
"C:\Program Files\Java\jdk1.8.0_121\bin\javaw" -cp "C:\Program Files\wildfly-10.1.0.Final\modules\system\layers\base\com\h2database\h2\main\h2-1.3.173.jar";C:\Users\IBM_ADMIN\workspaces\mars\TestMe\hsqldb-2.3.3.jar org.hsqldb.util.DatabaseManagerSwing --driver org.h2.Driver --url jdbc:h2:C:\Users\IBM_ADMIN\db\plannerdb --user sa --password sa
