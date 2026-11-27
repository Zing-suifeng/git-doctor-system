package com.intern.doctor.controller;

import com.intern.doctor.common.Result;
import com.intern.doctor.dto.CheckRequest;
import com.intern.doctor.service.DoctorService;
import com.intern.doctor.vo.CheckReportVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 体检接口
 */
@RestController
@RequestMapping("/api/doctor")
@CrossOrigin(origins = {"http://localhost:5173"})
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /**
     * 发起体检：POST /api/doctor/check
     * body: {"repoUrl": "https://github.com/owner/repo"}
     */
    @PostMapping("/check")
    public Result<CheckReportVO> check(@Valid @RequestBody CheckRequest request) {
        return Result.success(doctorService.check(request.getRepoUrl()));
    }
}
