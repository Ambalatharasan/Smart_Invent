package com.stockwise.api.service;

import com.stockwise.api.dto.InventoryDtos.InventoryRequest;
import com.stockwise.api.dto.InventoryDtos.InventoryCsvImportResponse;
import com.stockwise.api.dto.InventoryDtos.InventoryResponse;
import com.stockwise.api.dto.InventoryDtos.StockAdjustmentRequest;
import com.stockwise.api.entity.ActivityType;
import com.stockwise.api.entity.InventoryItem;
import com.stockwise.api.entity.StockStatus;
import com.stockwise.api.repository.InventoryItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class InventoryService {
    private static final String ITEM_NUM_PREFIX = "ITEM-";
    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Grocery",
            "Apparel",
            "Body Care",
            "Home Goods",
            "Lifestyle",
            "Stationery",
            "Electronics",
            "Hardware",
            "Medical",
            "Other"
    );
    private static final List<String> INVENTORY_TEMPLATE_HEADERS = List.of(
            "id",
            "itemNum",
            "name",
            "category",
            "supplier",
            "stock",
            "reorderPoint",
            "unitCost",
            "retailPrice",
            "leadTime",
            "location",
            "notes",
            "updatedAt"
    );

    private final InventoryItemRepository inventoryItemRepository;
    private final ActivityLogService activityLogService;

    public InventoryService(InventoryItemRepository inventoryItemRepository, ActivityLogService activityLogService) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> list(String search, String category, StockStatus status) {
        List<InventoryItem> items = inventoryItemRepository.findFiltered(
                normalized(search),
                normalized(category),
                status == null ? null : status.name()
        );
        return items.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        Map<String, String> categories = new LinkedHashMap<>();
        DEFAULT_CATEGORIES.forEach(category -> categories.put(category.toLowerCase(Locale.ROOT), category));
        inventoryItemRepository.findDistinctCategories()
                .stream()
                .filter(category -> category != null && !category.isBlank())
                .forEach(category -> categories.putIfAbsent(category.toLowerCase(Locale.ROOT), category));
        return categories.values().stream()
                .filter(category -> !"Other".equalsIgnoreCase(category))
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryResponse get(Long id) {
        return toResponse(findItem(id));
    }

    @Transactional
    public InventoryResponse create(InventoryRequest request, String actorEmail) {
        validateInventoryRequest(request);
        String itemNum = generateNextItemNum();
        InventoryItem item = new InventoryItem(
                itemNum,
                request.name().trim(),
                request.category().trim(),
                request.supplier().trim(),
                request.quantity(),
                request.reorderPoint(),
                request.unitCost(),
                request.retailPrice(),
                request.leadTimeDays(),
                request.location().trim(),
                request.notes(),
                request.lastRestockDate(),
                request.lastRestockQuantity()
        );
        InventoryItem saved = inventoryItemRepository.save(item);
        activityLogService.log(ActivityType.ITEM_CREATED, "Created inventory item " + saved.getSku(), saved.getSku(), saved.getQuantity(), saved.getLastRestockDate(), actorEmail);
        return toResponse(saved);
    }

    @Transactional
    public InventoryResponse update(Long id, InventoryRequest request, String actorEmail) {
        validateInventoryRequest(request);
        InventoryItem item = findItem(id);
        String requestedSku = request.sku() == null ? "" : request.sku().trim();
        if (!requestedSku.isBlank() && !item.getSku().equalsIgnoreCase(requestedSku)) {
            throw new IllegalArgumentException("Item.Num cannot be changed");
        }
        item.setName(request.name().trim());
        item.setCategory(request.category().trim());
        item.setSupplier(request.supplier().trim());
        item.setQuantity(request.quantity());
        item.setReorderPoint(request.reorderPoint());
        item.setUnitCost(request.unitCost());
        item.setRetailPrice(request.retailPrice());
        item.setLeadTimeDays(request.leadTimeDays());
        item.setLocation(request.location().trim());
        item.setNotes(request.notes());
        item.setLastRestockDate(request.lastRestockDate());
        item.setLastRestockQuantity(request.lastRestockQuantity());
        activityLogService.log(ActivityType.ITEM_UPDATED, "Updated inventory item " + item.getSku(), item.getSku(), item.getQuantity(), item.getLastRestockDate(), actorEmail);
        return toResponse(item);
    }

    @Transactional
    public InventoryResponse adjustStock(Long id, StockAdjustmentRequest request, String actorEmail) {
        if (request.quantityChange() == 0) {
            throw new IllegalArgumentException("Quantity change cannot be zero");
        }
        InventoryItem item = findItem(id);
        int before = item.getQuantity();
        item.setQuantity(before + request.quantityChange());
        ActivityType activityType = ActivityType.STOCK_ADJUSTED;
        LocalDate restockDate = item.getLastRestockDate();
        Integer restockQuantity = item.getLastRestockQuantity();
        if (request.quantityChange() > 0) {
            activityType = ActivityType.RESTOCK_RECEIVED;
            restockDate = LocalDate.now();
            restockQuantity = request.quantityChange();
            item.setLastRestockDate(restockDate);
            item.setLastRestockQuantity(restockQuantity);
        }
        String message = "%s adjusted by %+d units. Reason: %s".formatted(item.getSku(), request.quantityChange(), request.reason());
        activityLogService.log(activityType, message, item.getSku(), request.quantityChange(), restockDate, actorEmail);
        return toResponse(item);
    }

    @Transactional
    public void delete(Long id, String actorEmail) {
        InventoryItem item = findItem(id);
        inventoryItemRepository.delete(item);
        activityLogService.log(ActivityType.ITEM_UPDATED, "Deleted inventory item " + item.getSku(), item.getSku(), null, null, actorEmail);
    }

    @Transactional(readOnly = true)
    public String exportItemsCsv() {
        List<String> rows = new ArrayList<>();
        rows.add(csvRow(INVENTORY_TEMPLATE_HEADERS));
        inventoryItemRepository.findAll().stream()
                .sorted(Comparator.comparing(InventoryItem::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(item -> rows.add(csvRow(List.of(
                        stringValue(item.getId()),
                        item.getSku(),
                        item.getName(),
                        item.getCategory(),
                        item.getSupplier(),
                        stringValue(item.getQuantity()),
                        stringValue(item.getReorderPoint()),
                        stringValue(item.getUnitCost()),
                        stringValue(item.getRetailPrice()),
                        stringValue(item.getLeadTimeDays()),
                        item.getLocation(),
                        stringValue(item.getNotes()),
                        stringValue(item.getUpdatedAt())
                ))));
        return String.join("\n", rows) + "\n";
    }

    public String inventoryTemplateCsv() {
        return csvRow(INVENTORY_TEMPLATE_HEADERS) + "\n"
                + csvRow(List.of(
                "",
                "",
                "Sample Product",
                "Grocery",
                "Sample Supplier",
                "25",
                "10",
                "5.50",
                "9.99",
                "7",
                "Aisle 1",
                "Optional notes",
                ""
        )) + "\n";
    }

    @Transactional
    public InventoryCsvImportResponse importItems(byte[] content, String fileName, String actorEmail) {
        List<List<String>> rows = parseInventoryFile(content, fileName);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Inventory import file is empty");
        }
        Map<String, Integer> headers = headers(rows.get(0));
        requireHeaders(headers, List.of(
                "name",
                "category",
                "supplier",
                "stock",
                "reorderpoint",
                "unitcost",
                "leadtime",
                "location"
        ));
        requireAnyHeader(headers, List.of("retailprice", "price"), "retailPrice");

        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (isBlankRow(row)) {
                continue;
            }
            int rowNumber = index + 1;
            try {
                String itemNum = optionalItemNum(row, headers);
                String idValue = value(row, headers, "id");
                InventoryItem item = findImportTarget(idValue, itemNum);
                boolean isCreate = item == null;
                String finalItemNum = isCreate
                        ? (itemNum.isBlank() ? generateNextItemNum() : validatedNewItemNum(itemNum))
                        : item.getSku();

                if (isCreate) {
                    BigDecimal unitCost = parseMoney(required(row, headers, "unitcost", "unitCost"), "Unit Cost");
                    BigDecimal retailPrice = parseMoney(requiredAny(row, headers, "Retail Price", "retailprice", "price"), "Retail Price");
                    validateMoney(unitCost, retailPrice);
                    item = new InventoryItem(
                            finalItemNum,
                            required(row, headers, "name", "name"),
                            required(row, headers, "category", "category"),
                            required(row, headers, "supplier", "supplier"),
                            parseInt(required(row, headers, "stock", "stock"), "stock"),
                            parseInt(required(row, headers, "reorderpoint", "reorderPoint"), "reorderPoint"),
                            unitCost,
                            retailPrice,
                            parseInt(required(row, headers, "leadtime", "leadTime"), "leadTime"),
                            required(row, headers, "location", "location"),
                            value(row, headers, "notes"),
                            null,
                            null
                    );
                } else {
                    applyInventoryImportRow(item, row, headers);
                }

                InventoryItem saved = inventoryItemRepository.save(item);
                ActivityType activityType = isCreate ? ActivityType.ITEM_CREATED : ActivityType.ITEM_UPDATED;
                String action = isCreate ? "Imported inventory item " : "Updated inventory item from import ";
                activityLogService.log(activityType, action + saved.getSku(), saved.getSku(), saved.getQuantity(), saved.getLastRestockDate(), actorEmail);
                if (isCreate) {
                    created++;
                } else {
                    updated++;
                }
            } catch (RuntimeException ex) {
                skipped++;
                errors.add("Row " + rowNumber + ": " + ex.getMessage());
            }
        }

        return new InventoryCsvImportResponse(created, updated, skipped, errors);
    }

    private InventoryItem findItem(Long id) {
        return inventoryItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found"));
    }

    private InventoryItem findImportTarget(String idValue, String itemNum) {
        InventoryItem itemById = null;
        if (idValue != null && !idValue.isBlank()) {
            Long id = Long.parseLong(idValue.trim());
            itemById = inventoryItemRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Inventory id " + id + " not found"));
        }
        InventoryItem itemBySku = itemNum == null || itemNum.isBlank()
                ? null
                : inventoryItemRepository.findBySkuIgnoreCase(itemNum.trim()).orElse(null);
        if (itemById != null && itemBySku != null && !itemById.getId().equals(itemBySku.getId())) {
            throw new IllegalArgumentException("Item.Num already belongs to another inventory item");
        }
        return itemById == null ? itemBySku : itemById;
    }

    private void applyInventoryImportRow(InventoryItem item, List<String> row, Map<String, Integer> headers) {
        String requestedItemNum = optionalItemNum(row, headers);
        if (!requestedItemNum.isBlank()) {
            inventoryItemRepository.findBySkuIgnoreCase(requestedItemNum)
                    .filter(existing -> !existing.getId().equals(item.getId()))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Item.Num already belongs to another inventory item");
                    });
            item.setSku(requestedItemNum.trim());
        }
        BigDecimal unitCost = parseMoney(required(row, headers, "unitcost", "Unit Cost"), "Unit Cost");
        BigDecimal retailPrice = parseMoney(requiredAny(row, headers, "Retail Price", "retailprice", "price"), "Retail Price");
        validateMoney(unitCost, retailPrice);
        item.setName(required(row, headers, "name", "name"));
        item.setCategory(required(row, headers, "category", "category"));
        item.setSupplier(required(row, headers, "supplier", "supplier"));
        item.setQuantity(parseInt(required(row, headers, "stock", "stock"), "stock"));
        item.setReorderPoint(parseInt(required(row, headers, "reorderpoint", "reorderPoint"), "reorderPoint"));
        item.setUnitCost(unitCost);
        item.setRetailPrice(retailPrice);
        item.setLeadTimeDays(parseInt(required(row, headers, "leadtime", "leadTime"), "leadTime"));
        item.setLocation(required(row, headers, "location", "location"));
        item.setNotes(value(row, headers, "notes"));
    }

    private void validateInventoryRequest(InventoryRequest request) {
        if (request.category() == null || request.category().trim().isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }
        validateMoney(request.unitCost(), request.retailPrice());
    }

    private static void validateMoney(BigDecimal unitCost, BigDecimal retailPrice) {
        if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid Unit Cost");
        }
        if (retailPrice == null || retailPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid Retail Price");
        }
        if (retailPrice.compareTo(unitCost) < 0) {
            throw new IllegalArgumentException("Retail Price cannot be less than Unit Cost");
        }
    }

    private synchronized String generateNextItemNum() {
        int nextNumber = inventoryItemRepository.findGeneratedItemNumbers()
                .stream()
                .mapToInt(InventoryService::generatedItemNumValue)
                .max()
                .orElse(0) + 1;
        String candidate;
        do {
            candidate = ITEM_NUM_PREFIX + String.format(Locale.ROOT, "%04d", nextNumber++);
        } while (inventoryItemRepository.existsBySkuIgnoreCase(candidate));
        return candidate;
    }

    private String validatedNewItemNum(String itemNum) {
        String value = itemNum == null ? "" : itemNum.trim();
        if (value.isBlank()) {
            return generateNextItemNum();
        }
        if (inventoryItemRepository.existsBySkuIgnoreCase(value)) {
            throw new IllegalArgumentException("Duplicate Item.Num");
        }
        return value;
    }

    private static int generatedItemNumValue(String itemNum) {
        if (itemNum == null) {
            return 0;
        }
        String value = itemNum.trim().toUpperCase(Locale.ROOT);
        if (!value.startsWith(ITEM_NUM_PREFIX)) {
            return 0;
        }
        try {
            return Integer.parseInt(value.substring(ITEM_NUM_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String optionalItemNum(List<String> row, Map<String, Integer> headers) {
        String itemNum = value(row, headers, "itemnum");
        if (itemNum.isBlank()) {
            itemNum = value(row, headers, "sku");
        }
        return itemNum.trim();
    }

    private static List<List<String>> parseInventoryFile(byte[] content, String fileName) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Inventory import file is empty");
        }
        String normalizedName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        boolean looksLikeXlsx = normalizedName.endsWith(".xlsx") || (content.length > 2 && content[0] == 'P' && content[1] == 'K');
        if (looksLikeXlsx) {
            return parseXlsx(content);
        }
        return parseCsv(new String(content, StandardCharsets.UTF_8));
    }

    private static List<List<String>> parseXlsx(byte[] content) {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), zipInputStream.readAllBytes());
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not read Excel workbook");
        }

        byte[] worksheet = entries.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("xl/worksheets/") && entry.getKey().endsWith(".xml"))
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Excel workbook does not contain a worksheet"));
        List<String> sharedStrings = parseSharedStrings(entries.get("xl/sharedStrings.xml"));
        return parseWorksheet(worksheet, sharedStrings);
    }

    private static List<String> parseSharedStrings(byte[] sharedStringsXml) {
        if (sharedStringsXml == null) {
            return List.of();
        }
        Document document = parseXml(sharedStringsXml, "shared strings");
        NodeList nodes = document.getElementsByTagName("si");
        List<String> values = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            values.add(textFromDescendants((Element) nodes.item(index), "t"));
        }
        return values;
    }

    private static List<List<String>> parseWorksheet(byte[] worksheetXml, List<String> sharedStrings) {
        Document document = parseXml(worksheetXml, "worksheet");
        NodeList rowNodes = document.getElementsByTagName("row");
        List<List<String>> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rowNodes.getLength(); rowIndex++) {
            Element rowNode = (Element) rowNodes.item(rowIndex);
            NodeList cellNodes = rowNode.getElementsByTagName("c");
            List<String> row = new ArrayList<>();
            for (int cellIndex = 0; cellIndex < cellNodes.getLength(); cellIndex++) {
                Element cell = (Element) cellNodes.item(cellIndex);
                int columnIndex = columnIndex(cell.getAttribute("r"), row.size());
                while (row.size() < columnIndex) {
                    row.add("");
                }
                row.add(xlsxCellValue(cell, sharedStrings));
            }
            if (!isBlankRow(row)) {
                rows.add(row);
            }
        }
        return rows;
    }

    private static Document parseXml(byte[] content, String label) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(content));
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new IllegalArgumentException("Could not parse Excel " + label);
        }
    }

    private static String xlsxCellValue(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        if ("s".equals(type)) {
            String rawIndex = firstText(cell, "v");
            if (rawIndex.isBlank()) {
                return "";
            }
            int sharedIndex = Integer.parseInt(rawIndex);
            return sharedIndex >= 0 && sharedIndex < sharedStrings.size() ? sharedStrings.get(sharedIndex) : "";
        }
        if ("inlineStr".equals(type)) {
            return textFromDescendants(cell, "t");
        }
        return firstText(cell, "v");
    }

    private static int columnIndex(String cellReference, int fallback) {
        if (cellReference == null || cellReference.isBlank()) {
            return fallback;
        }
        int result = 0;
        for (int index = 0; index < cellReference.length(); index++) {
            char current = Character.toUpperCase(cellReference.charAt(index));
            if (current < 'A' || current > 'Z') {
                break;
            }
            result = (result * 26) + (current - 'A' + 1);
        }
        return result == 0 ? fallback : result - 1;
    }

    private static String firstText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
    }

    private static String textFromDescendants(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < nodes.getLength(); index++) {
            text.append(nodes.item(index).getTextContent());
        }
        return text.toString();
    }

    private static void requireHeaders(Map<String, Integer> headers, List<String> requiredHeaders) {
        List<String> missing = requiredHeaders.stream()
                .filter(header -> !headers.containsKey(header))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Inventory template is missing columns: " + String.join(", ", missing));
        }
    }

    private static void requireAnyHeader(Map<String, Integer> headers, List<String> aliases, String label) {
        boolean present = aliases.stream().anyMatch(headers::containsKey);
        if (!present) {
            throw new IllegalArgumentException("Inventory template is missing column: " + label);
        }
    }

    private static String required(List<String> row, Map<String, Integer> headers, String header, String label) {
        String value = value(row, headers, header);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private static String requiredAny(List<String> row, Map<String, Integer> headers, String label, String... aliases) {
        for (String alias : aliases) {
            String value = value(row, headers, alias);
            if (!value.isBlank()) {
                return value.trim();
            }
        }
        throw new IllegalArgumentException(label + " is required");
    }

    private static int parseInt(String value, String label) {
        try {
            return new BigDecimal(value.replace(",", "").trim()).intValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a whole number");
        }
    }

    private static BigDecimal parseMoney(String value, String label) {
        try {
            String numeric = value
                    .replaceAll("(?i)inr|rs\\.?", "")
                    .replace("₹", "")
                    .replace("$", "")
                    .replace(",", "")
                    .trim();
            return new BigDecimal(numeric);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a valid number");
        }
    }

    private static String csvRow(List<String> cells) {
        return cells.stream()
                .map(InventoryService::escapeCsv)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private static String escapeCsv(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static Map<String, Integer> headers(List<String> headerRow) {
        Map<String, Integer> headers = new LinkedHashMap<>();
        for (int index = 0; index < headerRow.size(); index++) {
            headers.put(normalizeHeader(headerRow.get(index)), index);
        }
        return headers;
    }

    private static String normalizeHeader(String header) {
        return header == null ? "" : header.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String value(List<String> row, Map<String, Integer> headers, String header) {
        Integer index = headers.get(header);
        if (index == null || index >= row.size()) {
            return "";
        }
        return row.get(index).trim();
    }

    private static boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(cell -> cell == null || cell.isBlank());
    }

    private static List<List<String>> parseCsv(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        String text = content == null ? "" : content.replace("\uFEFF", "");
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (quoted) {
                if (current == '"') {
                    if (index + 1 < text.length() && text.charAt(index + 1) == '"') {
                        cell.append('"');
                        index++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(current);
                }
                continue;
            }
            if (current == '"') {
                quoted = true;
            } else if (current == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (current == '\n') {
                row.add(cell.toString());
                rows.add(row);
                row = new ArrayList<>();
                cell.setLength(0);
            } else if (current != '\r') {
                cell.append(current);
            }
        }
        row.add(cell.toString());
        if (!isBlankRow(row)) {
            rows.add(row);
        }
        return rows;
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private InventoryResponse toResponse(InventoryItem item) {
        return new InventoryResponse(
                item.getId(),
                item.getSku(),
                item.getName(),
                item.getCategory(),
                item.getSupplier(),
                item.getQuantity(),
                item.getReorderPoint(),
                item.getUnitCost(),
                item.getRetailPrice(),
                item.getLeadTimeDays(),
                item.getLocation(),
                item.getNotes(),
                item.stockStatus(),
                item.suggestedOrderQuantity(),
                item.inventoryValue(),
                item.retailValue(),
                item.getLastRestockDate(),
                item.getLastRestockQuantity(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
