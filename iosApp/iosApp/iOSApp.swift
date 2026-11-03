import ComposeApp
import SwiftUI
import UIKit
import UserNotifications

/// Push glue (F20). Firebase Messaging is optional: add the Firebase iOS SDK (Swift Package Manager,
/// `FirebaseMessaging`) and `GoogleService-Info.plist` (never committed), then uncomment the Firebase lines.
/// See docs/RELEASE.md.
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        // FirebaseApp.configure()
        // Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional {
                DispatchQueue.main.async { application.registerForRemoteNotifications() }
            }
        }
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        // Messaging.messaging().apnsToken = deviceToken
    }

    // A push arrived while the app is open: refresh the list/badge and still show the banner.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        IosPush.shared.onMessageReceived()
        completionHandler([.banner, .list, .sound])
    }

    // The user tapped a push: open the screen it points to.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let info = response.notification.request.content.userInfo
        IosPush.shared.onNotificationOpened(
            referenceType: info["referenceType"] as? String,
            referenceId: info["referenceId"] as? String
        )
        completionHandler()
    }
}

// extension AppDelegate: MessagingDelegate {
//     func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
//         if let token = fcmToken { IosPush.shared.onNewToken(token: token) }
//     }
// }

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                // financemanage://<referenceType>/<id>
                .onOpenURL { url in _ = IosPush.shared.onDeepLink(url: url.absoluteString) }
        }
    }
}
