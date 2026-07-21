package com.petropricing.app.web;

import com.petropricing.app.domain.DemandForecast;
import com.petropricing.app.domain.Formula;
import com.petropricing.app.importer.BulkUploadResult;
import com.petropricing.app.importer.BulkUploadService;
import com.petropricing.app.repository.DemandForecastRepository;
import com.petropricing.app.repository.FormulaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Controller
public class UploadController {

    private static final int PREVIEW_LIMIT = 50;

    private final BulkUploadService bulkUploadService;
    private final DemandForecastRepository demandForecastRepository;
    private final FormulaRepository formulaRepository;

    public UploadController(
            BulkUploadService bulkUploadService,
            DemandForecastRepository demandForecastRepository,
            FormulaRepository formulaRepository
    ) {
        this.bulkUploadService = bulkUploadService;
        this.demandForecastRepository = demandForecastRepository;
        this.formulaRepository = formulaRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        populatePreview(model, Collections.emptyList());
        return "index";
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public String upload(
            @RequestParam("files") MultipartFile[] files,
            Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        BulkUploadResult result = bulkUploadService.importAll(files);
        populatePreview(model, result.getLines());

        if ("true".equals(hxRequest)) {
            return "index :: page";
        }
        return "index";
    }

    private void populatePreview(Model model, List<String> uploadLines) {
        var forecasts = demandForecastRepository.findAll(
                PageRequest.of(0, PREVIEW_LIMIT, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent();

        var formulas = formulaRepository.findAll(
                PageRequest.of(0, PREVIEW_LIMIT, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent();

        model.addAttribute("uploadLines", uploadLines);
        model.addAttribute("forecastRows", forecasts);
        model.addAttribute("formulaRows", formulas);
    }
}

