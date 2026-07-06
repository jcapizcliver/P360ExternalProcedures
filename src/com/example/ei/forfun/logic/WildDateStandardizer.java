package com.example.ei.forfun.logic;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WildDateStandardizer {

    public enum AmbiguityPolicy {
        STRICT_UNIQUE,
        PREFER_DMY,
        PREFER_MDY,
        PREFER_YMD
    }

    public record ProfileResult(
        String raw,
        String cleaned,
        String structuralMask,
        String compactStructuralMask,
        String semanticMask,
        boolean interpretable,
        boolean ambiguous,
        String normalized,
        List<String> possibleNormalizedValues,
        List<String> possibleInterpretations,
        double confidence,
        String note
    ) {}

    public record MaskSummary(
        String semanticMask,
        String compactStructuralMask,
        int total,
        int interpretable,
        int ambiguous,
        int notInterpretable,
        List<String> examples
    ) {}

    public record BatchSummary(
        int total,
        int interpretable,
        int ambiguous,
        int notInterpretable,
        List<MaskSummary> masks
    ) {}

    private enum TokenType {
        NUMBER,
        WORD,
        MONTH,
        WEEKDAY,
        TIME,
        AMPM,
        OFFSET,
        TIMEZONE_WORD,
        SEPARATOR
    }

    private record Token(
        TokenType type,
        String text,
        String normalized,
        Integer number,
        int tokenIndex
    ) {}

    private record DateCandidate(
        int year,
        int month,
        int day,
        TimeParts time,
        String interpretation,
        double score,
        String note
    ) {
        LocalDateTime toLocalDateTime() {
            TimeParts safeTime = time == null ? TimeParts.midnight() : time;

            return LocalDateTime.of(
                year,
                month,
                day,
                safeTime.hour,
                safeTime.minute,
                safeTime.second,
                safeTime.nano
            );
        }
    }

    private record ParseDecision(
        boolean interpretable,
        boolean ambiguous,
        DateCandidate chosen,
        List<DateCandidate> candidates,
        double confidence,
        String note
    ) {}

    private record TimeParts(int hour, int minute, int second, int nano) {
        static TimeParts midnight() {
            return new TimeParts(0, 0, 0, 0);
        }
    }

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Mexico_City");

    private static final DateTimeFormatter OUTPUT_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "[+-]\\d{2}:?\\d{2}|\\d{1,2}:\\d{2}(?::\\d{2})?(?:[.,]\\d{1,9})?|\\d+|[\\p{L}.]+|\\S"
    );

    private static final Map<String, Integer> MONTHS = buildMonths();
    private static final Map<String, Boolean> WEEKDAYS = buildWeekdays();

    private WildDateStandardizer() {}

    public static ProfileResult analyze(String raw) {
        return analyze(raw, DEFAULT_ZONE, AmbiguityPolicy.STRICT_UNIQUE);
    }

    public static ProfileResult analyze(String raw, ZoneId zone, AmbiguityPolicy ambiguityPolicy) {
        String cleaned = clean(raw);
        String structuralMask = structuralMask(cleaned);
        String compactStructuralMask = compactMask(structuralMask);
        List<Token> tokens = tokenize(cleaned);
        String semanticMask = semanticMask(tokens);

        ParseDecision decision = parse(cleaned, tokens, zone, ambiguityPolicy);

        if (decision.interpretable()) {
            String normalized = decision.chosen() == null
                ? null
                : OUTPUT_FORMATTER.format(decision.chosen().toLocalDateTime());

            return new ProfileResult(
                raw,
                cleaned,
                structuralMask,
                compactStructuralMask,
                semanticMask,
                true,
                decision.ambiguous(),
                normalized,
                normalizedValues(decision.candidates()),
                interpretations(decision.candidates()),
                decision.confidence(),
                decision.note()
            );
        }

        return new ProfileResult(
            raw,
            cleaned,
            structuralMask,
            compactStructuralMask,
            semanticMask,
            false,
            decision.ambiguous(),
            null,
            normalizedValues(decision.candidates()),
            interpretations(decision.candidates()),
            decision.confidence(),
            decision.note()
        );
    }

    public static Optional<String> normalize(String raw) {
        return normalize(raw, DEFAULT_ZONE, AmbiguityPolicy.STRICT_UNIQUE);
    }

    public static Optional<String> normalize(String raw, ZoneId zone, AmbiguityPolicy ambiguityPolicy) {
        ProfileResult result = analyze(raw, zone, ambiguityPolicy);

        if (!result.interpretable() || result.normalized() == null) {
            return Optional.empty();
        }

        return Optional.of(result.normalized());
    }

    public static String normalizeOrNull(String raw) {
        return normalize(raw).orElse(null);
    }

    public static BatchSummary profileAll(Collection<String> values) {
        return profileAll(values, DEFAULT_ZONE, AmbiguityPolicy.STRICT_UNIQUE);
    }

    public static BatchSummary profileAll(
        Collection<String> values,
        ZoneId zone,
        AmbiguityPolicy ambiguityPolicy
    ) {
        Map<String, Accumulator> grouped = new LinkedHashMap<>();

        int total = 0;
        int interpretable = 0;
        int ambiguous = 0;

        for (String value : values) {
            total++;

            ProfileResult profile = analyze(value, zone, ambiguityPolicy);

            if (profile.interpretable()) {
                interpretable++;
            }

            if (profile.ambiguous()) {
                ambiguous++;
            }

            String key = profile.semanticMask() + " || " + profile.compactStructuralMask();
            Accumulator acc = grouped.computeIfAbsent(key, ignored -> new Accumulator());

            acc.semanticMask = profile.semanticMask();
            acc.compactStructuralMask = profile.compactStructuralMask();
            acc.total++;

            if (profile.interpretable()) {
                acc.interpretable++;
            }

            if (profile.ambiguous()) {
                acc.ambiguous++;
            }

            if (acc.examples.size() < 5) {
                acc.examples.add(profile.cleaned());
            }
        }

        List<MaskSummary> summaries = new ArrayList<>();

        for (Accumulator acc : grouped.values()) {
            summaries.add(new MaskSummary(
                acc.semanticMask,
                acc.compactStructuralMask,
                acc.total,
                acc.interpretable,
                acc.ambiguous,
                acc.total - acc.interpretable,
                List.copyOf(acc.examples)
            ));
        }

        summaries.sort(
            Comparator.comparingInt(MaskSummary::total)
                .reversed()
                .thenComparing(MaskSummary::semanticMask)
        );

        return new BatchSummary(
            total,
            interpretable,
            ambiguous,
            total - interpretable,
            summaries
        );
    }

    private static ParseDecision parse(
        String cleaned,
        List<Token> tokens,
        ZoneId zone,
        AmbiguityPolicy ambiguityPolicy
    ) {
        if (cleaned == null || cleaned.isBlank()) {
            return new ParseDecision(false, false, null, List.of(), 0.0, "Valor vacío.");
        }

        List<DateCandidate> candidates = new ArrayList<>();

        candidates.addAll(epochCandidates(cleaned, tokens, zone));
        candidates.addAll(namedMonthCandidates(tokens));
        candidates.addAll(compactNumericCandidates(cleaned, tokens));
        candidates.addAll(looseNumericCandidates(tokens));

        List<DateCandidate> valid = uniqueTopCandidates(candidates);

        if (valid.isEmpty()) {
            return new ParseDecision(
                false,
                false,
                null,
                List.of(),
                0.0,
                "No se pudo reconstruir una fecha confiable con los tokens encontrados."
            );
        }

        return choose(valid, ambiguityPolicy);
    }

    private static ParseDecision choose(List<DateCandidate> candidates, AmbiguityPolicy policy) {
        List<DateCandidate> uniqueDateTimes = uniqueByNormalizedValue(candidates);

        if (uniqueDateTimes.size() == 1) {
            DateCandidate chosen = uniqueDateTimes.get(0);

            return new ParseDecision(
                true,
                false,
                chosen,
                uniqueDateTimes,
                clampConfidence(chosen.score()),
                chosen.note()
            );
        }

        if (policy == AmbiguityPolicy.STRICT_UNIQUE) {
            return new ParseDecision(
                true,
                true,
                null,
                uniqueDateTimes,
                0.50,
                "Fecha interpretable pero ambigua; no se normalizó porque AmbiguityPolicy=STRICT_UNIQUE."
            );
        }

        String preferred = switch (policy) {
            case PREFER_DMY -> "DMY";
            case PREFER_MDY -> "MDY";
            case PREFER_YMD -> "YMD";
            case STRICT_UNIQUE -> null;
        };

        for (DateCandidate candidate : uniqueDateTimes) {
            if (candidate.interpretation().contains(preferred)) {
                return new ParseDecision(
                    true,
                    true,
                    candidate,
                    uniqueDateTimes,
                    Math.min(clampConfidence(candidate.score()), 0.70),
                    "Fecha ambigua; se normalizó usando política " + policy + "."
                );
            }
        }

        return new ParseDecision(
            true,
            true,
            null,
            uniqueDateTimes,
            0.50,
            "Fecha interpretable pero ambigua; la política " + policy + " no resolvió el empate."
        );
    }

    private static List<DateCandidate> epochCandidates(String cleaned, List<Token> tokens, ZoneId zone) {
        List<DateCandidate> candidates = new ArrayList<>();

        if (cleaned.matches("-?\\d{10}|-?\\d{13}|-?\\d{16}|-?\\d{19}")) {
            addEpochCandidate(candidates, cleaned, zone, 1.00);
            return candidates;
        }

        List<Token> longNumbers = tokens.stream()
            .filter(token -> token.type() == TokenType.NUMBER)
            .filter(token -> token.text().matches("\\d{10}|\\d{13}|\\d{16}|\\d{19}"))
            .toList();

        if (longNumbers.size() == 1) {
            addEpochCandidate(candidates, longNumbers.get(0).text(), zone, 0.90);
        }

        return candidates;
    }

    private static void addEpochCandidate(
        List<DateCandidate> candidates,
        String value,
        ZoneId zone,
        double score
    ) {
        try {
            String abs = value.startsWith("-") ? value.substring(1) : value;
            Instant instant;
            String interpretation;

            if (abs.length() == 10) {
                instant = Instant.ofEpochSecond(Long.parseLong(value));
                interpretation = "EPOCH_SECONDS";
            } else if (abs.length() == 13) {
                instant = Instant.ofEpochMilli(Long.parseLong(value));
                interpretation = "EPOCH_MILLIS";
            } else if (abs.length() == 16) {
                long micros = Long.parseLong(value);
                instant = Instant.ofEpochSecond(
                    Math.floorDiv(micros, 1_000_000L),
                    Math.floorMod(micros, 1_000_000L) * 1_000L
                );
                interpretation = "EPOCH_MICROS";
            } else {
                long nanos = Long.parseLong(value);
                instant = Instant.ofEpochSecond(
                    Math.floorDiv(nanos, 1_000_000_000L),
                    Math.floorMod(nanos, 1_000_000_000L)
                );
                interpretation = "EPOCH_NANOS";
            }

            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, zone);

            candidates.add(new DateCandidate(
                dateTime.getYear(),
                dateTime.getMonthValue(),
                dateTime.getDayOfMonth(),
                new TimeParts(
                    dateTime.getHour(),
                    dateTime.getMinute(),
                    dateTime.getSecond(),
                    dateTime.getNano()
                ),
                interpretation,
                score,
                "Fecha reconstruida desde " + interpretation + " usando zona " + zone + "."
            ));
        } catch (RuntimeException ignored) {
            // No candidate.
        }
    }

    private static List<DateCandidate> namedMonthCandidates(List<Token> tokens) {
        List<DateCandidate> candidates = new ArrayList<>();

        List<Token> months = tokens.stream()
            .filter(t -> t.type() == TokenType.MONTH)
            .toList();

        List<Token> years = tokens.stream()
            .filter(t -> t.type() == TokenType.NUMBER)
            .filter(t -> t.number() != null)
            .filter(WildDateStandardizer::looksLikeYearInNamedMonth)
            .toList();

        List<Token> possibleDays = tokens.stream()
            .filter(t -> t.type() == TokenType.NUMBER)
            .filter(t -> t.number() != null)
            .filter(t -> t.number() >= 1 && t.number() <= 31)
            .toList();

        if (months.isEmpty() || years.isEmpty() || possibleDays.isEmpty()) {
            return candidates;
        }

        TimeParts time = findTime(tokens);
        boolean hasWeekday = tokens.stream().anyMatch(t -> t.type() == TokenType.WEEKDAY);
        boolean hasTimezoneWord = tokens.stream().anyMatch(t -> t.type() == TokenType.TIMEZONE_WORD);
        boolean hasOffset = tokens.stream().anyMatch(t -> t.type() == TokenType.OFFSET);

        for (Token monthToken : months) {
            int month = MONTHS.get(monthToken.normalized());

            for (Token yearToken : years) {
                int year = normalizeYear(yearToken.number());

                for (Token dayToken : possibleDays) {
                    int day = dayToken.number();

                    if (!isValidDate(year, month, day)) {
                        continue;
                    }

                    double distancePenalty = tokenDistancePenalty(monthToken, dayToken, yearToken);
                    double score = 0.98 - distancePenalty;
                    String note = "Fecha reconstruida por tokens con mes textual.";

                    if (hasWeekday) {
                        note += " Se ignoró el día de semana porque no es necesario para construir la fecha.";
                    }

                    if (hasTimezoneWord) {
                        score = Math.min(score, 0.88);
                        note += " Se preservó la hora escrita; abreviaturas como CST/CDT/EST son ambiguas si no traen offset explícito.";
                    }

                    if (hasOffset) {
                        score = Math.min(score, 0.90);
                        note += " Se detectó offset; el formato final no incluye zona, así que se preservó la hora local escrita.";
                    }

                    candidates.add(new DateCandidate(
                        year,
                        month,
                        day,
                        time,
                        "TEXT_MONTH",
                        score,
                        note
                    ));
                }
            }
        }

        return candidates;
    }

    private static double tokenDistancePenalty(Token month, Token day, Token year) {
        int maxDistance = Math.max(
            Math.abs(month.tokenIndex() - day.tokenIndex()),
            Math.abs(month.tokenIndex() - year.tokenIndex())
        );

        return Math.min(0.20, maxDistance * 0.02);
    }

    private static List<DateCandidate> compactNumericCandidates(String cleaned, List<Token> tokens) {
        List<DateCandidate> candidates = new ArrayList<>();
        List<String> compactValues = new ArrayList<>();

        if (cleaned.matches("\\d{8}|\\d{14}|\\d{17}")) {
            compactValues.add(cleaned);
        }

        List<Token> compactTokens = tokens.stream()
            .filter(t -> t.type() == TokenType.NUMBER)
            .filter(t -> t.text().matches("\\d{8}|\\d{14}|\\d{17}"))
            .toList();

        if (compactTokens.size() == 1 && compactValues.isEmpty()) {
            compactValues.add(compactTokens.get(0).text());
        }

        for (String value : compactValues) {
            addCompactCandidates(candidates, value);
        }

        return candidates;
    }

    private static void addCompactCandidates(List<DateCandidate> candidates, String value) {
        if (value.length() == 8) {
            int a = parseInt(value.substring(0, 4));
            int b = parseInt(value.substring(4, 6));
            int c = parseInt(value.substring(6, 8));

            addCandidateIfValid(
                candidates,
                a,
                b,
                c,
                TimeParts.midnight(),
                "COMPACT_YMD",
                0.90,
                "Fecha compacta interpretada como yyyyMMdd."
            );

            int x = parseInt(value.substring(0, 2));
            int y = parseInt(value.substring(2, 4));
            int z = parseInt(value.substring(4, 8));

            addCandidateIfValid(
                candidates,
                normalizeYear(z),
                y,
                x,
                TimeParts.midnight(),
                "COMPACT_DMY",
                0.72,
                "Fecha compacta interpretable como ddMMyyyy."
            );

            addCandidateIfValid(
                candidates,
                normalizeYear(z),
                x,
                y,
                TimeParts.midnight(),
                "COMPACT_MDY",
                0.72,
                "Fecha compacta interpretable como MMddyyyy."
            );

            return;
        }

        if (value.length() == 14 || value.length() == 17) {
            int year = parseInt(value.substring(0, 4));
            int month = parseInt(value.substring(4, 6));
            int day = parseInt(value.substring(6, 8));
            int hour = parseInt(value.substring(8, 10));
            int minute = parseInt(value.substring(10, 12));
            int second = parseInt(value.substring(12, 14));
            int nano = 0;

            if (value.length() == 17) {
                nano = parseInt(value.substring(14, 17)) * 1_000_000;
            }

            if (isValidTime(hour, minute, second)) {
                addCandidateIfValid(
                    candidates,
                    year,
                    month,
                    day,
                    new TimeParts(hour, minute, second, nano),
                    "COMPACT_YMD_TIME",
                    0.94,
                    "Fecha compacta interpretada como yyyyMMddHHmmss o yyyyMMddHHmmssSSS."
                );
            }
        }
    }

    private static List<DateCandidate> looseNumericCandidates(List<Token> tokens) {
        List<DateCandidate> candidates = new ArrayList<>();

        List<Token> numbers = tokens.stream()
            .filter(token -> token.type() == TokenType.NUMBER)
            .filter(token -> token.number() != null)
            .filter(token -> token.text().length() <= 4)
            .toList();

        if (numbers.size() < 3) {
            return candidates;
        }

        TimeParts time = findTime(tokens);
        boolean hasTimeZoneWord = tokens.stream().anyMatch(t -> t.type() == TokenType.TIMEZONE_WORD);
        boolean hasOffset = tokens.stream().anyMatch(t -> t.type() == TokenType.OFFSET);

        for (int i = 0; i <= numbers.size() - 3; i++) {
            Token first = numbers.get(i);
            Token second = numbers.get(i + 1);
            Token third = numbers.get(i + 2);

            addAllNumericCandidates(candidates, first, second, third, time, hasTimeZoneWord, hasOffset);
        }

        return candidates;
    }

    private static void addAllNumericCandidates(
        List<DateCandidate> candidates,
        Token first,
        Token second,
        Token third,
        TimeParts time,
        boolean hasTimeZoneWord,
        boolean hasOffset
    ) {
        int a = first.number();
        int b = second.number();
        int c = third.number();

        double baseScore = baseNumericScore(first, second, third);
        String tzNote = "";

        if (hasTimeZoneWord) {
            baseScore = Math.min(baseScore, 0.78);
            tzNote += " Se preservó la hora escrita; abreviaturas de zona son ambiguas sin offset explícito.";
        }

        if (hasOffset) {
            baseScore = Math.min(baseScore, 0.82);
            tzNote += " Se detectó offset; el formato final no incluye zona, así que se preservó la hora local escrita.";
        }

        addCandidateIfValid(
            candidates,
            normalizeYear(a),
            b,
            c,
            time,
            "YMD",
            baseScore + (first.text().length() == 4 ? 0.12 : 0.00),
            "Fecha numérica interpretable como YMD." + tzNote
        );

        addCandidateIfValid(
            candidates,
            normalizeYear(c),
            b,
            a,
            time,
            "DMY",
            baseScore + (third.text().length() == 4 ? 0.10 : 0.00),
            "Fecha numérica interpretable como DMY." + tzNote
        );

        addCandidateIfValid(
            candidates,
            normalizeYear(c),
            a,
            b,
            time,
            "MDY",
            baseScore + (third.text().length() == 4 ? 0.10 : 0.00),
            "Fecha numérica interpretable como MDY." + tzNote
        );
    }

    private static double baseNumericScore(Token first, Token second, Token third) {
        int tokenSpan = third.tokenIndex() - first.tokenIndex();
        double score = 0.72;

        if (tokenSpan <= 4) {
            score += 0.08;
        } else if (tokenSpan >= 8) {
            score -= 0.08;
        }

        if (first.text().length() == 4 || third.text().length() == 4) {
            score += 0.05;
        }

        return score;
    }

    private static void addCandidateIfValid(
        List<DateCandidate> candidates,
        int year,
        int month,
        int day,
        TimeParts time,
        String interpretation,
        double score,
        String note
    ) {
        if (year < 1000 || year > 9999) {
            return;
        }

        if (!isValidDate(year, month, day)) {
            return;
        }

        TimeParts safeTime = time == null ? TimeParts.midnight() : time;

        candidates.add(new DateCandidate(
            year,
            month,
            day,
            safeTime,
            interpretation,
            score,
            note
        ));
    }

    private static List<DateCandidate> uniqueTopCandidates(List<DateCandidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<DateCandidate> valid = candidates.stream()
            .filter(candidate -> isValidDate(candidate.year(), candidate.month(), candidate.day()))
            .sorted(Comparator.comparingDouble(DateCandidate::score).reversed())
            .toList();

        if (valid.isEmpty()) {
            return List.of();
        }

        double bestScore = valid.get(0).score();
        double threshold = bestScore - 0.12;

        List<DateCandidate> top = valid.stream()
            .filter(candidate -> candidate.score() >= threshold)
            .toList();

        return uniqueByNormalizedValue(top);
    }

    private static List<DateCandidate> uniqueByNormalizedValue(List<DateCandidate> candidates) {
        Map<String, DateCandidate> byValue = new LinkedHashMap<>();

        for (DateCandidate candidate : candidates) {
            String value = OUTPUT_FORMATTER.format(candidate.toLocalDateTime());
            DateCandidate previous = byValue.get(value);

            if (previous == null || candidate.score() > previous.score()) {
                byValue.put(value, candidate);
            }
        }

        return new ArrayList<>(byValue.values());
    }

    private static List<String> normalizedValues(List<DateCandidate> candidates) {
        Set<String> values = new LinkedHashSet<>();

        for (DateCandidate candidate : candidates) {
            values.add(OUTPUT_FORMATTER.format(candidate.toLocalDateTime()));
        }

        return List.copyOf(values);
    }

    private static List<String> interpretations(List<DateCandidate> candidates) {
        List<String> values = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (DateCandidate candidate : candidates) {
            String normalized = OUTPUT_FORMATTER.format(candidate.toLocalDateTime());
            String item = candidate.interpretation() + " => " + normalized;

            if (seen.add(item)) {
                values.add(item);
            }
        }

        return List.copyOf(values);
    }

    private static TimeParts findTime(List<Token> tokens) {
        TimeParts found = null;

        for (Token token : tokens) {
            if (token.type() == TokenType.TIME) {
                found = parseTimeParts(token.text());
                break;
            }
        }

        if (found == null) {
            return TimeParts.midnight();
        }

        Optional<Token> ampm = tokens.stream()
            .filter(token -> token.type() == TokenType.AMPM)
            .findFirst();

        if (ampm.isEmpty()) {
            return found;
        }

        int hour = found.hour();

        if (ampm.get().normalized().equals("pm") && hour >= 1 && hour <= 11) {
            hour += 12;
        } else if (ampm.get().normalized().equals("am") && hour == 12) {
            hour = 0;
        }

        return new TimeParts(hour, found.minute(), found.second(), found.nano());
    }

    private static TimeParts parseTimeParts(String raw) {
        String s = raw.replace(',', '.');
        String[] mainAndFraction = s.split("\\.", 2);
        String[] parts = mainAndFraction[0].split(":");

        int hour = parts.length > 0 ? parseInt(parts[0]) : 0;
        int minute = parts.length > 1 ? parseInt(parts[1]) : 0;
        int second = parts.length > 2 ? parseInt(parts[2]) : 0;
        int nano = 0;

        if (mainAndFraction.length == 2) {
            String fraction = mainAndFraction[1];

            if (fraction.length() > 9) {
                fraction = fraction.substring(0, 9);
            }

            while (fraction.length() < 9) {
                fraction += "0";
            }

            nano = parseInt(fraction);
        }

        if (!isValidTime(hour, minute, second)) {
            return TimeParts.midnight();
        }

        return new TimeParts(hour, minute, second, nano);
    }

    private static List<Token> tokenize(String text) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text == null ? "" : text);
        int tokenIndex = 0;

        while (matcher.find()) {
            String raw = matcher.group();
            String normalized = normalizeWord(raw);

            if (raw.matches("[+-]\\d{2}:?\\d{2}")) {
                tokens.add(new Token(TokenType.OFFSET, raw, raw, null, tokenIndex));
            } else if (raw.matches("\\d{1,2}:\\d{2}(?::\\d{2})?(?:[.,]\\d{1,9})?")) {
                tokens.add(new Token(TokenType.TIME, raw, raw, null, tokenIndex));
            } else if (raw.matches("\\d+")) {
                tokens.add(new Token(TokenType.NUMBER, raw, raw, parseSmallIntOrNull(raw), tokenIndex));
            } else if (MONTHS.containsKey(normalized)) {
                tokens.add(new Token(TokenType.MONTH, raw, normalized, null, tokenIndex));
            } else if (WEEKDAYS.containsKey(normalized)) {
                tokens.add(new Token(TokenType.WEEKDAY, raw, normalized, null, tokenIndex));
            } else if (normalized.equals("am") || normalized.equals("pm")) {
                tokens.add(new Token(TokenType.AMPM, raw, normalized, null, tokenIndex));
            } else if (raw.matches("[A-Z]{2,5}")) {
                tokens.add(new Token(TokenType.TIMEZONE_WORD, raw, raw, null, tokenIndex));
            } else if (raw.matches("[\\p{L}.]+")) {
                tokens.add(new Token(TokenType.WORD, raw, normalized, null, tokenIndex));
            } else {
                tokens.add(new Token(TokenType.SEPARATOR, raw, raw, null, tokenIndex));
            }

            tokenIndex++;
        }

        return tokens;
    }

    private static String semanticMask(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();

        for (Token token : tokens) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }

            sb.append(switch (token.type()) {
                case NUMBER -> "N" + token.text().length();
                case WORD -> "WORD";
                case MONTH -> "MONTH";
                case WEEKDAY -> "WEEKDAY";
                case TIME -> "TIME";
                case AMPM -> "AMPM";
                case OFFSET -> "OFFSET";
                case TIMEZONE_WORD -> "TZ";
                case SEPARATOR -> token.text();
            });
        }

        return sb.toString();
    }

    private static String structuralMask(String value) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (Character.isDigit(c)) {
                sb.append('9');
            } else if (Character.isLetter(c)) {
                sb.append('A');
            } else if (Character.isWhitespace(c)) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private static String compactMask(String mask) {
        if (mask == null || mask.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        char current = mask.charAt(0);
        int count = 1;

        for (int i = 1; i < mask.length(); i++) {
            char c = mask.charAt(i);

            if (c == current) {
                count++;
            } else {
                appendCompact(sb, current, count);
                current = c;
                count = 1;
            }
        }

        appendCompact(sb, current, count);

        return sb.toString();
    }

    private static void appendCompact(StringBuilder sb, char c, int count) {
        if (count == 1) {
            sb.append(c);
        } else if (c == '9' || c == 'A' || c == ' ') {
            sb.append(c).append('{').append(count).append('}');
        } else {
            sb.append(String.valueOf(c).repeat(count));
        }
    }

    private static String clean(String raw) {
        if (raw == null) {
            return "";
        }

        String s = raw
            .replace('\u00A0', ' ')
            .replace('\r', ' ')
            .replace('\n', ' ')
            .trim();

        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1).trim();
        }

        return s.replaceAll("\\s+", " ");
    }

    private static String normalizeWord(String raw) {
        return raw
            .toLowerCase(Locale.ROOT)
            .replace(".", "")
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ü", "u")
            .replace("ñ", "n");
    }

    private static boolean looksLikeYearInNamedMonth(Token token) {
        int n = token.number();

        return token.text().length() == 4 || (n >= 70 && n <= 99);
    }

    private static int normalizeYear(int n) {
        if (n >= 0 && n <= 69) {
            return 2000 + n;
        }

        if (n >= 70 && n <= 99) {
            return 1900 + n;
        }

        return n;
    }

    private static boolean isValidDate(int year, int month, int day) {
        try {
            LocalDate.of(year, month, day);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isValidTime(int hour, int minute, int second) {
        return hour >= 0 && hour <= 23
            && minute >= 0 && minute <= 59
            && second >= 0 && second <= 59;
    }

    private static int parseInt(String value) {
        return Integer.parseInt(value);
    }

    private static Integer parseSmallIntOrNull(String value) {
        if (value == null || value.length() > 9) {
            return null;
        }

        return Integer.parseInt(value);
    }

    private static double clampConfidence(double value) {
        if (value < 0.0) {
            return 0.0;
        }

        if (value > 1.0) {
            return 1.0;
        }

        return value;
    }

    private static Map<String, Integer> buildMonths() {
        Map<String, Integer> m = new HashMap<>();

        putMonth(m, 1, "jan", "january", "ene", "enero");
        putMonth(m, 2, "feb", "february", "febrero");
        putMonth(m, 3, "mar", "march", "marzo");
        putMonth(m, 4, "apr", "april", "abr", "abril");
        putMonth(m, 5, "may", "mayo");
        putMonth(m, 6, "jun", "june", "junio");
        putMonth(m, 7, "jul", "july", "julio");
        putMonth(m, 8, "aug", "august", "ago", "agosto");
        putMonth(m, 9, "sep", "sept", "september", "septiembre");
        putMonth(m, 10, "oct", "october", "octubre");
        putMonth(m, 11, "nov", "november", "noviembre");
        putMonth(m, 12, "dec", "december", "dic", "diciembre");

        return m;
    }

    private static void putMonth(Map<String, Integer> map, int month, String... names) {
        for (String name : names) {
            map.put(normalizeWord(name), month);
        }
    }

    private static Map<String, Boolean> buildWeekdays() {
        Map<String, Boolean> m = new HashMap<>();

        for (String value : List.of(
            "mon", "monday", "lun", "lunes",
            "tue", "tuesday", "mar", "martes",
            "wed", "wednesday", "mie", "miercoles",
            "thu", "thursday", "jue", "jueves",
            "fri", "friday", "vie", "viernes",
            "sat", "saturday", "sab", "sabado",
            "sun", "sunday", "dom", "domingo"
        )) {
            m.put(normalizeWord(value), true);
        }

        return m;
    }

    private static final class Accumulator {
        String semanticMask;
        String compactStructuralMask;
        int total;
        int interpretable;
        int ambiguous;
        final List<String> examples = new ArrayList<>();
    }
}