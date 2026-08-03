package com.sendit.user;

import com.sendit.collection.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserProfileService {
    private final UserRepository users;

    public UserProfileService(UserRepository users) {
        this.users = users;
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

    public UserProfileDtos.Response updatePreferences(
            String email,
            UserProfileDtos.PreferencesUpdateRequest request
    ) {
        User user = owned(email);
        user.updateTravelPreferences(request.preferredTransport(), request.travelWithPet());
        return response(user);
    }

    private User owned(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private UserProfileDtos.Response response(User user) {
        return new UserProfileDtos.Response(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getPreferredTransport() == null
                        ? "PUBLIC_TRANSIT"
                        : user.getPreferredTransport(),
                user.isTravelWithPet());
    }
}
