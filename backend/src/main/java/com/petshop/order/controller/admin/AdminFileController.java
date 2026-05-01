package com.petshop.order.controller.admin;

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

    @PostMapping("/upload")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(fileService.upload(file));
    }

    @DeleteMapping("/{key}")
    public R<Void> delete(@PathVariable String key) {
        fileService.delete(key);
        return R.ok();
    }
}
