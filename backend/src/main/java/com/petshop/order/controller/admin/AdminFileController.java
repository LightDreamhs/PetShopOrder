package com.petshop.order.controller.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.petshop.order.common.BusinessException;
import com.petshop.order.common.R;
import com.petshop.order.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
public class AdminFileController {

    private final FileService fileService;

    private void checkManager() {
        if (!StpUtil.hasRole("BOSS") && !StpUtil.hasRole("MANAGER")) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    @PostMapping("/upload")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        checkManager();
        return R.ok(fileService.upload(file));
    }

    @DeleteMapping("/{key}")
    public R<Void> delete(@PathVariable String key) {
        checkManager();
        fileService.delete(key);
        return R.ok();
    }
}
