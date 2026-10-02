# Groove on Minikube

Groove runs as a fully containerised stack on Kubernetes. This document covers local deployment using Minikube and plain Kubernetes manifests (no Helm, no Kustomize). This is **additional** to `docker-compose.yml`, not a replacement — Compose stays the easy day-to-day dev option.

## Architecture

**16 workloads, 17 Pods across 5 layers:**

| Layer          | Component                | Kind        | Replicas |
| -------------- | ------------------------ | ----------- | -------: |
| Application    | order-service            | Deployment  |        2 |
| Application    | payment-service          | Deployment  |        1 |
| Application    | inventory-service        | Deployment  |        1 |
| Application    | product-service          | Deployment  |        1 |
| Application    | gateway                  | Deployment  |        1 |
| Application    | frontend                 | Deployment  |        1 |
| Messaging      | Kafka                    | StatefulSet |        1 |
| Messaging      | Apicurio Schema Registry | Deployment  |        1 |
| Messaging      | Kafka Exporter           | Deployment  |        1 |
| Database       | PostgreSQL               | StatefulSet |        1 |
| Database       | MongoDB                  | StatefulSet |        1 |
| Authentication | Keycloak                 | Deployment  |        1 |
| Observability  | Prometheus               | Deployment  |        1 |
| Observability  | Loki                     | Deployment  |        1 |
| Observability  | Alloy                    | Deployment  |        1 |
| Observability  | Grafana                  | Deployment  |        1 |

`order-service` runs with 2 replicas. `FOR UPDATE SKIP LOCKED` prevents concurrent outbox workers from claiming the same row — see "Testing Concurrent Processing".

This table is the core Groove stack only. ArgoCD (optional, 7 more workloads in its own `argocd` namespace) is covered separately in "GitOps with ArgoCD" below.

## Prerequisites

