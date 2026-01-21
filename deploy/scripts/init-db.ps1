$ErrorActionPreference = "Stop"

Write-Host "Waiting for MySQL pod to be ready..."
kubectl wait --for=condition=ready pod -l app=mysql --timeout=120s

Write-Host "Running MySQL init job..."
kubectl delete job mysql-init --ignore-not-found
kubectl apply -f ..\k8s\mysql-init-job.yaml

Write-Host "Following MySQL init job logs..."
kubectl logs -f job/mysql-init

Write-Host "Database initialization finished."