# light-controller
The runtime control pane for the light-platform

## Build and Start

For testing locally, you don't need to create the artifact for the document, source code, and the fatjar. You can build and start the server with the following command.

```
mvn clean install exec:exec
```

or

```
mvn clean package exec:exec
```

If you want to build the fatjar and other artifacts, please use the following command.

```
mvn clean install -Prelease
```

With the fatjar in the target directory, you can start the server with the following command.

```
java -jar target/controller.jar
```

## Test

By default, the OAuth2 JWT security verification is enabled. You can use Curl or Postman to test your service. For example,


```
curl -k https://localhost:8438/services
```

You should have an error message.

```
{"statusCode":401,"code":"ERR10002","message":"MISSING_AUTH_TOKEN","description":"No Authorization header or the token is not bearer type","severity":"ERROR"}
```

## Security

The OAuth JWT token verifier protects all endpoints, but it is enabled in the openapi-security.yml config file. The following command uses a long-lived test token to get all registered services.

```
curl -k -X GET https://localhost:8438/services \
  -H 'Authorization: Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTk2Mjk4NTUyOCwianRpIjoiT0Y4VGFZSDRVSjNTOFZyNnJ3REdDQSIsImlhdCI6MTY0NzYyNTUyOCwibmJmIjoxNjQ3NjI1NDA4LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlaHVAZ21haWwuY29tIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzMiLCJyb2xlcyI6InVzZXIgQ3RsUGx0QWRtaW4gQ3RsUGx0UmVhZCBDdGxQbHRXcml0ZSIsInNjb3BlIjpbInBvcnRhbC5yIiwicG9ydGFsLnciXX0.MIWNwUfdVsV7rjctaeugFYgzsbnolUeXsIrvOdj9bFrkM4UfShKOD3XnkOpRU2TNcp2pa2wla-5bSN5p-aQQ1fnpk_2QW_E7GbWHv0Rbj3Epq_yLB8DJAjoeEYn2Ux9OrssYYtMuq63kd3FflOi10wr01sZ47tZQleQPzCetsm2hZOZGZU8gSwBSlYXJs4bxTaYNlPnRNVEBZEgiprxyLbwssDZISTcFWBsOlCEzBKrLqeQdDXxRzp9HlZprXzq30rtuRrTfwGBC39x3miAyNbPV8dqokzCc8PzTpwC7irmGv3PoXJ-IiroJV_-s83ZrAUcDnjxuNFZ4x02kjjwf9g'
```

If you have any registered service, you can expect the following message.

```
{"com.networknt.petstore-3.0.1|test1":[{"protocol":"https","address":"172.17.0.1","port":9443}]}
```

### Register

```
# reference.v1 instance 1
curl -k --location --request POST 'https://localhost:8438/services' \
--header 'Content-Type: application/json' \
--data-raw '{"serviceId":"com.networknt.reference.v1","tag":"uat","address":"192.168.1.144","port":8000,"check":{"id":"check-com.networknt.reference.v1:192.168.1.144:8080", "deregisterCriticalServiceAfter":"1m","http":"https://192.168.1.144:8080/health/com.networknt.reference.v1","tlsSkipVerify":true,"interval":"10s"}}'

# reference.v1 instance 2
curl -k --location --request POST 'https://localhost:8438/services' \
--header 'Content-Type: application/json' \
--data-raw '{"serviceId":"com.networknt.reference.v1","tag":"uat","address":"192.168.1.145","port":8000,"check":{"id":"check-com.networknt.reference.v1:192.168.1.145:8080","deregisterCriticalServiceAfter":"1m","http":"https://192.168.1.145:8080/health/com.networknt.reference.v1","tlsSkipVerify":true,"interval":"10s"}}'

# reference.v1 instance 3
curl -k --location --request POST 'https://localhost:8438/services' \
--header 'Content-Type: application/json' \
--data-raw '{"serviceId":"com.networknt.reference.v1","tag":"uat","address":"192.168.1.146","port":8000,"check":{"id":"check-com.networknt.reference.v1:192.168.1.146:8080","deregisterCriticalServiceAfter":"1m","http":"https://192.168.1.145:8080/health/com.networknt.reference.v1","tlsSkipVerify":true,"interval":"10s"}}'

# reference.v2 instance 1
curl -k --location --request POST 'https://localhost:8438/services' \
--header 'Content-Type: application/json' \
--data-raw '{"serviceId":"com.networknt.reference.v2","tag":"uat","address":"192.168.1.144","port":8001,"check":{"id":"check-com.networknt.reference.v2:192.168.1.144:8080", "deregisterCriticalServiceAfter":"1m","http":"https://192.168.1.144:8080/health/com.networknt.reference.v1","tlsSkipVerify":true,"interval":"10s"}}'

# reference.v2 instance 2
curl -k --location --request POST 'https://localhost:8438/services' \
--header 'Content-Type: application/json' \
--data-raw '{"serviceId":"com.networknt.reference.v2","tag":"uat","address":"192.168.1.145","port":8001,"check":{"id":"check-com.networknt.reference.v2:192.168.1.145:8080", "deregisterCriticalServiceAfter":"1m","http":"https://192.168.1.145:8080/health/com.networknt.reference.v1","tlsSkipVerify":true,"interval":"10s"}}'

```

