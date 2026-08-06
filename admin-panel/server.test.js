const assert = require("node:assert/strict");
const http = require("node:http");
const test = require("node:test");

const { createApp } = require("./server");

const listen = (server) => new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
const close = (server) => new Promise((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
const urlFor = (server) => `http://127.0.0.1:${server.address().port}`;

test("admin session requires an AccessToken and validates it upstream", async (context) => {
  const upstreamRequests = [];
  const upstream = http.createServer((req, res) => {
    upstreamRequests.push({ method: req.method, url: req.url, accessToken: req.headers.accesstoken });
    res.setHeader("Content-Type", "application/json");
    res.setHeader("AccessToken", "renewed-token");
    res.end(JSON.stringify({ statusCode: 200, data: [] }));
  });
  await listen(upstream);
  context.after(() => close(upstream));

  const admin = createApp({ backendBaseUrl: urlFor(upstream) }).listen(0, "127.0.0.1");
  await new Promise((resolve) => admin.once("listening", resolve));
  context.after(() => close(admin));

  const unauthenticated = await fetch(`${urlFor(admin)}/api/session`);
  assert.equal(unauthenticated.status, 401);

  const authenticated = await fetch(`${urlFor(admin)}/api/session`, {
    headers: { AccessToken: "admin-token" },
  });
  assert.equal(authenticated.status, 200);
  assert.equal(authenticated.headers.get("AccessToken"), "renewed-token");
  assert.deepEqual(await authenticated.json(), { authenticated: true });
  assert.deepEqual(upstreamRequests, [{
    method: "GET",
    url: "/api/admin/notifications/event-flags",
    accessToken: "admin-token",
  }]);
});

test("notification preview is proxied only after admin validation", async (context) => {
  const upstreamRequests = [];
  const upstream = http.createServer((req, res) => {
    let body = "";
    req.on("data", (chunk) => { body += chunk; });
    req.on("end", () => {
      upstreamRequests.push({ method: req.method, url: req.url, body: body ? JSON.parse(body) : null });
      res.setHeader("Content-Type", "application/json");
      if (req.url.endsWith("/preview")) {
        res.end(JSON.stringify({ statusCode: 200, data: { recipientCount: 2 } }));
        return;
      }
      res.end(JSON.stringify({ statusCode: 200, data: [] }));
    });
  });
  await listen(upstream);
  context.after(() => close(upstream));

  const admin = createApp({ backendBaseUrl: urlFor(upstream) }).listen(0, "127.0.0.1");
  await new Promise((resolve) => admin.once("listening", resolve));
  context.after(() => close(admin));

  const requestBody = {
    mode: "ACTUAL",
    appVariant: "DEV",
    targetType: "ALL",
    targetValue: "ALL",
    title: "공지",
    body: "내용",
    actionType: "HOME",
    actionParams: {},
    confirm: true,
  };
  const response = await fetch(`${urlFor(admin)}/api/notifications/preview`, {
    method: "POST",
    headers: { AccessToken: "admin-token", "Content-Type": "application/json" },
    body: JSON.stringify(requestBody),
  });

  assert.equal(response.status, 200);
  assert.equal((await response.json()).data.recipientCount, 2);
  assert.equal(upstreamRequests.length, 2);
  assert.equal(upstreamRequests[0].url, "/api/admin/notifications/event-flags");
  assert.deepEqual(upstreamRequests[1], {
    method: "POST",
    url: "/api/admin/notifications/dispatches/preview",
    body: requestBody,
  });
});
