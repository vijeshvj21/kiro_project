package com.project.kiro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailParser {

    private static final Logger log = LoggerFactory.getLogger(EmailParser.class);

    // Matches Rs. / Rs / INR / ₹ followed by amount with optional commas and decimals
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "(?:Rs\\.?\\s*|INR\\s*|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        Pattern.CASE_INSENSITIVE
    );

    private static final List<String> DEBIT_KEYWORDS = List.of(
        "debited", "debit", "withdrawn", "paid", "purchase", "spent"
    );
    private static final List<String> CREDIT_KEYWORDS = List.of(
        "credited", "credit", "received", "refund"
    );

    // Date patterns
    private static final Pattern DATE_PATTERN_NUMERIC = Pattern.compile(
        "(\\d{2})[-/](\\d{2})[-/](\\d{4})"
    );
    private static final Pattern DATE_PATTERN_ALPHA = Pattern.compile(
        "(\\d{2})\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{4})",
        Pattern.CASE_INSENSITIVE
    );

    private static final ZoneId APP_TIMEZONE = ZoneId.of("Asia/Kolkata");

    /**
     * Parses a bank email and extracts transaction data.
     * Returns empty if the email is unparseable.
     */
    public Optional<ParsedTransaction> parse(EmailContent email) {
        if (email == null || email.body() == null || email.body().isBlank()) {
            return Optional.empty();
        }

        // Extract amount
        Optional<BigDecimal> amount = extractAmount(email.body());
        if (amount.isEmpty()) {
            log.debug("No parseable amount found in email");
            return Optional.empty();
        }

        // Detect transaction type
        Optional<TransactionType> type = detectTransactionType(email.body());
        if (type.isEmpty()) {
            log.debug("Could not determine transaction type");
            return Optional.empty();
        }

        LocalDate date = extractDate(email.body(), email.receivedDate());
        String merchant = extractMerchant(email.body(), email.subject());
        return Optional.of(new ParsedTransaction(
            amount.get(),
            type.get(),
            date,
            merchant
        ));
    }

    /**
     * Detects transaction type by scanning for debit/credit keywords.
     * Case-insensitive. If both types found, uses first occurrence position.
     * Returns empty if neither keyword type found.
     */
    public Optional<TransactionType> detectTransactionType(String body) {
        String lowerBody = body.toLowerCase();

        int firstDebitPos = Integer.MAX_VALUE;
        int firstCreditPos = Integer.MAX_VALUE;

        for (String keyword : DEBIT_KEYWORDS) {
            int pos = lowerBody.indexOf(keyword);
            if (pos >= 0 && pos < firstDebitPos) {
                firstDebitPos = pos;
            }
        }

        for (String keyword : CREDIT_KEYWORDS) {
            int pos = lowerBody.indexOf(keyword);
            if (pos >= 0 && pos < firstCreditPos) {
                firstCreditPos = pos;
            }
        }

        if (firstDebitPos == Integer.MAX_VALUE && firstCreditPos == Integer.MAX_VALUE) {
            return Optional.empty(); // Neither found - unparseable
        }

        if (firstDebitPos <= firstCreditPos) {
            return Optional.of(TransactionType.DEBIT);
        } else {
            return Optional.of(TransactionType.CREDIT);
        }
    }

    /**
     * Extracts the first INR amount from the email body.
     * Supports formats: Rs. 450.00, INR 1,200.50, ₹500
     * Strips commas (handles Indian grouping like 1,20,000).
     * Returns BigDecimal with scale 2 and HALF_UP rounding.
     */
    public Optional<BigDecimal> extractAmount(String body) {
        Matcher matcher = AMOUNT_PATTERN.matcher(body);
        int matchCount = 0;
        BigDecimal firstAmount = null;

        while (matcher.find()) {
            matchCount++;
            if (matchCount == 1) {
                String rawAmount = matcher.group(1).replace(",", "");
                try {
                    firstAmount = new BigDecimal(rawAmount).setScale(2, RoundingMode.HALF_UP);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse amount: {}", rawAmount);
                    return Optional.empty();
                }
            }
        }

        if (matchCount > 1) {
            log.warn("Multiple amounts detected in email body ({}), using first match", matchCount);
        }

        return Optional.ofNullable(firstAmount);
    }

    /**
     * Extracts the transaction date from email body.
     * Supports DD-MM-YYYY, DD/MM/YYYY, DD MMM YYYY.
     * Falls back to the email's received date.
     * Clamps future dates to today.
     */
    public LocalDate extractDate(String body, LocalDate receivedDate) {
        LocalDate extracted = null;

        // Try DD-MM-YYYY or DD/MM/YYYY
        Matcher numericMatcher = DATE_PATTERN_NUMERIC.matcher(body);
        if (numericMatcher.find()) {
            try {
                int day = Integer.parseInt(numericMatcher.group(1));
                int month = Integer.parseInt(numericMatcher.group(2));
                int year = Integer.parseInt(numericMatcher.group(3));
                extracted = LocalDate.of(year, month, day);
            } catch (Exception e) {
                log.debug("Failed to parse numeric date: {}", numericMatcher.group());
            }
        }

        // Try DD MMM YYYY if numeric didn't work
        if (extracted == null) {
            Matcher alphaMatcher = DATE_PATTERN_ALPHA.matcher(body);
            if (alphaMatcher.find()) {
                try {
                    String dateStr = alphaMatcher.group(1) + " " + alphaMatcher.group(2) + " " + alphaMatcher.group(3);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
                    extracted = LocalDate.parse(dateStr, formatter);
                } catch (Exception e) {
                    log.debug("Failed to parse alpha date: {}", alphaMatcher.group());
                }
            }
        }

        // Fallback to received date
        if (extracted == null) {
            extracted = receivedDate;
        }

        // Clamp future dates to today
        LocalDate today = LocalDate.now(APP_TIMEZONE);
        if (extracted != null && extracted.isAfter(today)) {
            extracted = today;
        }

        return extracted;
    }

    // Merchant extraction - uses word boundaries to avoid matching "at" within words
    // Matches: "paid to MERCHANT", "debited for MERCHANT", "at MERCHANT", "transferred to MERCHANT"
    private static final Pattern MERCHANT_PATTERN = Pattern.compile(
        "(?:(?:paid|transferred|debited|sent)\\s+(?:to|at|for)|\\bVPA\\b|\\bUPI/)\\s*([A-Za-z][A-Za-z0-9\\s&.'-]{2,40})",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern for "Info: ...merchant..." common in HDFC/ICICI emails
    private static final Pattern INFO_PATTERN = Pattern.compile(
        "(?:Info|Ref|Reference|Remarks?)\\s*[:\\-]\\s*(?:UPI/)?([A-Za-z][A-Za-z0-9\\s&.'-]{2,50})",
        Pattern.CASE_INSENSITIVE
    );

    // UPI ID pattern: "merchant@upi" or "SWIGGY@paytm" etc.
    private static final Pattern UPI_PATTERN = Pattern.compile(
        "([A-Za-z][A-Za-z0-9.]{2,30})@(?:upi|paytm|ybl|okaxis|okhdfcbank|ibl|axl|apl)",
        Pattern.CASE_INSENSITIVE
    );

    // Known merchant keywords — longer/more specific keywords first to avoid false matches
    private static final List<String> KNOWN_MERCHANTS = List.of(
        "swiggy", "zomato", "blinkit", "bigbasket", "zepto", "dunzo",
        "amazon", "flipkart", "myntra", "meesho",
        "netflix", "spotify", "hotstar", "youtube",
        "uber", "rapido", "redbus", "makemytrip",
        "phonepe", "paytm", "gpay", "google pay",
        "petrol", "indian oil", "hp petrol", "bharat petroleum",
        "ola", "metro", "jio", "airtel", "vodafone",
        "sip", "mutual fund", "mf ", "groww", "zerodha", "kuvera",
        "auto debit", "auto-debit", "mandate", "nach", "emi",
        "lic premium", "lic policy", "life insurance", "insurance premium", "ppf", "nps", "investment"
    );

    // Terms to filter out — these are UPI codes, not merchant names
    private static final List<String> NOISE_TERMS = List.of(
        "p2m", "p2p", "p2a", "upi", "neft", "imps", "rtgs", "null"
    );

    /**
     * Extracts the merchant name from the email body.
     * Strategy:
     * 1. Look for known merchant keywords directly in the body (most reliable)
     * 2. Try UPI ID pattern (merchant@upi)
     * 3. Try "paid to / debited for / transferred to" patterns
     * 4. Try "Info:/Ref:/Remarks:" patterns
     * 5. Fallback to subject line or "Bank Transaction"
     */
    public String extractMerchant(String body, String subject) {
        String merchant = null;
        String lowerBody = body.toLowerCase();

        // Strategy 1: Direct keyword search (most reliable for categorization)
        // Use word-boundary matching for short keywords (<=4 chars) to avoid false matches
        for (String keyword : KNOWN_MERCHANTS) {
            if (keyword.length() <= 4) {
                // Short keywords need word boundary matching
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "\\b" + java.util.regex.Pattern.quote(keyword) + "\\b",
                    java.util.regex.Pattern.CASE_INSENSITIVE
                );
                if (pattern.matcher(lowerBody).find()) {
                    merchant = keyword;
                    break;
                }
            } else {
                if (lowerBody.contains(keyword.toLowerCase())) {
                    merchant = keyword;
                    break;
                }
            }
        }

        // Strategy 2: Try UPI ID pattern
        if (merchant == null) {
            Matcher upiMatcher = UPI_PATTERN.matcher(body);
            if (upiMatcher.find()) {
                merchant = upiMatcher.group(1).trim();
            }
        }

        // Strategy 3: Try structured patterns ("paid to X", "transferred to X")
        if (merchant == null) {
            Matcher merchantMatcher = MERCHANT_PATTERN.matcher(body);
            if (merchantMatcher.find()) {
                merchant = merchantMatcher.group(1).trim();
                merchant = merchant.replaceAll("[.,;:!?]+$", "").trim();
            }
        }

        // Strategy 4: Try Info/Ref/Remarks pattern
        if (merchant == null) {
            Matcher infoMatcher = INFO_PATTERN.matcher(body);
            if (infoMatcher.find()) {
                merchant = infoMatcher.group(1).trim();
                merchant = merchant.replaceAll("[.,;:!?]+$", "").trim();
            }
        }

        // Strategy 5: Fallback to subject
        if (merchant == null || merchant.isBlank()) {
            if (subject != null && !subject.isBlank()) {
                merchant = subject.trim();
            }
        }

        // Final fallback
        if (merchant == null || merchant.isBlank()) {
            merchant = "Bank Transaction";
        }

        // Filter out noise terms (P2M, UPI codes, etc.)
        if (NOISE_TERMS.contains(merchant.trim().toLowerCase())) {
            merchant = "Bank Transaction";
        }

        // Normalise to title case and truncate
        merchant = toTitleCase(merchant);
        if (merchant.length() > 255) {
            merchant = merchant.substring(0, 255);
        }

        return merchant;
    }

    /**
     * Converts a string to title case (first letter of each word uppercase, rest lowercase).
     */
    private String toTitleCase(String input) {
        if (input == null || input.isBlank()) return input;

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c) || c == '-' || c == '\'') {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }
}
