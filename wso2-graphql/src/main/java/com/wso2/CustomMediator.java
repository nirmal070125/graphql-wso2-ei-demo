package com.wso2;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;


public class CustomMediator extends AbstractMediator {
    private GraphQL graphQL;
    private String schemaPath;

    @Override
    public boolean mediate(MessageContext synCtx) {

        // execute the graphql query while passing the message context
        ExecutionResult executionResult = graphQL.execute((String) synCtx.getProperty("query"), synCtx);
        // convert the result of the query to a Json message and set it in the message context
        synCtx.setProperty("result", new Gson().toJson(executionResult.toSpecification()));
        return true;
    }

    public void init() {
        try {
            // load the graphql schema
            URL url = new File(schemaPath).toURI().toURL();
            String sdl = Resources.toString(url, Charsets.UTF_8);
            // build the schema
            GraphQLSchema graphQLSchema = buildSchema(sdl);
            this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
        } catch (IOException e) {
            log.error("Failed to initiate the custom mediator", e);
        }

    }

    private GraphQLSchema buildSchema(String sdl) {
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        RuntimeWiring runtimeWiring = buildWiring();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    private RuntimeWiring buildWiring() {
        // build the wiring with data sources
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("employeeById", getEmployeeById()))
                .type(newTypeWiring("Employee")
                        .dataFetcher("contact", getContactDataFetcher()))
                .build();
    }

    private DataFetcher getEmployeeById() {
        return dataFetchingEnvironment -> {
            try {
                // retrieve the message context from the data fetching environment
                MessageContext ctx = dataFetchingEnvironment.getContext();
                // employeeById(id: ID): Employee --> id is available via the data fetching environment
                String employeeId = dataFetchingEnvironment.getArgument("id");

                // Extract each fields' value by evaluating corresponding Xpaths
                SynapseXPath newXpath = new SynapseXPath(ctx.getEnvelope().getBody(), "//Employee[@id=" + employeeId + "]/name");
                String name = newXpath.stringValueOf(ctx);

                newXpath = new SynapseXPath(ctx.getEnvelope().getBody(), "//Employee[@id=" + employeeId + "]/salary");
                String salary = newXpath.stringValueOf(ctx);

                newXpath = new SynapseXPath(ctx.getEnvelope().getBody(), "//Employee[@id=" + employeeId + "]/contactId");
                String contactId = newXpath.stringValueOf(ctx);

                // populate an immutable key-value map - keys should be same as in the schema and in addition you need
                // to pass the id/reference to find out the remaining objects
                return ImmutableMap.of("name", name, "salary", salary, "contactId", contactId);
            } catch (JaxenException e) {
                log.error("Failed to retrieve employee", e);
            }
            return "";
        };
    }

    private DataFetcher getContactDataFetcher() {
        return dataFetchingEnvironment -> {
            try {
                MessageContext ctx = dataFetchingEnvironment.getContext();
                // retrieving the map inserted by #getEmployeeById
                Map<String, String> employee = dataFetchingEnvironment.getSource();

                String contactId = employee.get("contactId");

                // get contact's email address
                SynapseXPath newXpath = new SynapseXPath(ctx.getEnvelope().getBody(), "//Contact[@id=" + contactId + "]/email");
                String email = newXpath.stringValueOf(ctx);

                return ImmutableMap.of("email", email);
            } catch (JaxenException e) {
                log.error("Failed to retrieve contact", e);
            }
            return "";
        };
    }

    public void setSchemaPath(String newValue) {
        schemaPath = newValue;
        // once the schema is set, initialize the graph ql
        init();
    }

    public String getSchemaPath() {
        return schemaPath;
    }

}
