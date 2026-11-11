# Hospital Registration Worker — corrected imports

Camunda 8.9.12, Spring Boot 3 starter, Java 17+.

The key correction from the first package is the annotation package:
`io.camunda.client.annotation.JobWorker` and
`io.camunda.client.annotation.Variable`.

BPMN job type must be exactly `validate-registration`.

Inputs read:
- patientReference
- appointmentPreference
- symptomsSummary
- symptomsSeverity
- urgencyIndicator
- breathingDifficulty

Outputs:
- registrationValid
- registrationMessage

Run from this directory:
```powershell
mvn clean package
mvn spring-boot:run
```

Keep C8Run running. Local endpoints are configured for gRPC
`http://localhost:26500` and REST `http://localhost:8080`.
This is a synthetic educational validation worker, not a clinical decision system.
