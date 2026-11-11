# Hospital Appointment and Patient Triage — Camunda 8 Lab 3

## 1. Project overview

This project demonstrates an educational hospital appointment workflow using Camunda 8. It collects synthetic registration information, validates the registration with a Java/Spring Boot job worker, evaluates an educational DMN decision table, checks appointment availability through a mock REST API, and either books a slot or routes the request to a human-review task.

> **Important safety notice:** This is an educational workflow demonstration. Its triage categories and rules are synthetic, are not clinically validated, and must not be used for real medical triage, diagnosis, or patient-care decisions. Use synthetic data only. Do not store real patient information, credentials, tokens, or secrets in this repository.

## 2. Technology and local environment

The project was developed and tested with the following local setup:

| Component | Version / setting |
|---|---|
| Camunda 8 Run | 8.9.12, normal mode |
| Camunda Modeler | 5.50.1 |
| Registration worker | Java 17, Spring Boot 3.5.8, Camunda Spring Boot starter 8.9.12 |
| Scheduling API | Java 21 runtime, Spring Boot 3.5.8; Maven compiler target Java 17 |
| Maven | 3.9.9 |
| Camunda REST endpoint | `http://localhost:8080/v2` |
| Camunda gRPC endpoint | `http://localhost:26500` |
| Operate | `http://localhost:8080/operate` |
| Tasklist | `http://localhost:8080/tasklist` |
| Scheduling API | `http://localhost:8081` |

The registration worker's `pom.xml` pins Apache HttpClient 5.6.1 to avoid the incompatible 5.5.1 runtime that produced a `NoSuchMethodError` during startup. Keep this dependency override when building the worker with the tested Camunda starter version.

## 3. Repository layout

The project root is expected to have this structure:

```text
Hospital-Appointment-Triage/
├── bpmn/
│   └── hospital-appointment.bpmn
├── dmn/
│   └── hospital-triage.dmn
├── registration-worker/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/hospital/worker/
│       │   ├── RegistrationWorkerApplication.java
│       │   └── RegistrationValidationWorker.java
│       └── resources/
│           └── application.yml
├── scheduling-api/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/hospital/scheduling/
│       │   ├── SchedulingApiApplication.java
│       │   └── AppointmentController.java
│       └── resources/
│           └── application.properties
├── connector-config/
├── test-cases/
├── screenshots/
└── README.md
```

`connector-config/` and `test-cases/` can hold exported connector notes, request/response examples, and a test log. Keep the actual BPMN, DMN, Java sources, and screenshots in the folders shown above.

## 4. Workflow outline

1. **Register patient and capture symptoms** — a Tasklist user enters synthetic process variables.
2. **Validate registration** — the Java external job worker handles job type `validate-registration` and returns validation variables.
3. **Registration gateway** — valid registrations continue; invalid or incomplete registrations follow the correction/review path.
4. **Evaluate educational triage rules** — the BPMN business-rule task evaluates the DMN decision with ID `hospital-triage` and writes the result to `triageCategory`.
5. **Triage routing** — the workflow routes Routine, Priority Review, and Urgent Assessment outcomes to the corresponding workflow path or staff task.
6. **Check appointment availability** — a REST Outbound Connector calls the mock scheduling API.
7. **Availability gateway** — available slots continue to booking; no slots route to the staff-review task.
8. **Book appointment** — a second REST Outbound Connector calls the mock booking endpoint and records the booking result.
9. **Operate monitoring and recovery** — process instances, variables, activity history, and incidents are inspected in Operate. Human tasks are handled in Tasklist.

Human tasks used in the model include registration, correction of incomplete registration, priority review, immediate human assessment, review of unclassified requests, and staff review when no suitable slots are available. These are Camunda user tasks. The local Tasklist setup used the `hospital-staff` candidate group for staff-facing tasks; the group must exist and be available to the intended Tasklist users in the runtime identity configuration.

## 5. Process variables

The following variables are used by the demonstrated workflow. Text values are JSON strings; `breathingDifficulty` is a Boolean.