### Query All

```
curl -k https://localhost:8438/services
```

The result should be

```
{
  "com.networknt.reference.v2|uat": {
    "nodes": "[{\"address\":\"192.168.1.144\",\"port\":8001},{\"address\":\"192.168.1.145\",\"port\":8001}]"
  },
  "com.networknt.reference.v1|uat": {
    "nodes": "[{\"address\":\"192.168.1.144\",\"port\":8000},{\"address\":\"192.168.1.145\",\"port\":8000},{\"address\":\"192.168.1.146\",\"port\":8000}]"
  }
}
```

### Query ServiceId

```
curl -k 'https://localhost:8438/services?serviceId=com.networknt.reference.v1&tag=uat'
```

### Deregister

```
curl -k --request DELETE 'https://localhost:8438/services?serviceId=com.networknt.reference.v1&tag=uat&address=192.168.1.146&port=8000'
```

### Server Info


```
curl -k https://localhost:8438/services/info/172.18.0.1:8443
```

### Check Status

```
curl -k https://localhost:8438/services/check/com.networknt.petstore-3.0.1:172.18.0.1:8443
```

And result:

```
{"lastExecuteTimestamp":1605563379912,"lastFailedTimestamp":0,"serviceId":"com.networknt.petstore-3.0.1","address":"172.18.0.1","port":8443,"tlsSkipVerify":true,"http":"https://172.18.0.1:8443/health/com.networknt.petstore-3.0.1","interval":10000,"id":"com.networknt.petstore-3.0.1:172.18.0.1:8443","deregisterCriticalServiceAfter":120000}
```
### Get: All modules

```
curl -k https://localhost:8438/services/modules?protocol=https&address=172.18.0.1&port=9443
```

And result:

```
[
    "com.networknt.correlation.CorrelationHandler",
    "com.networknt.server.Server",
    "com.networknt.handler.Handler",
    "com.networknt.audit.AuditHandler",
    "com.networknt.traceability.TraceabilityHandler",
    ...
]
```

### Post: Reload Module Configuration

```
curl -k https://localhost:8438/services/modules
```
Request Body:
```
{
		"protocol": "https",
        "address": "172.18.0.1",
        "port": 9443,
		"modules": [	"com.networknt.audit.AuditHandler",
					    "com.networknt.traceability.TraceabilityHandler",
					    "com.networknt.service.SingletonServiceFactory"
				]
}
```

And result:

```
[
    "com.networknt.audit.AuditHandler",
    "com.networknt.traceability.TraceabilityHandler",
]
```
Response 200
```

### Post: Shutdown Service
```
curl -k https://localhost:8438/services/shutdown
```
Request Body:
```
{
		"protocol": "https",
        "address": "172.18.0.1",
        "port": 9443
}
```
And result(from light-4j):

```
[
    "tag":"dev",
    "serviceId":"com.networknt.petstore-3.0.1",
	 "time":"1605563379912"
]
```
Response 200
```
