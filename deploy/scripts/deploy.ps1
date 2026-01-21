kubectl apply -f ..\k8s
kubectl get pods
kubectl get svc
kubectl get ingress

# Port-forward ingress controller to localhost
kubectl -n ingress-nginx port-forward svc/ingress-nginx-controller 8081:80
