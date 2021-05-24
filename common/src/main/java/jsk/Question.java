package jsk;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Question {

    public static class Data {
        final public String url;
        final public String title;
        @JsonProperty("favorite_count") final public Integer favoriteCount;
        @JsonProperty("view_count") final public Integer viewCount;
        @JsonDeserialize(using = QuestionTagsDeserializer.class) final public List<String> tags;
        final public String body;

        public Data(String url,
                    String title,
                    Integer favoriteCount,
                    Integer viewCount,
                    List<String> tags,
                    String body) {
            this.url = url;
            this.title = title;
            this.favoriteCount = favoriteCount;
            this.viewCount = viewCount;
            this.tags = tags;
            this.body = body;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "url='" + url + '\'' +
                    ", title='" + title + '\'' +
                    ", favoriteCount=" + favoriteCount +
                    ", viewCount=" + viewCount +
                    ", tags=" + tags +
                    '}';
        }
    }

    public static class QuestionTagsDeserializer extends JsonDeserializer<List<String>> {

        @Override
        public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            // data is [foo|bar] so we need to manually split it
            JsonDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(ctxt.constructType(List.class));
            Object o = deserializer.deserialize(p, ctxt);
            if (o instanceof List<?>) {
                List<?> l = (List<?>) o;
                if ((l.size() == 1) && (l.get(0) instanceof String)) {
                    String s = (String) l.get(0);
                    return Arrays.asList(s.split("\\|"));
                }
            }

            return Collections.emptyList();
        }
    }

}
