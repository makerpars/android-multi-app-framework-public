import { onRequest } from "firebase-functions/v2/https";

const REGION = "europe-west1";

export const healthCheck = onRequest(
    { region: REGION, cors: true },
    async (_req, res) => {
        res.status(200).json({
            ok: true,
            service: "firebase-functions",
            region: REGION,
            timestamp: new Date().toISOString(),
        });
    },
);
