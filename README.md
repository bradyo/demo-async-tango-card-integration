# Demo Async RaaS API Integration Application

This application demonstrates a bulletproof asynchronous [Tango Card RaaS API](https://integration-www.tangocard.com/raas_api_console/v2/) 
integration for sending digital gift card rewards.

In this model, we create an order transaction in the database and queue up the order for 
processing by an asynchronous worker. 

This model is suitable for integrations with the constraints:

- Reward response data is not needed immediately. For example: the reward data is emailed
  after order is placed.
- Need to have low latency when placing orders. For example: the order is placed on a website
  or mobile app and needs a quick response time. 
  
This model is not suitable for integrations with the following constraints:

- Reward response data is needed immediately. For example: the reward data is displayed
  immediately to the user or needs to be passed to a downstream synchronous process.
  

## Running Example App

```
mvn spring-boot:run
```

### Create an Order

```bash
curl -X POST http://localhost:8080/orders \
-H 'Content-type: application/json' \
-d '{
  "amount": "100.00"
}'
```

```json
{
  "referenceNumber": "0001-8743-019357"
}
```

### Show Internal Order Data

```bash
curl http://localhost:8080/internal/orders
```

```json
[ {
  "id": 1,
  "amount" : 100.00,
  "raasExternalId" : "62f410f3-7c34-4ffd-9aec-c4227cc0b15b",
  "raasOrderRefId" : null,
  "tries" : 0,
  "status" : "new",
  "referenceNumber" : "1141-7194-044581",
  "user" : {
    "id" : 1,
    "name" : "Test User",
    "emailAddress" : "test@domain.com"
  }
}, {
  "id": 2,
  "amount" : 100.00,
  "raasExternalId" : "ed0ef356-14b0-4137-81a8-c93bfbc879fb",
  "raasOrderRefId" : "RA170604-256-11",
  "tries" : 1,
  "status" : "processed",
  "referenceNumber" : "3583-8668-868195",
  "user" : {
    "id" : 1,
    "name" : "Test User",
    "emailAddress" : "test@domain.com"
  }
}]
```

## Run Tests

### Unit/Integration Tests

```
mvn test
```

### RaaS Http Client Functional Test

```
mvn test -Dtest-groups=http-raas-client-functional-tests
```
  
## Explanation

The process is as follows:

1. Save an Order to the database (See: [OrderService placeOrder method](https://github.com/bradyo/demo-async-raas-integration/blob/b96237fe25d9860cfef338718d632bf5ecc2fb55/src/main/java/demo/async_tangocard_integration/order/OrderService.java#L32))

    - Start database transaction
    - Generate a unique public Order Reference Number to return to customers and save to Order
    - Generate a unique RaaS externalID that ensures duplicate requests do not count as separate RaaS orders
      (this might happen due to retry logic or networking issues) and save to Order
    - Commit transaction
  
2. Add the Order ID to a processing message queue

3. Return the Order Reference Number to the customer

4. Async worker reads Order IDs off the message queue for processing

5. Process the Order (See: [OrderService processOrder method](https://github.com/bradyo/demo-async-raas-integration/blob/b96237fe25d9860cfef338718d632bf5ecc2fb55/src/main/java/demo/async_tangocard_integration/order/OrderService.java#L52))

    - Start database transaction
    - Lock Order row in the database for update since we don't want Order's being processed in parallel
    - Check that Order status is still unprocessed in case another Worker has processed the Order already
      (this might happen for distributed message queues that don't guarantee exactly-once delivery).
    - Send order request to RaaS API
    - Save RaaS Order orderRefID to our Order
    - Commit transaction
    - Delete message from the queue (if an error occurs before or during delete, checking the Order status
      before processing will prevent the Order from being processed twice)


## Design

This example uses [Ports-and-Adapters architecture](http://alistair.cockburn.us/Hexagonal+architecture) to
integrate with the RaaS API. This architecture makes infrastructural components "pluggable" using "Port" interfaces
closely modeled against the application domain. 

When running the application locally during development, we don't want to be unnecessarily bound to 
the RaaS API sandbox environment. Instead, we swap in a `StubRaasClient` so we can better control the
behavior of the adapter and not be dependent on an external service during development (see [OrderControllerWebTest.java](https://github.com/bradyo/demo-async-raas-integration/blob/master/src/test/java/demo/async_tangocard_integration/order/OrderControllerWebTest.java)). The `HttpRaasClient` 
adapter, which will be used in a production configuration, can be fully tested using the RaaS API sandbox environment (see [HttpRaasClientFunctionalTest](https://github.com/bradyo/demo-async-raas-integration/blob/master/src/test/java/demo/async_tangocard_integration/raas_client/HttpRaasClientFunctionalTest.java)).

For application integration tests, we can control the application logic by plugging in different `RaasClient` 
"Adapters" with different runtime behaviors. This way we can simulate responses from the `RaasClient`
such as success, errors, or timeouts and verify how our application behaves.

Integration testing the application using `RaasClient` mock adapters combined with functional testing 
of the `HttpRaasClient` adapter against the RaaS API Sandbox will ensure things will work as expected 
when we plug in the `HttpRaasClient` adapter in production. 

