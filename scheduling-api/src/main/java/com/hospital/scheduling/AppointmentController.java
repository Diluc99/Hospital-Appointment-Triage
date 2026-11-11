
package com.hospital.scheduling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AtomicLong bookingCounter = new AtomicLong(1000);

    // Request for checking appointment availability
    public record AvailabilityRequest(
            String patientReference,
            String appointmentPreference,
            String urgencyIndicator
    ) {}

    // Request for booking an appointment
    public record BookingRequest(
            String patientReference,
            String slotId,
            String urgencyCategory
    ) {}

    // Response for availability
    public record AvailabilityResponse(
            boolean available,
            List<AppointmentSlot> slots
    ) {}

    public record AppointmentSlot(
            String slotId,
            String appointmentTime
    ) {}

    // Response for booking
    public record BookingResponse(
            String bookingStatus,
            String bookingReference
    ) {}

    // Check appointment availability
    @PostMapping("/availability")
    public AvailabilityResponse checkAvailability(
            @RequestBody AvailabilityRequest request) {

        if (request.patientReference() == null ||
                request.patientReference().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Patient reference is required"
            );
        }

        // Simulate scheduling API failure
        if ("API_FAILURE".equalsIgnoreCase(
                request.appointmentPreference())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Scheduling service temporarily unavailable"
            );
        }

        // Simulate no available appointment slots
        if ("NO_SLOTS".equalsIgnoreCase(
                request.appointmentPreference())) {
            return new AvailabilityResponse(false, List.of());
        }

        // Normal availability response
        List<AppointmentSlot> slots = List.of(
                new AppointmentSlot("SLOT-001", "09:00 AM"),
                new AppointmentSlot("SLOT-002", "11:00 AM"),
                new AppointmentSlot("SLOT-003", "02:00 PM")
        );

        return new AvailabilityResponse(true, slots);
    }

    // Book an appointment
    @PostMapping("/book")
    public BookingResponse bookAppointment(
            @RequestBody BookingRequest request) {

        if (request.patientReference() == null ||
                request.patientReference().isBlank() ||
                request.slotId() == null ||
                request.slotId().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Patient reference and slot ID are required"
            );
        }

        String bookingReference =
                "HOSP-" + bookingCounter.incrementAndGet();

        return new BookingResponse(
                "CONFIRMED",
                bookingReference
        );
    }
}
