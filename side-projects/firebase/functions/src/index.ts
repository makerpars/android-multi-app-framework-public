import * as admin from "firebase-admin";

// Firebase Admin SDK başlat
admin.initializeApp();

// Function export'ları
// MOVED to direct Firestore SDK writes from Android client (FirestorePushRegistrationSender)
// export { registerDevice } from "./registerDevice";
export { dispatchNotifications } from "./dispatchNotifications";
export { otherAppsFeed } from "./otherAppsFeed";
export { sendTestNotification } from "./sendTestNotification";
export { deviceCoverageReport } from "./deviceCoverageReport";
export { adPerformance, generateAdPerformanceWeeklyReport } from "./adPerformanceReport";
export { adminAccessCheck } from "./adminAccessCheck";
export { adminGetRemoteConfig, adminUpdateRemoteConfig } from "./adminRemoteConfig";
export { adminGetFlavorHubSummary, adminGetAnalyticsSummary, adminGetRevenueSummary } from "./adminSummary";
export { healthCheck } from "./healthCheck";
export { recaptchaVerify } from "./recaptchaVerify";
export { verifyPurchase } from "./verifyPurchase";
