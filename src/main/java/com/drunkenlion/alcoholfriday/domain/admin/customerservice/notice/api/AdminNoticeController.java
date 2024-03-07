package com.drunkenlion.alcoholfriday.domain.admin.customerservice.notice.api;

import com.drunkenlion.alcoholfriday.domain.admin.customerservice.notice.dto.NoticeSaveRequest;
import com.drunkenlion.alcoholfriday.domain.admin.customerservice.notice.application.AdminNoticeService;
import com.drunkenlion.alcoholfriday.domain.admin.customerservice.notice.dto.NoticeSaveResponse;
import com.drunkenlion.alcoholfriday.global.common.response.PageResponse;
import com.drunkenlion.alcoholfriday.global.security.auth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RequiredArgsConstructor
@RequestMapping("/v1/admin")
@Tag(name = "v1-admin-notice", description = "관리자용 공지사항 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    @Operation(summary = "공지사항 상세 조회", description = "관리자 권한 - 공지사항 상세 조회")
    @GetMapping(value = "notices/{id}")
    public ResponseEntity<NoticeSaveResponse> getNotice(@PathVariable("id") Long id,
                                                        @AuthenticationPrincipal UserPrincipal user) {
        NoticeSaveResponse noticeSaveResponse = adminNoticeService.getNotice(id, user.getMember());
        return ResponseEntity.ok().body(noticeSaveResponse);
    }
}