| Variable | Type | Purpose |
|---|---|---|
| `patientReference` | String | Synthetic patient reference |
| `appointmentPreference` | String | Requested time/preference; mock API also recognises `NO_SLOTS` and `API_FAILURE` test controls |
| `symptomsSummary` | String | Synthetic symptoms summary collected at registration |
| `symptomsSeverity` | String | Educational DMN input, such as `Mild`, `Moderate`, or `Severe` |
| `urgencyIndicator` | String | Educational DMN/API input, such as `Routine`, `Medium`, or `High` |
| `breathingDifficulty` | Boolean | Educational DMN input (`true` or `false`) |
| `registrationValid` | Boolean | Registration worker validation outcome |
| `registrationMessage` | String | Registration validation message |
| `triageCategory` | String | DMN output: `Routine`, `Priority Review`, or `Urgent Assessment` |
| `available` | Boolean | Indicates whether the mock scheduling API returned slots |
| `availableSlots` | List of objects | Available mock appointment slots, each with `slotId` and `appointmentTime` |
| `bookingStatus` | String | Booking result, for example `CONFIRMED` |
| `bookingReference` | String | Synthetic booking reference returned by the mock API |

The registration worker validation was tested using six inputs: `patientReference`, `appointmentPreference`, `symptomsSummary`, `symptomsSeverity`, `urgencyIndicator`, and `breathingDifficulty`. A successful validation returned `registrationValid = true` and the message `Registration fields validated`.

## 6. DMN decision

- DMN file: `dmn/hospital-triage.dmn`
- Decision ID: `hospital-triage`
- Decision output variable in BPMN: `triageCategory`
- Input dimensions: `symptomsSeverity`, `urgencyIndicator`, and `breathingDifficulty`
- Output categories: `Routine`, `Priority Review`, and `Urgent Assessment`
- Hit policy: `FIRST`

The rule for the synthetic combination `Mild` + `Routine` + `false` was corrected so it returns `Routine`. The fallback rule is evaluated after the more specific rules because the table uses the `FIRST` hit policy. The rules are demonstration rules only and are not medical guidance.

The DMN file's Modeler execution metadata was updated to Camunda 8-compatible metadata (`Camunda Cloud`, version `8.2.0`) while retaining the decision ID and rules. The BPMN and DMN resources must both be deployed to the Camunda 8 cluster; deploying only the BPMN does not make an undeployed decision available at runtime.

## 7. Registration worker

The worker is a Spring Boot application in `registration-worker/`.

- Job type: `validate-registration`
- Worker registration shown in the startup log: `registrationValidationWorker#validateRegistration`
- Camunda REST address: `http://localhost:8080`
- Camunda gRPC address: `http://localhost:26500`
- Local authentication mode: `none` (local C8Run environment only)

### Build and run

Open a PowerShell window:

```powershell
cd C:\CamundaProjects\Hospital-Appointment-Triage\registration-worker
mvn clean package
mvn spring-boot:run
```

Keep this window open. A successful start includes a log entry similar to:

```text
Starting job worker: registrationValidationWorker#validateRegistration with type validate-registration
Started RegistrationWorkerApplication
```

If Maven reports an incorrect `JAVA_HOME`, set it for the current PowerShell session to an installed JDK directory. For example, the tested Adoptium JDK 21 directory on the development machine was:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Use the actual JDK installation path on the machine running the project; do not copy a path that does not exist on that machine.

## 8. Mock Scheduling API

The mock API is a Spring Boot application in `scheduling-api/`, configured to listen on port `8081`.

### Start the API

Open a separate PowerShell window:

```powershell
cd C:\CamundaProjects\Hospital-Appointment-Triage\scheduling-api
mvn clean package
mvn spring-boot:run
```

A successful startup log includes `Tomcat started on port 8081` and `Started SchedulingApiApplication`.

### 8.1 Check availability

**Endpoint**

```text
POST http://localhost:8081/api/appointments/availability
Content-Type: application/json
```

