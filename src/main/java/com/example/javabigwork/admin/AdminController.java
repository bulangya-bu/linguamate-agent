package com.example.javabigwork.admin;

import com.example.javabigwork.admin.AdminService.AdminNewsPage;
import com.example.javabigwork.admin.AdminService.AdminReadingRecordView;
import com.example.javabigwork.admin.AdminService.AdminSummary;
import com.example.javabigwork.admin.AdminService.AdminUserProfile;
import com.example.javabigwork.admin.AdminService.AdminUserView;
import com.example.javabigwork.admin.AdminService.AdminWordPage;
import com.example.javabigwork.admin.AdminService.NewsUpsertRequest;
import com.example.javabigwork.admin.AdminService.PasswordResetRequest;
import com.example.javabigwork.admin.AdminService.UserStatusRequest;
import com.example.javabigwork.admin.AdminService.UserUpdateRequest;
import com.example.javabigwork.admin.AdminService.WordUpsertRequest;
import com.example.javabigwork.common.ApiResponse;
import com.example.javabigwork.news.entity.NewsArticle;
import com.example.javabigwork.vocabulary.entity.Word;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/summary")
    public ApiResponse<AdminSummary> summary() {
        return ApiResponse.ok(adminService.summary());
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserView>> users(@RequestParam(defaultValue = "") String search) {
        return ApiResponse.ok(adminService.users(search));
    }

    @GetMapping("/users/{id}/profile")
    public ApiResponse<AdminUserProfile> userProfile(@PathVariable Long id) {
        return ApiResponse.ok(adminService.userProfile(id));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<AdminUserView> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok(adminService.updateUser(id, request));
    }

    @GetMapping("/users/{id}/readings")
    public ApiResponse<List<AdminReadingRecordView>> userReadings(@PathVariable Long id) {
        return ApiResponse.ok(adminService.userReadings(id));
    }

    @PostMapping("/users/{id}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long id, @RequestBody UserStatusRequest request) {
        adminService.updateUserStatus(id, request);
        return ApiResponse.ok("用户状态已更新", null);
    }

    @PostMapping("/users/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestBody PasswordResetRequest request) {
        adminService.resetPassword(id, request);
        return ApiResponse.ok("密码已重置", null);
    }

    @GetMapping("/words")
    public ApiResponse<AdminWordPage> words(
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(adminService.words(category, search, page, size));
    }

    @PostMapping("/words")
    public ApiResponse<Word> createWord(@RequestBody WordUpsertRequest request) {
        return ApiResponse.ok(adminService.createWord(request));
    }

    @PutMapping("/words/{id}")
    public ApiResponse<Word> updateWord(@PathVariable Long id, @RequestBody WordUpsertRequest request) {
        return ApiResponse.ok(adminService.updateWord(id, request));
    }

    @DeleteMapping("/words/{id}")
    public ApiResponse<Void> deleteWord(@PathVariable Long id) {
        adminService.deleteWord(id);
        return ApiResponse.ok("单词已删除", null);
    }

    @GetMapping("/news")
    public ApiResponse<AdminNewsPage> news(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(adminService.news(search, page, size));
    }

    @PostMapping("/news")
    public ApiResponse<NewsArticle> createNews(@RequestBody NewsUpsertRequest request) {
        return ApiResponse.ok(adminService.createNews(request));
    }

    @PutMapping("/news/{id}")
    public ApiResponse<NewsArticle> updateNews(@PathVariable Long id, @RequestBody NewsUpsertRequest request) {
        return ApiResponse.ok(adminService.updateNews(id, request));
    }

    @DeleteMapping("/news/{id}")
    public ApiResponse<Void> deleteNews(@PathVariable Long id) {
        adminService.deleteNews(id);
        return ApiResponse.ok("新闻已删除", null);
    }
}
