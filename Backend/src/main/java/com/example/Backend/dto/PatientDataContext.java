package com.example.Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDataContext {

    private String patientName;
    private Integer age;
    private String gender;
    private List<MedicalRecordResponse> records;
    private List<BillingResponse> billingHistory;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Serializes this context to a clean plain-text block ready for LLM consumption.
     * Returns empty string sections (not omitted) when fields are absent so the LLM
     * sees consistent structure across queries.
     */
    public String toContextString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PATIENT CLINICAL CONTEXT:\n");
        sb.append("Name: ").append(safe(patientName)).append("\n");
        sb.append("Age: ").append(age != null ? age.toString() : "N/A").append("\n");
        sb.append("Gender: ").append(safe(gender)).append("\n\n");

        sb.append("MEDICAL RECORDS:\n");
        if (records == null || records.isEmpty()) {
            sb.append("(no records on file)\n");
        } else {
            for (MedicalRecordResponse r : records) {
                sb.append("- Date: ")
                  .append(r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FMT) : "N/A")
                  .append("\n");
                sb.append("  Diagnosis: ").append(safe(r.getDiagnosis())).append("\n");
                sb.append("  Treatment: ").append(safe(r.getTreatmentPlan())).append("\n");
                sb.append("  Drugs: ").append(formatDrugs(r.getDrugs())).append("\n");
                sb.append("  Doctor: ")
                  .append(safe(r.getDoctorName()))
                  .append(r.getDoctorSpecialization() != null
                          ? " (" + r.getDoctorSpecialization() + ")"
                          : "")
                  .append("\n");
            }
        }

        sb.append("\nBILLING HISTORY:\n");
        if (billingHistory == null || billingHistory.isEmpty()) {
            sb.append("(no billing on file)\n");
        } else {
            for (BillingResponse b : billingHistory) {
                sb.append("- ₹")
                  .append(b.getTotalCost() != null ? b.getTotalCost().toPlainString() : "0")
                  .append(" — ")
                  .append(safe(b.getDescription()))
                  .append(" (")
                  .append(b.getCreatedAt() != null ? b.getCreatedAt().format(DATE_FMT) : "N/A")
                  .append(")\n");
            }
        }

        return sb.toString().trim();
    }

    private String safe(String s) {
        return s == null || s.isBlank() ? "N/A" : s;
    }

    private String formatDrugs(List<DrugDTO> drugs) {
        if (drugs == null || drugs.isEmpty()) return "none";
        return drugs.stream()
                .map(d -> d.getName() + " " + (d.getDosage() != null ? d.getDosage() : "")
                        + (d.getPurpose() != null ? " (" + d.getPurpose() + ")" : ""))
                .map(String::trim)
                .collect(Collectors.joining(", "));
    }
}