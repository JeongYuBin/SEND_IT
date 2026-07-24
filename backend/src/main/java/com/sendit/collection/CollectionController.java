package com.sendit.collection;

import com.sendit.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collections")
@Transactional
public class CollectionController {
    private final CollectionRepository repository;
    private final UserRepository userRepository;

    public CollectionController(CollectionRepository repository, UserRepository userRepository) {
        this.repository = repository; this.userRepository = userRepository;
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    CollectionResponse create(Principal principal, @Valid @RequestBody CollectionRequest request) {
        var user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return response(repository.save(new Collection(user, request.name(), request.description())));
    }

    @GetMapping @Transactional(readOnly = true)
    List<CollectionResponse> list(Principal principal) {
        return repository.findByUserEmailOrderByCreatedAtDesc(principal.getName())
                .stream().map(this::response).toList();
    }

    @PatchMapping("/{id}")
    CollectionResponse update(Principal principal, @PathVariable Long id,
                              @Valid @RequestBody CollectionRequest request) {
        var collection = owned(principal, id);
        collection.update(request.name(), request.description());
        return response(collection);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Principal principal, @PathVariable Long id) {
        repository.delete(owned(principal, id));
    }

    private Collection owned(Principal principal, Long id) {
        return repository.findByIdAndUserEmail(id, principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("컬렉션을 찾을 수 없습니다."));
    }
    private CollectionResponse response(Collection value) {
        return new CollectionResponse(value.getId(), value.getName(), value.getDescription(),
                value.getCoverImageUrl(), value.getCreatedAt());
    }
    public record CollectionRequest(@NotBlank @Size(max=100) String name,
                                    @Size(max=500) String description) {}
    public record CollectionResponse(Long id, String name, String description,
                                     String coverImageUrl, Instant createdAt) {}
}

