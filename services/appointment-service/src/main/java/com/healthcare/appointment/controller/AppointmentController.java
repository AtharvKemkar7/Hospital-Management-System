package com.healthcare.appointment.controller;

import com.healthcare.appointment.dto.request.CancelAppointmentRequest;
import com.healthcare.appointment.dto.request.CreateAppointmentRequest;
import com.healthcare.appointment.dto.request.RescheduleAppointmentRequest;
import com.healthcare.appointment.dto.response.AppointmentResponse;
import com.healthcare.appointment.entity.Appointment;
import com.healthcare.appointment.security.CurrentPrincipalService;
import com.healthcare.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService service;
    private final CurrentPrincipalService current;

    public AppointmentController(AppointmentService service, CurrentPrincipalService current) {
        this.service = service;
        this.current = current;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest req) {
        Appointment a = service.create(current.currentUserId(), current.currentRole(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(AppointmentResponse.from(a));
    }

    @GetMapping("/me")
    public List<AppointmentResponse> me() {
        // The service enforces role-based ownership.
        var role = current.currentRole();
        List<Appointment> list = switch (role) {
            case PATIENT -> service.listMineAsPatient(current.currentUserId());
            case DOCTOR -> service.listMineAsDoctor(current.currentUserId());
            default -> List.of();
        };
        return list.stream().map(AppointmentResponse::from).toList();
    }

    @GetMapping("/doctor/me")
    public List<AppointmentResponse> doctorMe() {
        return service.listMineAsDoctor(current.currentUserId())
                .stream().map(AppointmentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AppointmentResponse getById(@PathVariable("id") UUID id) {
        return AppointmentResponse.from(
                service.getByIdAuthorized(id, current.currentUserId(), current.currentRole()));
    }

    @PatchMapping("/{id}/confirm")
    public AppointmentResponse confirm(@PathVariable("id") UUID id) {
        return AppointmentResponse.from(
                service.confirm(current.currentUserId(), current.currentRole(), id));
    }

    @PatchMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable("id") UUID id,
                                      @RequestBody(required = false) @Valid CancelAppointmentRequest req) {
        return AppointmentResponse.from(
                service.cancel(current.currentUserId(), current.currentRole(), id, req));
    }

    @PatchMapping("/{id}/complete")
    public AppointmentResponse complete(@PathVariable("id") UUID id) {
        return AppointmentResponse.from(
                service.complete(current.currentUserId(), current.currentRole(), id));
    }

    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponse> reschedule(@PathVariable("id") UUID id,
                                                          @Valid @RequestBody RescheduleAppointmentRequest req) {
        Appointment a = service.reschedule(current.currentUserId(), current.currentRole(), id, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(AppointmentResponse.from(a));
    }
}
