## Run

```bash
./gradlew run
```

## API

Server implements Server-sent events to provide information during an operation progress.
Requests data is posted as form-urlencoded.

### Withdrawal

```bash
curl -v -o - -d "donator=0&addr=addr0&amount=2" http://localhost:10080/withdraw/    
*   Trying 127.0.0.1:10080...
* Connected to localhost (127.0.0.1) port 10080 (#0)
> POST /withdraw/ HTTP/1.1
> Host: localhost:10080
> User-Agent: curl/7.81.0
> Accept: */*
> Content-Length: 29
> Content-Type: application/x-www-form-urlencoded
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 303 See Other
< Date: Tue, 17 Oct 2023 16:09:03 GMT
< Transfer-encoding: chunked
< Content-type: text/event-stream
< Location: /result/cdad1c8e-d00c-41de-9976-c1f510f6d1c4
< Cache-control: no-store
< 
[User[id=0]] trying hold 2.00000
[User[id=0]] 2.00000 held: 91.0000 available
[Address[value=addr0]] User[id=0] trying withdraw 2.00000
withdrew 2.00000 from User[id=0] to Address[value=addr0]

```

Server responses with redirect with location of the operation result. It might be used only once.

```bash
curl -v -o - http://localhost:10080/result/cdad1c8e-d00c-41de-9976-c1f510f6d1c4       
*   Trying 127.0.0.1:10080...
* Connected to localhost (127.0.0.1) port 10080 (#0)
> GET /result/cdad1c8e-d00c-41de-9976-c1f510f6d1c4 HTTP/1.1
> Host: localhost:10080
> User-Agent: curl/7.81.0
> Accept: */*
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 200 OK
< Date: Tue, 17 Oct 2023 16:12:14 GMT
< Content-length: 0
< 
```

In case of operation failure the response will be 502.

### Deposit

```bash
curl -v -o - -d "donator=0&recipient=1&amount=5" -X POST http://localhost:10080/tx/
Note: Unnecessary use of -X or --request, POST is already inferred.
*   Trying 127.0.0.1:10080...
* Connected to localhost (127.0.0.1) port 10080 (#0)
> POST /tx/ HTTP/1.1
> Host: localhost:10080
> User-Agent: curl/7.81.0
> Accept: */*
> Content-Length: 30
> Content-Type: application/x-www-form-urlencoded
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 303 See Other
< Date: Tue, 17 Oct 2023 16:14:12 GMT
< Transfer-encoding: chunked
< Content-type: text/event-stream
< Location: /result/b7dded08-3698-4d9c-a3d3-5039e87683e9
< Cache-control: no-store
< 
[User[id=0]] trying hold 5.00000
[User[id=0]] 5.00000 held: 86.0000 available
[User[id=1]] balance increased by 5.00000: total 110.000
```