**Normal request**

```json
{
  "patientReference": "PAT-1001",
  "appointmentPreference": "MORNING",
  "urgencyIndicator": "Routine"
}
```

**Normal response**

```json
{
  "available": true,
  "slots": [
    { "slotId": "SLOT-001", "appointmentTime": "09:00 AM" },
    { "slotId": "SLOT-002", "appointmentTime": "11:00 AM" },
    { "slotId": "SLOT-003", "appointmentTime": "02:00 PM" }
  ]
}
```

**No-slots test control:** set `appointmentPreference` to `NO_SLOTS`. The API returns `available: false` and an empty `slots` array.

**Failure test control:** set `appointmentPreference` to `API_FAILURE`. The API responds with HTTP `503 Service Unavailable`. This is an intentional test-only failure switch in the mock controller.

PowerShell test:

```powershell
$body = @{
    patientReference = "PAT-1001"
    appointmentPreference = "MORNING"
    urgencyIndicator = "Routine"
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:8081/api/appointments/availability" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

### 8.2 Book an appointment

**Endpoint**

```text
POST http://localhost:8081/api/appointments/book
Content-Type: application/json
```

**Request**

```json
{
  "patientReference": "PAT-1001",
  "slotId": "SLOT-001",
  "urgencyCategory": "Routine"
}
```

**Response example**

```json
{
  "bookingStatus": "CONFIRMED",
  "bookingReference": "HOSP-1001"
}
```

The mock API generates booking references in memory. These are synthetic demonstration references, not real hospital appointment confirmations. Restarting the API resets its in-memory booking counter.

PowerShell test:

```powershell
$body = @{
    patientReference = "PAT-1001"
    slotId = "SLOT-001"
    urgencyCategory = "Routine"
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:8081/api/appointments/book" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

## 9. REST Connector configuration

The BPMN contains two REST Outbound Connector tasks.

### Availability connector

- Method: `POST`
- URL: `http://localhost:8081/api/appointments/availability`
- Header: `Content-Type: application/json`
- Request fields: `patientReference`, `appointmentPreference`, `urgencyIndicator`
- Output mapping stores `response.body.available` as `available` and `response.body.slots` as `availableSlots`.

The FEEL output expression should be entered without an extra leading `=` inside the expression editor:

```feel
{
  available: response.body.available,
  availableSlots: response.body.slots
}
```

### Booking connector

- Method: `POST`
- URL: `http://localhost:8081/api/appointments/book`
- Header: `Content-Type: application/json`
- Request fields: `patientReference`, `slotId`, `urgencyCategory`
- Output variables: `bookingStatus`, `bookingReference`

Check the connector's request and output mappings in Modeler if the API contract or process variable names are changed. In particular, make sure the booking request uses a `slotId` returned in `availableSlots`.

## 10. Deploy and run

1. Start Camunda 8 Run in normal mode from its installation directory:

   ```powershell
   c8run.exe start
   ```

2. Start the Registration Worker in its own PowerShell window.
3. Start the Scheduling API in another PowerShell window.
4. Open `bpmn/hospital-appointment.bpmn` and deploy it to the Camunda 8 Self-Managed cluster at `http://localhost:8080/v2` with authentication set to `None` for this local C8Run environment.
5. Open `dmn/hospital-triage.dmn` and deploy it to the same cluster. Verify the decision ID is `hospital-triage`.
6. Open Tasklist at `http://localhost:8080/tasklist` and start a fresh process instance.
7. Claim the registration user task and add the six registration variables. Use JSON strings for text values (including double quotes) and a JSON Boolean for `breathingDifficulty` (`true` or `false`, without quotes).
8. Complete the task and follow execution in Operate at `http://localhost:8080/operate`.

Deploy changed BPMN or DMN resources before starting a fresh instance if you want the new version to be used. Existing process instances continue with the version on which they started; redeployment does not rewrite an instance already in progress.

## 11. Test scenarios and observed results

The assignment's minimum Lab 3 scenarios are Routine with an available slot, Priority Review, Urgent Assessment, no available slots, and scheduling API failure followed by recovery.

| Scenario | Example synthetic input / trigger | Observed result |
|---|---|---|
| Routine with availability | `Mild`, `Routine`, `breathingDifficulty=false`, preference `MORNING` | Registration passed; `triageCategory=Routine`; `available=true`; three mock slots returned; booking confirmed. One observed result was patient `PAT-1005`, booking reference `HOSP-1003`. |
| Priority staff review | `Moderate`, `Medium`, `breathingDifficulty=false` | User confirmed the Priority Review path worked and its staff task was created. |
| Urgent assessment | `Severe`, `High`, `breathingDifficulty=true` | User confirmed the Urgent Assessment path worked and its human-review task was created. |
| No slots | Set `appointmentPreference=NO_SLOTS` with a valid Routine registration | API returned `available=false` and `availableSlots=[]`; Operate/Tasklist showed the staff-review task for no suitable slots. |
| Scheduling API failure | Set `appointmentPreference=API_FAILURE` | API returned HTTP 503; Operate showed an incident on the availability connector (`Job: No retries left`). |

### Failure and recovery note

The failure was reproduced and inspected in Operate. After changing the process variable to `MORNING`, the failed job continued to return HTTP 503 on retry. A direct PowerShell request to the same API with `MORNING` then returned three slots, and a subsequent fresh process instance completed booking successfully. Therefore, API recovery and successful subsequent execution were demonstrated, but successful retry of the *original* failed job instance was not conclusively verified. Do not describe the original incident as successfully recovered unless a later screenshot/log confirms that the same instance's incident cleared and proceeded.

## 12. Evidence screenshots

Save screenshots captured from the actual running implementation in `screenshots/`. Suggested filenames:

```text
screenshots/
├── 01-bpmn-process-model.png
├── 02-dmn-decision-table.png
├── 03-registration-worker-started.png
├── 04-scheduling-api-started.png
├── 05-tasklist-registration.png
├── 06-operate-routine-booking.png
├── 07-operate-priority-review.png
├── 08-operate-urgent-assessment.png
├── 09-no-available-slots-review.png
├── 10-api-failure-incident.png
└── 11-api-healthy-and-fresh-instance-recovery.png
```

Use genuine screenshots from your own Modeler, Tasklist, Operate, and terminal sessions. Do not use placeholder screenshots. The assignment also asks for test evidence, request/response examples, source code, and a concise implementation report.

## 13. Limitations and assumptions

- This is a local educational demonstration using synthetic data and a mock scheduling API.
- The DMN rules are not clinically validated and must not be used for patient care.
- The mock API does not connect to a hospital information system, calendar, clinician roster, or persistent database.
- Booking references are generated in memory and do not represent real appointments.
- The local C8Run setup uses no authentication for the worker connection and is intended for local development only. Do not expose these endpoints to an untrusted network.
- Staff task visibility depends on assignment configuration and the runtime identity's membership in the configured candidate group (for this project, `hospital-staff`).
- A completed process may not appear in Operate's default running-instance view; use the finished-instance filter/history to inspect completed instances.

## 14. Lab 3 deliverable checklist

- [ ] BPMN model saved in `bpmn/hospital-appointment.bpmn`.
- [ ] DMN model saved in `dmn/hospital-triage.dmn` and deployed with decision ID `hospital-triage`.
- [ ] Registration worker source code, `pom.xml`, and `application.yml` included.
- [ ] Scheduling API source code, `pom.xml`, and `application.properties` included.
- [ ] REST Connector settings and request/response examples documented.
- [ ] Process variables and routing explained.
- [ ] Five minimum scenarios recorded with actual outcomes.
- [ ] Tasklist screenshots show human-review tasks.
- [ ] Operate screenshots show process execution, variables, and the scheduling incident.
- [ ] Original failed-job retry outcome clearly documented; capture additional evidence if the same-instance retry is later completed.
- [ ] README and concise implementation report included.
