package com.logistics.dashboard.controller;

import com.logistics.dashboard.ai.model.QueryRequest;
import com.logistics.dashboard.ai.model.QueryResponse;
import com.logistics.dashboard.ai.orchestrator.NLUOrchestrator;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final NLUOrchestrator nluOrchestrator;

    public QueryController(NLUOrchestrator nluOrchestrator) {
        this.nluOrchestrator = nluOrchestrator;
    }

    @PostMapping
    public ResponseEntity<QueryResponse> processQuery(@RequestBody QueryRequest request) {
        QueryResponse response = nluOrchestrator.processQuery(request);

        if (response.getError() != null) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<QueryResponse> testQuery(
            @RequestParam(required = false) String question,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<String> carriers,
            @RequestParam(required = false) List<String> regions) {

        QueryRequest request = new QueryRequest();
        request.setQuestion(question != null ? question : "How many orders were delivered last week?");
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setCarriers(carriers);
        request.setRegions(regions);

        QueryResponse response = nluOrchestrator.processQuery(request);
        return ResponseEntity.ok(response);
    }
}