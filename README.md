**config-processor-maven-plugin**

Template placeholder substitution from values in multiple filter file, generating config for each target environments.


For example the following inputs:

```
/src/main/config/filter/dev.filter
/src/main/config/filter/uat.filter
/src/main/config/filter/prod.filter
/src/main/config/template/deploy/ci_deploy.sh
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

