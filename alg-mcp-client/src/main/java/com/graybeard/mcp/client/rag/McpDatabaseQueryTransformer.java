package com.graybeard.mcp.client.rag;

import dev.langchain4j.Experimental;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpPromptMessage;
import dev.langchain4j.mcp.client.McpTextContent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.DefaultQueryTransformer;
import dev.langchain4j.rag.query.transformer.ExpandingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;

import java.util.*;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureGreaterThanZero;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * A {@link QueryTransformer} that utilizes a {@link ChatLanguageModel} to expand a given {@link Query}.
 * <br>
 * Refer to {@link #DEFAULT_PROMPT_TEMPLATE} and implementation for more details.
 * <br>
 * <br>
 * Configurable parameters (optional):
 * <br>
 * - {@link #promptTemplate}: The prompt template used to instruct the LLM to expand the provided {@link Query}.
 * <br>
 * - {@link #n}: The number of {@link Query}s to generate. Default value is 3.
 *
 * @see DefaultQueryTransformer
 * @see CompressingQueryTransformer
 * @see ExpandingQueryTransformer
 */
@Experimental
public class McpDatabaseQueryTransformer implements QueryTransformer {

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from(
            "You are an expert in writing SQL queries.\n" +
                    "You have access to a {{sqlDialect}} database with the following structure:\n" +
                    "{{databaseStructure}}\n" +
                    "If a user asks a question that can be answered by querying this database, generate an SQL SELECT query.\n" +
                    "Do not output anything else aside from a valid SQL statement!"
    );

    private final PromptTemplate promptTemplate;
    private final ChatLanguageModel chatLanguageModel;
    private final McpClient mcpClient;
    private final String sqlDialect;
    private final String databaseStructure;

    private final int maxRetries;
    public static final int DEFAULT_N = 1;

    public McpDatabaseQueryTransformer(ChatLanguageModel chatLanguageModel, McpClient mcpClient) {
        this(chatLanguageModel, mcpClient, DEFAULT_PROMPT_TEMPLATE, DEFAULT_N);
    }

    public McpDatabaseQueryTransformer(ChatLanguageModel chatLanguageModel, McpClient mcpClient, int n) {
        this(chatLanguageModel, mcpClient, DEFAULT_PROMPT_TEMPLATE, n);
    }

    public McpDatabaseQueryTransformer(ChatLanguageModel chatLanguageModel, McpClient mcpClientDatabase, PromptTemplate promptTemplate) {
        this(chatLanguageModel, mcpClientDatabase, ensureNotNull(promptTemplate, "promptTemplate"), DEFAULT_N);
    }

    public McpDatabaseQueryTransformer(ChatLanguageModel chatLanguageModel, McpClient mcpClientDatabase, PromptTemplate promptTemplate, Integer n) {
        this.chatLanguageModel = ensureNotNull(chatLanguageModel, "chatLanguageModel");
        this.mcpClient = ensureNotNull(mcpClientDatabase, "mcpClientDatabase");
        this.sqlDialect = getSqlDialect(mcpClientDatabase);
        this.databaseStructure = generateDDL(mcpClientDatabase);
        this.promptTemplate = getOrDefault(promptTemplate, DEFAULT_PROMPT_TEMPLATE);
        this.maxRetries = ensureGreaterThanZero(getOrDefault(n, DEFAULT_N), "n");
    }

    // TODO (for v2)
    // - provide a few rows of data for each table in the prompt
    // - option to select a list of tables to use/ignore

    public static String getSqlDialect(McpClient mcpClientDatabase) {
        McpGetPromptResult prompt = mcpClientDatabase.getPrompt("DatabaseService_getSqlDialect", Map.of());
        McpPromptMessage message = prompt.messages().get(0);
        return ((McpTextContent) message.content()).text();
    }

    private static String generateDDL(McpClient mcpClientDatabase) {
        McpGetPromptResult prompt = mcpClientDatabase.getPrompt("DatabaseService_generateDDL", Map.of());
        McpPromptMessage message = prompt.messages().get(0);
        return ((McpTextContent) message.content()).text();
    }

    public static McpSqlDatabaseQueryTransformerBuilder builder() {
        return new McpSqlDatabaseQueryTransformerBuilder();
    }

    @Override
    public Collection<Query> transform(Query query) {
        String sqlQuery = null;
        String errorMessage = null;

        List<String> queries = new ArrayList<>();
        int attemptsLeft = maxRetries + 1;
        while (attemptsLeft > 0) {
            attemptsLeft--;

            sqlQuery = generateSqlQuery(query, sqlQuery, errorMessage);

            sqlQuery = clean(sqlQuery);

            try {
                validate(sqlQuery);
                String content = format(sqlQuery, getSqlType(sqlQuery));
                queries.add(content);
            } catch (Exception e) {
                errorMessage = e.getMessage();
            }
        }
        return queries.stream()
                .map(queryText -> query.metadata() == null
                        ? Query.from(queryText)
                        : Query.from(queryText, query.metadata()))
                .collect(toList());
    }

    protected String generateSqlQuery(Query naturalLanguageQuery, String previousSqlQuery, String previousErrorMessage) {

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createSystemPrompt().toSystemMessage());
        messages.add(UserMessage.from(naturalLanguageQuery.text()));

        if (previousSqlQuery != null && previousErrorMessage != null) {
            messages.add(AiMessage.from(previousSqlQuery));
            messages.add(UserMessage.from(previousErrorMessage));
        }

        return chatLanguageModel.chat(messages).aiMessage().text();
    }

    protected Prompt createSystemPrompt() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("sqlDialect", sqlDialect);
        variables.put("databaseStructure", databaseStructure);
        return promptTemplate.apply(variables);
    }

    protected String clean(String sqlQuery) {
        if (sqlQuery.contains("```sql")) {
            return sqlQuery.substring(sqlQuery.indexOf("```sql") + 6, sqlQuery.lastIndexOf("```"));
        } else if (sqlQuery.contains("```")) {
            return sqlQuery.substring(sqlQuery.indexOf("```") + 3, sqlQuery.lastIndexOf("```"));
        }
        return sqlQuery;
    }

    protected void validate(String sqlQuery) {

    }

    protected String getSqlType(String sqlQuery) {
        try {
            net.sf.jsqlparser.statement.Statement statement = CCJSqlParserUtil.parse(sqlQuery);

            if (statement instanceof Select) {
                return "QUERY";
            } else if (statement instanceof Update || statement instanceof Insert || statement instanceof Delete) {
                return "OPERATION";
            } else if (statement instanceof CreateTable/* || statement instanceof Drop*/) {
                return "CREATE";
            } else {
                return "unknow";
            }
        } catch (JSQLParserException e) {
            return "";
        }
    }


    private static String format(String sqlQuery, String sqlType) {
        switch (sqlType) {
            case "QUERY":
                return String.format("Execute a SELECT query on the jdbc database':\n%s", sqlQuery);
            case "OPERATION":
                return String.format("Execute a INSERT, UPDATE or DELETE query on the jdbc database':\n%s", sqlQuery);
            case "CREATE":
                return String.format("Create new table in the jdbc database':\n%s", sqlQuery);
            default:
               return "";
        }
    }

    protected List<String> parse(String queries) {
        return stream(queries.split("\n"))
                .filter(Utils::isNotNullOrBlank)
                .collect(toList());
    }

    public static class McpSqlDatabaseQueryTransformerBuilder {
        private McpClient mcpClient;
        private PromptTemplate promptTemplate;
        private ChatLanguageModel chatLanguageModel;
        private Integer maxRetries;

        McpSqlDatabaseQueryTransformerBuilder() {
        }

        public McpSqlDatabaseQueryTransformerBuilder mcpClient(McpClient mcpClient) {
            this.mcpClient = mcpClient;
            return this;
        }

        public McpSqlDatabaseQueryTransformerBuilder promptTemplate(PromptTemplate promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public McpSqlDatabaseQueryTransformerBuilder chatLanguageModel(ChatLanguageModel chatLanguageModel) {
            this.chatLanguageModel = chatLanguageModel;
            return this;
        }

        public McpSqlDatabaseQueryTransformerBuilder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public McpSqlDatabaseQueryTransformerBuilder build() {
            return new McpSqlDatabaseQueryTransformerBuilder();
        }

        public String toString() {
            return "SqlDatabaseContentRetriever.SqlDatabaseContentRetrieverBuilder(mcpClient=" + this.mcpClient + ", promptTemplate=" + this.promptTemplate + ", chatLanguageModel=" + this.chatLanguageModel + ", maxRetries=" + this.maxRetries + ")";
        }
    }
}
