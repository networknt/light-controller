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
java -jar target/fat.jar
```

## Test

By default, the OAuth2 JWT security verification is disabled. You can use Curl or Postman to test your service right after the code generation. For example,


```
curl -k https://localhost:8443/v1/pets
```


## Security

The OAuth JWT token verifier protects all endpoints, but it is disabled in the generated openapi-security.yml config file. If you want to turn on the security,  change the src/main/resources/config/openapi-security.yml   enableVerifyJwt to true and restart the server.


To access the server, there is a long-lived token below issued by my
oauth2 server [light-oauth2](https://github.com/networknt/light-oauth2)

```
Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA
```

Add "Authorization" header with value as above token and a dummy message will return from the generated stub. Here is an example.

```
curl -k -X GET https://localhost:8443/v1/pets \
  -H 'Authorization: Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA' 
```

### Register

```
# reference.v1 instance 1
curl -k --location --request POST 'https://localhost:8438/services' \
--header 'Content-Type: application/json' \
--data-raw '{"serviceId":"com.networknt.reference.v1","tag":"uat","address":"192.168.1.144","port":8000,"check":{"deregisterCriticalServiceAfter":"1m","http":"https://192.168.1.144:8080/health/com.networknt.reference.v1","tlsSkipVerify":true,"interval":"10s"}}'

# reference.v1 instance 2
curl -k --location --request POST 'https://localhost:8438/services' \
--header 'Content-Type: application/json' \
--data-raw '{"serviceId":"com.networknt.reference.v1","tag":"uat","address":"192.168.1.145","port":8000,"check":{"deregisterCriticalServiceAfter":"1m","http":"https://192.168.1.145:8080/health/com.networknt.reference.v1","tlsSkipVerify":true,"interval":"10s"}}'

# reference.v1 instance 3
curl -k --location --request POST 'https://localhost:8438/services' \
--header 'Content-Type: application/json' \
--data-raw '{"serviceId":"com.networknt.reference.v1","tag":"uat","address":"192.168.1.146","port":8000,"check":{"deregisterCriticalServiceAfter":"1m","http":"https://192.168.1.145:8080/health/com.networknt.reference.v1","tlsSkipVerify":true,"interval":"10s"}}'

# reference.v2 instance 1
curl -k --location --request POST 'https://localhost:8438/services' \
--header 'Content-Type: application/json' \
--data-raw '{"serviceId":"com.networknt.reference.v2","tag":"uat","address":"192.168.1.144","port":8001,"check":{"deregisterCriticalServiceAfter":"1m","http":"https://192.168.1.144:8080/health/com.networknt.reference.v1","tlsSkipVerify":true,"interval":"10s"}}'

# reference.v2 instance 2
curl -k --location --request POST 'https://localhost:8438/services' \
--header 'Content-Type: application/json' \
--data-raw '{"serviceId":"com.networknt.reference.v2","tag":"uat","address":"192.168.1.145","port":8001,"check":{"deregisterCriticalServiceAfter":"1m","http":"https://192.168.1.145:8080/health/com.networknt.reference.v1","tlsSkipVerify":true,"interval":"10s"}}'

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

