package org.dromara.apply.controller.app;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.apply.util.AppLoginHelper;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 小程序端 - 图片上传（本地存储，通过 /app-file/** 访问）
 *
 * @author want-do-apply
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/file")
public class AppFileController {

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${apply.upload-dir:upload/app}")
    private String uploadDir;

    @Value("${apply.upload-base-url:}")
    private String baseUrl;

    /**
     * 上传图片
     *
     * @param file 文件
     * @return url / name / size
     */
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestPart("file") MultipartFile file) {
        AppLoginHelper.requireUserId();
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要上传的图片");
        }
        String original = file.getOriginalFilename() == null ? "image.jpg" : file.getOriginalFilename();
        String ext = "";
        int idx = original.lastIndexOf('.');
        if (idx > -1) {
            ext = original.substring(idx + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED.contains(ext)) {
            throw new ServiceException("只支持 jpg / png / gif / webp 格式");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new ServiceException("图片不能超过 10MB");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadDir, datePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("保存上传文件失败", e);
            throw new ServiceException("上传失败，请稍后再试");
        }

        String relative = "/app-file/" + datePath + "/" + fileName;
        String url = baseUrl == null || baseUrl.isBlank() ? relative : baseUrl + relative;

        Map<String, Object> result = new HashMap<>(4);
        result.put("url", url);
        result.put("name", original);
        result.put("size", file.getSize());
        return R.ok(result);
    }
}
