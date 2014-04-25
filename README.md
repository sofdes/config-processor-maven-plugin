**config-processor-maven-plugin**

```
<dependency>
  <groupId>com.ariht</groupId>
  <artifactId>config-processor-maven-plugin</artifactId>
  <version>0.9.2</version>
</dependency>
```

Generates config and scripts for multiple target environments using
template placeholder substitution from values in multiple filter files.

For example the following inputs:

```
/src/main/config/filter/dev.filter
/src/main/config/filter/uat.filter
/src/main/config/filter/prod.filter
/src/main/config/template/ci_deploy.sh
/src/main/config/template/properties/db_connection_properties.sh
/src/main/config/template/properties/webapp_properties.sh
```

outputs:


```
/target/generated-config/dev/ci_deploy.sh
/target/generated-config/dev/properties/db_connection_properties.sh
/target/generated-config/dev/properties/webapp_properties.sh

/target/generated-config/uat/ci_deploy.sh
/target/generated-config/uat/properties/db_connection_properties.sh
/target/generated-config/uat/properties/webapp_properties.sh

/target/generated-config/prod/ci_deploy.sh
/target/generated-config/prod/properties/db_connection_properties.sh
/target/generated-config/prod/properties/webapp_properties.sh
```

