import UIKit
import WebKit
import UniformTypeIdentifiers

final class ShareViewController: TrustedWebViewController {
    override func viewDidLoad() {
        sharing = true
        super.viewDidLoad()
        preferredContentSize = CGSize(width: 420, height: max(190, UIScreen.main.bounds.height * 0.27))
        let items = extensionContext?.inputItems as? [NSExtensionItem] ?? []
        let providers = items.flatMap { $0.attachments ?? [] }
        if let provider = providers.first(where: { $0.hasItemConformingToTypeIdentifier(UTType.url.identifier) }) {
            provider.loadItem(forTypeIdentifier: UTType.url.identifier, options: nil) { [weak self] value, _ in
                let text = (value as? URL)?.absoluteString ?? (value as? String)
                DispatchQueue.main.async { self?.openShare(text) }
            }
        } else if let provider = providers.first(where: { $0.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) }) {
            provider.loadItem(forTypeIdentifier: UTType.plainText.identifier, options: nil) { [weak self] value, _ in
                DispatchQueue.main.async { self?.openShare(value as? String) }
            }
        } else { openShare(items.first?.attributedContentText?.string) }
    }
    private func openShare(_ text: String?) {
        guard let origin = origin else { showError("SendIT 서비스 주소를 설정해 주세요."); return }
        var url = URLComponents(url: origin.appendingPathComponent("share-target"), resolvingAgainstBaseURL: false)!
        url.queryItems = [URLQueryItem(name: "native", value: "ios"), URLQueryItem(name: "text", value: String((text ?? "").prefix(10000)))]
        if let destination = url.url { webView.load(URLRequest(url: destination)) }
    }
}
