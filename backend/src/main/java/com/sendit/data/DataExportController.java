package com.sendit.data;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/data")
public class DataExportController {
    private final DataExportService service;

    public DataExportController(DataExportService service) {
        this.service = service;
    }

    @GetMapping("/export")
    ResponseEntity<DataExportDtos.Response> export(Principal principal) {
        String filename = "send-it-backup-" + LocalDate.now(ZoneId.of("Asia/Seoul")) + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(service.export(principal.getName()));
    }
}
