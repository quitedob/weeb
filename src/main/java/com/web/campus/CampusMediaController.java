package com.web.campus;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/campus")
public class CampusMediaController {
    private final CampusMediaService media;
    public CampusMediaController(CampusMediaService media) { this.media = media; }

    @PostMapping(value = "/schools/{schoolId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> upload(@Userid Long actor, @PathVariable long schoolId,
                                                  @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.success(media.upload(actor(actor), schoolId, file));
    }
    @GetMapping("/media/{id}")
    public ResponseEntity<byte[]> read(@Userid Long actor, @PathVariable String id) throws IOException {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff").header("Vary", "Authorization")
                .header("Content-Disposition", "inline; filename=campus.png").body(media.read(actor(actor), id));
    }
    @DeleteMapping("/media/{id}")
    public ApiResponse<Map<String, Boolean>> delete(@Userid Long actor, @PathVariable String id) {
        media.delete(actor(actor), id);
        return ApiResponse.success(Map.of("success", true));
    }

    private static long actor(Long actor) {
        if (actor == null || actor <= 0) throw new CampusException(401, "请先登录");
        return actor;
    }
}
