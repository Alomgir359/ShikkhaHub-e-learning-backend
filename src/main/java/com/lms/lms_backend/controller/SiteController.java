package com.lms.lms_backend.controller;

import com.lms.lms_backend.service.SiteSettingService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Public, read-only site content for the React app. */
@RestController
@RequestMapping("/api/site")
@CrossOrigin(origins = "http://localhost:3000")
public class SiteController {

    private final SiteSettingService settings;

    public SiteController(SiteSettingService settings) {
        this.settings = settings;
    }

    private String mediaUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/site/media/").path(fileName).toUriString();
    }

    // Landing page video section
    @GetMapping("/landing-video")
    public ResponseEntity<Map<String, Object>> landingVideo() {
        Map<String, Object> res = new HashMap<>();
        boolean enabled = !"false".equalsIgnoreCase(settings.get(SiteSettingService.VIDEO_ENABLED, "true"));
        String source = settings.get(SiteSettingService.VIDEO_SOURCE, "UPLOAD");
        String url = "LINK".equalsIgnoreCase(source)
                ? settings.get(SiteSettingService.VIDEO_LINK)
                : mediaUrl(settings.get(SiteSettingService.VIDEO_FILE));

        res.put("enabled", enabled && url != null && !url.isBlank());
        res.put("source", source);
        res.put("videoUrl", url);
        res.put("posterUrl", mediaUrl(settings.get(SiteSettingService.VIDEO_POSTER)));
        res.put("title", settings.get(SiteSettingService.VIDEO_TITLE, "ShikkhaHub-এ কীভাবে শেখানো হয়, দেখে নিন"));
        res.put("subtitle", settings.get(SiteSettingService.VIDEO_SUBTITLE,
                "মুখস্থ নয়, বোঝা — এক মিনিটে আমাদের শেখানোর পদ্ধতি দেখুন।"));
        return ResponseEntity.ok(res);
    }

    // bKash / Nagad numbers shown in the enrollment instructions
    @GetMapping("/payment-info")
    public ResponseEntity<Map<String, Object>> paymentInfo() {
        Map<String, Object> res = new HashMap<>();
        res.put("bkashNumber", settings.get(SiteSettingService.BKASH_NUMBER));
        res.put("nagadNumber", settings.get(SiteSettingService.NAGAD_NUMBER));
        res.put("note", settings.get(SiteSettingService.PAYMENT_NOTE,
                "অবশ্যই \"Send Money\" করবেন, \"Payment\" বা \"Cash Out\" নয়। পেমেন্ট যাচাই হতে সাধারণত ২–১২ ঘণ্টা সময় লাগে।"));
        res.put("supportPhone", settings.get(SiteSettingService.SUPPORT_PHONE));
        return ResponseEntity.ok(res);
    }

    // Streams uploaded site media (supports HTTP Range, so the video can be seeked)
    @GetMapping("/media/{fileName:.+}")
    public ResponseEntity<Resource> media(@PathVariable String fileName) {
        try {
            Path base = settings.siteDir().toAbsolutePath().normalize();
            Path file = base.resolve(fileName).normalize();
            if (!file.startsWith(base)) return ResponseEntity.badRequest().build();
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) return ResponseEntity.notFound().build();
            MediaType type = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .contentType(type)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
