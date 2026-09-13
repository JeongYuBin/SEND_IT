import UIKit
import WebKit

class TrustedWebViewController: UIViewController, WKNavigationDelegate, WKScriptMessageHandler {
    var webView: WKWebView!
    var sharing = false
    var onExternalLink: ((URL) -> Void)?
    private var currentSession: String?
    var origin: URL? {
        guard let value = Bundle.main.object(forInfoDictionaryKey: "SendITOrigin") as? String,
              let url = URL(string: value), url.scheme == "https", url.host != nil else { return nil }
        return url
    }
    func trusted(_ url: URL?) -> Bool {
        guard let url = url, let origin = origin else { return false }
        return url.scheme == origin.scheme && url.host == origin.host && url.port == origin.port
    }
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = sharing ? .clear : .systemBackground
        let configuration = WKWebViewConfiguration()
        if sharing { configuration.websiteDataStore = .nonPersistent() }
        // Credentials are injected into the main frame only, and only on the configured origin.
        currentSession = SessionStore.read()
        let script = sessionScript(currentSession)
        configuration.userContentController.addUserScript(WKUserScript(source: script, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        configuration.userContentController.add(WeakMessageHandler(self), name: "sendit")
        webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = self
        webView.isOpaque = !sharing
        webView.backgroundColor = sharing ? .clear : .systemBackground
        webView.scrollView.backgroundColor = .clear
        webView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(webView)
        NSLayoutConstraint.activate([webView.leadingAnchor.constraint(equalTo: view.leadingAnchor), webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
          webView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor), webView.bottomAnchor.constraint(equalTo: view.bottomAnchor)])
    }
    private func literal(_ value: String) -> String {
        let data = try! JSONSerialization.data(withJSONObject: [value], options: [])
        return String(data: data, encoding: .utf8)!.dropFirst().dropLast().description
    }
    private func sessionScript(_ session: String?) -> String {
        let operation = session.map { "localStorage.setItem('sendit-auth', \(literal($0)));" } ?? "localStorage.removeItem('sendit-auth');"
        let expected = origin.map { URLComponents(url: $0, resolvingAgainstBaseURL: false)!.scheme! + "://" + $0.host! + ($0.port.map { ":\($0)" } ?? "") } ?? ""
        return "if (location.origin === \(literal(expected))) { \(operation) }"
    }
    func synchronizeSession() {
        let session = SessionStore.read()
        guard session != currentSession, trusted(webView?.url) else { return }
        currentSession = session
        webView.evaluateJavaScript(sessionScript(session) + "location.reload();", completionHandler: nil)
    }
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        webView.configuration.userContentController.removeAllUserScripts()
    }
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if trusted(navigationAction.request.url) { decisionHandler(.allow); return }
        if !sharing, let url = navigationAction.request.url, ["https", "http"].contains(url.scheme ?? "") { onExternalLink?(url) }
        decisionHandler(.cancel)
    }
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard message.frameInfo.isMainFrame,
              message.frameInfo.securityOrigin.protocol == origin?.scheme,
              message.frameInfo.securityOrigin.host == origin?.host,
              (message.frameInfo.securityOrigin.port == (origin?.port ?? 443)
                || (origin?.port == nil && message.frameInfo.securityOrigin.port == 0)),
              let raw = message.body as? String, let data = raw.data(using: .utf8),
              let payload = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return }
        if payload["type"] as? String == "session", let value = payload["value"] as? String {
            if SessionStore.write(value) { currentSession = value }
        }
        if payload["type"] as? String == "close", sharing { extensionContext?.completeRequest(returningItems: nil, completionHandler: nil) }
    }
    func showError(_ text: String) {
        let label = UILabel(); label.text = text; label.numberOfLines = 0; label.textAlignment = .center
        label.frame = view.bounds.insetBy(dx: 24, dy: 80); label.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(label)
    }
}

private final class WeakMessageHandler: NSObject, WKScriptMessageHandler {
    weak var delegate: WKScriptMessageHandler?
    init(_ delegate: WKScriptMessageHandler) { self.delegate = delegate }
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        delegate?.userContentController(userContentController, didReceive: message)
    }
}
