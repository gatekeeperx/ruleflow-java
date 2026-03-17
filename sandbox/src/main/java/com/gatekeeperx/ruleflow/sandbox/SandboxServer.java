package com.gatekeeperx.ruleflow.sandbox;

import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.util.*;

public class SandboxServer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    record EvaluateRequest(String workflow, JsonObject payload, JsonObject lists) {}

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 7070;

        var app = Javalin.create(config -> {
            config.staticFiles.add("/static", Location.CLASSPATH);
        }).start(port);

        app.get("/", ctx -> ctx.redirect("/index.html"));

        app.post("/evaluate", ctx -> {
            EvaluateRequest req = GSON.fromJson(ctx.body(), EvaluateRequest.class);

            Map<String, Object> payload = req.payload() != null
                ? GSON.fromJson(req.payload(), new TypeToken<Map<String, Object>>() {}.getType())
                : Map.of();

            Map<String, List<?>> lists = req.lists() != null
                ? parseLists(req.lists())
                : Map.of();

            try {
                WorkflowResult result = new Workflow(req.workflow()).evaluate(payload, lists);
                ctx.contentType("application/json").result(GSON.toJson(serialize(result)));
            } catch (Exception e) {
                ctx.status(400).contentType("application/json")
                   .result(GSON.toJson(Map.of("error", true, "message", e.getMessage())));
            }
        });

        System.out.println("Ruleflow sandbox → http://localhost:" + port);
    }

    private static Map<String, List<?>> parseLists(JsonObject json) {
        Map<String, List<?>> result = new HashMap<>();
        for (var entry : json.entrySet()) {
            List<Object> list = GSON.fromJson(entry.getValue(), new TypeToken<List<Object>>() {}.getType());
            result.put(entry.getKey(), list);
        }
        return result;
    }

    private static Map<String, Object> serialize(WorkflowResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("result", r.getResult());
        m.put("ruleset", r.getRuleSet());
        m.put("rule", r.getRule());
        m.put("variables", r.getVariables());
        m.put("actions", r.getActionCalls().stream()
            .map(a -> Map.of("name", a.getName(), "params", a.getParams()))
            .toList());
        if (r.getMatchedRules() != null) {
            m.put("matchedRules", r.getMatchedRules().stream()
                .map(mr -> Map.of(
                    "ruleset", String.valueOf(mr.getRuleSet()),
                    "rule",    String.valueOf(mr.getRule()),
                    "result",  String.valueOf(mr.getResult())
                )).toList());
        }
        m.put("error", r.isError());
        return m;
    }
}