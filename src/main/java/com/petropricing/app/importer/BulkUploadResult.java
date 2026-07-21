package com.petropricing.app.importer;

import java.util.ArrayList;
import java.util.List;

public class BulkUploadResult {
    private final List<String> lines = new ArrayList<>();

    public void addLine(String line) {
        lines.add(line);
    }

    public List<String> getLines() {
        return lines;
    }
}

