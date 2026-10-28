package com.uss.service;

import com.uss.bindings.ChangePasswordRequest;
import com.uss.bindings.UpdateProfileRequest;
import com.uss.bindings.UserProfileResponse;
import com.uss.models.User;
import com.uss.repository.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

@Service
public class ProfileService {
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final ProfilePictureStorage pictureStorage;

    public ProfileService(UserRepo userRepo, PasswordEncoder passwordEncoder,
                          ProfilePictureStorage pictureStorage) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.pictureStorage = pictureStorage;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User user) {
        return response(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        if (!user.getUsername().equalsIgnoreCase(request.username())
                && userRepo.existsByUsernameIgnoreCase(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username is already taken");
        }
        user.setUsername(request.username());
        return response(userRepo.save(user));
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password must differ from current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepo.save(user);
    }

    @Transactional
    public void uploadProfilePicture(User user, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Multipart field 'file' is required");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Profile picture must be 5 MB or smaller");
        }
        String extension = imageExtension(file.getContentType());
        if (extension == null) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PNG, JPEG, and GIF images are supported");
        }
        byte[] content = file.getBytes();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
        if (image == null || !matchesSignature(content, extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Uploaded file is not a valid image");
        }
        String oldKey = user.getProfilePictureKey();
        String newKey = pictureStorage.store(content, extension);
        user.setProfilePictureKey(newKey);
        userRepo.save(user);
        if (oldKey != null) {
            pictureStorage.delete(oldKey);
        }
    }

    @Transactional(readOnly = true)
    public ProfilePictureStorage.StoredPicture getProfilePicture(User user) throws IOException {
        if (user.getProfilePictureKey() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Profile picture not found");
        }
        return pictureStorage.load(user.getProfilePictureKey());
    }

    @Transactional
    public void deleteProfilePicture(User user) throws IOException {
        String key = user.getProfilePictureKey();
        if (key == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Profile picture not found");
        }
        pictureStorage.delete(key);
        user.setProfilePictureKey(null);
        userRepo.save(user);
    }

    public User findAuthenticatedUser(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
    }

    private UserProfileResponse response(User user) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getUsername(),
                user.getProfilePictureKey() != null);
    }

    private String imageExtension(String contentType) {
        if (contentType == null) {
            return null;
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            default -> null;
        };
    }

    private boolean matchesSignature(byte[] content, String extension) {
        return switch (extension) {
            case "png" -> content.length >= 8 && content[0] == (byte) 0x89
                    && content[1] == 0x50 && content[2] == 0x4e && content[3] == 0x47
                    && content[4] == 0x0d && content[5] == 0x0a
                    && content[6] == 0x1a && content[7] == 0x0a;
            case "jpg" -> content.length >= 3 && content[0] == (byte) 0xff
                    && content[1] == (byte) 0xd8 && content[2] == (byte) 0xff;
            case "gif" -> content.length >= 6 && content[0] == 'G' && content[1] == 'I'
                    && content[2] == 'F' && content[3] == '8'
                    && (content[4] == '7' || content[4] == '9') && content[5] == 'a';
            default -> false;
        };
    }
}
