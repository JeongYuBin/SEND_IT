package com.sendit.share;

import com.sendit.share.ShareDtos.CreateShareRequest;
import com.sendit.share.ShareDtos.ShareAcceptedResponse;
import com.sendit.share.ShareDtos.ShareDetailResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/shares")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    ShareAcceptedResponse create(
            Principal principal,
            @Valid @RequestBody CreateShareRequest request
    ) {
        return shareService.create(principal.getName(), request);
    }

    @PostMapping(path = "/media", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ShareAcceptedResponse createMedia(
            Principal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sharedText", required = false) String sharedText
    ) {
        return shareService.createMedia(principal.getName(), file, sharedText);
    }

    @GetMapping("/{shareId}")
    ShareDetailResponse get(Principal principal, @PathVariable Long shareId) {
        return shareService.get(principal.getName(), shareId);
    }

    @GetMapping
    List<ShareDetailResponse> list(Principal principal) {
        return shareService.list(principal.getName());
    }

    @PostMapping("/{shareId}/reanalyze")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ShareAcceptedResponse reanalyze(Principal principal, @PathVariable Long shareId) {
        return shareService.reanalyze(principal.getName(), shareId);
    }
}
