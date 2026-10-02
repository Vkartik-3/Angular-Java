#!/bin/bash
set -e

NAMESPACE=shopflow
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/../.."

# 8192/4 is a reasonable default, not a universal one — Docker Desktop's own
# memory cap varies by machine. Override with e.g. MINIKUBE_MEMORY=6144 if
# minikube start fails to allocate.
MINIKUBE_MEMORY="${MINIKUBE_MEMORY:-8192}"
MINIKUBE_CPUS="${MINIKUBE_CPUS:-4}"

echo "==> Starting Minikube (memory=${MINIKUBE_MEMORY} cpus=${MINIKUBE_CPUS})"
minikube start --memory="$MINIKUBE_MEMORY" --cpus="$MINIKUBE_CPUS"
minikube addons enable metrics-server >/dev/null

# minikube reuses a pre-existing profile's memory/cpu silently if one already
# exists, ignoring the flags above — this bit us in development.
REQUESTED_BYTES=$((MINIKUBE_MEMORY * 1024 * 1024))
ACTUAL_MEM=$(docker inspect minikube --format '{{.HostConfig.Memory}}' 2>/dev/null || echo 0)
if [ "$ACTUAL_MEM" -gt 0 ] && [ "$ACTUAL_MEM" -lt "$REQUESTED_BYTES" ]; then
  echo "WARNING: existing minikube profile has less memory ($((ACTUAL_MEM / 1024 / 1024))Mi) than requested (${MINIKUBE_MEMORY}Mi)."
  echo "  Run 'minikube delete' first if you want MINIKUBE_MEMORY to actually apply."
fi

echo "==> Namespace and secrets"
kubectl apply -f infra/k8s/namespace.yaml
if [ ! -f infra/k8s/secrets.yaml ]; then
  cp infra/k8s/secrets.yaml.template infra/k8s/secrets.yaml
  echo "Created infra/k8s/secrets.yaml from the template — fill in your own"
  echo "credentials, then re-run this script."
  exit 1
fi
kubectl apply -f infra/k8s/secrets.yaml

echo "==> Config from source files"
apply_configmap() {
  kubectl create configmap "$1" --from-file="$2" -n "$NAMESPACE" \
    --dry-run=client -o yaml | kubectl apply -f -
}
apply_configmap mongo-init infra/docker/mongo-init.js
apply_configmap keycloak-realm infra/keycloak/shopflow-realm.json
apply_configmap prometheus-config infra/monitoring/prometheus/prometheus.yml
apply_configmap loki-config infra/monitoring/loki/loki.yml
apply_configmap alloy-config infra/monitoring/alloy/config-k8s.alloy
apply_configmap grafana-datasources infra/monitoring/grafana/provisioning/datasources
apply_configmap grafana-dashboards infra/monitoring/grafana/provisioning/dashboards

echo "==> Database"
kubectl apply -f infra/k8s/database/
kubectl wait --for=condition=ready pod -l app=postgres -n "$NAMESPACE" --timeout=180s
kubectl wait --for=condition=ready pod -l app=mongo -n "$NAMESPACE" --timeout=180s

echo "==> Messaging"
kubectl apply -f infra/k8s/messaging/
# Generous timeouts below account for a fully cold image pull (nothing cached
# yet) on top of each service's own startup time — a first-ever run needs to
# pull every image in the stack, which took kafka alone past 4 minutes in
# testing.
kubectl wait --for=condition=ready pod -l app=kafka -n "$NAMESPACE" --timeout=480s
kubectl wait --for=condition=ready pod -l app=apicurio-registry -n "$NAMESPACE" --timeout=300s

echo "==> Auth"
kubectl apply -f infra/k8s/auth/
kubectl wait --for=condition=ready pod -l app=keycloak -n "$NAMESPACE" --timeout=480s

echo "==> App tier"
kubectl apply -f infra/k8s/app/
kubectl wait --for=condition=ready pod -l app=order-service -n "$NAMESPACE" --timeout=420s
kubectl wait --for=condition=ready pod -l app=gateway -n "$NAMESPACE" --timeout=240s
kubectl wait --for=condition=ready pod -l app=frontend -n "$NAMESPACE" --timeout=180s

# Observability is genuinely optional and adds real memory pressure on a
# tight machine — don't let it hard-fail the whole script if it struggles.
if [ "${SKIP_OBSERVABILITY:-}" != "1" ]; then
  echo "==> Observability (set SKIP_OBSERVABILITY=1 to skip)"
  set +e
  kubectl apply -f infra/k8s/observability/prometheus-deployment.yaml -f infra/k8s/observability/prometheus-service.yaml
  kubectl wait --for=condition=ready pod -l app=prometheus -n "$NAMESPACE" --timeout=240s
  kubectl apply -f infra/k8s/observability/loki-deployment.yaml -f infra/k8s/observability/loki-service.yaml
  kubectl wait --for=condition=ready pod -l app=loki -n "$NAMESPACE" --timeout=240s
  kubectl apply -f infra/k8s/observability/alloy-rbac.yaml -f infra/k8s/observability/alloy-deployment.yaml -f infra/k8s/observability/alloy-service.yaml
  kubectl wait --for=condition=ready pod -l app=alloy -n "$NAMESPACE" --timeout=240s
  kubectl apply -f infra/k8s/observability/grafana-deployment.yaml -f infra/k8s/observability/grafana-service.yaml
  kubectl wait --for=condition=ready pod -l app=grafana -n "$NAMESPACE" --timeout=300s
  set -e
fi

echo "==> Port-forwarding"
pkill -f "port-forward svc/frontend" 2>/dev/null || true
pkill -f "port-forward svc/keycloak" 2>/dev/null || true
pkill -f "port-forward svc/gateway" 2>/dev/null || true
pkill -f "port-forward svc/grafana" 2>/dev/null || true
pkill -f "port-forward svc/argocd-server" 2>/dev/null || true

kubectl port-forward svc/frontend 4200:80 -n "$NAMESPACE" >/dev/null 2>&1 &
disown
kubectl port-forward svc/keycloak 8180:8080 -n "$NAMESPACE" >/dev/null 2>&1 &
disown
kubectl port-forward svc/gateway 8090:8090 -n "$NAMESPACE" >/dev/null 2>&1 &
disown
if [ "${SKIP_OBSERVABILITY:-}" != "1" ]; then
  kubectl port-forward svc/grafana 3000:3000 -n "$NAMESPACE" >/dev/null 2>&1 &
  disown
fi
if kubectl get svc argocd-server -n argocd &>/dev/null; then
  kubectl port-forward svc/argocd-server -n argocd 8080:443 >/dev/null 2>&1 &
  disown
fi
sleep 3

echo
echo "ShopFlow is running:"
echo "  App:      http://localhost:4200"
echo "  Gateway:  http://localhost:8090/api"
echo "  Keycloak: http://localhost:8180"
if [ "${SKIP_OBSERVABILITY:-}" != "1" ]; then
  echo "  Grafana:  http://localhost:3000"
fi
if kubectl get svc argocd-server -n argocd &>/dev/null; then
  echo "  ArgoCD:   https://localhost:8080"
fi
echo
echo "Port-forwards die if their pod restarts — re-run this script (it's"
echo "idempotent) or manually re-run the kubectl port-forward commands above."
