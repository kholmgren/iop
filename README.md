```bash
loadtest -T "application/json" -p make_payload.js http://localhost:8080/write -n 1000 -c 10 
```

```bash
docker run -d --hostname rabbit --name rabbit -e RABBITMQ_DEFAULT_USER=user -e RABBITMQ_DEFAULT_PASS=pass -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```


