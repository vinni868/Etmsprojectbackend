package com.lms.controller;

import com.lms.entity.ScheduledClass;
import com.lms.service.ScheduledClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class ScheduledClassController {

    @Autowired
    private ScheduledClassService scheduledClassService;

    @GetMapping("/schedule-classes")
    public List<ScheduledClass> getAll() {
        return scheduledClassService.getAllScheduledClasses();
    }

    @PostMapping("/schedule-classes")
    public ScheduledClass create(@RequestBody ScheduledClass scheduledClass) throws Exception {
        return scheduledClassService.scheduleClass(scheduledClass);
    }
}