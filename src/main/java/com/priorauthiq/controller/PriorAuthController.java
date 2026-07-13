package com.priorauthiq.controller;

import com.priorauthiq.model.Decision;
import com.priorauthiq.model.Determination;
import com.priorauthiq.service.PriorAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Prior-authorization triage API.
 *
 * <pre>
 * POST /api/prior-auth            submit a request, run triage, return a Determination
 * GET  /api/determinations        list determinations (optional ?decision= filter)
 * GET  /api/determinations/{id}   fetch a specific determination
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class PriorAuthController {

    private final PriorAuthService priorAuthService;

    public PriorAuthController(PriorAuthService priorAuthService) {
        this.priorAuthService = priorAuthService;
    }

    @PostMapping("/prior-auth")
    @ResponseStatus(HttpStatus.CREATED)
    public Determination submit(@Valid @RequestBody PriorAuthSubmission submission) {
        return priorAuthService.triage(submission.toRequest());
    }

    @GetMapping("/determinations")
    public List<Determination> list(@RequestParam(required = false) Decision decision) {
        return priorAuthService.list(decision);
    }

    @GetMapping("/determinations/{id}")
    public ResponseEntity<Determination> get(@PathVariable String id) {
        return priorAuthService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
