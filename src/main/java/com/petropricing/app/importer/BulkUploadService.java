package com.petropricing.app.importer;

import com.petropricing.app.domain.*;
import com.petropricing.app.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class BulkUploadService {

    private final DemandForecastRepository demandForecastRepository;
    private final FormulaRepository formulaRepository;
    private final FormulaComponentRepository formulaComponentRepository;
    private final TermTypeRepository termTypeRepository;
    private final MaterialGroupRepository materialGroupRepository;
    private final QuoteMappingRepository quoteMappingRepository;
    private final QuoteValueRepository quoteValueRepository;
    private final CurrencyRateRepository currencyRateRepository;

    public BulkUploadService(
            DemandForecastRepository demandForecastRepository,
            FormulaRepository formulaRepository,
            FormulaComponentRepository formulaComponentRepository,
            TermTypeRepository termTypeRepository,
            MaterialGroupRepository materialGroupRepository,
            QuoteMappingRepository quoteMappingRepository,
            QuoteValueRepository quoteValueRepository,
            CurrencyRateRepository currencyRateRepository
    ) {
        this.demandForecastRepository = demandForecastRepository;
        this.formulaRepository = formulaRepository;
        this.formulaComponentRepository = formulaComponentRepository;
        this.termTypeRepository = termTypeRepository;
        this.materialGroupRepository = materialGroupRepository;
        this.quoteMappingRepository = quoteMappingRepository;
        this.quoteValueRepository = quoteValueRepository;
        this.currencyRateRepository = currencyRateRepository;
    }

    public BulkUploadResult importAll(MultipartFile[] files) {
        BulkUploadResult result = new BulkUploadResult();

        if (files == null || files.length == 0) {
            result.addLine("❌ Нет файлов для загрузки.");
            return result;
        }

        List<IncomingFile> incoming = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String original = file.getOriginalFilename();
            if (original == null) original = "unknown.xlsx";
            TargetType target = detectTargetType(original);
            incoming.add(new IncomingFile(original, file, target));
        }

        // sort by priority (lowest first)
        incoming.sort(Comparator
                .comparingInt((IncomingFile f) -> f.targetType != null ? f.targetType.priority : Integer.MAX_VALUE)
                .thenComparing(f -> f.originalFilename));

        for (IncomingFile item : incoming) {
            if (item.targetType == null) {
                result.addLine("❌ " + item.originalFilename + " ➔ Неизвестный тип файла (не удалось сопоставить с таблицей).");
                continue;
            }
            try {
                int rows = importOne(item.targetType, item.file);
                result.addLine("📄 " + item.originalFilename + " ➔ Успешно импортировано в " + item.targetType.humanName + " (" + rows + " строк)");
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                result.addLine("❌ " + item.originalFilename + " ➔ Ошибка: " + msg);
            }
        }

        return result;
    }

    private record IncomingFile(String originalFilename, MultipartFile file, TargetType targetType) {}

    private enum TargetType {
        TERM_TYPES(10, "term_types", "Таблица TermType"),
        MATERIAL_GROUPS(20, "material_groups", "Таблица MaterialGroup"),
        FORMULAS(30, "formulas", "Таблица Formula"),
        QUOTE_MAPPING(40, "quote_mapping", "Таблица QuoteMapping"),
        FORMULA_COMPONENTS(50, "formula_components", "Таблица FormulaComponent"),
        QUOTES(60, "quotes", "Таблица QuoteValue"),
        CURRENCY_RATES(70, "currency_rates", "Таблица CurrencyRate"),
        SSP(80, "ssp", "Таблица DemandForecast");

        private final int priority;
        private final String detectToken;
        private final String humanName;

        TargetType(int priority, String detectToken, String humanName) {
            this.priority = priority;
            this.detectToken = detectToken;
            this.humanName = humanName;
        }
    }

    private TargetType detectTargetType(String originalFilename) {
        String name = originalFilename.toLowerCase(Locale.ROOT);

        // Be explicit about “quotes vs quote_mapping”
        if (name.contains("term_types")) return TargetType.TERM_TYPES;
        if (name.contains("material_groups")) return TargetType.MATERIAL_GROUPS;

        if (name.contains("quote_mapping")) return TargetType.QUOTE_MAPPING;
        if (name.contains("formula_components")) return TargetType.FORMULA_COMPONENTS;

        if (name.contains("formulas")) return TargetType.FORMULAS;

        if (name.contains("quotes") && !name.contains("quote_mapping")) return TargetType.QUOTES;
        if (name.contains("currency_rates")) return TargetType.CURRENCY_RATES;
        if (name.contains("ssp")) return TargetType.SSP;

        // Fallback: try to match by generic detect token
        for (TargetType t : TargetType.values()) {
            if (name.contains(t.detectToken)) return t;
        }
        return null;
    }

    private int importOne(TargetType targetType, MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream()) {
            return switch (targetType) {
                case TERM_TYPES -> importTermTypes(in);
                case MATERIAL_GROUPS -> importMaterialGroups(in);
                case FORMULAS -> importFormulas(in);
                case QUOTE_MAPPING -> importQuoteMapping(in);
                case FORMULA_COMPONENTS -> importFormulaComponents(in);
                case QUOTES -> importQuotes(in);
                case CURRENCY_RATES -> importCurrencyRates(in);
                case SSP -> importSsp(in);
            };
        }
    }

    private static Sheet pickSheet(Workbook workbook, String preferredSheetName) {
        Sheet sheet = workbook.getSheet(preferredSheetName);
        if (sheet != null) return sheet;
        // fallback
        return workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
    }

    private static Map<String, Integer> readHeaderIndex(Sheet sheet, int headerRowIndex) {
        Row header = sheet.getRow(headerRowIndex);
        if (header == null) return Map.of();

        Map<String, Integer> idx = new HashMap<>();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        for (Cell cell : header) {
            if (cell == null) continue;
            String raw = formatter.formatCellValue(cell);
            String key = normKey(raw);
            if (!key.isEmpty()) {
                idx.put(key, cell.getColumnIndex());
            }
        }
        return idx;
    }

    private static int findHeaderRow(Sheet sheet, Set<String> requiredKeywords) {
        // Scan first N rows and look for the best match.
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        int maxScan = Math.min(25, sheet.getLastRowNum() + 1);
        int bestRow = 0;
        int bestScore = -1;

        for (int r = 0; r < maxScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            int score = 0;
            for (Cell cell : row) {
                if (cell == null) continue;
                String raw = formatter.formatCellValue(cell);
                String key = normKey(raw);
                if (key.isEmpty()) continue;
                for (String kw : requiredKeywords) {
                    String kwNorm = normKey(kw);
                    if (key.equals(kwNorm) || key.contains(kwNorm)) {
                        score++;
                    }
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestRow = r;
            }
        }
        return bestRow;
    }

    private static Integer findIndex(Map<String, Integer> headerIndex, String keyword) {
        if (headerIndex == null || headerIndex.isEmpty()) return null;
        String kwNorm = normKey(keyword);
        Integer exact = headerIndex.get(kwNorm);
        if (exact != null) return exact;
        // If we only have "contains" matches (e.g. "формула" matches "ключ_формулы"),
        // choose the closest/shortest match to reduce false positives.
        Integer best = null;
        int bestLen = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> e : headerIndex.entrySet()) {
            if (!e.getKey().contains(kwNorm)) continue;
            int len = e.getKey().length();
            if (len < bestLen) {
                bestLen = len;
                best = e.getValue();
            }
        }
        return best;
    }

    private static String normKey(String s) {
        if (s == null) return "";
        String out = s.trim().toLowerCase(Locale.ROOT);
        // Normalize punctuation into underscores, keep Russian letters/digits
        out = out.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "_");
        out = out.replaceAll("_+", "_");
        out = out.replaceAll("^_|_$", "");
        return out;
    }

    private static String cellString(Cell cell, DataFormatter formatter) {
        if (cell == null) return null;
        String v = formatter.formatCellValue(cell);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private static Integer cellInteger(Cell cell, DataFormatter formatter) {
        String s = cellString(cell, formatter);
        if (s == null) return null;
        String cleaned = s.replace(" ", "").replace(',', '.');
        try {
            return (int) Math.round(Double.parseDouble(cleaned));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal cellDecimal(Cell cell, DataFormatter formatter) {
        String s = cellString(cell, formatter);
        if (s == null) return null;
        String cleaned = s.replace(" ", "").replace(',', '.');
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate cellLocalDate(Cell cell, DataFormatter formatter) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date d = cell.getDateCellValue();
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        String s = cellString(cell, formatter);
        if (s == null) return null;
        return parseLocalDate(s);
    }

    private static LocalDateTime cellLocalDateTime(Cell cell, DataFormatter formatter) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date d = cell.getDateCellValue();
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        String s = cellString(cell, formatter);
        if (s == null) return null;
        return parseLocalDateTime(s);
    }

    private static LocalDate parseLocalDate(String s) {
        String trimmed = s.trim();
        if (trimmed.length() >= 10) {
            // Handle "2026-06-01 00:00:00"
            trimmed = trimmed.substring(0, 10);
        }
        List<DateTimeFormatter> fmts = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy.MM.dd")
        );
        for (DateTimeFormatter fmt : fmts) {
            try {
                return LocalDate.parse(trimmed, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private static LocalDateTime parseLocalDateTime(String s) {
        String trimmed = s.trim();
        // If it's date only, interpret as start of day
        if (trimmed.length() >= 10) {
            String maybeDate = trimmed.substring(0, 10);
            LocalDate date = parseLocalDate(maybeDate);
            if (date != null && trimmed.length() <= 10) {
                return date.atStartOfDay();
            }
        }

        List<DateTimeFormatter> fmts = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        );
        for (DateTimeFormatter fmt : fmts) {
            try {
                return LocalDateTime.parse(trimmed, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        // Last resort: try parse as instant-like without timezone
        return null;
    }

    private int importSsp(InputStream in) throws Exception {
        demandForecastRepository.deleteAll();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = pickSheet(workbook, "ssp");
            if (sheet == null) return 0;

            Set<String> required = Set.of("row_id", "period", "forecast", "client_id");
            int headerRow = findHeaderRow(sheet, required);
            Map<String, Integer> headerIndex = readHeaderIndex(sheet, headerRow);

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<DemandForecast> rows = new ArrayList<>();
            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                if (isRowEmpty(row)) continue;

                DemandForecast entity = new DemandForecast();
                entity.setRowId(cellInteger(getCell(row, headerIndex, "row_id"), formatter));
                entity.setPeriod(cellLocalDate(getCell(row, headerIndex, "period"), formatter));
                entity.setCustomerAsv(cellString(getCell(row, headerIndex, "customer_asv"), formatter));
                entity.setCustomer(cellString(getCell(row, headerIndex, "customer"), formatter));
                entity.setMtrNsiCode(cellString(getCell(row, headerIndex, "mtr_nsi_code"), formatter));
                entity.setMtrNsiName(cellString(getCell(row, headerIndex, "mtr_nsi_name"), formatter));
                entity.setContract(cellString(getCell(row, headerIndex, "contract"), formatter));
                entity.setCurrency(cellString(getCell(row, headerIndex, "currency"), formatter));
                entity.setForecast(cellDecimal(getCell(row, headerIndex, "forecast"), formatter));
                entity.setCountry(cellString(getCell(row, headerIndex, "country"), formatter));
                entity.setRegion(cellString(getCell(row, headerIndex, "region"), formatter));
                entity.setMarket(cellString(getCell(row, headerIndex, "market"), formatter));
                entity.setClientId(cellString(getCell(row, headerIndex, "client_id"), formatter));
                entity.setClientName(cellString(getCell(row, headerIndex, "client_name"), formatter));

                rows.add(entity);
            }
            demandForecastRepository.saveAll(rows);
            return rows.size();
        }
    }

    private int importFormulas(InputStream in) throws Exception {
        formulaRepository.deleteAll();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = pickSheet(workbook, "formulas");
            if (sheet == null) return 0;

            Set<String> required = Set.of("ключ_формулы", "формула", "неактивна");
            int headerRow = findHeaderRow(sheet, required);
            Map<String, Integer> headerIndex = readHeaderIndex(sheet, headerRow);

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<Formula> entities = new ArrayList<>();
            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                if (isRowEmpty(row)) continue;

                Formula entity = new Formula();
                entity.setFormulaKey(cellString(getCell(row, headerIndex, "ключ формулы"), formatter));
                entity.setFormulaText(cellString(getCell(row, headerIndex, "формула"), formatter));
                entity.setMaterialGroupM(cellString(getCell(row, headerIndex, "подгруппа материалов"), formatter));
                entity.setBusinessPartner(cellString(getCell(row, headerIndex, "деловой партнер"), formatter));
                entity.setValidFrom(cellLocalDateTime(getCell(row, headerIndex, "действительно с"), formatter));
                entity.setValidTo(cellLocalDate(getCell(row, headerIndex, "действительно по"), formatter));
                entity.setCreatedOn(cellLocalDate(getCell(row, headerIndex, "дата создания"), formatter));
                entity.setInactive(cellString(getCell(row, headerIndex, "неактивна"), formatter));
                entity.setPriceType(cellString(getCell(row, headerIndex, "тип цены"), formatter));
                entity.setCurrencyDocument(cellString(getCell(row, headerIndex, "валютадокумента"), formatter));
                entity.setMaterialCode(cellString(getCell(row, headerIndex, "материал"), formatter));
                entity.setClientId(cellString(getCell(row, headerIndex, "client_id"), formatter));
                entity.setClientName(cellString(getCell(row, headerIndex, "client_name"), formatter));

                entities.add(entity);
            }

            formulaRepository.saveAll(entities);
            return entities.size();
        }
    }

    private int importFormulaComponents(InputStream in) throws Exception {
        formulaComponentRepository.deleteAll();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = pickSheet(workbook, "components");
            if (sheet == null) return 0;

            Set<String> required = Set.of("формула", "номер терма", "тип фиксированного терма");
            int headerRow = findHeaderRow(sheet, required);
            Map<String, Integer> headerIndex = readHeaderIndex(sheet, headerRow);

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<FormulaComponent> entities = new ArrayList<>();
            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                if (isRowEmpty(row)) continue;

                FormulaComponent entity = new FormulaComponent();
                entity.setFormulaKey(cellString(getCell(row, headerIndex, "формула"), formatter));
                entity.setTermNumber(cellInteger(getCell(row, headerIndex, "номер терма"), formatter));
                entity.setConditionType(cellString(getCell(row, headerIndex, "вид условий"), formatter));
                entity.setVariableName(cellString(getCell(row, headerIndex, "имя переменной"), formatter));
                entity.setValidFrom(cellLocalDateTime(getCell(row, headerIndex, "действительно с"), formatter));
                entity.setValidTo(cellLocalDateTime(getCell(row, headerIndex, "действительно по"), formatter));
                entity.setTermTypeCode(cellString(getCell(row, headerIndex, "тип фиксированного терма"), formatter));
                entity.setValueAmount(cellDecimal(getCell(row, headerIndex, "значение"), formatter));
                entity.setTermCurrency(cellString(getCell(row, headerIndex, "валюта"), formatter));

                entity.setQuoteName(cellString(getCell(row, headerIndex, "имя котировки"), formatter));
                entity.setQuoteKind(cellString(getCell(row, headerIndex, "вид котировки"), formatter));

                entity.setCalcRule(cellInteger(getCell(row, headerIndex, "правило расчета"), formatter));
                entity.setCurrencyPeriodRuleCode(cellString(getCell(row, headerIndex, "правило опред. периода: валютный курс"), formatter));
                entity.setCurrencyPeriodRule(cellString(getCell(row, headerIndex, "прав. опред. периода"), formatter));
                entity.setTermCalcKind(cellInteger(getCell(row, headerIndex, "вид расчета терма"), formatter));
                entity.setFactor1(cellInteger(getCell(row, headerIndex, "фактор 1"), formatter));
                entity.setFactor2(cellInteger(getCell(row, headerIndex, "фактор 2"), formatter));
                entity.setQuoteOrigin(cellString(getCell(row, headerIndex, "происхождение котировки"), formatter));

                entities.add(entity);
            }

            formulaComponentRepository.saveAll(entities);
            return entities.size();
        }
    }

    private int importTermTypes(InputStream in) throws Exception {
        termTypeRepository.deleteAll();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = pickSheet(workbook, "term_types");
            if (sheet == null) return 0;

            Set<String> required = Set.of("type_code", "category");
            int headerRow = findHeaderRow(sheet, required);
            Map<String, Integer> headerIndex = readHeaderIndex(sheet, headerRow);

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<TermType> entities = new ArrayList<>();
            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                if (isRowEmpty(row)) continue;

                TermType entity = new TermType();
                entity.setTypeCode(cellString(getCell(row, headerIndex, "type_code"), formatter));
                entity.setTypeLabel(cellString(getCell(row, headerIndex, "type_label"), formatter));
                entity.setCategory(cellString(getCell(row, headerIndex, "category"), formatter));
                entity.setFixedTermKind(cellString(getCell(row, headerIndex, "fixed_term_kind"), formatter));
                entity.setDescription(cellString(getCell(row, headerIndex, "description"), formatter));
                entity.setValueSource(cellString(getCell(row, headerIndex, "value_source"), formatter));
                entities.add(entity);
            }

            termTypeRepository.saveAll(entities);
            return entities.size();
        }
    }

    private int importMaterialGroups(InputStream in) throws Exception {
        materialGroupRepository.deleteAll();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = pickSheet(workbook, "material_groups");
            if (sheet == null) return 0;

            Set<String> required = Set.of("hname", "code_nsi", "leaf");
            int headerRow = findHeaderRow(sheet, required);
            Map<String, Integer> headerIndex = readHeaderIndex(sheet, headerRow);

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<MaterialGroup> entities = new ArrayList<>();
            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                if (isRowEmpty(row)) continue;

                MaterialGroup entity = new MaterialGroup();
                entity.setHname(cellString(getCell(row, headerIndex, "hname"), formatter));
                entity.setCodeNsi(cellString(getCell(row, headerIndex, "code_nsi"), formatter));
                entity.setV_MtrName(cellString(getCell(row, headerIndex, "V_MTR_name"), formatter));
                entity.setMtrPfNameLvl2(cellString(getCell(row, headerIndex, "mtr_pf_name_lvl_2"), formatter));
                entity.setMtrPfNameLvl3(cellString(getCell(row, headerIndex, "mtr_pf_name_lvl_3"), formatter));
                entity.setTlevel(cellString(getCell(row, headerIndex, "tlevel"), formatter));
                entity.setDatuv(cellLocalDate(getCell(row, headerIndex, "datuv"), formatter));
                entity.setDatub(cellLocalDate(getCell(row, headerIndex, "datub"), formatter));
                entity.setLeaf(cellString(getCell(row, headerIndex, "leaf"), formatter));
                entities.add(entity);
            }

            materialGroupRepository.saveAll(entities);
            return entities.size();
        }
    }

    private int importQuoteMapping(InputStream in) throws Exception {
        quoteMappingRepository.deleteAll();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = pickSheet(workbook, "quote_mapping");
            if (sheet == null) return 0;

            Set<String> required = Set.of("происхождение котировки", "имя котировки", "валюта котировки");
            int headerRow = findHeaderRow(sheet, required);
            Map<String, Integer> headerIndex = readHeaderIndex(sheet, headerRow);

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<QuoteMapping> entities = new ArrayList<>();
            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                if (isRowEmpty(row)) continue;

                QuoteMapping entity = new QuoteMapping();
                entity.setQuoteOrigin(cellString(getCell(row, headerIndex, "происхождение котировки"), formatter));
                entity.setQuoteKind(cellString(getCell(row, headerIndex, "вид котировки"), formatter));
                entity.setQuoteName(cellString(getCell(row, headerIndex, "имя котировки"), formatter));
                entity.setDlId(cellString(getCell(row, headerIndex, "id в озере данных"), formatter));
                entity.setDlId2(cellString(getCell(row, headerIndex, "id2 в озере данных"), formatter));
                entity.setQuoteCurrency(cellString(getCell(row, headerIndex, "валюта котировки"), formatter));
                entities.add(entity);
            }

            quoteMappingRepository.saveAll(entities);
            return entities.size();
        }
    }

    private int importQuotes(InputStream in) throws Exception {
        quoteValueRepository.deleteAll();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = pickSheet(workbook, "quotes");
            if (sheet == null) return 0;

            Set<String> required = Set.of("quote_code", "quote_date", "tech_load_ts");
            int headerRow = findHeaderRow(sheet, required);
            Map<String, Integer> headerIndex = readHeaderIndex(sheet, headerRow);

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Map<QuoteKey, QuoteValue> dedup = new LinkedHashMap<>();

            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                if (isRowEmpty(row)) continue;

                QuoteValue entity = new QuoteValue();
                entity.setQuoteType(cellString(getCell(row, headerIndex, "quote_type"), formatter));
                entity.setQuoteName(cellString(getCell(row, headerIndex, "quote_name"), formatter));
                entity.setTechQuoteName(cellString(getCell(row, headerIndex, "tech_quote_name"), formatter));
                entity.setQuoteCode(cellString(getCell(row, headerIndex, "quote_code"), formatter));
                entity.setQuoteDate(cellLocalDate(getCell(row, headerIndex, "quote_date"), formatter));
                entity.setQuoteCurrency(cellString(getCell(row, headerIndex, "quote_currency"), formatter));
                entity.setQuoteVal(cellDecimal(getCell(row, headerIndex, "quote_val"), formatter));
                entity.setTechLoadTs(cellLocalDateTime(getCell(row, headerIndex, "tech_load_ts"), formatter));

                QuoteKey key = new QuoteKey(entity.getQuoteCode(), entity.getQuoteDate(), entity.getQuoteType());
                QuoteValue existing = dedup.get(key);
                if (existing == null) {
                    dedup.put(key, entity);
                } else if (betterTechLoadTs(existing.getTechLoadTs(), entity.getTechLoadTs())) {
                    dedup.put(key, entity);
                }
            }

            quoteValueRepository.saveAll(dedup.values());
            return dedup.size();
        }
    }

    private boolean betterTechLoadTs(LocalDateTime existing, LocalDateTime candidate) {
        if (candidate == null) return false;
        if (existing == null) return true;
        return candidate.isAfter(existing);
    }

    private record QuoteKey(String quoteCode, LocalDate quoteDate, String quoteType) {}

    private int importCurrencyRates(InputStream in) throws Exception {
        currencyRateRepository.deleteAll();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = pickSheet(workbook, "currency_rates");
            if (sheet == null) return 0;

            Set<String> required = Set.of("currency_name", "calday", "currency_value");
            int headerRow = findHeaderRow(sheet, required);
            Map<String, Integer> headerIndex = readHeaderIndex(sheet, headerRow);

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<CurrencyRate> entities = new ArrayList<>();
            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                if (isRowEmpty(row)) continue;

                CurrencyRate entity = new CurrencyRate();
                entity.setVersionType(cellString(getCell(row, headerIndex, "version_type"), formatter));
                entity.setCurrencyName(cellString(getCell(row, headerIndex, "currency_name"), formatter));
                entity.setCalmonth(cellString(getCell(row, headerIndex, "calmonth"), formatter));
                entity.setCalday(cellLocalDate(getCell(row, headerIndex, "calday"), formatter));
                entity.setCurrencyValue(cellDecimal(getCell(row, headerIndex, "currency_value"), formatter));
                entities.add(entity);
            }

            currencyRateRepository.saveAll(entities);
            return entities.size();
        }
    }

    private static boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell == null) continue;
            if (cell.getCellType() == CellType.BLANK) continue;
            String s;
            try {
                s = new DataFormatter(Locale.ROOT).formatCellValue(cell);
            } catch (Exception e) {
                continue;
            }
            if (s != null && !s.trim().isEmpty()) return false;
        }
        return true;
    }

    private static Cell getCell(Row row, Map<String, Integer> headerIndex, String keyword) {
        Integer idx = findIndex(headerIndex, keyword);
        if (idx == null) return null;
        return row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    }
}

