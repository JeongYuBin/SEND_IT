package com.sendit.user;

import com.sendit.collection.ResourceNotFoundException;
import com.sendit.share.MediaStorageCleaner;
import com.sendit.share.SharedContent;
import com.sendit.share.SharedContentRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserProfileService {
    private final UserRepository users;
    private final SharedContentRepository sharedContents;
    private final MediaStorageCleaner mediaStorageCleaner;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(
            UserRepository users,
            SharedContentRepository sharedContents,
            MediaStorageCleaner mediaStorageCleaner,
            PasswordEncoder passwordEncoder
    ) {
        this.users = users;
        this.sharedContents = sharedContents;
        this.mediaStorageCleaner = mediaStorageCleaner;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileDtos.Response get(String email) {
        return response(owned(email));
    }

    public UserProfileDtos.Response update(
            String email,
            UserProfileDtos.UpdateRequest request
    ) {
        User user = owned(email);
        user.updateNickname(request.nickname().trim());
        return response(user);
    }

    public void delete(String email, UserProfileDtos.DeleteRequest request) {
        User user = owned(email);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        List<String> mediaKeys = new ArrayList<>();
        for (SharedContent content
                : sharedContents.findAllByUserEmailOrderByCreatedAtDesc(email)) {
            mediaKeys.add(content.getMediaStorageKey());
            mediaKeys.add(content.getMediaAudioStorageKey());
            mediaKeys.addAll(content.getMediaFrameKeys());
        }
        users.delete(user);
        users.flush();
        mediaStorageCleaner.deleteAll(mediaKeys);
    }

    private User owned(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private UserProfileDtos.Response response(User user) {
        return new UserProfileDtos.Response(
                user.getId(), user.getUsername(), user.getEmail(), user.getNickname());
    }
}
