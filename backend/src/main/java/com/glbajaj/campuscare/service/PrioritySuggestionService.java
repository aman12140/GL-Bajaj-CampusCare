package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.dto.IssueDtos.PrioritySuggestion;
import com.glbajaj.campuscare.entity.Category;
import com.glbajaj.campuscare.entity.Priority;
import com.glbajaj.campuscare.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * RULE-BASED priority suggestion. This is NOT artificial intelligence: it is a simple keyword scoring system
 * that anyone can read and explain. The student/admin can always override the suggestion.
 *
 * Rules (highest matching level wins):
 *   CRITICAL - safety words (fire, sparks, shock, flooding, gas leak, collapse ...)
 *   HIGH     - outage words (no water, power cut, not working) combined with a wide scope (all, multiple, whole ...)
 *              or a hard outage phrase ("no internet", "no power" ...)
 *   MEDIUM   - something is broken / leaking / not working, or a shared-facility category
 *   LOW      - everything else (minor, cosmetic, requests)
 */
@Service
public class PrioritySuggestionService {
    // Whole-word matching (regex \b) is used so that e.g. "firewall" does not trigger the safety word "fire".
    private static final List<String> SAFETY = List.of("fire", "sparks", "sparking", "spark", "short circuit", "short-circuit",
            "electric shock", "electrocuted", "electrocution", "shock", "shocks", "smoke", "burning", "burnt smell", "gas leak",
            "flood", "flooded", "flooding", "exposed wire", "live wire", "collapse", "collapsed", "falling ceiling", "injury",
            "injured", "emergency", "unsafe", "danger", "dangerous", "hazard", "fainted");
    private static final List<String> OUTAGE = List.of("not working", "no internet", "no wifi", "no wi-fi", "no water", "no power",
            "power cut", "blackout", "outage", "is down", "stopped working", "not available", "no electricity", "no light");
    private static final List<String> SCOPE = List.of("all", "whole", "entire", "multiple", "many", "several", "everyone",
            "every", "lab", "labs", "classroom", "classrooms", "hostel", "floor", "block", "students");
    private static final List<String> HARD_OUTAGE = List.of("no internet", "no wifi", "no wi-fi", "no water", "no power",
            "power cut", "blackout", "no electricity", "lift stuck", "stuck in lift");
    private static final List<String> BROKEN = List.of("broken", "damaged", "leak", "leaking", "leakage", "not working", "dirty",
            "blocked", "clogged", "overflow", "overflowing", "stuck", "loose", "flicker", "flickering", "noise", "smell",
            "cracked", "missing");
    private static final List<String> MINOR = List.of("minor", "small", "slight", "cosmetic", "paint", "request", "suggestion", "whenever");

    private final CategoryRepository categoryRepository;

    public PrioritySuggestionService(CategoryRepository categoryRepository) { this.categoryRepository = categoryRepository; }

    @Transactional(readOnly = true)
    public PrioritySuggestion suggest(String title, String description, Long categoryId) {
        String text = ((title == null ? "" : title) + " " + (description == null ? "" : description)).toLowerCase(Locale.ROOT);
        String categoryName = "";
        if (categoryId != null) {
            categoryName = categoryRepository.findById(categoryId).map(Category::getName).orElse("").toLowerCase(Locale.ROOT);
        }
        List<String> reasons = new ArrayList<>();

        List<String> safetyHits = hits(text, SAFETY);
        if (!safetyHits.isEmpty()) {
            reasons.add("Safety-related words found: " + String.join(", ", safetyHits));
            return result(Priority.CRITICAL, reasons);
        }
        List<String> hard = hits(text, HARD_OUTAGE);
        List<String> outage = hits(text, OUTAGE);
        List<String> scope = hits(text, SCOPE);
        if (!hard.isEmpty()) {
            reasons.add("Service outage phrase found: " + String.join(", ", hard));
            if (!scope.isEmpty()) reasons.add("Affects a wide area / many users: " + String.join(", ", scope));
            return result(Priority.HIGH, reasons);
        }
        if (!outage.isEmpty() && !scope.isEmpty()) {
            reasons.add("Something is not working (" + String.join(", ", outage) + ") and it seems to affect many users (" + String.join(", ", scope) + ")");
            return result(Priority.HIGH, reasons);
        }
        List<String> broken = hits(text, BROKEN);
        if (!broken.isEmpty()) {
            reasons.add("Fault words found: " + String.join(", ", broken));
            return result(Priority.MEDIUM, reasons);
        }
        List<String> minor = hits(text, MINOR);
        if (!minor.isEmpty()) {
            reasons.add("Looks minor: " + String.join(", ", minor));
            return result(Priority.LOW, reasons);
        }
        if (categoryName.contains("security") || categoryName.contains("medical")) {
            reasons.add("Category '" + categoryName + "' is usually time-sensitive");
            return result(Priority.MEDIUM, reasons);
        }
        reasons.add("No urgent or safety keywords found");
        return result(Priority.LOW, reasons);
    }

    private static List<String> hits(String text, List<String> words) {
        List<String> found = new ArrayList<>();
        for (String w : words) {
            if (Pattern.compile("\\b" + Pattern.quote(w) + "\\b").matcher(text).find()) found.add(w);
        }
        return found;
    }

    private static PrioritySuggestion result(Priority p, List<String> reasons) {
        return new PrioritySuggestion(p, reasons, "Rule-based suggestion (keyword rules, not AI). You can change it.");
    }
}