- [Minikube](https://minikube.sigs.k8s.io/docs/start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- Docker Desktop running
- 8 GB RAM available (6 GB minimum with `MINIKUBE_MEMORY=6144` — Docker Desktop's own memory cap varies by machine)

Application images use `ghcr.io/vkartik-3/angular-java/<service>:latest`. Enable publishing in GitHub Actions before deploying, or build the images in Minikube locally after compiling the Java services:

```bash
mvn package -DskipTests
minikube start
eval $(minikube docker-env)
docker compose build
```

Compose requires a local `.env` populated from `.env.example`. For private registry packages, configure an image pull secret locally.

## Quick Start

```bash
cp infra/k8s/secrets.yaml.template infra/k8s/secrets.yaml
# Fill in every blank password in infra/k8s/secrets.yaml.
./infra/k8s/start.sh
```

`start.sh` starts Minikube if necessary, deploys the stack in dependency order, waits for each tier to become ready, and starts the required port-forwards. It's idempotent — safe to re-run any time.

### Access

- **Application:** http://localhost:4200
- **Gateway:** http://localhost:8090/api
- **Keycloak:** http://localhost:8180
- **Grafana:** http://localhost:3000

## Configuration

### Secrets

Credentials are not committed to the repository:

```bash
cp infra/k8s/secrets.yaml.template infra/k8s/secrets.yaml
```

Fill all blank password fields in the ignored `infra/k8s/secrets.yaml` before applying it. PostgreSQL and MongoDB credentials are supplied to the services through Secret references; application properties contain no production passwords. Keep local credentials outside version control.

After startup, create customer and administrator users in the `groove` Keycloak realm with your own passwords and the required `user`/`admin` roles. Realm imports contain only public client and role definitions.

### Environment Variables

| Variable             | Default | Description                                      |
| -------------------- | ------- | ------------------------------------------------ |
| `MINIKUBE_MEMORY`    | `8192`  | Minikube memory in MB                            |
| `MINIKUBE_CPUS`      | `4`     | Minikube CPU count                               |
| `SKIP_OBSERVABILITY` | unset   | Set to `1` to skip Prometheus/Loki/Alloy/Grafana |

```bash
# Low-memory machine
MINIKUBE_MEMORY=6144 ./infra/k8s/start.sh

# Skip observability
SKIP_OBSERVABILITY=1 ./infra/k8s/start.sh
```

## Deployment

### What `start.sh` does

1. Starts Minikube with the configured resources, enables `metrics-server`
2. Creates the `groove` namespace and applies Secrets
3. Creates ConfigMaps from the source configuration files (Compose already uses these same files — one source of truth for both deployment methods)
4. Deploys infrastructure in dependency order — database → messaging → auth → application — waiting for each tier to be ready before starting the next
5. Applies observability (unless `SKIP_OBSERVABILITY=1`)
6. Starts port-forwards in the background — including ArgoCD's UI, if it's already installed (see "GitOps with ArgoCD")

Bringing everything up in one shot rather than staged can starve Kafka of the CPU it needs to finish its own startup, which then cascades into every dependent service — that's why the ordering and waiting matters, not just convenience.

### Manual Deployment

For step-by-step deployment or debugging — this is what `start.sh` actually automates:

```bash
# Namespace and Secrets
kubectl apply -f infra/k8s/namespace.yaml
kubectl apply -f infra/k8s/secrets.yaml

# ConfigMaps generated from existing source files
kubectl create configmap mongo-init \
  --from-file=infra/docker/mongo-init.js \
  -n groove --dry-run=client -o yaml | kubectl apply -f -

kubectl create configmap keycloak-realm \
  --from-file=infra/keycloak/groove-realm.json \
  -n groove --dry-run=client -o yaml | kubectl apply -f -

kubectl create configmap prometheus-config \
  --from-file=infra/monitoring/prometheus/prometheus.yml \
  -n groove --dry-run=client -o yaml | kubectl apply -f -

kubectl create configmap loki-config \
  --from-file=infra/monitoring/loki/loki.yml \
  -n groove --dry-run=client -o yaml | kubectl apply -f -

kubectl create configmap alloy-config \
  --from-file=infra/monitoring/alloy/config-k8s.alloy \
  -n groove --dry-run=client -o yaml | kubectl apply -f -

kubectl create configmap grafana-datasources \
  --from-file=infra/monitoring/grafana/provisioning/datasources \
  -n groove --dry-run=client -o yaml | kubectl apply -f -

kubectl create configmap grafana-dashboards \
  --from-file=infra/monitoring/grafana/provisioning/dashboards \
  -n groove --dry-run=client -o yaml | kubectl apply -f -

# Database
kubectl apply -f infra/k8s/database/
kubectl wait --for=condition=ready pod -l app=postgres -n groove --timeout=180s
kubectl wait --for=condition=ready pod -l app=mongo -n groove --timeout=180s

# Messaging — timeouts are generous on purpose: a fully cold image pull
# (nothing cached yet) took Kafka past 4 minutes in testing
kubectl apply -f infra/k8s/messaging/
kubectl wait --for=condition=ready pod -l app=kafka -n groove --timeout=480s
kubectl wait --for=condition=ready pod -l app=apicurio-registry -n groove --timeout=300s

# Auth
kubectl apply -f infra/k8s/auth/
kubectl wait --for=condition=ready pod -l app=keycloak -n groove --timeout=480s

# Application
kubectl apply -f infra/k8s/app/
kubectl wait --for=condition=ready pod -l app=order-service -n groove --timeout=420s

# Observability (optional)
kubectl apply -f infra/k8s/observability/prometheus-deployment.yaml -f infra/k8s/observability/prometheus-service.yaml
kubectl apply -f infra/k8s/observability/loki-deployment.yaml -f infra/k8s/observability/loki-service.yaml
kubectl apply -f infra/k8s/observability/alloy-rbac.yaml -f infra/k8s/observability/alloy-deployment.yaml -f infra/k8s/observability/alloy-service.yaml
kubectl apply -f infra/k8s/observability/grafana-deployment.yaml -f infra/k8s/observability/grafana-service.yaml

# Port-forwards
kubectl port-forward svc/frontend 4200:80 -n groove &
kubectl port-forward svc/gateway 8090:8090 -n groove &
kubectl port-forward svc/keycloak 8180:8080 -n groove &
kubectl port-forward svc/grafana 3000:3000 -n groove &
```

## Verifying the Deployment

```bash
kubectl get pods -n groove
kubectl get services -n groove
kubectl top pods -n groove   # needs the metrics-server addon, enabled by start.sh
```

Expected: every pod `STATUS Running`, `READY 1/1`. A handful of early restarts is normal on a resource-constrained node — the probes are designed to self-heal that; it's only a problem if a pod is stuck in `CrashLoopBackOff` or `Pending` and not recovering.

## Testing Concurrent Processing

`order-service` runs 2 replicas sharing the same PostgreSQL outbox and saga-timeout tables. `FOR UPDATE SKIP LOCKED` prevents concurrent workers from claiming the same row. The outbox publisher's batch size is deliberately reduced to 5 in this deployment (vs. 50 elsewhere) so that a run of this size can't be drained by one replica in a single poll, forcing both replicas to actually compete for rows.

Requires `curl` and `python3`.

Place 10 orders simultaneously:

```bash
TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/groove/protocol/openid-connect/token \
  -d "client_id=groove-app&grant_type=password" \
  --data-urlencode "username=$E2E_USER_USERNAME" \
  --data-urlencode "password=$E2E_USER_PASSWORD" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

PRODUCT_ID=$(curl -s http://localhost:8090/api/products -H "Authorization: Bearer $TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

for i in $(seq 1 10); do
  curl -s -D - -o /dev/null http://localhost:8090/api/orders \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: $(python3 -c 'import uuid; print(uuid.uuid4())')" \
    -d "{\"items\":[{\"productId\":\"$PRODUCT_ID\",\"quantity\":1,\"price\":1.00}]}" \
    | grep -i '^location:' | sed 's#.*/orders/##' | tr -d '\r' > "/tmp/order-id-$i.txt" &
done
wait
cat /tmp/order-id-*.txt > /tmp/order-ids.txt
echo "orders placed: $(wc -l < /tmp/order-ids.txt)"
```

Verify all 10 sagas completed (allow ~15s for the outbox to flush):

```bash
sleep 15
while read -r id; do
  echo "=== $id ==="
  curl -s "http://localhost:8090/api/orders/$id" -H "Authorization: Bearer $TOKEN" \
    | grep -o '"status":"[A-Z_]*"' | tail -n +2
done < /tmp/order-ids.txt
```

Expect 10 distinct order IDs, each with a complete, consistent saga sequence and no repeated or missing steps. The exact steps depend on the mode configured in the admin panel.

Optional — confirm both replicas actually competed (not just that nothing broke): count `"Outbox sent"` log lines per pod. Both should be non-zero and roughly even.

```bash
for pod in $(kubectl get pods -n groove -l app=order-service -o jsonpath='{.items[*].metadata.name}'); do
  echo "=== $pod ==="
  kubectl logs "$pod" -n groove --since=2m | grep -c "Outbox sent"
done
```

This exercises concurrent outbox processing across the two `order-service` pods for real, not just the replica count in a manifest.

## GitOps with ArgoCD

CI (GitHub Actions) and CD (ArgoCD) are deliberately separate here: CI builds, tests, and pushes an image, then pins its tag in `infra/k8s/app/*.yaml` and pushes that commit; ArgoCD is the thing that actually watches Git and reconciles the cluster to match it. Scoped to `infra/k8s/app/` only — the database/messaging/auth/observability tiers stay `start.sh`'s job; see the comment in `infra/k8s/argocd/application.yaml` for why.

Not installed by `start.sh` automatically — it's opt-in. Install it once with the steps below; after that, `start.sh` detects it on every subsequent run and manages its port-forward alongside the others.

ArgoCD's full install (application-controller, applicationset-controller, dex-server, notifications-controller, redis, repo-server, server/UI — 7 more workloads) adds real memory pressure on top of the core stack. If you're already close to your `MINIKUBE_MEMORY` ceiling, raise it before installing — on a running cluster, that means recreating it (`minikube delete` then `MINIKUBE_MEMORY=<higher> ./infra/k8s/start.sh`), since Minikube won't apply a new memory value to an existing profile.

### Install

`kubectl apply` fails on this manifest: the `applicationsets.argoproj.io` CRD's schema is large enough that the client-side `last-applied-configuration` annotation `apply` normally writes exceeds Kubernetes' 256KiB annotation limit. `--server-side` avoids it entirely (skips that annotation), which is why it's used here rather than a plain `apply`:

```bash
kubectl apply -f infra/k8s/argocd/namespace.yaml
kubectl apply -n argocd -f infra/k8s/argocd/install.yaml --server-side --force-conflicts
kubectl wait --for=condition=available deployment/argocd-server -n argocd --timeout=300s
kubectl apply -f infra/k8s/argocd/application.yaml
```

### Access the UI

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Open https://localhost:8080 (self-signed cert — browser warning is expected). Username `admin`, password:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
echo
```

### Demo the loop

Check current state:

```bash
kubectl get application groove-app -n argocd -o jsonpath='{.status.sync.status}{"\n"}{.status.health.status}{"\n"}'
```

Expect `Synced` / `Healthy`.

Push any change to `main` and watch ArgoCD reconcile without running `kubectl apply`:

```bash
kubectl get application groove-app -n argocd -w
kubectl get pods -n groove -w
```

Status transitions: `Synced` → `OutOfSync` → `Progressing` → `Synced` / `Healthy`.

## Kubernetes Concepts

| Concept                    | Where used                                                                                                |
| -------------------------- | --------------------------------------------------------------------------------------------------------- |
| Deployment                 | Application and observability services                                                                    |
| StatefulSet                | PostgreSQL, MongoDB, Kafka                                                                                 |
| PersistentVolumeClaim      | PostgreSQL, MongoDB, Loki, Grafana data volumes                                                           |
| ConfigMap                  | Application and observability configuration                                                               |
| Secret                     | Database credentials, Keycloak admin credentials                                                          |
| Liveness / readiness probe | Quarkus services: `/q/health/live`, `/q/health/ready` — product-service (Spring Boot): `/actuator/health` |
| Startup probe              | Every JVM-based Deployment — see "Implementation Notes"                                                   |
| Resource requests/limits   | Every container, sized from real `kubectl top` data, not guesses                                          |
| Multiple replicas          | `order-service` × 2                                                                                       |
| Rolling update strategy    | All 6 application Deployments — `maxUnavailable: 0`, `maxSurge: 1`                                        |
| Service / DNS              | Inter-service communication by Kubernetes Service name, matching Compose's names                          |
| Namespace                  | All Groove resources isolated in `groove`                                                             |
| Custom Resource Definition | ArgoCD's `Application`/`AppProject` types, in `argocd`                                                    |
| GitOps / continuous reconciliation | ArgoCD watches `infra/k8s/app/` in Git and reconciles the cluster to match — see "GitOps with ArgoCD"    |

## Troubleshooting

### Pod stuck in Pending

```bash
kubectl describe pod <pod-name> -n groove
```

Usually insufficient resources. Try `MINIKUBE_MEMORY=6144 ./infra/k8s/start.sh`, or if that's already the case, trim something else — see "Resource sizing" implementation note.

### Port-forward connection dropped

`kubectl port-forward` is tied to a specific pod, not the Service — a rollout, a crash, a rescheduling, anything that replaces the pod kills it silently (no error in your terminal). Re-run `./infra/k8s/start.sh` (idempotent, refreshes the port-forwards) or manually:

```bash
kubectl port-forward svc/frontend 4200:80 -n groove &
kubectl port-forward svc/gateway 8090:8090 -n groove &
kubectl port-forward svc/keycloak 8180:8080 -n groove &
kubectl port-forward svc/grafana 3000:3000 -n groove &
```

### Keycloak not ready

```bash
kubectl logs -f deployment/keycloak -n groove
```

Realm import alone takes ~100s; with a cold image pull on top, budget several minutes before it's actually stuck rather than just slow.

### Check logs for any service

```bash
kubectl logs -f deployment/order-service -n groove
kubectl logs -f statefulset/kafka -n groove
```

## Implementation Notes

- **Keycloak access** — reachable only via `localhost:8180`, not through Kubernetes DNS. order-, payment- and inventory-service hardcode `http://localhost:8180` as the JWT issuer, and the frontend has it baked into its build (`environment.prod.ts`). Changing the port breaks login for a reason no Kubernetes config can fix — port-forward is the only viable local path.
- **Ingress** — `infra/k8s/ingress.yaml` routes correctly but isn't part of the local workflow: OAuth2/PKCE needs a secure context (HTTPS or `localhost`), and `http://groove.local` is neither. Fixing that needs real TLS, which only pays off with a real domain and automated certs (cert-manager + Let's Encrypt on a cloud deployment). Ingress stays in the repo to show host-based routing is understood, and becomes relevant again on a cloud deployment.
- **Persistent storage** — PostgreSQL, MongoDB, Loki, and Grafana are backed by PersistentVolumeClaims, so their data survives pod restarts and replacements.
- **Polyglot persistence** — order-service (relational, Flyway-managed saga/outbox schema, JPA transactions) stays on PostgreSQL; product-service (a flat, document-shaped catalogue with no relations) moved to MongoDB. Its Spring Batch CSV importer uses `ResourcelessJobRepository` (Spring Batch 6's default when no relational `DataSource` is present) rather than a JDBC- or Mongo-backed job repository — each import is a one-shot, non-restartable run whose execution history nothing in this app ever queries back, so there's nothing worth persisting for it.
- **Kafka networking** — Kafka's Service is headless (`clusterIP: None`) with `publishNotReadyAddresses: true`. KRaft's controller quorum has the broker reach itself by Service name; a normal ClusterIP is a virtual IP, and a pod hairpinning back to itself through its own Service VIP isn't reliable on Minikube's basic bridge CNI. Headless DNS resolves straight to the real pod IP instead. `publishNotReadyAddresses` is needed because a headless Service only publishes a pod's DNS entry once it's Ready by default, and Kafka can't become Ready until it resolves and reaches itself.
- **JVM startup time** — every JVM-based Deployment has a `startupProbe` so liveness/readiness don't start evaluating before the service has finished starting. Keycloak's realm import alone took ~100s in testing, product-service ~30-45s.
- **Alloy RBAC and networking** — needs `pods` and `pods/log` in its RBAC Role to discover and tail pod logs, and an explicit `--server.http.listen-addr=0.0.0.0:12345` — it defaults to a loopback-only bind, which its own liveness/readiness probes can never reach.
- **Prometheus scraping** — with 2 order-service replicas, Prometheus only scrapes whichever pod the Service routes to per scrape, not both. Not broken, just not true per-replica metrics; would need `kubernetes_sd_configs` to fix properly.
- **Resource sizing** — requests/limits are set from real `kubectl top pods` data, not guesses, for every workload except MongoDB (new, not yet measured against a live cluster — it currently just mirrors PostgreSQL's numbers as a starting point). Several were significantly over-requested (postgres: 71Mi actual vs 512Mi originally); a couple run slightly over their request at peak (Keycloak, Grafana) but stay well under their limit.
- **product-service env vars** — Spring Boot, not Quarkus; needs `SPRING_MONGODB_HOST/DATABASE/USERNAME/PASSWORD/AUTHENTICATION_DATABASE` and `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` set explicitly, same values Compose uses.