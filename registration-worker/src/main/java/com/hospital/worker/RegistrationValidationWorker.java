package com.hospital.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RegistrationValidationWorker {
    private static final Logger log =
            LoggerFactory.getLogger(RegistrationValidationWorker.class);

    @JobWorker(type = "validate-registration")
    public Map<String, Object> validateRegistration(
            @Variable String patientReference,
            @Variable String appointmentPreference,
            @Variable String symptomsSummary,
            @Variable String symptomsSeverity,
            @Variable String urgencyIndicator,
            @Variable Boolean breathingDifficulty) {

        boolean valid = hasText(patientReference)
                && hasText(appointmentPreference)
                && hasText(symptomsSummary)
                && hasText(symptomsSeverity)
                && hasText(urgencyIndicator)
                && breathingDifficulty != null;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("registrationValid", valid);
        result.put("registrationMessage", valid
                ? "Registration fields validated"
                : "One or more required registration fields are missing");

        log.info("Registration validation completed for synthetic reference '{}': {}",
                patientReference, valid);
        return result;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
