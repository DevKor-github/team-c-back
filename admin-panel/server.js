const express = require("express");
const { execFile } = require("child_process");
const path = require("path");

const createApp = ({
  backendBaseUrl = String(process.env.BACKEND_BASE_URL || "http://app-dev:8080").replace(/\/$/, ""),
} = {}) => {
  const app = express();

  app.use(express.json({ limit: "64kb" }));

const authHeaders = (req) => {
  const accessToken = req.get("AccessToken");
  const refreshToken = req.get("refreshToken");
  const headers = { "Content-Type": "application/json" };

  if (accessToken) headers.AccessToken = accessToken;
  if (refreshToken) headers.refreshToken = refreshToken;
  return headers;
};

const readJson = async (response) => {
  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return { statusCode: response.status, message: text };
  }
};

const forwardBackendResponse = async (upstream, res) => {
  const renewedAccessToken = upstream.headers.get("AccessToken");
  if (renewedAccessToken) res.set("AccessToken", renewedAccessToken);
  res.status(upstream.status).json(await readJson(upstream));
};

const verifyAdmin = async (req, res, next) => {
  if (!req.get("AccessToken")) {
    return res.status(401).json({ message: "관리자 AccessToken이 필요합니다." });
  }

  try {
    const upstream = await fetch(`${backendBaseUrl}/api/admin/notifications/event-flags`, {
      headers: authHeaders(req),
    });

    if (!upstream.ok) {
      return forwardBackendResponse(upstream, res);
    }

    const renewedAccessToken = upstream.headers.get("AccessToken");
    if (renewedAccessToken) res.set("AccessToken", renewedAccessToken);
    next();
  } catch (error) {
    res.status(502).json({ message: `관리자 인증 서버 연결 실패: ${error.message}` });
  }
};

const proxyNotificationRequest = async (req, res, backendPath, method = req.method) => {
  try {
    const headers = authHeaders(req);
    const idempotencyKey = req.get("Idempotency-Key");
    if (idempotencyKey) headers["Idempotency-Key"] = idempotencyKey;

    const upstream = await fetch(`${backendBaseUrl}${backendPath}`, {
      method,
      headers,
      body: method === "GET" || method === "HEAD" ? undefined : JSON.stringify(req.body || {}),
    });
    await forwardBackendResponse(upstream, res);
  } catch (error) {
    res.status(502).json({ message: `푸시 서버 연결 실패: ${error.message}` });
  }
};

app.get("/api/session", verifyAdmin, (req, res) => {
  res.json({ authenticated: true });
});

app.post("/api/notifications/preview", verifyAdmin, (req, res) =>
  proxyNotificationRequest(req, res, "/api/admin/notifications/dispatches/preview", "POST"),
);

app.post("/api/notifications/dispatches", verifyAdmin, (req, res) =>
  proxyNotificationRequest(req, res, "/api/admin/notifications/dispatches", "POST"),
);

app.get("/api/notifications/dispatches", verifyAdmin, (req, res) => {
  const query = new URLSearchParams({
    page: String(req.query.page || 1),
    size: String(req.query.size || 20),
  });
  proxyNotificationRequest(req, res, `/api/admin/notifications/dispatches?${query}`, "GET");
});

app.get("/api/status", verifyAdmin, (req, res) => {
  execFile("docker", ["ps", "--format", "{{.Names}}\t{{.Status}}\t{{.Image}}"], (error, stdout) => {
    if (error) return res.status(500).json({ message: error.message });

    const containers = stdout
      .trim()
      .split("\n")
      .filter(Boolean)
      .map((line) => {
        const [name, status, image] = line.split("\t");
        return { name, status, image };
      });
    res.json({ containers });
  });
});

app.post("/api/deploy", verifyAdmin, (req, res) => {
  const { version, type } = req.body || {};
  if (!new Set(["dev", "prod"]).has(type)) {
    return res.status(400).json({ message: "지원하지 않는 배포 유형입니다." });
  }
  if (type === "prod" && !/^\d+$/.test(String(version || ""))) {
    return res.status(400).json({ message: "운영 배포 버전은 숫자여야 합니다." });
  }

  const scriptPath = type === "prod" ? "/scripts/deploy-prod.sh" : "/scripts/deploy-dev.sh";
  const args = type === "prod" ? [scriptPath, String(version)] : [scriptPath];
  execFile("bash", args, { cwd: "/scripts", maxBuffer: 1024 * 1024 }, (error, stdout, stderr) => {
    res.status(error ? 500 : 200).json({
      success: !error,
      output: `${stdout}${stderr || ""}`,
    });
  });
});

  app.use(express.static(path.join(__dirname, "public")));
  app.use((req, res) => res.sendFile(path.join(__dirname, "public", "index.html")));

  return app;
};

if (require.main === module) {
  const port = Number(process.env.PORT || 3000);
  createApp().listen(port, () => console.log(`Kodaero Admin Server ready on port ${port}`));
}

module.exports = { createApp };
