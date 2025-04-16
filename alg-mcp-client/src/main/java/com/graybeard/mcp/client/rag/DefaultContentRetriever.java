package com.graybeard.mcp.client.rag;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

import java.util.List;

/**
 * DefaultContentRetriever
 *
 * @author C.LY
 * @since 2025-04-16
 */
public class DefaultContentRetriever implements ContentRetriever {
    @Override
    public List<Content> retrieve(Query query) {
        Content content = Content.from(query.text());
        return List.of(content);
    }
}
