package com.petshop.order.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.petshop.order.common.BusinessException;
import com.petshop.order.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public Map<String, String> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("文件大小不能超过5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("仅支持jpg、png、webp格式");
        }

        String datePath = LocalDate.now().format(DATE_FMT);
        String ext = getExtension(file.getOriginalFilename());
        String filename = IdUtil.fastSimpleUUID() + ext;

        File baseDir = new File(uploadDir).getAbsoluteFile();
        File dir = new File(baseDir, datePath);
        FileUtil.mkdir(dir);

        File dest = new File(dir, filename);
        try {
            file.transferTo(dest);
        } catch (IOException | IllegalStateException e) {
            log.error("文件写入失败: dest={}, error={}", dest.getAbsolutePath(), e.getMessage(), e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }

        String key = datePath + "/" + filename;
        String url = "/uploads/" + key;
        return Map.of("url", url, "key", key);
    }

    @Override
    public void delete(String key) {
        File baseDir = new File(uploadDir).getAbsoluteFile();
        File file = new File(baseDir, key);
        try {
            if (!file.getCanonicalPath().startsWith(baseDir.getCanonicalPath() + File.separator)) {
                throw new BusinessException("非法文件路径");
            }
        } catch (IOException e) {
            throw new BusinessException("非法文件路径");
        }
        if (file.exists()) {
            FileUtil.del(file);
        }
    }

    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }
}
