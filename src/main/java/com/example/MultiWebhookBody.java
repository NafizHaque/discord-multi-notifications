package com.example;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
class MultiWebhookBody
{
    private String content;
    private List<Embed> embeds = new ArrayList<>();

    @Data
    static class Embed
    {
        final UrlEmbed image;
    }

    @Data
    static class UrlEmbed
    {
        final String url;
    }
}
