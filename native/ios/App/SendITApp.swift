import SwiftUI
import WebKit

@main struct SendITApp: App {
    var body: some Scene { WindowGroup { MainWebView().ignoresSafeArea(.container, edges: .bottom) } }
}
struct MainWebView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> MainViewController { MainViewController() }
    func updateUIViewController(_ controller: MainViewController, context: Context) {}
}
final class MainViewController: TrustedWebViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        onExternalLink = { UIApplication.shared.open($0) }
        if let origin = origin { webView.load(URLRequest(url: origin)) }
        else { showError("SendIT 서비스 주소를 설정해 주세요.") }
        NotificationCenter.default.addObserver(self, selector: #selector(activated), name: UIApplication.didBecomeActiveNotification, object: nil)
    }
    @objc private func activated() { synchronizeSession() }
    deinit { NotificationCenter.default.removeObserver(self) }
}
