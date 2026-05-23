import { useCallback, useEffect, useMemo, useState } from "react";
import { appBuildId, appBuildTime, localTimeZone, sortedApps } from "../helpers";
import { functionsBaseUrl } from "../firebase";

type HealthStatus = "unknown" | "checking" | "healthy" | "unhealthy";

type ServiceHealth = {
  name: string;
  url: string;
  status: HealthStatus;
  latencyMs: number | null;
  detail: string;
};

export default function SystemHealthPanel() {
  const serviceDefinitions = useMemo<ServiceHealth[]>(() => {
    const contentApiBase = import.meta.env.VITE_CONTENT_API_URL?.trim().replace(/\/$/, "");
    const services: ServiceHealth[] = [
      {
        name: "Firebase Functions",
        url: `${functionsBaseUrl}/healthCheck`,
        status: "unknown",
        latencyMs: null,
        detail: "Not checked",
      },
    ];

    if (contentApiBase) {
      services.unshift({
        name: "Content API",
        url: `${contentApiBase}/health`,
        status: "unknown",
        latencyMs: null,
        detail: "Not checked",
      });
    }

    return services;
  }, []);

  const [services, setServices] = useState<ServiceHealth[]>(serviceDefinitions);
  const [lastChecked, setLastChecked] = useState<string | null>(null);

  const checkHealth = useCallback(async () => {
    setServices(serviceDefinitions.map((service) => ({
      ...service,
      status: "checking" as HealthStatus,
      detail: "Checking…",
    })));

    const results = await Promise.all(
      serviceDefinitions.map(async (service) => {
        const start = performance.now();
        try {
          const res = await fetch(service.url, {
            method: service.url.includes("healthCheck") ? "POST" : "GET",
            headers: service.url.includes("healthCheck")
              ? { "Content-Type": "application/json" }
              : {},
            body: service.url.includes("healthCheck") ? "{}" : undefined,
            signal: AbortSignal.timeout(10000),
          });
          const latencyMs = Math.round(performance.now() - start);
          const isOk = res.ok || res.status === 401; // 401 = auth required but endpoint reachable
          return {
            ...service,
            status: isOk ? "healthy" as HealthStatus : "unhealthy" as HealthStatus,
            latencyMs,
            detail: isOk ? `HTTP ${res.status} · ${latencyMs}ms` : `HTTP ${res.status}`,
          };
        } catch (error) {
          const latencyMs = Math.round(performance.now() - start);
          return {
            ...service,
            status: "unhealthy" as HealthStatus,
            latencyMs,
            detail: error instanceof Error ? error.message : "Request failed",
          };
        }
      }),
    );

    setServices(results);
    setLastChecked(new Date().toLocaleTimeString());
  }, [serviceDefinitions]);

  useEffect(() => {
    checkHealth();
  }, [checkHealth]);

  const statusIcon = (status: HealthStatus) => {
    switch (status) {
      case "healthy": return "🟢";
      case "unhealthy": return "🔴";
      case "checking": return "🔄";
      default: return "⚪";
    }
  };

  return (
    <div className="single-panel-grid" id="tabpanel-system-health" role="tabpanel" aria-labelledby="tab-system-health">
      <main className="panel form-panel" role="main" aria-label="System health dashboard">
        <div className="panel-header">
          <h2>System Health</h2>
          <button className="btn-secondary" onClick={checkHealth} aria-label="Refresh health checks">
            Refresh
          </button>
        </div>

        {lastChecked && (
          <p className="muted" aria-live="polite">Last checked: {lastChecked}</p>
        )}
        {!import.meta.env.VITE_CONTENT_API_URL && (
          <p className="muted">
            Content API health check is hidden because <code>VITE_CONTENT_API_URL</code> is not configured for this deploy.
          </p>
        )}

        {/* Build Info */}
        <section className="subsection health-cards" aria-label="Build information">
          <h3>Build Info</h3>
          <div className="health-grid">
            <div className="health-card glass-card">
              <div className="health-card-label">Build</div>
              <div className="health-card-value"><code>{appBuildId}</code></div>
            </div>
            <div className="health-card glass-card">
              <div className="health-card-label">Build Time</div>
              <div className="health-card-value">{appBuildTime}</div>
            </div>
            <div className="health-card glass-card">
              <div className="health-card-label">Timezone</div>
              <div className="health-card-value">{localTimeZone}</div>
            </div>
            <div className="health-card glass-card">
              <div className="health-card-label">App Flavors</div>
              <div className="health-card-value">{sortedApps.length}</div>
            </div>
          </div>
        </section>

        {/* Service Status */}
        <section className="subsection health-cards" aria-label="Service status">
          <h3>Service Status</h3>
          <div className="service-list">
            {services.map((service) => (
              <div key={service.name} className={`service-card glass-card service-${service.status}`}>
                <div className="service-card-header">
                  <span className="service-icon" aria-hidden="true">{statusIcon(service.status)}</span>
                  <strong>{service.name}</strong>
                </div>
                <div className="service-card-detail">
                  <span className="muted">{service.detail}</span>
                  {service.latencyMs !== null && service.status === "healthy" && (
                    <span className="latency-badge">{service.latencyMs}ms</span>
                  )}
                </div>
                <code className="service-url">{service.url}</code>
              </div>
            ))}
          </div>
        </section>

        {/* Quick Links */}
        <section className="subsection health-cards" aria-label="Quick links">
          <h3>Quick Links</h3>
          <div className="health-grid">
            <a
              href="https://console.firebase.google.com"
              target="_blank"
              rel="noopener noreferrer"
              className="health-card glass-card link-card"
            >
              <span className="link-icon" aria-hidden="true">🔥</span>
              <span>Firebase Console</span>
            </a>
            <a
              href="https://dash.cloudflare.com"
              target="_blank"
              rel="noopener noreferrer"
              className="health-card glass-card link-card"
            >
              <span className="link-icon" aria-hidden="true">☁️</span>
              <span>Cloudflare Dashboard</span>
            </a>
            <a
              href="https://apps.admob.com"
              target="_blank"
              rel="noopener noreferrer"
              className="health-card glass-card link-card"
            >
              <span className="link-icon" aria-hidden="true">💰</span>
              <span>AdMob Dashboard</span>
            </a>
            <a
              href="https://github.com"
              target="_blank"
              rel="noopener noreferrer"
              className="health-card glass-card link-card"
            >
              <span className="link-icon" aria-hidden="true">🐙</span>
              <span>GitHub</span>
            </a>
          </div>
        </section>
      </main>
    </div>
  );
}
