# graphql-wso2-ei-demo
Build a GraphQL API using WSO2 EI

## Configurations

- Download WSO2 Enterprise Integrator 6.5.0 from wso2.com and extract it (the place is referred as {EI_HOME} hereafter).
- Download the GraphQL client jar `graphql-java-7.0.jar` from https://mvnrepository.com/artifact/com.graphql-java/graphql-java/7.0
- Add the jar file into the `{EI_HOME}/lib`
- Clone this Git repository, navigate to `wso2-graphql` folder and build the custom mediator using `mvn clean install`
- Copy the class mediator jar file from `target/wso2-graphql-1.0.0.jar` to `{EI_HOME}/lib`
- Get the full qualified path to `schema.graphqls` file in the cloned Git repository and update the `EmployeeGraphQLAPI.xml` file's 45th line to reflect the path to the GraphQL schema file.
- Deploy the API to the EI by copying the modified `EmployeeGraphQLAPI.xml` to `{EI_HOME}/repository/deployment/server/synapse-configs/default/api` and start the EI server.

## How To Run
eg: 

`curl -H "Content-Type: application/json" -v https://localhost:8243/employees/graphql -k -d @payload.json`

Create a payload.json file and add following jsons one at a time.

1.
- payload.json
```json
{
  "query": "{ employeeById(id:2) { name salary contact { email } }}"
}
```
- Response

```json
{
    "data": {
        "employeeById": {
            "name": "John",
            "salary": 222000000,
            "contact": {
                "email": "john@wso2.com"
            }
        }
    }
}
```

2.
- payload.json
```json
{
  "query": "{ employeeById(id:2) { name contact { email } }}"
}
```
- Response
```json
{
    "data": {
        "employeeById": {
            "name": "John",
            "contact": {
                "email": "john@wso2.com"
            }
        }
    }
}
```
