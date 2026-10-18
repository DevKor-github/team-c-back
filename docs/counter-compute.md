# Counter compute reduction — 2026-10-18

The `koyeon-live` collector lives at `/home/yjlee/infra/request-counter` under `kodaero-request-counter`. This change does not rebuild or replace the Kodaero application containers.

The old collector sent each small batch immediately to Neon; each visible dashboard also fetched Neon every second. The replacement retains log offsets, anonymized per-user second-window keys, aggregate live counts and pending events in the same durable SQLite database. It publishes only aggregate counts to the existing free Upstash cache and checkpoints compacted batches to the existing Neon ingest endpoint hourly. Response-loss retries remain protected by the stable per-user window keys. Pending events are retained during failures. A Redis outage does not prevent the Neon checkpoint.

The public counter reads the cache, polls every 10 seconds, and caches successful HTTP responses for 15 seconds at the CDN. Cache failure or a snapshot older than three minutes returns unavailable; it never substitutes zero or polls Neon as a fallback. Display may trail by approximately 25 seconds; Neon may trail live counts by up to one hour. Loss of the collector disk requires log/backup recovery for events not yet checkpointed. Do not reset `actions.sqlite`, WAL, identity secret, or offsets.

The operational helper `scripts/reduce-counter-compute.py` and the `counter_phase` input of `cd-dev.yml` support audit, apply and a one-time deployment validation checkpoint. Credentials and reviewed collector source arrive through a temporary protected secret. Source code, SQLite state and secrets remain protected on the host. Do not deploy an unrelated checkout, replay application actions or clear counter data.

- Initial audit: https://github.com/DevKor-github/team-c-back/actions/runs/39096303027
- Collector rollout: https://github.com/DevKor-github/team-c-back/actions/runs/39096342619
- Protected pre-cutover backup: `/home/yjlee/infra/counter-compute-20261018/20261018T041333Z`.
- Collector SHA-256: `727b58843e87f394bb394d28ec8b5a68bc430aac9d438b53f6878151f8fc3eb5`.
- Seeded counts: production 26,185, development 103. A later successful checkpoint matched SQLite and Neon at production 26,187 and development 103 with zero pending batches: https://github.com/DevKor-github/team-c-back/actions/runs/39096554981.
- Website source: Kodaero/Kodaero_RN_Expo `dev`, commit `a96870a`; Vercel `dpl_AnH2SYaSht6AwiWYy5QoZsp96VDw`, https://kodaero-koyeon-live.vercel.app.
- Code checks: eight JavaScript tests, seven Python tests, production build, and actual Redis script check rejecting late snapshot writes. No notification/provider calls were used.

Rollback requires preserving the current SQLite ledger and pending events. First checkpoint all pending events, then coordinate the website reader and collector; reverting only the collector or restoring an older SQLite backup can regress live display or lose uncheckpointed progress.

Production verification at 2026-10-18 13:18:48–13:19:59 KST: seven public counter HTTP reads returned successfully; the counters, batches and action_windows scan counts did not increase. The same sample covered RTinKorea's minute cron; its notification_jobs, call_rooms and call_clients scan counts also did not increase. The raw count-only receipt is protected at `/Users/818jsy/.config/neon-compute-reduction-20261018/live-audit.json`. The temporary GitHub credential secret was removed after verification. Existing application data and Neon projects were preserved.
